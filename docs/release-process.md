# Release Process

Releases are drafted automatically by
[Release Drafter](https://github.com/redis/jedis/blob/master/.github/workflows/release-drafter.yml)
and deployed by the
[Release workflow](https://github.com/redis/jedis/blob/master/.github/workflows/version-and-release.yml)
when a draft is published.

## Branches

| Branch          | Hosts               | Example          |
|-----------------|---------------------|------------------|
| `master`        | next minor or major | `8.1.0-SNAPSHOT` |
| `MAJOR.x`       | `MAJOR.0.PATCH`     | `7.x`, `8.x`     |
| `MAJOR.MINOR.x` | `MAJOR.MINOR.PATCH` | `7.5.x`, `8.1.x` |

- Minor and major releases, including betas, are tagged on `master`.
- Patch releases are tagged on their maintenance branch, never on `master`.
- A maintenance branch is cut from the GA tag commit (`8.1.x` from `v8.1.0`).
- Fixes land on `master` first and are backported with a PR titled
  `Backport #<pr> to <line>: <original title>`, carrying the original labels.

## How the draft is produced

On every push to `master` or a maintenance branch, Release Drafter updates one
draft per branch:

- **Version** is derived from the nearest GA tag reachable from the branch,
  ignoring prerelease tags: patch bump on maintenance branches, minor bump on
  `master`. Example: `8.x` at `v8.0.1` drafts `8.0.2`; `master` at `v8.0.0`
  drafts `8.1.0`.
- **Changes** are the pull requests merged to the branch since the latest GA
  release published from that branch. For the first patch after a branch is
  cut, the GA tag's commit date is used as the cutoff instead.
- Betas are prereleases and are not used as a base, so GA notes are cumulative
  since the previous GA.
- PRs labeled `skip-changelog` or `churn` are excluded. Unlabeled PRs appear
  uncategorized, so label backports.

## Publishing

1. Review the draft on the Releases page and edit the notes if needed.
2. Publish from the draft. This keeps the release target on the right branch,
   which the next draft on that branch depends on. Do not create releases
   from the tag page.
3. For a patch of an older line, untick **Set as the latest release**.
4. Publishing creates the tag and triggers the Release workflow, which sets
   the Maven version from the tag and deploys to Maven Central.

Major releases and betas are not derived automatically. Run the workflow
manually with the `version` input, for example `9.0.0` or `8.1.0-beta2`.

## Version bumps

The Release workflow sets the artifact version from the tag, so the `pom.xml`
version on a maintenance branch does not affect published artifacts. Bump the
snapshot version on `master` only after a minor or major release, with the PR
labeled `skip-changelog`.
