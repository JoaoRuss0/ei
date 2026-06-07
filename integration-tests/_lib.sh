#!/bin/bash
# Shared helpers for integration tests.
# Source from each test script: `source ./_lib.sh`

# Start one Kafka consumer per partition of $topic, each pinned to its
# current high-water mark. After calling, produce your message, then call
# `await_captured_message`.
#
# Why per-partition: smallrye-kafka emits without a key, so messages land on
# a random partition. Pinning to --partition + --offset bypasses the consumer
# group rebalance (~2s) so there's no startup race.
capture_next_message() {
    local topic="$1"
    local timeout="${2:-15000}"

    CAPTURE_OUT=$(mktemp)
    CAPTURE_ERR=$(mktemp)
    CAPTURE_PIDS=()

    local offsets
    offsets=$(../kafka-binary/bin/kafka-get-offsets.sh \
        --bootstrap-server "$KAFKA_CLUSTER" \
        --topic "$topic" --time -1 2>/dev/null)

    if [ -z "$offsets" ]; then
        >&2 echo "[WARN] capture_next_message: no partitions found for topic '$topic'"
        return
    fi

    local line p o
    while IFS= read -r line; do
        p=$(echo "$line" | awk -F: '{print $2}')
        o=$(echo "$line" | awk -F: '{print $3}')
        [ -z "$p" ] && continue

        ../kafka-binary/bin/kafka-console-consumer.sh \
            --bootstrap-server "$KAFKA_CLUSTER" \
            --topic "$topic" \
            --partition "$p" \
            --offset "$o" \
            --max-messages 1 \
            --timeout-ms "$timeout" \
            >> "$CAPTURE_OUT" 2>> "$CAPTURE_ERR" &
        CAPTURE_PIDS+=($!)
    done <<< "$offsets"
}

# Wait for any of the background consumers to receive a message. If one fires,
# kill the rest and return the captured payload. If none receive a message
# before timeout, surface the consumers' stderr to help diagnose.
await_captured_message() {
    local deadline=$(($(date +%s) + 30))

    while [ "$(date +%s)" -lt "$deadline" ]; do
        if [ -s "$CAPTURE_OUT" ]; then
            for pid in "${CAPTURE_PIDS[@]}"; do
                kill "$pid" 2>/dev/null || true
            done
            break
        fi
        local alive=0
        for pid in "${CAPTURE_PIDS[@]}"; do
            if kill -0 "$pid" 2>/dev/null; then alive=1; break; fi
        done
        [ "$alive" -eq 0 ] && break
        sleep 0.2
    done

    for pid in "${CAPTURE_PIDS[@]}"; do
        wait "$pid" 2>/dev/null || true
    done

    cat "$CAPTURE_OUT"

    if [ ! -s "$CAPTURE_OUT" ] && [ -s "$CAPTURE_ERR" ]; then
        >&2 echo "[DEBUG] consumer stderr:"
        >&2 cat "$CAPTURE_ERR"
    fi

    rm -f "$CAPTURE_OUT" "$CAPTURE_ERR"
}
