import test from 'node:test';
import assert from 'node:assert/strict';
import {authClient} from '../../src/lib/auth/client.ts';
test('login and logout obtain fresh CSRF and call Java; session lookup is no-store',async()=>{
 const previous=globalThis.fetch,calls=[];
 globalThis.fetch=async(path,init)=>{calls.push({path,init});return Response.json(path.endsWith('/csrf')?{token:'masked-fixture',headerName:'X-XSRF-TOKEN'}:{user:{id:'fixture',name:'Owner',email:'owner@example.invalid',createdAt:'2026-11-13T00:00:00Z',updatedAt:'2026-11-13T00:00:00Z'}})};
 try {
  assert.equal((await authClient.signIn.email({email:'owner@example.invalid',password:'fixture-password'})).error,null);
  assert.equal((await authClient.getSession()).error,null);
  assert.equal((await authClient.signOut()).error,null);
  assert.deepEqual(calls.map(x=>x.path),['/api/auth/csrf','/api/auth/sign-in/email','/api/auth/get-session','/api/auth/csrf','/api/auth/sign-out']);
  assert.equal(new Headers(calls[1].init.headers).get('X-XSRF-TOKEN'),'masked-fixture');
  assert.equal(calls[2].init.cache,'no-store');
 }finally{globalThis.fetch=previous;}
});
