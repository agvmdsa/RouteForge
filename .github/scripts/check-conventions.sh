#!/bin/bash
# Warns when a pull request's branch name or commit subjects do not follow the project's conventions. It never fails.
#
#   usage:  check-conventions.sh <base-sha> <head-sha> <branch>      (run inside the repository)
#   branch: NNN-short-name            three digits, a hyphen, then lowercase words joined by single hyphens
#   commit: RF-NNN: summary           the subject (first line) of every non-merge commit in <base-sha>..<head-sha>
#   stdout: one "::warning title=...::..." line per problem (GitHub shows these on the pull request)
#           and, unless $GITHUB_STEP_SUMMARY is set, a short markdown summary; when it is set the summary is appended to that file
#   exit:   always 0, including on wrong usage or an unreadable range (it then says it could not check)
#
# Compatible with bash 3.2.

set -u

BRANCH_PATTERN='^[0-9]{3}-[a-z0-9]+(-[a-z0-9]+)*$'
SUBJECT_PATTERN='^RF-[0-9]+: .+'

# Annotation messages must not contain raw %, CR or LF.
escape() {
  printf '%s' "$1" | tr -d '\r' | sed -e 's/%/%25/g'
}

warn() {
  # warn <title> <message>
  printf '::warning title=%s::%s\n' "$1" "$(escape "$2")"
}

if [ "$#" -ne 3 ]; then
  warn "Conventions not checked" "check-conventions.sh needs <base-sha> <head-sha> <branch>; the branch and commit names were not checked"
  exit 0
fi
base=$1
head=$2
branch=$3

summary=""
add_summary() {
  summary="$summary$1"$'\n'
}

add_summary "### Conventions (warnings only)"
add_summary ""

if [[ $branch =~ $BRANCH_PATTERN ]]; then
  add_summary "- Branch \`$branch\`: matches \`NNN-short-name\`"
else
  warn "Branch name" "Branch '$branch' does not match NNN-short-name (three digits, a hyphen, then lowercase words joined by hyphens), for example 018-ci-cd-pipeline"
  add_summary "- Branch \`$branch\`: does **not** match \`NNN-short-name\`"
fi

if ! commits=$(git rev-list --no-merges --reverse "$base..$head" 2>/dev/null); then
  warn "Conventions not checked" "Could not read the commits between $base and $head; the commit subjects were not checked"
  add_summary "- Commits: could not be read, so they were not checked"
else
  total=0
  flagged=0
  for sha in $commits; do
    total=$((total + 1))
    subject=$(git log -1 --format=%s "$sha")
    if ! [[ $subject =~ $SUBJECT_PATTERN ]]; then
      flagged=$((flagged + 1))
      short=$(git rev-parse --short "$sha")
      warn "Commit subject" "$short \"$subject\" does not match RF-NNN: summary (for example RF-120: Add the CI workflow)"
      add_summary "- Commit \`$short\` \"$subject\": does **not** match \`RF-NNN: summary\`"
    fi
  done
  add_summary "- Commits checked: $total, with a warning: $flagged"
fi

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  printf '%s' "$summary" >> "$GITHUB_STEP_SUMMARY"
else
  printf '%s' "$summary"
fi
exit 0
