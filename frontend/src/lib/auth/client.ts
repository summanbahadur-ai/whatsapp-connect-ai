"use client";

// Compatibility surface for preserved UI only. Authentication is not enabled
// until the Java signup/session checkpoints pass. Never treat a cookie as login.
export interface LocalUser {
  id: string;
  name: string;
  email: string;
  image?: string | null;
  emailVerified: boolean;
  createdAt: Date;
  updatedAt: Date;
}
type Result = {
  data: { user: LocalUser } | null;
  error: { message: string; status: number } | null;
};
async function unavailable(_input?: unknown): Promise<Result> {
  void _input;
  return {
    data: null,
    error: {
      message:
        "This feature is not available yet. Java authentication is being migrated.",
      status: 503,
    },
  };
}
export const authClient = {
  signIn: { email: unavailable },
  signUp: { email: unavailable },
  signOut: unavailable,
  getSession: unavailable,
  requestPasswordReset: unavailable,
  resetPassword: unavailable,
  updateUser: unavailable,
  changeEmail: unavailable,
  changePassword: unavailable,
  revokeSessions: unavailable,
  useSession: (): { data: { user: LocalUser } | null; isPending: boolean } => ({
    data: null,
    isPending: false,
  }),
};
