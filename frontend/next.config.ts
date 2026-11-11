import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import { backendOrigin } from "./config/backend.mjs";
import { SECURITY_HEADERS } from "./config/security-headers";

const java = backendOrigin();
const config: NextConfig = {
  output: "standalone",
  async rewrites() {
    return {
      beforeFiles: [
        { source: "/api/health", destination: java + "/actuator/health" },
        { source: "/api/:path*", destination: java + "/api/:path*" },
        { source: "/media/:path*", destination: java + "/media/:path*" },
      ],
    };
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "Cache-Control", value: "no-store" },
          ...SECURITY_HEADERS,
        ],
      },
    ];
  },
};
export default createNextIntlPlugin("./src/i18n/request.ts")(config);
