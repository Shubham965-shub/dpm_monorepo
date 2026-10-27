import { useMemo, useState } from 'react'
import { createFeedbackClient, FeedbackModal } from '@strides-pharma-science-ltd/feedback-widget'
import { MessageSquare } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { useAuth } from '@/hooks/useAuth'
import { useFeedbackToken } from '@/hooks/useFeedbackToken'
import { describeSubject, resolveSubject, WHOLE_DASHBOARD, withSubject, type FeedbackScope, type FeedbackSubject } from '@/lib/feedbackSubject'

// Fixed, not env-driven: sets "Application Name" and the voting catalog on the feedback service's
// own side (registered there as "helios") - unlike apiBaseUrl, this doesn't vary by environment.
const APP_KEY = 'helios'
const API_BASE_URL = import.meta.env.VITE_FEEDBACK_API_BASE_URL

const WHOLE_DASHBOARD_VALUE = ''
const selectClass = 'w-full rounded-md border border-input bg-background px-2 py-1.5 text-sm focus:ring-1 focus:ring-ring focus:outline-none'

/**
 * Opens the feedback modal from the header (see TopNav.tsx), after a short "what is this about" step: the dashboard is
 * already known, and the user can pick the module (section) and KPI they mean. Whatever is picked is stamped on the
 * feedback when it is sent (see feedbackSubject.ts), so a report never leaves it unclear which dashboard, module or KPI
 * it refers to.
 *
 * The widget's own FeedbackButton is not used - it takes no extra context - so this drives its FeedbackModal directly
 * with a client that adds the subject on submit, and its trigger is our own button, matching the theme toggle beside it.
 *
 * Authentication is a separate, purpose-built token (see backend FeedbackJwtService's own doc
 * comment on why it isn't this app's own session access token), fetched and kept fresh by
 * useFeedbackToken - the widget's getToken prop is synchronous, so the token has to already be in
 * hand by the time it's asked for.
 *
 * Mounted only inside the signed-in header, so there is always a user to send.
 */
export function FeedbackLauncher({ scope }: { scope: FeedbackScope }) {
  const { user } = useAuth()
  const getToken = useFeedbackToken()
  const [pickerOpen, setPickerOpen] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [subject, setSubject] = useState<FeedbackSubject>(WHOLE_DASHBOARD)

  const resolved = resolveSubject(scope, subject)
  const client = useMemo(() => {
    const base = createFeedbackClient({ apiBaseUrl: API_BASE_URL ?? '', getToken })
    return { ...base, submitFeedback: (payload: Parameters<typeof base.submitFeedback>[0]) => base.submitFeedback(withSubject(payload, resolved)) }
  }, [getToken, resolved])

  // An environment without the feedback service configured shows no control at all. The
  // alternative - a button that opens a modal which cannot submit - is worse than the feature
  // being absent.
  if (!API_BASE_URL) return null

  const moduleDomain = scope.domains.find((d) => d.key === resolved.module?.key)
  const kpiOptions = (moduleDomain ? [moduleDomain] : scope.domains).flatMap((d) => d.kpis)

  return (
    <div className="fb-root">
      <Popover open={pickerOpen} onOpenChange={setPickerOpen}>
        <PopoverTrigger asChild>
          <button
            type="button"
            title="Send feedback"
            className="flex items-center gap-1.5 rounded-full border border-brand/30 bg-brand/10 px-3 py-1 text-sm font-medium hover:bg-brand/20"
          >
            <MessageSquare className="size-4" />
            Feedback
          </button>
        </PopoverTrigger>
        <PopoverContent align="end" className="w-80 gap-3 p-3">
          <div>
            <p className="text-sm font-semibold">What is this feedback about?</p>
            <p className="text-muted-foreground text-xs">It is sent with your feedback so it is clear what you mean.</p>
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-muted-foreground text-[11px] font-medium">Dashboard</span>
            <span className="text-sm">{scope.dashboard}</span>
          </div>
          {scope.domains.length > 0 && (
            <>
              <label className="flex flex-col gap-1">
                <span className="text-muted-foreground text-[11px] font-medium">Module</span>
                <select
                  className={selectClass}
                  value={resolved.module?.key ?? WHOLE_DASHBOARD_VALUE}
                  onChange={(e) => setSubject({ moduleKey: e.target.value || null, kpiId: null })}
                >
                  <option value={WHOLE_DASHBOARD_VALUE}>Whole dashboard</option>
                  {scope.domains.map((d) => (
                    <option key={d.key} value={d.key}>
                      {d.label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1">
                <span className="text-muted-foreground text-[11px] font-medium">KPI</span>
                <select
                  className={selectClass}
                  value={resolved.kpi?.id ?? WHOLE_DASHBOARD_VALUE}
                  onChange={(e) => setSubject({ moduleKey: resolved.module?.key ?? null, kpiId: e.target.value || null })}
                >
                  <option value={WHOLE_DASHBOARD_VALUE}>{moduleDomain ? 'Whole module' : 'No specific KPI'}</option>
                  {kpiOptions.map((k) => (
                    <option key={k.id} value={k.id}>
                      {k.label}
                    </option>
                  ))}
                </select>
              </label>
            </>
          )}
          <p className="text-muted-foreground text-xs">Raising it on: {describeSubject(resolved)}</p>
          <Button
            size="sm"
            onClick={() => {
              setPickerOpen(false)
              setFormOpen(true)
            }}
          >
            Continue
          </Button>
        </PopoverContent>
      </Popover>
      <FeedbackModal
        open={formOpen}
        onOpenChange={setFormOpen}
        appKey={APP_KEY}
        client={client}
        // Cosmetic only: it fills the modal's read-only "Submitting as ..." line. The identity
        // stored against the feedback is read from the token server-side, so nothing here can
        // misattribute a submission.
        user={user ? { full_name: user.fullName, display_name: user.displayName, department: user.departments.join(', ') || null } : null}
      />
    </div>
  )
}
