#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-}"
VERSION_PATTERN='[0-9]+\.[0-9]+\.[0-9]+\+[a-z0-9.]+'

# $1 Key | $2 Value
function set_output() {
    echo "$1=$2" >> "${GITHUB_OUTPUT:-/dev/null}"
}

function get_bin_ver() {
    local ver
    ver=$(grep -E "^mod_version=" gradle.properties | grep -Eo "$VERSION_PATTERN" | head -n1)
    if [[ -z "$ver" ]]; then
        echo "ERROR: Could not find mod_version in gradle.properties matching pattern: $VERSION_PATTERN" >&2
        exit 1
    fi
    echo "$ver"
}

function get_mc_ver() {
    grep -E "^minecraft_version=" gradle.properties | cut -d'=' -f2 | tr -d '[:space:]'
}

function update_binary_version() {
    local new_version="$1"
    # Use a looser pattern for replacement so it matches whatever is currently in the file
    sed -i'' -E "s/mod_version=.*/mod_version=${new_version}/" gradle.properties
    echo "Updated gradle.properties mod_version -> ${new_version}"
}

echo "--- Version Resolution ---"

if [[ "$TAG" != refs/tags/* ]]; then
    echo "Non-release build detected, using debug suffix"
    mc_ver=$(get_mc_ver)
    short_commit=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    new_version="${mc_ver}+debug.${short_commit}"
    update_binary_version "$new_version"
else
    TAG="${TAG#refs/tags/}"
    echo "Release build detected, tag: $TAG"

    # Validate the tag looks like a version (with or without leading v)
    if [[ ! "$TAG" =~ ^v?[0-9]+\.[0-9]+\.[0-9]+(\+[a-z0-9.]+)?$ ]]; then
        echo "ERROR: Tag '$TAG' does not look like a valid version (expected X.Y.Z or vX.Y.Z)" >&2
        exit 1
    fi

    # Strip leading v so Fabric gets clean semver (0.0.8 not v0.0.8)
    SEMVER="${TAG#v}"
    update_binary_version "$SEMVER"
    set_output "binver" "$SEMVER"
    echo "Release binver set to: $SEMVER (tag: $TAG)"
fi

final_ver=$(get_bin_ver)
echo "Final version in gradle.properties: $final_ver"

echo ""
echo "--- Building ---"
./gradlew build --no-daemon --configure-on-demand -Dorg.gradle.parallel=true
