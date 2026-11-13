"use client";
import { useSyncExternalStore } from "react";
const subscribe = () => () => {};
const clientReady = () => true;
const serverReady = () => false;
/** Avoid accepting server-rendered form clicks before React can handle them. */
export function useHydrated() {
  return useSyncExternalStore(subscribe, clientReady, serverReady);
}
