import type {Metadata} from 'next';
import {cookies} from 'next/headers';
import {redirect} from 'next/navigation';
import {ModuleGate} from '@/components/migration/module-gate';
import {ProtectedShell} from '@/components/layout/protected-shell';
// Transport only: Java decides whether the stored session is valid.
import {backendOrigin} from '../../../config/backend.mjs';
export const metadata:Metadata={robots:{index:false,follow:false}};
export default async function Layout({children}:{children:React.ReactNode}) {
  const cookie=(await cookies()).get('WC_SESSION');
  if(!cookie)redirect('/login');
  let response:Response;
  try {
    response=await fetch(backendOrigin()+'/api/auth/get-session',{
      headers:{Cookie:'WC_SESSION='+cookie.value},cache:'no-store',redirect:'error',signal:AbortSignal.timeout(5000)
    });
  }catch{return <main className="p-8">Java backend is unavailable. <a href="/login">Back to sign in</a></main>;}
  if(response.status===401||response.status===403)redirect('/login');
  if(!response.ok)return <main className="p-8">Unable to verify your session. <a href="/login">Back to sign in</a></main>;
  const session=await response.json();
  if(!session?.user?.id)redirect('/login');
  return <ProtectedShell><ModuleGate>{children}</ModuleGate></ProtectedShell>;
}
