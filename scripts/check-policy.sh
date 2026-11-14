#!/usr/bin/env bash
set -euo pipefail
# Optional standalone JVM tests; pass jars you have already downloaded.
# KOTLINC=/path/kotlinc JUNIT_JAR=/path/junit.jar HAMCREST_JAR=/path/hamcrest.jar JSON_JAR=/path/json.jar OKHTTP_JAR=/path/okhttp.jar OKIO_JAR=/path/okio.jar bash scripts/check-policy.sh
ROOT="$(realpath "$(dirname "$0")/..")"
: "${KOTLINC:=kotlinc}"
: "${JUNIT_JAR:?Set JUNIT_JAR}"
: "${HAMCREST_JAR:?Set HAMCREST_JAR}"
: "${JSON_JAR:?Set JSON_JAR (org.json:json:20240303)}"
: "${OKHTTP_JAR:?Set OKHTTP_JAR (okhttp 4.12.0)}"
: "${OKIO_JAR:?Set OKIO_JAR (okio-jvm 3.6.0)}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$KOTLINC" "$ROOT/app/src/main/java/com/sakura/agent/AgentPolicy.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/NetworkPolicy.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/ModelCatalog.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/AgentPlanner.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/Skills.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/TextProtocol.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/ToolCallParser.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/AgentTools.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/MediaClient.kt" \
    "$ROOT/app/src/main/java/com/sakura/agent/WebClient.kt" \
    "$ROOT/app/src/test/java/com/sakura/agent/"*Test.kt \
    -cp "$JUNIT_JAR:$JSON_JAR:$OKHTTP_JAR:$OKIO_JAR" -include-runtime -d "$OUT/tests.jar"
java -cp "$OUT/tests.jar:$JUNIT_JAR:$HAMCREST_JAR:$JSON_JAR" org.junit.runner.JUnitCore \
    com.sakura.agent.AgentPolicyTest com.sakura.agent.NetworkPolicyTest com.sakura.agent.AgentPlannerTest \
    com.sakura.agent.SkillsTest com.sakura.agent.AgentV05Test