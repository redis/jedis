---
name: creating-description-for-gh-pr
description: Generate a clear, concise GitHub PR title and description from the diff between two local git branches, and save it to prDescription.md in the repo root. Use this whenever the user asks to write, generate, draft, or update a PR description or PR title from local branch changes — including phrasing like "summarize this diff into a PR description," "write a PR description for my current branch," "create a PR title and description," or any request to compare a base and target branch for PR purposes. Trigger even if the user doesn't name specific branches; this skill knows how to default them.
---

# PR Description Generator

Writes a GitHub PR title and description from the diff between two local git branches, focused on what reviewers need: purpose, key decisions/assumptions, behavioral or conceptual changes, and a brief testing note if relevant. Uses only standard git commands, so any agent can follow it.

## 1. Resolve branches

- Both named → use as given.
- Only one named → it's the **base**; target defaults to current branch.
- None named → base defaults to `main`, then `master` if `main` doesn't exist (`git branch --list`). **Only check these two names — no scanning other branches, no guessing.** If neither exists, stop and ask. Target defaults to current branch (`git branch --show-current`).

State the resolved base/target before proceeding.

## 2. Diff

- Confirm both branches exist (`git branch --list <name>`); stop if not.
- `git diff <base>...<target>` and `git log <base>..<target> --oneline`
- Empty diff → stop and say so, don't fabricate a description.

## 3. Analyze

From the diff/log, identify: purpose of the change; key decisions and trade-offs; assumptions the code relies on; behavioral changes (concrete: "X now rejects Y" not "changed validation"); conceptual/contract changes; breaking changes, new config/env vars, migrations; test changes (what new tests cover conceptually, why existing ones were adapted — not test-by-test).

Skip routine stuff (formatting, trivial renames). No file/line enumeration — write like explaining to a teammate, not a changelog.

## 4. Output structure

Write to `prDescription.md` (repo root, overwrite if exists):

```markdown
# <Title — imperative, <70 chars>

## Summary
<2-3 sentences>

## Key Decisions & Assumptions
<bullets — omit if none>

## Behavioral / Conceptual Changes
<bullets, concrete — omit if none>

## Testing
<2-3 sentences, only if diff touches tests — omit otherwise, don't write "no tests">

## Notes
<breaking changes, follow-ups — omit if none>
```

Keep it brief (readable in under a minute, ~150-200 words total unless the change is genuinely large). Omit empty sections rather than padding them. Never fabricate testing details, tickets, or names not evidenced in the diff — flag uncertainty in Notes instead.

## 5. Confirm

After writing, give a short confirmation (don't reprint the file) and offer to run `gh pr create --title "..." --body-file prDescription.md` if `gh` is available. Include the provided/resolved base and target branch names into the suggested command as part of the suggestion. At the end, ask if the user wants to open the PR now and wait for confirmation before proceeding. If the user declines, stop and say the PR description is ready in `prDescription.md`.
