#!/usr/bin/env bash
set -euo pipefail
# Optional standalone JVM tests; pass jars you have already downloaded.
# KOTLINC=/path/kotlinc JUNIT_JAR=/path/junit.jar HAMCREST_JAR=/path/hamcrest.jar JSON_JAR=/path/json.jar bash scripts/check-policy.sh
ROOT="$(realpath "$(dirname "$0")/..")"
: "${KOTLINC:=kotlinc}"
: "${JUNIT_JAR:?Set JUNIT_JAR}"
: "${HAMCREST_JAR:?Set HAMCREST_JAR}"
: "${JSON_JAR:?Set JSON_JAR (org.json:json:20240303)}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$KOTLINC" "$ROOT/app/src/main/java/com/sakura/agent/AgentPolicy.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/NetworkPolicy.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/ModelCatalog.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/AgentPlanner.kt" \
    "$ROOT/app/src/test/java/com/sakura/agent/"*Test.kt \
    -cp "$JUNIT_JAR:$JSON_JAR" -include-runtime -d "$OUT/tests.jar"
java -cp "$OUT/tests.jar:$JUNIT_JAR:$HAMCREST_JAR:$JSON_JAR" org.junit.runner.JUnitCore \
    com.sakura.agent.AgentPolicyTest com.sakura.agent.NetworkPolicyTest com.sakura.agent.AgentPlannerTest