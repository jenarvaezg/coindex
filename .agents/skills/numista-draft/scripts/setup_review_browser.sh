#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(git -C "$script_dir" rev-parse --show-toplevel)

for marker in spec.md data/numista-type-cache.json scripts/record-fixture.py; do
    if [ ! -e "$repo_root/$marker" ]; then
        echo "error: $repo_root is not the Coindex repository (missing $marker)" >&2
        exit 1
    fi
done

runtime_dir="$repo_root/.playwright-mcp"
npm install \
    --prefix "$runtime_dir" \
    --no-save \
    --no-package-lock \
    @playwright/mcp@0.0.78

test -f "$runtime_dir/node_modules/@playwright/mcp/cli.js"
echo "Numista review browser runtime installed in $runtime_dir"
