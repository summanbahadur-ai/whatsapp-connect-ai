export const modules = [
  { path: "/login", name: "Sign in", checkpoint: "CP05", available: true },
  {
    path: "/signup",
    name: "Create account",
    checkpoint: "CP04",
    available: true,
  },
  {
    path: "/forgot-password",
    name: "Password recovery",
    checkpoint: "CP06",
    available: false,
  },
  {
    path: "/reset-password",
    name: "Reset password",
    checkpoint: "CP06",
    available: false,
  },
  {
    path: "/join",
    name: "Account invitations",
    checkpoint: "CP09",
    available: false,
  },
  {
    path: "/dashboard",
    name: "Dashboard",
    checkpoint: "CP07",
    available: false,
  },
  {
    path: "/settings",
    name: "Settings",
    checkpoint: "CP08+",
    available: false,
  },
  { path: "/contacts", name: "Contacts", checkpoint: "CP15", available: false },
  {
    path: "/inbox",
    name: "Inbox",
    checkpoint: "later module",
    available: false,
  },
  {
    path: "/pipelines",
    name: "Pipelines",
    checkpoint: "later module",
    available: false,
  },
  {
    path: "/broadcasts",
    name: "Broadcasts",
    checkpoint: "later module",
    available: false,
  },
  {
    path: "/flows",
    name: "Flows",
    checkpoint: "later module",
    available: false,
  },
  {
    path: "/automations",
    name: "Automations",
    checkpoint: "later module",
    available: false,
  },
  {
    path: "/notifications",
    name: "Notifications",
    checkpoint: "later module",
    available: false,
  },
  {
    path: "/agents",
    name: "AI agents",
    checkpoint: "later module",
    available: false,
  },
  {
    path: "/api-docs",
    name: "API documentation",
    checkpoint: "later module",
    available: false,
  },
] as const;
export function moduleForPath(path: string) {
  return modules.find(
    (item) => path === item.path || path.startsWith(item.path + "/"),
  );
}
