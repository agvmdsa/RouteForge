#!/bin/bash
# Tests for ../check-conventions.sh (rows of specs/018-ci-cd-pipeline/contracts/scripts.md). Written before the script.
. "$(dirname "$0")/lib.sh"

SCRIPT="$(cd "$(dirname "$0")/.." && pwd)/check-conventions.sh"

# cc <base> <head> <branch>  -> runs the script inside $REPO; the summary goes to standard output (no GITHUB_STEP_SUMMARY)
cc() {
  run_cmd bash -c 'cd "$1" && shift && unset GITHUB_STEP_SUMMARY && bash "$@"' _ "$REPO" "$SCRIPT" "$@"
}

warnings() {
  # the warning lines of the last run
  printf '%s\n' "$OUT" | grep '^::warning' || true
}

count_warnings() {
  warnings | grep -c '^::warning' || true
}

# A repository with the start point recorded and one commit added per given subject; leaves BASE and HEAD_SHA set.
repo_with_commits() {
  make_repo
  BASE=$(head_sha)
  local subject
  for subject in "$@"; do
    add_commit "$subject"
  done
  HEAD_SHA=$(head_sha)
}

assert_no_warning() {
  assert_status 0
  assert_eq 0 "$(count_warnings)" "no warning expected, got: $(warnings)"
}

# ---- branch names

test_a_conforming_branch_has_no_warning() {
  repo_with_commits "RF-120: Add the CI workflow"
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  assert_no_warning
}

test_the_smallest_conforming_branch_has_no_warning() {
  repo_with_commits "RF-120: Add the CI workflow"
  cc "$BASE" "$HEAD_SHA" 001-a
  assert_no_warning
}

test_a_branch_without_the_number_warns() {
  repo_with_commits "RF-120: Add the CI workflow"
  cc "$BASE" "$HEAD_SHA" readme-open-and-share
  assert_status 0
  assert_eq 1 "$(count_warnings)"
  assert_contains "$(warnings)" "readme-open-and-share"
  assert_contains "$(warnings)" "NNN-short-name"
}

test_malformed_branches_each_warn() {
  repo_with_commits "RF-120: Add the CI workflow"
  local branch
  for branch in 18-x 0181-x 018- 018-Ci 018_ci 018-ci--x 018-ci- main Fix_Thing ""; do
    cc "$BASE" "$HEAD_SHA" "$branch"
    assert_status 0 "exit status for branch [$branch]"
    assert_eq 1 "$(count_warnings)" "one warning for branch [$branch]"
    assert_contains "$(warnings)" "NNN-short-name"
  done
}

# ---- commit subjects

test_a_conforming_commit_has_no_warning() {
  repo_with_commits "RF-120: Add the CI workflow"
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  assert_no_warning
}

test_numbers_of_any_length_and_any_summary_are_fine() {
  repo_with_commits "RF-1: x" "RF-119: introduce LatLng, replacing scattered Pair<Double, Double>" "RF-0058: with leading zeros"
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  assert_no_warning
}

test_a_commit_without_the_prefix_warns_with_hash_and_subject() {
  repo_with_commits "Add the CI workflow"
  local short
  short=$(git -C "$REPO" rev-parse --short HEAD)
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  assert_status 0
  assert_eq 1 "$(count_warnings)"
  assert_contains "$(warnings)" "$short"
  assert_contains "$(warnings)" "Add the CI workflow"
  assert_contains "$(warnings)" "RF-NNN: summary"
}

test_malformed_subjects_each_warn() {
  local subject
  for subject in "RF-: x" "RF-12 x" "RF-12:x" "rf-12: x" "RF-12:" "RF12: x" "RF-ab: x" " RF-12: x"; do
    repo_with_commits "$subject"
    cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
    assert_status 0 "exit status for subject [$subject]"
    assert_eq 1 "$(count_warnings)" "one warning for subject [$subject]"
  done
}

test_only_the_first_line_of_a_message_counts() {
  make_repo
  BASE=$(head_sha)
  add_commit "RF-120: Add the CI workflow" "this body says whatever it likes
and Fixes nothing"
  HEAD_SHA=$(head_sha)
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  assert_no_warning
}

test_a_merge_commit_is_not_examined() {
  make_repo
  BASE=$(head_sha)
  add_commit "RF-120: Add the CI workflow"
  add_merge_commit "Merge branch 'whatever' into main"
  HEAD_SHA=$(head_sha)
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  # the merge commit has a bad subject and is skipped; the commit on the side branch is conforming
  assert_no_warning
}

test_an_empty_range_has_no_commit_warnings() {
  make_repo
  BASE=$(head_sha)
  cc "$BASE" "$BASE" 018-ci-cd-pipeline
  assert_no_warning
}

test_an_unreadable_range_warns_once_and_still_exits_zero() {
  make_repo
  cc 1111111111111111111111111111111111111111 2222222222222222222222222222222222222222 018-ci-cd-pipeline
  assert_status 0
  assert_eq 1 "$(count_warnings)"
  assert_contains "$(warnings)" "Could not read the commits"
}

test_several_problems_give_one_warning_each() {
  repo_with_commits "Add one" "RF-120: fine" "fix two"
  cc "$BASE" "$HEAD_SHA" some-branch
  assert_status 0
  assert_eq 3 "$(count_warnings)"
}

test_wrong_argument_count_warns_and_exits_zero() {
  make_repo
  cc onlyone
  assert_status 0
  assert_eq 1 "$(count_warnings)"
  cc
  assert_status 0
  assert_eq 1 "$(count_warnings)"
}

# ---- output

test_a_percent_sign_in_a_subject_is_escaped_for_the_annotation() {
  repo_with_commits "100% wrong"
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  assert_contains "$(warnings)" "100%25 wrong"
}

test_the_summary_goes_to_the_summary_file_when_there_is_one() {
  repo_with_commits "Add the CI workflow"
  local summary
  summary=$(mktemp "${TMPDIR:-/tmp}/summary.XXXXXX")
  run_cmd bash -c 'cd "$1" && GITHUB_STEP_SUMMARY="$4" bash "$2" "$3" "$5" 018-ci-cd-pipeline' _ "$REPO" "$SCRIPT" "$BASE" "$summary" "$HEAD_SHA"
  assert_status 0
  assert_contains "$(cat "$summary")" "Conventions"
  assert_not_contains "$OUT" "### Conventions"
  assert_contains "$OUT" "::warning"
  rm -f "$summary"
}

test_the_summary_is_printed_when_there_is_no_summary_file() {
  repo_with_commits "RF-120: fine"
  cc "$BASE" "$HEAD_SHA" 018-ci-cd-pipeline
  assert_contains "$OUT" "### Conventions"
}

run_tests
