// Config for the dashboard's guided-tour coach marks (see CoachMarks.tsx) - ported from the
// "Helios — coach marks" prototype's own TIPS/TIPSET, adapted to this app's real screens and
// components instead of its demo markup. Kept as data (not inline in the engine) so wording can
// change without touching the engine itself, matching the prototype's own stated intent.

/** One tour per first-time screen visit (cxo/leadership/analyst/supervisor - matches the
 * prototype's home/ops split, its own "ops" family shared verbatim by both roles), plus two
 * cross-screen tours triggered by a first-time interaction rather than navigation: 'cards' (the
 * first KPI section ever expanded) and 'drill' (the first KPI detail view ever opened). Each is its
 * own one-time flag (see useCoachMarksSeen), independent of the others. */
export type CoachScreenKey = 'cxo' | 'leadership' | 'analyst' | 'supervisor' | 'cards' | 'drill'

type CoachTipFamily = 'home' | 'ops' | 'cards' | 'drill'

export const COACH_SCREEN_FAMILY: Record<CoachScreenKey, CoachTipFamily> = {
  cxo: 'home',
  leadership: 'home',
  analyst: 'ops',
  supervisor: 'ops',
  cards: 'cards',
  drill: 'drill',
}

/** Which "seen" flag a screen counts against (see useCoachMarksSeen) - every persona screen
 * (cxo/leadership/analyst/supervisor) shares one 'persona' flag instead of 4 independent ones, so
 * switching persona (ADMIN's own preview toggle, or a role change) never replays the tour just
 * because the lens changed - only a user who's never seen any persona tour yet (or an explicit
 * Replay) does. 'cards'/'drill' stay their own independent flags, since they're cross-cutting
 * interactions triggered by something the user does, not a screen they navigate to. */
export const COACH_SEEN_KEY: Record<CoachScreenKey, string> = {
  cxo: 'persona',
  leadership: 'persona',
  analyst: 'persona',
  supervisor: 'persona',
  cards: 'cards',
  drill: 'drill',
}

export interface CoachTip {
  /** Resolved via document.querySelector - the first match in DOM order, same as the prototype's
   * own engine (e.g. its plain `.star`/`.chev` selectors naturally land on the first section row
   * without needing a unique id per row). */
  selector: string
  title: string
  text: string
  /** Shown instead of `text` on a touch device (no hover) - matches the prototype's own
   * hover-vs-tap wording for the 2 tips that actually differ when read aloud this way. */
  textTouch?: string
}

export const COACH_TIPS: Record<CoachTipFamily, CoachTip[]> = {
  home: [
    {
      selector: '.coach-heatmap-cell',
      title: 'Every number is a KPI',
      text: 'Hover a number to see which KPI it is. Click it to open that KPI.',
      textTouch: 'Tap a number to open that KPI.',
    },
    {
      selector: '.coach-heatmap-controls',
      title: 'Focus on what needs attention',
      text: 'Show only KPIs past their action limit, or look back over more or fewer periods.',
    },
    {
      selector: '.coach-section-star',
      title: 'Keep a section at the top',
      text: 'Star a section and it stays first in the list.',
    },
  ],
  ops: [
    {
      selector: '.coach-section-star',
      title: 'Keep a section at the top',
      text: 'Star a section and it stays first in the list.',
    },
    {
      selector: '.coach-section-chevron',
      title: 'Open a section',
      text: 'Open a section to see each of its KPIs with its own trend chart.',
    },
  ],
  cards: [
    {
      selector: '.coach-kpi-filter',
      title: 'Filter one chart',
      text: "Filter just this KPI's chart, without changing the others.",
    },
    {
      selector: '.coach-kpi-expand',
      title: 'Open the full view',
      text: "Open this KPI's full page: the trend, Info and the records behind it.",
    },
  ],
  drill: [
    {
      selector: '.coach-modal-info',
      title: 'How this number is made',
      text: "Open Info to see where the data comes from and exactly how it's calculated.",
    },
    {
      selector: '.coach-modal-grain',
      title: 'Change the time grouping',
      text: 'Show the trend by day, week, month, quarter or year.',
    },
    {
      selector: '.coach-modal-chart',
      title: "See what's behind each point",
      text: "Hover the chart to see that period's exact value.",
      textTouch: "Tap the chart to see that period's exact value.",
    },
  ],
}

export function coachTipsFor(screenKey: CoachScreenKey): CoachTip[] {
  return COACH_TIPS[COACH_SCREEN_FAMILY[screenKey]]
}
