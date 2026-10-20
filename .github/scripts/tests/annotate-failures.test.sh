#!/bin/bash
# Tests for ../annotate-failures.sh: turns ktlint and unit-test failure reports into GitHub annotations. Written before the script.
. "$(dirname "$0")/lib.sh"

SCRIPT="$(cd "$(dirname "$0")/.." && pwd)/annotate-failures.sh"

# workspace -> a throwaway directory standing in for $GITHUB_WORKSPACE, sets WS
workspace() {
  WS=$(mktemp -d "${TMPDIR:-/tmp}/lib-ws.XXXXXX")
  LIB_TMP_DIRS="$LIB_TMP_DIRS $WS"
}

ktlint_report() {
  # ktlint_report <module> <file content>
  mkdir -p "$WS/$1/build/reports/ktlint/ktlintMainSourceSetCheck"
  printf '%s\n' "$2" > "$WS/$1/build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.txt"
}

junit_report() {
  # junit_report <module> <xml content>
  mkdir -p "$WS/$1/build/test-results/testDebugUnitTest"
  printf '%s\n' "$2" > "$WS/$1/build/test-results/testDebugUnitTest/TEST-com.example.SomeTest.xml"
}

annotate() {
  run_cmd bash "$SCRIPT" "$WS"
}

count() {
  # count <pattern>  -> number of lines of OUT starting with the pattern
  printf '%s\n' "$OUT" | grep -c "^$1" || true
}

test_nothing_to_report_prints_nothing() {
  workspace
  annotate
  assert_status 0
  assert_empty "$OUT"
}

test_a_ktlint_violation_becomes_an_error_on_its_file_and_line() {
  workspace
  ktlint_report core/domain "$WS/core/domain/src/main/kotlin/A.kt:12:5: Unnecessary blank line (standard:no-blank-line-before-rbrace)
$WS/core/domain/src/main/kotlin/B.kts:3:1: Unexpected indentation (4) (should be 8) (standard:indent)"
  annotate
  assert_status 0
  assert_eq 2 "$(count '::error')"
  assert_contains "$OUT" "::error file=core/domain/src/main/kotlin/A.kt,line=12,col=5,title=ktlint::Unnecessary blank line (standard:no-blank-line-before-rbrace)"
  assert_contains "$OUT" "::error file=core/domain/src/main/kotlin/B.kts,line=3,col=1,title=ktlint::Unexpected indentation (4) (should be 8) (standard:indent)"
}

test_lines_that_are_not_violations_are_ignored() {
  workspace
  ktlint_report app "not a violation
$WS/app/src/main/kotlin/A.kt:1:1: Missing newline (standard:final-newline)

just text: with colons"
  annotate
  assert_eq 1 "$(count '::error')"
}

test_only_the_first_ten_violations_are_shown_and_the_rest_is_counted() {
  workspace
  local lines="" i
  for i in 1 2 3 4 5 6 7 8 9 10 11 12 13; do
    lines="$lines$WS/app/src/main/kotlin/A.kt:$i:1: Problem $i (standard:rule)
"
  done
  ktlint_report app "$lines"
  annotate
  assert_eq 10 "$(count '::error')"
  assert_contains "$OUT" "13 violations"
  assert_contains "$OUT" "first 10"
}

test_a_report_in_an_unknown_format_is_shown_as_it_is_instead_of_nothing() {
  workspace
  ktlint_report core/domain "Lint error > Something unexpected happened
second line of the report"
  annotate
  assert_status 0
  assert_eq 0 "$(count '::error')"
  assert_contains "$OUT" "::notice title=ktlint report::core/domain/build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.txt: Lint error > Something unexpected happened"
  assert_contains "$OUT" "second line of the report"
}

test_an_empty_ktlint_report_prints_nothing() {
  workspace
  ktlint_report app ""
  annotate
  assert_empty "$OUT"
}

test_a_failed_test_becomes_an_error_with_its_class_and_name() {
  workspace
  junit_report feature/simulation/presentation '<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.SomeTest" tests="2" failures="1">
  <testcase name="works fine()" classname="com.example.SomeTest" time="0.001"/>
  <testcase name="auto-pause on go-to-downloads()" classname="com.example.SomeTest" time="0.002">
    <failure message="org.opentest4j.AssertionFailedError: expected: &lt;true&gt; but was: &lt;false&gt;" type="org.opentest4j.AssertionFailedError">
stack trace line
    </failure>
  </testcase>
</testsuite>'
  annotate
  assert_status 0
  assert_eq 1 "$(count '::error')"
  assert_contains "$OUT" "title=Test failed::com.example.SomeTest.auto-pause on go-to-downloads(): org.opentest4j.AssertionFailedError: expected: <true> but was: <false>"
}

test_percent_signs_are_escaped_for_the_annotation() {
  workspace
  ktlint_report app "$WS/app/src/main/kotlin/A.kt:1:1: 100% wrong (standard:rule)"
  annotate
  assert_contains "$OUT" "100%25 wrong"
}

test_ktlint_and_test_failures_are_both_reported() {
  workspace
  ktlint_report app "$WS/app/src/main/kotlin/A.kt:1:1: Problem (standard:rule)"
  junit_report core/domain '<testsuite><testcase name="t()" classname="C"><failure message="boom" type="E">x</failure></testcase></testsuite>'
  annotate
  assert_eq 2 "$(count '::error')"
}

run_tests
