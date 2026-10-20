#!/bin/bash
# Tests for ../release-version.sh (rows of specs/018-ci-cd-pipeline/contracts/scripts.md). Written before the script.
. "$(dirname "$0")/lib.sh"

SCRIPT="$(cd "$(dirname "$0")/.." && pwd)/release-version.sh"
NL=$'\n'

# rv <tag>  -> runs the script inside $REPO with the tag as its only argument
rv() {
  run_cmd bash -c 'cd "$1" && bash "$2" "$3"' _ "$REPO" "$SCRIPT" "$1"
}

assert_accepted() {
  # assert_accepted <tag> <version name> <version code>
  rv "$1"
  assert_status 0 "tag $1 should be accepted"
  assert_eq "version_name=$2${NL}version_code=$3" "$OUT" "output for $1"
  assert_empty "$ERR" "nothing on standard error for $1"
}

assert_refused() {
  # assert_refused <tag> <text expected in the error message>
  rv "$1"
  assert_status 1 "tag [$1] should be refused"
  assert_empty "$OUT" "nothing on standard output when [$1] is refused"
  assert_contains "$ERR" "error:" "error prefix for [$1]"
  assert_contains "$ERR" "$2" "message for [$1]"
}

test_v1_0_0() { make_repo; assert_accepted v1.0.0 1.0.0 10000; }
test_v1_2_3() { make_repo; assert_accepted v1.2.3 1.2.3 10203; }
test_v2_10_7() { make_repo; assert_accepted v2.10.7 2.10.7 21007; }
test_v0_0_1_is_code_1() { make_repo; assert_accepted v0.0.1 0.0.1 1; }
test_ceiling_is_inclusive() { make_repo; assert_accepted v210000.0.0 210000.0.0 2100000000; }
test_over_the_ceiling_is_refused() {
  make_repo
  assert_refused v210000.0.1 2100000000
  assert_refused v9999999.0.0 2100000000
  assert_refused v99999999999999999999.0.0 2100000000
}
test_zero_is_refused() { make_repo; assert_refused v0.0.0 "at least 1"; }

test_minor_above_99_is_refused() { make_repo; assert_refused v1.100.0 "between 0 and 99"; }
test_patch_above_99_is_refused() { make_repo; assert_refused v1.0.100 "between 0 and 99"; }
test_minor_and_patch_99_are_fine() { make_repo; assert_accepted v1.99.99 1.99.99 19999; }

test_malformed_tags_are_refused() {
  make_repo
  local tag
  for tag in v1.2 v1.2.0-rc1 release-1 1.2.3 v01.2.3 v1.02.3 v1.2.3.4 v1.2.x V1.2.3 " v1.2.3" "v1.2.3 " ""; do
    assert_refused "$tag" "vX.Y.Z"
  done
}

test_no_argument_is_a_usage_error() {
  make_repo
  run_cmd bash -c 'cd "$1" && bash "$2"' _ "$REPO" "$SCRIPT"
  assert_status 2 "usage error"
  assert_empty "$OUT"
  assert_contains "$ERR" "usage"
}

test_lower_than_an_existing_tag_is_refused() {
  make_repo
  add_tag v1.0.0
  add_tag v1.1.0
  assert_refused v1.0.1 "v1.1.0"
  assert_contains "$ERR" "larger"
}

test_just_below_an_existing_tag_is_refused() {
  make_repo
  add_tag v1.1.0
  assert_refused v1.0.99 "v1.1.0"
}

test_higher_than_every_existing_tag_is_accepted() {
  make_repo
  add_tag v1.0.0
  add_tag v1.1.0
  assert_accepted v1.1.1 1.1.1 10101
}

test_the_tag_itself_is_not_compared_with_itself() {
  make_repo
  add_tag v1.0.0
  add_tag v1.1.0
  assert_accepted v1.1.0 1.1.0 10100
}

test_tags_of_other_forms_are_ignored() {
  make_repo
  add_tag v1.2
  add_tag nightly
  add_tag v1.0.0-rc1
  add_tag v9.0.0-beta
  add_tag v03.0.0
  assert_accepted v1.0.0 1.0.0 10000
}

test_a_failed_tag_without_a_release_still_counts() {
  make_repo
  add_tag v2.0.0
  assert_refused v1.9.9 "v2.0.0"
}

run_tests
