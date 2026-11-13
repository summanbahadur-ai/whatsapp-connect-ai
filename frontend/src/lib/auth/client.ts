"use client";
import { useEffect, useState } from "react";
import { apiResult } from "../api/client.ts";

// Java owns credentials and PostgreSQL-backed sessions.
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
  error: { message: string; status: number; code?: string } | null;
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
interface SignupInput {
  name: string;
  email: string;
  password: string;
}
type SignupUser = Omit<LocalUser, "createdAt" | "updatedAt"> & {
  createdAt: string;
  updatedAt: string;
};
async function signup(input: SignupInput): Promise<Result> {
  const csrf = await apiResult<{ token: string; headerName: string }>(
    "/api/auth/csrf",
  );
  if (csrf.error) return { data: null, error: csrf.error };
  if (
    !csrf.data ||
    typeof csrf.data.token !== "string" ||
    !csrf.data.token ||
    !["X-XSRF-TOKEN", "X-CSRF-TOKEN"].includes(csrf.data.headerName)
  ) {
    return {
      data: null,
      error: {
        status: 502,
        message: "Unable to prepare secure signup. Try again.",
      },
    };
  }
  const result = await apiResult<{ user: SignupUser }>(
    "/api/auth/sign-up/email",
    {
      method: "POST",
      headers: { [csrf.data.headerName]: csrf.data.token },
      body: JSON.stringify(input),
    },
  );
  if (result.error) return { data: null, error: result.error };
  if (!result.data?.user)
    return {
      data: null,
      error: { status: 502, message: "Unable to confirm account creation." },
    };
  const user = result.data.user;
  return {
    data: {
      user: {
        ...user,
        createdAt: new Date(user.createdAt),
        updatedAt: new Date(user.updatedAt),
      },
    },
    error: null,
  };
}
async function securePost(path: string, input: unknown) {
  const csrf = await apiResult<{token: string; headerName: string}>("/api/auth/csrf");
  if (csrf.error) return {data: null, error: csrf.error};
  if (!csrf.data?.token || !["X-XSRF-TOKEN", "X-CSRF-TOKEN"].includes(csrf.data.headerName))
    return {data: null, error: {status: 502, message: "Unable to prepare secure request. Try again."}};
  return apiResult<{user: SignupUser}>(path, {method: "POST", headers: {[csrf.data.headerName]: csrf.data.token}, body: JSON.stringify(input)});
}
function userResult(result: Awaited<ReturnType<typeof securePost>>): Result {
  if (result.error) return {data: null, error: result.error};
  if (!result.data?.user) return {data:null,error:{status:502,message:"Unable to confirm session."}};
  return {data: {user: {...result.data.user, createdAt: new Date(result.data.user.createdAt),updatedAt:new Date(result.data.user.updatedAt)}},error:null};
}
async function signin(input: {email:string;password:string}):Promise<Result> {
  return userResult(await securePost("/api/auth/sign-in/email",input));
}
async function getSession():Promise<Result> {
  return userResult(await apiResult<{user:SignupUser}>("/api/auth/get-session"));
}
function useJavaSession(): {data:{user:LocalUser}|null;isPending:boolean} {
  const [state,setState]=useState<{data:{user:LocalUser}|null;isPending:boolean}>({data:null,isPending:true});
  useEffect(()=>{
    let active=true;let sequence=0;
    async function refresh() {
      const current=++sequence;
      const result=await getSession();
      if(active && current===sequence)setState({data:result.data,isPending:false});
    }
    void refresh();
    const focus=()=>{if(!document.hidden)void refresh();};
    window.addEventListener("focus",focus);window.addEventListener("pageshow",focus);
    document.addEventListener("visibilitychange",focus);
    const timer=setInterval(focus,30000);
    return()=>{active=false;clearInterval(timer);window.removeEventListener("focus",focus);window.removeEventListener("pageshow",focus);document.removeEventListener("visibilitychange",focus);};
  },[]);
  return state;
}
export const authClient = {
  signIn: {email:signin}, signUp: {email:signup},
  signOut: async()=>securePost("/api/auth/sign-out",{}), getSession,
  requestPasswordReset:unavailable, resetPassword:unavailable, updateUser:unavailable,
  changeEmail:unavailable, changePassword:unavailable, revokeSessions:unavailable,
  useSession:useJavaSession,
};
