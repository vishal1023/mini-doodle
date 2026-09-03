#!/usr/bin/env bash
# Seeds a small demo dataset via the API, extending the walkthrough in the README:
# 4 users, several slots each, two booked meetings, one cancelled meeting.
# Requires: app running (docker compose up --build), curl, jq.
set -euo pipefail

BASE="${BASE:-http://localhost:8080/api/v1}"
DAY="2026-09-01"
NEXT_DAY="2026-09-02"
RUN_ID=$RANDOM

create_user() {
  curl -s -X POST "$BASE/users" -H "Content-Type: application/json" \
    -d "{\"name\":\"$1\",\"email\":\"$2\"}" | jq -r .id
}

create_slot() {
  local user_id=$1 start=$2 duration=$3
  curl -s -X POST "$BASE/users/$user_id/slots" -H "Content-Type: application/json" \
    -d "{\"startTime\":\"$start\",\"durationMinutes\":$duration}" | jq -r .id
}

book_slot() {
  local user_id=$1 slot_id=$2 title=$3 description=$4 participants_json=$5
  curl -s -X POST "$BASE/users/$user_id/slots/$slot_id/meetings" \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"$title\",\"description\":\"$description\",\"participantUserIds\":$participants_json}" \
    | jq -r .id
}

echo "Creating users (run id $RUN_ID)..."
ADA=$(create_user "Ada Lovelace" "ada.${RUN_ID}@example.com")
CHARLES=$(create_user "Charles Babbage" "charles.${RUN_ID}@example.com")
GRACE=$(create_user "Grace Hopper" "grace.${RUN_ID}@example.com")
ALAN=$(create_user "Alan Turing" "alan.${RUN_ID}@example.com")
echo "  Ada=$ADA Charles=$CHARLES Grace=$GRACE Alan=$ALAN"

echo "Creating Ada's slots..."
ADA_SLOT_1=$(create_slot "$ADA" "${DAY}T09:00:00Z" 60)   # will be booked
ADA_SLOT_2=$(create_slot "$ADA" "${DAY}T10:00:00Z" 60)   # left free
ADA_SLOT_3=$(create_slot "$ADA" "${DAY}T14:00:00Z" 90)   # will be booked, then cancelled

echo "Creating Grace's slot..."
GRACE_SLOT_1=$(create_slot "$GRACE" "${DAY}T11:00:00Z" 30)   # will be booked

echo "Booking meetings..."
MEETING_1=$(book_slot "$ADA" "$ADA_SLOT_1" "Design sync" "Weekly design sync" "[\"$CHARLES\"]")
MEETING_2=$(book_slot "$ADA" "$ADA_SLOT_3" "Roadmap review" "Q3 roadmap review" "[\"$CHARLES\",\"$ALAN\"]")
MEETING_3=$(book_slot "$GRACE" "$GRACE_SLOT_1" "1:1" "Grace/Ada 1:1" "[\"$ADA\"]")
echo "  Meeting1=$MEETING_1 Meeting2=$MEETING_2 Meeting3=$MEETING_3"

echo "Cancelling Meeting2 to demonstrate slot reverting to FREE..."
curl -s -X DELETE "$BASE/meetings/$MEETING_2" > /dev/null

echo
echo "=== Ada's availability for ${DAY} ==="
curl -s "$BASE/users/$ADA/availability?from=${DAY}T00:00:00Z&to=${NEXT_DAY}T00:00:00Z" | jq .

echo
echo "=== Charles's meetings as participant ==="
curl -s "$BASE/users/$CHARLES/meetings?role=PARTICIPANT" | jq .

echo
echo "=== Ada's meetings as participant (booked by Grace) ==="
curl -s "$BASE/users/$ADA/meetings?role=PARTICIPANT" | jq .

cat <<EOF

Seeded IDs (export for further manual poking):
  export ADA=$ADA CHARLES=$CHARLES GRACE=$GRACE ALAN=$ALAN
  export ADA_SLOT_1=$ADA_SLOT_1 ADA_SLOT_2=$ADA_SLOT_2 ADA_SLOT_3=$ADA_SLOT_3 GRACE_SLOT_1=$GRACE_SLOT_1
  export MEETING_1=$MEETING_1 MEETING_2=$MEETING_2 MEETING_3=$MEETING_3
EOF
