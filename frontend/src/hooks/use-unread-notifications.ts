'use client';
import { useLocalPolling } from './use-local-polling';
export function useUnreadNotifications(): number {
  return (
    useLocalPolling<{ notifications: number }>('/api/unread-counts', false)
      ?.notifications ?? 0
  );
}
