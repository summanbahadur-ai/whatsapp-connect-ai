import legacyBaseline from "./config/legacy-lint-baseline.mjs";
import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  ...legacyBaseline,
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    ".cache/**",
    "local-data/**",
    ".superpowers/**",
    // Vendored minified opus-recorder encoder worker (served statically).
    "public/opus/**",
    // Generated Swagger UI distribution is checked by its upstream package.
    "public/api-docs-assets/**",
  ]),
]);

export default eslintConfig;
