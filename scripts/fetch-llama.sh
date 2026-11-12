#!/usr/bin/env bash
set -euo pipefail
ROOT="$(realpath "$(dirname "$0")/..")"
REV="$(tr -d '\r\n' < "$ROOT/llama.version")"
[[ "$REV" =~ ^[0-9a-f]{40}$ ]] || { echo "Invalid llama.version" >&2; exit 1; }
TARGET="$ROOT/third_party/llama.cpp"
if [[ -d "$TARGET/.git" ]]; then
    HEAD="$(git -C "$TARGET" rev-parse HEAD)"
    [[ "$HEAD" == "$REV" ]] || { echo "Existing llama.cpp uses a different revision. Preserve/rename it before fetching." >&2; exit 1; }
    [[ -z "$(git -C "$TARGET" status --porcelain)" ]] || { echo "Existing llama.cpp has local changes; refusing to overwrite." >&2; exit 1; }
    echo "llama.cpp already pinned at $REV"
    exit 0
fi
[[ ! -e "$TARGET" ]] || { echo "Destination already exists; refusing to overwrite." >&2; exit 1; }
mkdir -p "$ROOT/third_party"
git init "$TARGET"
git -C "$TARGET" remote add origin https://github.com/ggml-org/llama.cpp.git
git -C "$TARGET" fetch --depth 1 origin "$REV"
git -C "$TARGET" checkout --detach FETCH_HEAD
[[ "$(git -C "$TARGET" rev-parse HEAD)" == "$REV" ]]
echo "llama.cpp fetched at $REV. Its MIT license is in third_party/llama.cpp/LICENSE."
