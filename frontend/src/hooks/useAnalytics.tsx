import { PostHogProvider } from 'posthog-js/react'
import { useEffect, useRef, type ReactNode } from 'react'
import { initAnalytics, isAnalyticsEnabled, posthog } from '@/lib/analytics'
import { useAuth } from './useAuth'

/**
 * Wires the app to PostHog. When analytics are disabled (no key/host configured) this is a
 * transparent passthrough, so nothing changes for local dev.
 *
 * Nothing is initialized here. PostHogProvider given a `client` never calls init() itself, so the
 * instance handed to it stays dormant until the identity bridge starts it - which is what keeps a
 * signed-out visitor off the network entirely.
 *
 * Must render inside AuthProvider - the identity bridge reads the auth session to identify the
 * signed-in user and reset on logout.
 */
export function AnalyticsProvider({ children }: { children: ReactNode }) {
  if (!isAnalyticsEnabled) {
    return <>{children}</>
  }

  return (
    <PostHogProvider client={posthog}>
      <AnalyticsIdentityBridge />
      {children}
    </PostHogProvider>
  )
}

/**
 * The tracking gate, and the only thing that ever starts PostHog.
 *
 * init() is deferred to the first confirmed session rather than run at boot, because initializing
 * does more than arm the capture path: it fetches the project's remote config and writes PostHog's
 * cookies. Opting out suppresses *events*, not that - so a visitor who never signs in would still
 * show up in the network tab. Deferring is the only way there is genuinely no PostHog before
 * sign-in.
 *
 * On a confirmed session this starts the client, opts in and identifies the user; on logout or
 * session expiry it resets and opts back out. Renders nothing.
 */
function AnalyticsIdentityBridge() {
  const { user } = useAuth()
  // Email is the stable distinct_id, consistent across whatever else might share this project.
  const identifiedEmail = useRef<string | null>(null)

  useEffect(() => {
    if (user?.email) {
      if (identifiedEmail.current !== user.email) {
        // The first network contact with PostHog in the page's life. Idempotent, so a second
        // sign-in of a session is a no-op here.
        initAnalytics()

        // Order matters: reset() clears consent back to the opted-out default, so it must never
        // run after this. It only runs on the logout branch below, which is why opting in here is
        // safe.
        //
        // captureEventName: false suppresses PostHog's $opt_in event - consent is implied by
        // signing in, not a thing the user did.
        posthog.opt_in_capturing({ captureEventName: false })

        posthog.identify(user.email, {
          email: user.email,
          name: user.fullName,
          department: user.departments.length > 0 ? user.departments.join(', ') : undefined,
          title: user.title ?? undefined,
          roles: user.roles,
        })
        // The pageview init() would normally have captured was suppressed by the opt-out, and the
        // next one only arrives on a route change. Without this, a signed-in user reloading the
        // page is never recorded as having viewed it.
        posthog.capture('$pageview')

        identifiedEmail.current = user.email
      }
      return
    }

    // A stored session is confirmed against the backend (see useAuth's restoreSession) before it
    // counts as a real user here, so a stale one never identifies.
    if (identifiedEmail.current !== null) {
      posthog.reset()
      // reset() already returns consent to the opted-out default; saying so explicitly leaves no
      // doubt about the state capture is left in.
      posthog.opt_out_capturing()
      identifiedEmail.current = null
    }
  }, [user])

  return null
}
