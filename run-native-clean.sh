#!/bin/zsh

set -euo pipefail

BIN="./build/native/nativeCompile/app"

exec env -i LANG="pt_BR.UTF-8" LC_ALL="pt_BR.UTF-8" "$BIN"