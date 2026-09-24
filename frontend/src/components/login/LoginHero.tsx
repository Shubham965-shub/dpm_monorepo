import { useEffect, useState } from 'react'
import { DEPARTMENTS, renderPlateSvg } from './loginDepartments'

const ROTATION_INTERVAL_MS = 6000

/**
 * Cycles the department plate artwork every ROTATION_INTERVAL_MS, matching the design mockup's
 * timing exactly (see dpm-login-rotating-hero.html's `tick()`). `paused` is owned by the parent
 * (LoginPage pauses on hover/focus in the sign-in card - never animate behind someone typing a
 * password) rather than by hover state here, since the sign-in card sits outside this component.
 */
export function LoginHero({ paused }: { paused: boolean }) {
  const [index, setIndex] = useState(0)
  // Doesn't change over the component's lifetime - a lazy initializer avoids re-querying matchMedia
  // every render without touching a ref during render (not allowed - see react-hooks/refs).
  const [reduceMotion] = useState(
    () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches,
  )

  useEffect(() => {
    if (paused || reduceMotion) return
    const timer = setInterval(() => {
      setIndex((i) => (i + 1) % DEPARTMENTS.length)
    }, ROTATION_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [paused, reduceMotion])

  const dept = DEPARTMENTS[index]

  return (
    <div className="dpm-login__hero">
      <div className="dpm-login__hero-copy">
        <h3>Every number on this plant, in one place.</h3>
        <p>Sign in to see how your section is performing right now — the same figures your HOD and the CXO are looking at.</p>
      </div>
      <div
        key={`${dept.id}-${index}`}
        className="dpm-login__hero-stage"
        // Static, locally-generated SVG markup (see renderPlateSvg) - not user input.
        dangerouslySetInnerHTML={{ __html: renderPlateSvg(dept) }}
      />
    </div>
  )
}
