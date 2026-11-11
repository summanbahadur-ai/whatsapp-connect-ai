# Migration status and resume instructions

CP03 accepted by user on 11 November 2026. User authorized local commit/merge into
main and proceeding to CP04. Verification rerun passed: 7 unit/boundary tests,
6 browser tests, typecheck/build, lint exit0 with60 legacy warnings.
A separate question about pushing main to GitHub is pending; do not push without
that answer. ALWAYS ask before each future commit and push.

Next authorized work: CP04 Java signup and owner provisioning. Create
feature/cp04-java-signup from merged main. Implement signup/CSRF/password hashing,
atomic credentials/account/profile provisioning and original signup UI wiring.
Stop after CP04 tests/manual review. Login/session behavior remains CP05.
No default old-app API or database fallback. See database/README.md and CP04 plan.
Every SQL structure change requires a new migration plus updated database/schema.sql.
