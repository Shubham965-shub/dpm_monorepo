import { useCallback, useState } from 'react'
import { useAuth } from './useAuth'

function storageKeyFor(userId: string): string {
  return `dpm.coachMarksSeen.${userId}`
}

function readSeen(userId: string): Set<string> {
  try {
    const raw = localStorage.getItem(storageKeyFor(userId))
    const parsed: unknown = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? new Set(parsed as string[]) : new Set()
  } catch {
    return new Set()
  }
}

/**
 * Per-user "has this coach-mark tour already run" flags, persisted in localStorage keyed by user id
 * - same mechanism as useFavoriteSections.ts (no backend table for per-user tour state exists yet).
 * Known gap vs. the coach-marks prototype's own dev notes, which ask for backend persistence
 * specifically so a tour already seen doesn't come back on a new device - this falls back to
 * showing it again there instead.
 *
 * Keyed by COACH_SEEN_KEY's own string (see coachMarks.ts), not directly by screen - several
 * screens share one flag there (every persona screen shares 'persona'), so this hook doesn't need
 * its own opinion on that grouping.
 */
export function useCoachMarksSeen() {
  const { user } = useAuth()
  const userId = user?.id ?? null
  // Lazy initializer only, same reasoning as useFavoriteSections.ts: this hook's owner (CoachTourProvider)
  // only mounts while a user is logged in and fully unmounts on logout, so a user-id change always
  // comes with a fresh mount.
  const [seen, setSeen] = useState<Set<string>>(() => (userId ? readSeen(userId) : new Set()))

  const markSeen = useCallback(
    (key: string) => {
      if (!userId) return
      setSeen((prev) => {
        if (prev.has(key)) return prev
        const next = new Set(prev).add(key)
        try {
          localStorage.setItem(storageKeyFor(userId), JSON.stringify(Array.from(next)))
        } catch {
          // localStorage can throw (private browsing, quota, disabled) - the tour still won't
          // re-run this session, it just won't stay dismissed after a reload.
        }
        return next
      })
    },
    [userId],
  )

  const resetSeen = useCallback(() => {
    if (!userId) return
    setSeen(new Set())
    try {
      localStorage.removeItem(storageKeyFor(userId))
    } catch {
      // Best effort, same as markSeen above.
    }
  }, [userId])

  return { seen, markSeen, resetSeen }
}
