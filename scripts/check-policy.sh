#!/usr/bin/env bash
set -euo pipefail
# Optional standalone JVM tests; pass jars you have already downloaded.
# KOTLINC=/path/kotlinc JUNIT_JAR=/path/junit-4.13.2.jar HAMCREST_JAR=/path/hamcrest-core-1.3.jar bash scripts/check-policy.sh
ROOT="$(realpath "$(dirname "$0")/..")"
: "${KOTLINC:=kotlinc}"
: "${JUNIT_JAR:?Set JUNIT_JAR}"
: "${HAMCREST_JAR:?Set HAMCREST_JAR}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$KOTLINC" "$ROOT/app/src/main/java/com/sakura/agent/AgentPolicy.kt" \
    "$ROOT/app/src/test/java/com/sakura/agent/AgentPolicyTest.kt" \
    -cp "$JUNIT_JAR" -include-runtime -d "$OUT/tests.jar"
java -cp "$OUT/tests.jar:$JUNIT_JAR:$HAMCREST_JAR" org.junit.runner.JUnitCore com.sakura.agent.AgentPolicyTest