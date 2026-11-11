import type {Metadata} from 'next';
import {ModuleGate} from '@/components/migration/module-gate';
export const metadata:Metadata={robots:{index:false,follow:false}};
export default function Layout({children}:{children:React.ReactNode}) {
  return <ModuleGate>{children}</ModuleGate>;
}
