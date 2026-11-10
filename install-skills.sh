#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if (( $# > 1 )); then
  printf 'Usage: %s [destination-skills-directory]\n' "$0" >&2
  exit 2
fi

if (( $# == 1 )); then
  destination="$1"
else
  codex_home="${CODEX_HOME:-${HOME:?HOME must be set when CODEX_HOME is unset}/.codex}"
  destination="$codex_home/skills"
fi

mkdir -p -- "$destination"

mapfile -d '' skill_files < <(find "$script_dir" -type f -path '*/.agents/skills/*/SKILL.md' -print0)
if (( ${#skill_files[@]} == 0 )); then
  printf 'No repository skills found under */.agents/skills/.\n' >&2
  exit 1
fi

for skill_file in "${skill_files[@]}"; do
  skill_dir="${skill_file%/SKILL.md}"
  cp -R -- "$skill_dir" "$destination/"
  printf 'Installed %s to %s\n' "${skill_dir##*/}" "$destination"
done
