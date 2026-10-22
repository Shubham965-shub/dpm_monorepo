import posthog from 'posthog-js'

const KEY = import.meta.env.VITE_POSTHOG_KEY
const HOST = import.meta.env.VITE_POSTHOG_HOST
// Stamps every event so this app's data is filterable within a self-hosted project potentially
// shared across other Strides apps. Override via env if desired.
const APP = import.meta.env.VITE_POSTHOG_APP ?? 'helios'

/**
 * Analytics are opt-in: without both a project key and a self-hosted host the client is never
 * initialized and every export below is a no-op - local dev and any environment that omits the
 * vars stays completely free of PostHog.
 *
 * Signed-in users only. The client starts opted out and captures nothing - no pageviews, no
 * autocapture, no session recording - until the identity bridge in useAnalytics.tsx opts in, which
 * it does only for a confirmed session. An anonymous visitor sitting on the login screen produces
 * no events at all.
 */
export const isAnalyticsEnabled = Boolean(KEY && HOST)

let initialized = false

/** Initialize the PostHog client exactly once. Safe to call repeatedly. */
export function initAnalytics(): void {
  if (initialized || !isAnalyticsEnabled) {
    return
  }
  initialized = true

  posthog.init(KEY as string, {
    api_host: HOST,
    // The tracking gate. Everything below is configured but dormant: nothing is captured until
    // opt_in_capturing(), which only happens once the auth session is confirmed. Signing out calls
    // reset(), which clears consent back to this default - so the gate closes again without extra
    // bookkeeping.
    opt_out_capturing_by_default: true,
    // SPA: capture a $pageview on every history change, not just first load.
    capture_pageview: 'history_change',
    capture_pageleave: true,
    autocapture: true,
    // Only create person profiles for identified (signed-in) users.
    person_profiles: 'identified_only',
    persistence: 'localStorage+cookie',
    // Session replay. Also enable it in the self-hosted project settings, else no recordings are
    // produced regardless of this flag.
    // Privacy: mask every input value (typed text stays hidden), but keep the rest of the page
    // visible so recordings are actually useful. Mark any sensitive element with the `ph-mask`
    // class to hide it too.
    disable_session_recording: false,
    session_recording: {
      maskAllInputs: true,
    },
    loaded: (ph) => {
      if (import.meta.env.DEV) {
        ph.debug(false)
      }
    },
  })

  // Super property attached to every captured event as `app`, so this app's data is filterable
  // within a shared self-hosted project.
  posthog.register({ app: APP })
}

export { posthog }
