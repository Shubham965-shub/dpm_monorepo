import { createContext, useCallback, useContext, useEffect, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '@/lib/utils'
import { useCoachMarksSeen } from '@/hooks/useCoachMarksSeen'
import { coachTipsFor, COACH_SCREEN_FAMILY, COACH_SEEN_KEY, type CoachScreenKey, type CoachTip } from '@/lib/coachMarks'

interface ResolvedStep extends CoachTip {
  el: Element
}

interface ActiveTour {
  screenKey: CoachScreenKey
  /** Which seen-flag completing this tour marks (see COACH_SEEN_KEY) - several screenKeys can share
   * one, so this is tracked separately from screenKey rather than re-derived from it everywhere. */
  seenKey: string
  steps: ResolvedStep[]
  index: number
}

// Matches the coach-marks prototype's own post-navigation settle delay (time for the just-rendered
// screen's layout - and, for the 'cards' family, a just-expanded section's card grid - to actually
// paint) before querying selectors; shorter for 'cards' since that only waits on one section's grid,
// not a whole screen navigation.
const SCREEN_START_DELAY_MS = 650
const SECTION_START_DELAY_MS = 350

function resolveSteps(screenKey: CoachScreenKey): ResolvedStep[] {
  return coachTipsFor(screenKey)
    .map((tip) => {
      const el = document.querySelector(tip.selector)
      return el ? { ...tip, el } : null
    })
    .filter((step): step is ResolvedStep => step != null && step.el.getClientRects().length > 0)
}

interface CoachTourContextValue {
  /** First-visit trigger - a no-op once `screenKey` has already run, or while another tour is
   * active (so switching screens quickly can't stack two tours). Safe to call on every render of
   * whatever "arriving at this screen" means for the caller (see DashboardPage's lens effect). */
  requestTour: (screenKey: CoachScreenKey) => void
  /** TopNav's "Replay tour" button - clears every screen's seen flag and immediately restarts
   * `screenKey`'s tour, bypassing the seen check (matches the prototype's own resetTips). */
  replay: (screenKey: CoachScreenKey) => void
}

const CoachTourContext = createContext<CoachTourContextValue | null>(null)

export function useCoachTour() {
  const ctx = useContext(CoachTourContext)
  if (!ctx) throw new Error('useCoachTour must be used within CoachTourProvider')
  return ctx
}

/** Wraps the signed-in app (see App.tsx) - owns which tour (if any) is currently showing, and the
 * per-user "already seen" flags (see useCoachMarksSeen) that gate first-visit auto-start. */
export function CoachTourProvider({ children }: { children: ReactNode }) {
  const { seen, markSeen, resetSeen } = useCoachMarksSeen()
  const [active, setActive] = useState<ActiveTour | null>(null)
  const pendingRef = useRef<number | null>(null)

  const clearPending = () => {
    if (pendingRef.current != null) {
      window.clearTimeout(pendingRef.current)
      pendingRef.current = null
    }
  }
  useEffect(() => clearPending, [])

  const begin = useCallback((screenKey: CoachScreenKey) => {
    const steps = resolveSteps(screenKey)
    if (steps.length === 0) return
    setActive({ screenKey, seenKey: COACH_SEEN_KEY[screenKey], steps, index: 0 })
  }, [])

  const requestTour = useCallback(
    (screenKey: CoachScreenKey) => {
      if (seen.has(COACH_SEEN_KEY[screenKey]) || active || pendingRef.current != null) return
      const delay = COACH_SCREEN_FAMILY[screenKey] === 'cards' ? SECTION_START_DELAY_MS : SCREEN_START_DELAY_MS
      pendingRef.current = window.setTimeout(() => {
        pendingRef.current = null
        begin(screenKey)
      }, delay)
    },
    [seen, active, begin],
  )

  const replay = useCallback(
    (screenKey: CoachScreenKey) => {
      clearPending()
      setActive(null)
      resetSeen()
      // A short delay (not begin() inline) so the just-cleared overlay actually unmounts first -
      // same screenKey can otherwise re-resolve the same, still-mounted steps mid-transition.
      pendingRef.current = window.setTimeout(() => {
        pendingRef.current = null
        begin(screenKey)
      }, 50)
    },
    [begin, resetSeen],
  )

  const endActive = useCallback(() => {
    setActive((prev) => {
      if (prev) markSeen(prev.seenKey)
      return null
    })
  }, [markSeen])

  const nextStep = useCallback(() => {
    setActive((prev) => {
      if (!prev) return prev
      if (prev.index >= prev.steps.length - 1) {
        markSeen(prev.seenKey)
        return null
      }
      return { ...prev, index: prev.index + 1 }
    })
  }, [markSeen])

  useEffect(() => {
    if (!active) return
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') endActive()
      if (e.key === 'Enter' || e.key === 'ArrowRight') nextStep()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [active, endActive, nextStep])

  const value = useMemo(() => ({ requestTour, replay }), [requestTour, replay])

  return (
    <CoachTourContext.Provider value={value}>
      {children}
      {active && <CoachTourOverlay tour={active} onNext={nextStep} onSkip={endActive} />}
    </CoachTourContext.Provider>
  )
}

function CoachTourOverlay({ tour, onNext, onSkip }: { tour: ActiveTour; onNext: () => void; onSkip: () => void }) {
  const step = tour.steps[tour.index]
  const touch = useMemo(() => window.matchMedia('(hover: none)').matches, [])
  const [rect, setRect] = useState<DOMRect | null>(null)
  const tipRef = useRef<HTMLDivElement>(null)
  // Measured (not read from the ref during render, which the react-hooks/refs lint rule disallows)
  // so top/left below can position against the tooltip's actual height instead of a guess - falls
  // back to a reasonable guess for the first paint, before this effect has run even once.
  const [tipHeight, setTipHeight] = useState(140)
  useLayoutEffect(() => {
    if (tipRef.current) setTipHeight(tipRef.current.offsetHeight)
  })

  // One combined effect, not two: live scroll/resize tracking must not attach until the step's own
  // programmatic scrollIntoView has actually settled, or the two fight each other - the live
  // listener re-measures on every tick of the native smooth-scroll animation, each one racing this
  // component's own 300ms top/left transition toward a still-moving target, which reads as the
  // tooltip lurching into place twice (once chasing the scroll, once snapping to the final rect).
  useEffect(() => {
    let cancelled = false
    let settleTimeout: number | null = null
    const reposition = () => {
      if (!cancelled && document.body.contains(step.el)) setRect(step.el.getBoundingClientRect())
    }
    const attachLiveTracking = () => {
      window.addEventListener('resize', reposition)
      window.addEventListener('scroll', reposition, true)
    }
    const r0 = step.el.getBoundingClientRect()
    // Matches the prototype's own needScroll check (near the top/bottom chrome, not just off-screen).
    const needsScroll = r0.top < 80 || r0.bottom > window.innerHeight - 200
    if (needsScroll) {
      // Hide the previous step's spotlight/tooltip for the duration of the scroll instead of letting
      // it sit frozen (fixed positioning) at its old on-screen spot while the page scrolls behind it.
      // Deferred (not called inline here) - setState synchronously inside an effect body risks a
      // cascading render.
      window.setTimeout(() => {
        if (!cancelled) setRect(null)
      }, 0)
      step.el.scrollIntoView({ block: 'center', behavior: 'smooth' })
      settleTimeout = window.setTimeout(() => {
        reposition()
        attachLiveTracking()
      }, 420)
    } else {
      reposition()
      attachLiveTracking()
    }
    return () => {
      cancelled = true
      if (settleTimeout != null) window.clearTimeout(settleTimeout)
      window.removeEventListener('resize', reposition)
      window.removeEventListener('scroll', reposition, true)
    }
  }, [step])

  if (!rect) return null

  const pad = 6
  const tipWidth = 300
  const gap = 16
  const th = tipHeight
  const below = rect.bottom + pad + gap + th < window.innerHeight - 8 || rect.top < th + gap + 20
  const top = below ? rect.bottom + pad + gap : rect.top - pad - gap - th
  const cx = rect.left + rect.width / 2
  const left = Math.max(12, Math.min(cx - tipWidth / 2, window.innerWidth - tipWidth - 12))
  const arrowLeft = Math.max(16, Math.min(cx - left - 7, tipWidth - 30))

  return createPortal(
    <div className="fixed inset-0 z-[200]" role="presentation">
      <div
        className="pointer-events-none fixed rounded-[10px] transition-all duration-300 ease-out"
        style={{
          left: rect.left - pad,
          top: rect.top - pad,
          width: rect.width + pad * 2,
          height: rect.height + pad * 2,
          boxShadow: '0 0 0 9999px rgba(17,24,20,.55)',
        }}
      >
        <span className="motion-reduce:animate-none absolute -inset-1 animate-ping rounded-[13px] border-2 border-emerald-400" />
      </div>
      <div
        ref={tipRef}
        role="dialog"
        aria-live="polite"
        className="fixed w-[300px] max-w-[calc(100vw-24px)] rounded-2xl bg-card p-4 pb-3.5 shadow-2xl transition-[top,left] duration-300"
        style={{ top, left }}
      >
        <span
          className="absolute size-3.5 rotate-45 rounded-sm bg-card"
          style={{ left: arrowLeft, ...(below ? { top: -7 } : { bottom: -7 }) }}
        />
        <div className="text-[11.5px] font-bold tracking-wide text-emerald-600 uppercase">
          Tip {tour.index + 1} of {tour.steps.length}
        </div>
        <h4 className="mt-1 mb-1 text-base font-semibold text-foreground">{step.title}</h4>
        <p className="text-sm text-muted-foreground">{(touch && step.textTouch) || step.text}</p>
        <div className="mt-3.5 flex items-center justify-between gap-2">
          <button type="button" onClick={onSkip} className="text-muted-foreground text-xs underline">
            Skip tips
          </button>
          <div className="flex gap-1">
            {tour.steps.map((_, i) => (
              <span key={i} className={cn('size-1.5 rounded-full', i === tour.index ? 'bg-emerald-600' : 'bg-muted-foreground/30')} />
            ))}
          </div>
          <button
            type="button"
            onClick={onNext}
            className="rounded-md bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-emerald-700"
          >
            {tour.index === tour.steps.length - 1 ? 'Got it' : 'Next'}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
