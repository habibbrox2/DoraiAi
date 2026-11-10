#!/usr/bin/env bash
set -euo pipefail
usage(){ echo "Usage: $0 create <agent> <task> [base] | list | status <path> | remove <path> [base]"; }
repo="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo 'Run inside a Git worktree.' >&2; exit 2; }
action="${1:-list}"
case "$action" in
  list) git -C "$repo" worktree list --porcelain ;;
  create)
    agent="${2:?agent required}"; task="${3:?task required}"; base="${4:-main}"
    [[ "$agent" =~ ^[A-Za-z0-9._-]+$ && "$task" =~ ^[A-Za-z0-9._-]+$ ]] || { echo 'Invalid agent/task identifier.' >&2; exit 2; }
    agent_lc="$(printf '%s' "$agent" | tr '[:upper:]' '[:lower:]')"; task_lc="$(printf '%s' "$task" | tr '[:upper:]' '[:lower:]')"; branch="agent/$agent_lc/$task_lc"
    root="$(dirname "$repo")/worktrees"; path="$root/$agent_lc-$task_lc"
    [[ ! -e "$path" ]] || { echo "Refusing to overwrite $path" >&2; exit 2; }
    git -C "$repo" fetch origin "$base"
    if git -C "$repo" show-ref --verify --quiet "refs/heads/$branch"; then echo "Branch exists: $branch" >&2; exit 2; fi
    mkdir -p "$root"; git -C "$repo" worktree add -b "$branch" "$path" "origin/$base"
    printf '{"task_id":"%s","agent":"%s","branch":"%s","base":"origin/%s","allowed_paths":[],"reserved_paths":[],"validation_profile":"Auto"}\n' "$task" "$agent" "$branch" "$base" > "$path/.agent-task.json"
    echo "Created $path ($branch). Edit .agent-task.json allowed_paths before starting the agent."
    ;;
  status) path="${2:?path required}"; git -C "$path" status --short --branch ;;
  remove)
    path="${2:?path required}"; base="${3:-main}"
    [[ -f "$path/.git" || -d "$path/.git" ]] || { echo 'Not a Git worktree.' >&2; exit 2; }
    [[ -z "$(git -C "$path" status --porcelain)" ]] || { echo 'Refusing to remove dirty worktree.' >&2; exit 2; }
    git -C "$repo" fetch origin "$base"
    if ! git -C "$path" merge-base --is-ancestor HEAD "origin/$base"; then
      # Squash merges lose branch ancestry. Accept only an exact tree match with
      # a commit reachable from the selected base; otherwise preserve the worktree.
      target_tree="$(git -C "$path" rev-parse 'HEAD^{tree}')" || { echo 'Unable to inspect worktree tree.' >&2; exit 2; }
      if ! git -C "$repo" log -r --format='%T' "origin/$base" | grep -Fxq "$target_tree"; then
        echo 'Refusing to remove unmerged or divergent worktree.' >&2
        exit 2
      fi
    fi
    git -C "$repo" worktree remove "$path"
    ;;
  *) usage; exit 2 ;;
esac
