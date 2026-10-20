#!/bin/bash
# Tiny test helpers for the scripts in .github/scripts. Source this file from a *.test.sh, define functions named
# test_*, and finish the file with `run_tests`. Compatible with bash 3.2 (macOS) and bash 5 (the runner): no associative
# arrays, no mapfile/readarray, no ${var,,}.
#
# Every test function runs in its own subshell; the first failing assertion prints why and ends that test.

set -u

LIB_TMP_DIRS=""
OUT=""
ERR=""
STATUS=0
REPO=""

lib_cleanup() {
  local d
  for d in $LIB_TMP_DIRS; do
    rm -rf "$d"
  done
}
trap lib_cleanup EXIT

lib_fail() {
  echo "    assertion failed: $*" >&2
  exit 1
}

assert_eq() {
  # assert_eq <expected> <actual> [message]
  if [ "$1" != "$2" ]; then
    lib_fail "${3:-values differ}: expected [$1] but got [$2]"
  fi
}

assert_contains() {
  # assert_contains <haystack> <needle> [message]
  case "$1" in
    *"$2"*) ;;
    *) lib_fail "${3:-text does not contain the expected part}: [$2] not found in [$1]" ;;
  esac
}

assert_not_contains() {
  # assert_not_contains <haystack> <needle> [message]
  case "$1" in
    *"$2"*) lib_fail "${3:-text contains a part it should not}: [$2] found in [$1]" ;;
    *) ;;
  esac
}

assert_empty() {
  # assert_empty <value> [message]
  if [ -n "$1" ]; then
    lib_fail "${2:-expected empty}: got [$1]"
  fi
}

run_cmd() {
  # run_cmd <command> [args...]  -> sets STATUS, OUT (stdout) and ERR (stderr) without aborting the test
  local out_file err_file
  out_file=$(mktemp "${TMPDIR:-/tmp}/lib-out.XXXXXX")
  err_file=$(mktemp "${TMPDIR:-/tmp}/lib-err.XXXXXX")
  "$@" >"$out_file" 2>"$err_file"
  STATUS=$?
  OUT=$(cat "$out_file")
  ERR=$(cat "$err_file")
  rm -f "$out_file" "$err_file"
}

assert_status() {
  # assert_status <expected status> [message]   (checks the STATUS left by run_cmd)
  assert_eq "$1" "$STATUS" "${2:-exit status} (stdout [$OUT], stderr [$ERR])"
}

make_repo() {
  # make_repo -> creates a throwaway git repository on branch main with one commit, sets REPO to its path.
  # The real repository is never touched; the directory is removed when the test file ends.
  REPO=$(mktemp -d "${TMPDIR:-/tmp}/lib-repo.XXXXXX")
  LIB_TMP_DIRS="$LIB_TMP_DIRS $REPO"
  git -C "$REPO" init -q -b main
  git -C "$REPO" config user.name "Test"
  git -C "$REPO" config user.email "test@example.com"
  git -C "$REPO" config commit.gpgsign false
  git -C "$REPO" config tag.gpgsign false
  git -C "$REPO" commit -q --allow-empty -m "RF-1: first commit"
}

add_commit() {
  # add_commit <subject> [body]  -> an empty commit on the current branch
  if [ "$#" -ge 2 ]; then
    git -C "$REPO" commit -q --allow-empty -m "$1" -m "$2"
  else
    git -C "$REPO" commit -q --allow-empty -m "$1"
  fi
}

add_tag() {
  # add_tag <name>  -> a lightweight tag on the current commit
  git -C "$REPO" tag "$1"
}

add_merge_commit() {
  # add_merge_commit <subject>  -> merges a one-commit side branch into the current branch with a merge commit
  local current side
  current=$(git -C "$REPO" rev-parse --abbrev-ref HEAD)
  side="side-$$-$RANDOM"
  git -C "$REPO" checkout -q -b "$side"
  git -C "$REPO" commit -q --allow-empty -m "RF-9: side branch work"
  git -C "$REPO" checkout -q "$current"
  git -C "$REPO" merge -q --no-ff -m "$1" "$side" >/dev/null
}

head_sha() {
  git -C "$REPO" rev-parse HEAD
}

run_tests() {
  # Runs every function whose name starts with test_, each in a subshell; prints one line per test and a count.
  local fn output status total=0 failed=0
  for fn in $(declare -F | awk '{print $3}' | grep '^test_'); do
    total=$((total + 1))
    output=$( ( "$fn" ) 2>&1 )
    status=$?
    if [ "$status" -eq 0 ]; then
      echo "  ok    $fn"
    else
      failed=$((failed + 1))
      echo "  FAIL  $fn"
      if [ -n "$output" ]; then
        echo "$output" | sed 's/^/        /'
      fi
    fi
  done
  echo "  $total tests, $failed failed"
  [ "$failed" -eq 0 ]
}
