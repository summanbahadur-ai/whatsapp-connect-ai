import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import ts from "typescript";
const root = path.resolve(import.meta.dirname, "../..");
function files(dir) {
  return fs
    .readdirSync(dir, { withFileTypes: true })
    .flatMap((entry) =>
      entry.isDirectory()
        ? files(path.join(dir, entry.name))
        : [path.join(dir, entry.name)],
    );
}
test("frontend imports cannot reach SQL, auth server, workers, provider clients or Node services", () => {
  const violations = [];
  for (const file of files(path.join(root, "src")).filter((file) =>
    /\.[cm]?tsx?$/.test(file),
  )) {
    const source = fs.readFileSync(file, "utf8");
    for (const item of ts.preProcessFile(source, true, true).importedFiles) {
      if (
        /^(?:pg|better-auth|nodemailer|server-only)(?:\/|$)|^node:|@\/lib\/(?:db(?:\/|$)|auth\/server|whatsapp\/meta-api|ai\/(?:generate|config)|webhooks|automations\/engine|flows\/engine)/.test(
          item.fileName,
        )
      )
        violations.push(path.relative(root, file) + " -> " + item.fileName);
    }
    const env = [...source.matchAll(/process\.env\.([A-Z_]+)/g)].map(
      (match) => match[1],
    );
    assert.deepEqual(
      env.filter((key) => key !== "NEXT_PUBLIC_APP_LOCALE"),
      [],
      file,
    );
  }
  assert.deepEqual(violations, []);
});
test("no TypeScript business route handlers or old authentication proxy are copied", () => {
  const source = files(path.join(root, "src"));
  assert.deepEqual(
    source.filter((file) => /[\\/]route\.[jt]s$/.test(file)),
    [],
  );
  assert.equal(fs.existsSync(path.join(root, "src/proxy.ts")), false);
  const pkg = JSON.parse(
    fs.readFileSync(path.join(root, "package.json"), "utf8"),
  );
  for (const dep of ["pg", "better-auth", "nodemailer", "server-only"])
    assert.equal(pkg.dependencies[dep], undefined);
});
