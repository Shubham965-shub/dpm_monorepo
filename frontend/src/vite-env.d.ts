/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  /** PostHog project API key (self-hosted). Analytics stay disabled when unset - see lib/analytics.ts. */
  readonly VITE_POSTHOG_KEY?: string
  /** PostHog self-hosted instance URL, e.g. https://posthog.internal.example.com */
  readonly VITE_POSTHOG_HOST?: string
  /** App name stamped on every PostHog event as the `app` super property (defaults to 'helios'). */
  readonly VITE_POSTHOG_APP?: string
  /** Feedback widget service base URL. The launcher hides itself when unset - see FeedbackLauncher.tsx. */
  readonly VITE_FEEDBACK_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
