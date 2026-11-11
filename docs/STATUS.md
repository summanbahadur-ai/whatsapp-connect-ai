# Migration status and resume instructions

Updated: 11 November 2026.

## Current state

- Specification and initial folder layout prepared.
- No frontend copied, no Java application generated, no dependencies installed.
- No new Git repository initialized or remote repository published yet.
- No new PostgreSQL database/roles/migrations executed.
- No customer data, secrets, uploads or source runtime configuration copied.
- No old application behavior, original Git branch or Meta callback changed.
- No application tests/builds have run in this new project: there is no app yet.
- CP00 documentation inventory is complete; architecture choices remain proposed
  until user review. All implementation checkpoints are pending.

## Documentation verification

The source baseline contains 90 API route files, 151 documented API operations,
24 page files, 41 tables and 122 source test files. Prepared documentation passed
local link checks, OpenAPI parser validation, explicit handler coverage, the
CP00–CP30 checkpoint-sequence check and private-configuration/credential-pattern
checks. These are documentation checks, not Java implementation or runtime tests.

## Next action

Review `SPECIFICATION.md`, `ARCHITECTURE.md` and `DATABASE_AND_SECURITY.md`.
Confirm the fresh separate database/account assumption, then start **CP01 only**
from `MIGRATION_CHECKPOINTS.md`. Inspect tools and create the foundational Java/UI
scaffold in small checkpoints. Stop after that checkpoint's tests and review.

## Resume in another conversation

Open `D:\whatsapp-connect-ai` as the workspace and say:

> Read README.md and docs/STATUS.md, then the referenced specification and
> checkpoint plan. Continue only the next approved checkpoint, test it, update
> the status and stop for my review. Do not modify the old project/database.

## Checkpoint evidence template

Create `docs/checkpoints/CPxx.md` after each checkpoint with:

```text
Checkpoint:
Date:
Source baseline/version:
Started from commit:
Files/features changed:
Schema migration versions:
Commands and exit codes:
Automated results:
Manual user steps and observed results:
Known limitations / unfinished screens:
Provider side effects (if any):
Rollback / compatibility notes:
Stable commit/tag:
User review: pending / accepted / changes requested
Next permitted checkpoint:
```

Record failing checks honestly. Never label a checkpoint accepted before the user
has tested/reviewed it. Do not store credentials, reset links or customer payloads
in evidence files.
