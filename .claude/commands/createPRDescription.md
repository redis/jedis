---
description: Generate a PR title and description from the diff between two local branches, using the creating-description-for-gh-pr skill, and save to prDescription.md
argument-hint: [base-branch] [target-branch]
---

Use the "creating-description-for-gh-pr" skill to generate a PR title and description.

Arguments provided to this command: base = `$1`, target = `$2` (either may be empty).

Pass this context into the skill's branch resolution logic (Step 1). Since these are positional arguments, only three states are possible:
- Both `$1` and `$2` given → `$1` is the explicit base branch, `$2` is the explicit target branch.
- Only `$1` given (`$2` empty) → `$1` is the explicit base branch; let the skill default the target branch.
- Neither given (both empty) → let the skill default both base and target branches.

`$1` cannot be empty while `$2` is non-empty — do not handle that case.

Do not duplicate or reinterpret the skill's instructions; follow them as written, including the output structure, the writing-style constraints, and the save/confirm steps.
