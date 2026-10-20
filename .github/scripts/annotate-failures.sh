#!/bin/bash
# Turns the failure reports of a Gradle run into GitHub annotations, so that a failed check names its first problems on the
# pull request itself without opening the logs or downloading the reports.
#
#   usage:  annotate-failures.sh [workspace]       (default: the current directory)
#   reads:  <workspace>/**/build/reports/ktlint/**/*.txt         "path:line:col: message (rule)"
#           <workspace>/**/build/test-results/**/TEST-*.xml       <testcase name classname> with a <failure message=...>
#   stdout: "::error file=...,line=...,title=ktlint::..." and "::error title=Test failed::Class.name: message" lines,
#           at most 10 of each kind, plus a "::notice" saying how many there were when there are more
#   exit:   always 0
#
# Compatible with bash 3.2.

set -u

root=${1:-.}
root=${root%/}
LIMIT=10

escape() {
  printf '%s' "$1" | tr -d '\r' | sed -e 's/%/%25/g'
}

unxml() {
  printf '%s' "$1" | sed -e 's/&lt;/</g' -e 's/&gt;/>/g' -e 's/&quot;/"/g' -e "s/&apos;/'/g" -e 's/&#10;/ /g' -e 's/&#13;//g' -e 's/&amp;/\&/g'
}

VIOLATION='^(.+\.kts?):([0-9]+):([0-9]+): (.*)$'

ktlint_reports() {
  find "$root" -path '*/build/reports/ktlint/*' -name '*.txt' 2>/dev/null | sort
}

ktlint_total=0
while IFS= read -r report; do
  [ -z "$report" ] && continue
  while IFS= read -r line; do
    if [[ $line =~ $VIOLATION ]]; then
      ktlint_total=$((ktlint_total + 1))
      if [ "$ktlint_total" -le "$LIMIT" ]; then
        file=${BASH_REMATCH[1]}
        file=${file#"$root"/}
        printf '::error file=%s,line=%s,col=%s,title=ktlint::%s\n' "$file" "${BASH_REMATCH[2]}" "${BASH_REMATCH[3]}" "$(escape "${BASH_REMATCH[4]}")"
      fi
    fi
  done < "$report"
done < <(ktlint_reports)
# A report in a format this script does not know must not make the failure invisible: show its first lines as they are.
if [ "$ktlint_total" -eq 0 ]; then
  shown=0
  while IFS= read -r report; do
    [ -z "$report" ] && continue
    while IFS= read -r line; do
      [ -z "$line" ] && continue
      shown=$((shown + 1))
      if [ "$shown" -le "$LIMIT" ]; then
        printf '::notice title=ktlint report::%s: %s\n' "${report#"$root"/}" "$(escape "$line")"
      fi
    done < "$report"
  done < <(ktlint_reports)
fi
if [ "$ktlint_total" -gt "$LIMIT" ]; then
  printf '::notice title=ktlint::%s violations in total; the first %s are shown (the full report is in the failure-reports artifact)\n' "$ktlint_total" "$LIMIT"
fi

test_total=0
while IFS= read -r xml; do
  [ -z "$xml" ] && continue
  current=""
  while IFS= read -r line; do
    case "$line" in
      *"<testcase "*)
        name=$(printf '%s' "$line" | sed -n 's/.*<testcase name="\([^"]*\)".*/\1/p')
        class=$(printf '%s' "$line" | sed -n 's/.*classname="\([^"]*\)".*/\1/p')
        current="$(unxml "$class").$(unxml "$name")"
        ;;
    esac
    case "$line" in
      *"<failure "*)
        test_total=$((test_total + 1))
        if [ "$test_total" -le "$LIMIT" ]; then
          message=$(printf '%s' "$line" | sed -n 's/.*<failure message="\([^"]*\)".*/\1/p')
          printf '::error title=Test failed::%s\n' "$(escape "$current: $(unxml "$message")")"
        fi
        ;;
    esac
  done < "$xml"
done < <(find "$root" -path '*/build/test-results/*' -name 'TEST-*.xml' 2>/dev/null | sort)
if [ "$test_total" -gt "$LIMIT" ]; then
  printf '::notice title=Test failed::%s failed tests in total; the first %s are shown\n' "$test_total" "$LIMIT"
fi

exit 0
