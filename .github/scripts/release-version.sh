#!/bin/bash
# Turns a release tag (vX.Y.Z) into the app's version name and number, and refuses a tag that cannot be released.
#
#   usage:  release-version.sh <tag>          (run inside the repository: the ordering rule reads its tags)
#   stdout: version_name=X.Y.Z                 (on success; the form $GITHUB_OUTPUT takes)
#           version_code=N
#   stderr: one line starting "error:"        (on refusal)
#   exit:   0 valid, 1 refused, 2 wrong usage
#
# version_code = X * 10000 + Y * 100 + Z. Y and Z must be 0-99 and the number must be between 1 and 2100000000, so a
# larger tag always gives a larger number. The number must also be larger than that of every other tag of the form vX.Y.Z,
# whether or not that tag's run published a Release. Compatible with bash 3.2.

set -u

MAX_CODE=2100000000
STRICT='^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$'

refuse() {
  echo "error: $*" >&2
  exit 1
}

if [ "$#" -ne 1 ]; then
  echo "usage: release-version.sh <tag>   (for example v1.2.3)" >&2
  exit 2
fi
tag=$1

if ! [[ $tag =~ $STRICT ]]; then
  refuse "[$tag] is not a release tag: it must be exactly vX.Y.Z (three whole numbers, no leading zeros, no suffix)"
fi
major=${BASH_REMATCH[1]}
minor=${BASH_REMATCH[2]}
patch=${BASH_REMATCH[3]}

if [ "${#minor}" -gt 2 ] || [ "${#patch}" -gt 2 ]; then
  refuse "minor and patch must be between 0 and 99 (got $minor and $patch in $tag)"
fi
# Guard the arithmetic below: a major part longer than 6 digits is far past the ceiling anyway.
if [ "${#major}" -gt 6 ]; then
  refuse "the major part of $tag makes the version number exceed $MAX_CODE"
fi

code=$((major * 10000 + minor * 100 + patch))
if [ "$code" -gt "$MAX_CODE" ]; then
  refuse "version number $code of $tag is above the maximum $MAX_CODE"
fi
if [ "$code" -lt 1 ]; then
  refuse "version number of $tag must be at least 1 (it gives $code)"
fi

if ! tags=$(git tag --list 'v*' 2>/dev/null); then
  refuse "could not read the tags of the repository"
fi
max_code=0
max_tag=""
for other in $tags; do
  [ "$other" = "$tag" ] && continue
  if [[ $other =~ $STRICT ]]; then
    o_minor=${BASH_REMATCH[2]}
    o_patch=${BASH_REMATCH[3]}
    [ "${#BASH_REMATCH[1]}" -gt 6 ] && continue
    [ "${#o_minor}" -gt 2 ] && continue
    [ "${#o_patch}" -gt 2 ] && continue
    o_code=$((${BASH_REMATCH[1]} * 10000 + o_minor * 100 + o_patch))
    if [ "$o_code" -gt "$max_code" ]; then
      max_code=$o_code
      max_tag=$other
    fi
  fi
done
if [ "$code" -le "$max_code" ]; then
  refuse "version number $code of $tag is not larger than $max_code of $max_tag: releases must increase (if $max_tag was never released, delete that tag first)"
fi

printf 'version_name=%s.%s.%s\n' "$major" "$minor" "$patch"
printf 'version_code=%s\n' "$code"
