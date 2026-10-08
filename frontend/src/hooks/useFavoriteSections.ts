import { useCallback, useState } from 'react'
import { flushSync } from 'react-dom'
import { useAuth } from './useAuth'

// A plain domain key (string), not the QC-only SectionKey union - SectionSummaryList now renders
// any KpiDomain[] (see its own `domains` prop), including qaKpiConfig.ts's QA section keys, and a
// favorited section is just whatever key was starred, regardless of which domain set it came from.
function storageKeyFor(userId: string): string {
  return `dpm.favoriteSections.${userId}`
}

function readFavorites(userId: string): Set<string> {
  try {
    const raw = localStorage.getItem(storageKeyFor(userId))
    const parsed: unknown = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? new Set(parsed as string[]) : new Set()
  } catch {
    return new Set()
  }
}

/**
 * Per-user favorited dashboard sections (the star on each SectionSummaryBar), persisted in
 * localStorage keyed by user id - survives logout/login on the same browser, and never leaks
 * between accounts on a shared machine (same "no cross-account leak" concern useAuth's
 * queryClient.clear() on login addresses for query data).
 */
export function useFavoriteSections() {
  const { user } = useAuth()
  const userId = user?.id ?? null
  // Lazy initializer only (no re-sync effect): DashboardPage - and everything under it, including
  // this hook - only mounts while a user is logged in (see App.tsx) and fully unmounts on logout,
  // so a user-id change always comes with a fresh mount, never a live swap under this same instance.
  const [favorites, setFavorites] = useState<Set<string>>(() => (userId ? readFavorites(userId) : new Set()))

  const toggleFavorite = useCallback(
    (key: string) => {
      if (!userId) return
      const applyToggle = () =>
        setFavorites((prev) => {
          const next = new Set(prev)
          if (next.has(key)) next.delete(key)
          else next.add(key)
          try {
            localStorage.setItem(storageKeyFor(userId), JSON.stringify(Array.from(next)))
          } catch {
            // localStorage can throw (private browsing, quota, disabled) - the toggle still
            // applies for this session, it just won't survive a reload.
          }
          return next
        })
      // Animates the section list's reorder (see SectionSummaryList's per-row view-transition-name)
      // instead of it jumping straight to the new order - Firefox/older Safari lack this API, so
      // they just fall back to the plain (still correct) instant reorder. flushSync is required:
      // startViewTransition snapshots the DOM synchronously around this callback, but a bare
      // setState wouldn't have committed to the DOM yet by the time the callback returns.
      if (typeof document.startViewTransition === 'function') {
        document.startViewTransition(() => flushSync(applyToggle))
      } else {
        applyToggle()
      }
    },
    [userId],
  )

  return { favorites, toggleFavorite }
}
