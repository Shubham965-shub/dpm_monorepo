import { FeedbackButton } from '@strides-pharma-science-ltd/feedback-widget'
import { MessageSquare } from 'lucide-react'
import { useAuth } from '@/hooks/useAuth'
import { useFeedbackToken } from '@/hooks/useFeedbackToken'

// Fixed, not env-driven: sets "Application Name" and the voting catalog on the feedback service's
// own side (registered there as "helios") - unlike apiBaseUrl, this doesn't vary by environment.
const APP_KEY = 'helios'
const API_BASE_URL = import.meta.env.VITE_FEEDBACK_API_BASE_URL

/**
 * Opens the feedback modal from the header (see TopNav.tsx).
 *
 * The widget's own floating pill is deliberately not used - its trigger is replaced with our own
 * button (`children`), matching the theme toggle beside it instead of hovering over every page in
 * the widget's styling.
 *
 * Authentication is a separate, purpose-built token (see backend FeedbackJwtService's own doc
 * comment on why it isn't this app's own session access token), fetched and kept fresh by
 * useFeedbackToken - the widget's getToken prop is synchronous, so the token has to already be in
 * hand by the time it's asked for.
 *
 * Mounted only inside the signed-in header, so there is always a user to send.
 */
export function FeedbackLauncher() {
  const { user } = useAuth()
  const getToken = useFeedbackToken()

  // An environment without the feedback service configured shows no control at all. The
  // alternative - a button that opens a modal which cannot submit - is worse than the feature
  // being absent.
  if (!API_BASE_URL) return null

  return (
    <FeedbackButton
      appKey={APP_KEY}
      apiBaseUrl={API_BASE_URL}
      getToken={getToken}
      // Cosmetic only: it fills the modal's read-only "Submitting as ..." line. The identity
      // stored against the feedback is read from the token server-side, so nothing here can
      // misattribute a submission.
      user={user ? { full_name: user.fullName, display_name: user.displayName, department: user.departments.join(', ') || null } : null}
    >
      <button
        type="button"
        title="Send feedback"
        className="flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-sm font-medium text-white hover:bg-white/20"
      >
        <MessageSquare className="size-4" />
        Feedback
      </button>
    </FeedbackButton>
  )
}
