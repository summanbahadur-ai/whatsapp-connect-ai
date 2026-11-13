"use client";
import {useEffect,useState} from "react";
import {AuthProvider,useAuth} from "@/hooks/use-auth";
import {Sidebar} from "./sidebar";
import {Header} from "./header";
function Shell({children}:{children:React.ReactNode}) {
  const {user,loading,profileLoading,accountStatus}=useAuth();
  const [open,setOpen]=useState(false);
  useEffect(()=>{if(!loading&&!user)window.location.replace("/login");},[user,loading]);
  if(loading||!user||profileLoading)return <main className="p-8" role="status">Checking your session…</main>;
  if(accountStatus!=="ready")return <main className="p-8">Unable to load account access. <a href="/login">Back to sign in</a></main>;
  return <div className="flex h-dvh overflow-hidden"><Sidebar open={open} onClose={()=>setOpen(false)}/><div className="flex min-w-0 flex-1 flex-col"><Header onOpenSidebar={()=>setOpen(true)}/><div className="min-h-0 flex-1 overflow-auto">{children}</div></div></div>;
}
export function ProtectedShell({children}:{children:React.ReactNode}) {
  return <AuthProvider><Shell>{children}</Shell></AuthProvider>;
}
