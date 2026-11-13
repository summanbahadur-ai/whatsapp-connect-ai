'use client';
import { useLocalPolling } from './use-local-polling';
export function useTotalUnread(): number {
  return (
    useLocalPolling<{ conversations: number }>('/api/unread-counts', false)
      ?.conversations ?? 0
  );
}
