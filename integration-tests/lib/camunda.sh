#!/bin/bash
# Helper functions for Camunda 8 c8run E2E tests.
# Source me after addresses.sh so $addressCamunda / $CAMUNDA_URL is defined.

CAMUNDA_REST="http://${addressCamunda:-${CAMUNDA_URL%/operate}}:8080"
CAMUNDA_REST="${CAMUNDA_REST/http:\/\/http:\/\//http://}"

# Resolve Camunda base URL from addresses.sh exports (CAMUNDA_URL ends with :8080).
if [ -n "$CAMUNDA_URL" ]; then
    CAMUNDA_REST="http://${CAMUNDA_URL#http://}"
fi

camunda_start_process() {
    # camunda_start_process <processDefinitionId> <variables-json>
    # Returns the response body (containing processInstanceKey).
    local pdId="$1"
    local variables="${2:-{}}"

    curl -s -L -X POST "$CAMUNDA_REST/v2/process-instances" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "{\"processDefinitionId\": \"$pdId\", \"variables\": $variables, \"awaitCompletion\": false}"
}

camunda_start_process_await() {
    # Same as above but awaits completion (blocks up to 30s).
    local pdId="$1"
    local variables="${2:-{}}"

    curl -s -L -X POST "$CAMUNDA_REST/v2/process-instances" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "{\"processDefinitionId\": \"$pdId\", \"variables\": $variables, \"awaitCompletion\": true, \"requestTimeout\": 30000}"
}

camunda_complete_user_task() {
    # camunda_complete_user_task <userTaskKey> <variables-json>
    local taskKey="$1"
    local variables="${2:-{}}"

    curl -s -L -X POST "$CAMUNDA_REST/v2/user-tasks/$taskKey/completion" \
        -H "Content-Type: application/json" \
        -d "{\"variables\": $variables}"
}

camunda_search_user_tasks() {
    # camunda_search_user_tasks <processInstanceKey>
    local piKey="$1"
    curl -s -L -X POST "$CAMUNDA_REST/v2/user-tasks/search" \
        -H "Content-Type: application/json" \
        -d "{\"filter\": {\"processInstanceKey\": \"$piKey\"}}"
}

camunda_search_descendant_pis() {
    # Returns all processInstanceKeys whose root is <rootPiKey>, including the root itself.
    local rootPi="$1"
    local descendants
    descendants=$(curl -s -L -X POST "$CAMUNDA_REST/v2/process-instances/search" \
        -H "Content-Type: application/json" \
        -d "{\"filter\": {\"parentProcessInstanceKey\": \"$rootPi\"}}" \
        | python3 -c "import sys,json; d=json.load(sys.stdin); print(' '.join(i['processInstanceKey'] for i in d.get('items',[])))" 2>/dev/null)
    echo "$rootPi $descendants"
}

camunda_complete_all_pending_in_tree() {
    # Polls for pending user tasks across the entire process tree rooted at <rootPi>
    # and completes each with <variables-json>. Returns once no task pending and
    # the process tree is stable (no active descendants for 2 consecutive checks).
    local rootPi="$1"
    local variables="${2:-{}}"
    local completed=""
    local stable_idle=0
    for i in $(seq 1 90); do
        # 1. Find ALL descendant PIs (root + every child)
        local pi_keys
        pi_keys=$(camunda_search_descendant_pis "$rootPi")
        # 2. For each PI in tree, try to complete one CREATED user task
        local found=0
        for pi in $pi_keys; do
            local task_json
            task_json=$(curl -s -L -X POST "$CAMUNDA_REST/v2/user-tasks/search" \
                -H "Content-Type: application/json" \
                -d "{\"filter\": {\"processInstanceKey\": \"$pi\", \"state\":\"CREATED\"}}")
            local task_key
            task_key=$(echo "$task_json" | python3 -c "import sys,json; d=json.load(sys.stdin); items=d.get('items',[]); print(items[0]['userTaskKey'] if items else '')" 2>/dev/null)
            if [ -n "$task_key" ] && ! echo "$completed" | grep -q "$task_key"; then
                echo "  Completing user task $task_key (PI $pi)"
                curl -s -L -X POST "$CAMUNDA_REST/v2/user-tasks/$task_key/completion" \
                    -H "Content-Type: application/json" \
                    -d "{\"variables\": $variables}" > /dev/null
                completed="$completed $task_key"
                found=1
            fi
        done
        # 3. If we found nothing this round, sleep + count active descendants
        if [ "$found" = "0" ]; then
            sleep 2
            local active_descendants
            active_descendants=$(curl -s -L -X POST "$CAMUNDA_REST/v2/process-instances/search" \
                -H "Content-Type: application/json" \
                -d "{\"filter\": {\"state\":\"ACTIVE\"},\"page\":{\"limit\":1000}}" \
                | python3 -c "
import sys,json
d=json.load(sys.stdin)
root = '$rootPi'
def descendants(items, root):
    keep = set([root])
    changed = True
    while changed:
        changed = False
        for i in items:
            p = i.get('parentProcessInstanceKey')
            if p and p in keep and i['processInstanceKey'] not in keep:
                keep.add(i['processInstanceKey']); changed=True
    return [i for i in items if i['processInstanceKey'] in keep]
print(len(descendants(d.get('items', []), root)))" 2>/dev/null)
            active_descendants="${active_descendants:-0}"
            if [ "$active_descendants" = "0" ] && [ -n "$completed" ]; then
                stable_idle=$((stable_idle + 1))
                if [ "$stable_idle" -ge 2 ]; then
                    echo "  Process tree completed."
                    return 0
                fi
            else
                stable_idle=0
            fi
        else
            stable_idle=0
        fi
    done
    echo "  Timed out waiting for tasks (completed: $completed)."
    return 1
}

assert_kafka_message_on_topic() {
    # assert_kafka_message_on_topic <topic> <timeout-ms>
    local topic="$1"
    local timeout="${2:-20000}"
    ../kafka-binary/bin/kafka-console-consumer.sh \
        --bootstrap-server "$KAFKA_CLUSTER" \
        --topic "$topic" \
        --from-beginning \
        --timeout-ms "$timeout" \
        --max-messages 1 2>&1
}
