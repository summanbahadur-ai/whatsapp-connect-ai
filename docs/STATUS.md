# Migration status and resume instructions

Updated:13 November2026.

## Git and approval
- CP03, CP04 signup and CP05 login/sessions are accepted.
- User explicitly approved committing the working changes, merging CP05 into main,
  and pushing the feature branch and main to GitHub on13November2026.
- Integration uses feature/cp05-java-sessions and main; retain the feature reference.
- Final Git commit IDs and remote synchronization are verifiable with git log and
  git ls-remote origin main feature/cp05-java-sessions.
- ALWAYS ask before each future commit and push; this approval covers this integration.
- Original repo and omni_connect remain untouched.
## Accepted checkpoint: CP05
Java login, PostgreSQL Spring Session JDBC, auth-context, logout and the protected
React shell implemented. All verification passed;
read docs/checkpoints/CP05.md and .cache/cp05-* logs before claiming completion.
No SQL changes: existing V1 session tables reused; database/schema.sql synchronized.
Dashboard metrics, password recovery/settings and other business modules stay pending.

## Resume
CP05 verification/evidence completed; local backend8081 and frontend3001 are running.
Do not start CP06 until user accepts and authorizes it. Future commits/pushes still require explicit approval.
Run scripts/start-backend.ps1 -Database and scripts/start-frontend.ps1 if needed.
Review at http://127.0.0.1:3001/login using the account created during accepted CP04.

## Final CP05 verification
41 Java tests (1 unit+40integration),13 frontend unit tests, typecheck/build,
six Chrome transport tests and three real Java/PostgreSQL browser tests passed.
Lint:60 inherited warnings, no errors. Six-path auth OpenAPI valid; SQL synchronized.
Login URL http://127.0.0.1:3001/login; forwarded health UP and page200 confirmed.
Session restart/rotation/revocation, idle/absolute expiry and removed membership tested.
User accepted CP05 and authorized its integration. See checkpoints/CP05.md for exact evidence.