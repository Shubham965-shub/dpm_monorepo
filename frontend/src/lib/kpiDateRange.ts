import type { Grain } from './kpiSchemas'
import { startOfIsoWeek, formatIsoWeekTick } from './isoWeek'
import { formatMonthLabel, formatPeriodTick, withYearIfNotCurrent } from './periodFormat'

export interface MonthColumn {
  /** "YYYY-MM", matches the first 7 chars of a KpiCardResponse.periodStart. */
  key: string
  label: string
  /** This month's own [from, to] bounds, capped at the overall range's `to` for the current
   * (possibly partial) month - lets a whole-range KPI (chartType: 'bar', which sums over its
   * entire queried window into one row per category rather than one row per period) be queried
   * one month at a time instead of only ever showing a single combined total. */
  from: string
  to: string
}

export interface WeekColumn {
  /** The week's own Monday, "YYYY-MM-DD" - matches a weekly-grain row's periodStart exactly (see
   * periodFormat.ts's "a week starts on its Monday"), unlike MonthColumn's key which is only a
   * "YYYY-MM" prefix match. */
  key: string
  /** "W38" - see isoWeek.ts's formatIsoWeekTick. */
  label: string
  /** This week's own [from, to] bounds, capped at the overall range's `to` for the current
   * (possibly partial) week - same reasoning as MonthColumn's. */
  from: string
  to: string
}

export function toIsoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// Trailing window per grain, ending today (inclusive of the current, possibly-partial period) -
// per-product decision: daily 30 days, weekly 12 weeks, monthly 6 months, quarterly 4 quarters,
// yearly 3 years.
export function getGrainDateRange(grain: Grain, today: Date = new Date()): { from: string; to: string } {
  const to = toIsoDate(today)
  switch (grain) {
    case 'daily': {
      const from = new Date(today)
      from.setDate(from.getDate() - 29)
      return { from: toIsoDate(from), to }
    }
    case 'weekly': {
      const from = new Date(today)
      from.setDate(from.getDate() - (12 * 7 - 1))
      return { from: toIsoDate(from), to }
    }
    case 'monthly': {
      const from = new Date(today.getFullYear(), today.getMonth() - 5, 1)
      return { from: toIsoDate(from), to }
    }
    case 'quarterly': {
      const quarterStartMonth = Math.floor(today.getMonth() / 3) * 3
      const from = new Date(today.getFullYear(), quarterStartMonth - 9, 1)
      return { from: toIsoDate(from), to }
    }
    case 'yearly': {
      const from = new Date(today.getFullYear() - 2, 0, 1)
      return { from: toIsoDate(from), to }
    }
  }
}

// Independent of the Overall Heatmap's own user-adjustable 3/4/5-month pill (OverallHeatmapDemo) -
// SectionSummaryList's sidebar tier counts (and DashboardPage's page-level severity badge summary,
// which shares that same query) only ever need the latest month, so this window stays fixed.
export const SIDEBAR_HEATMAP_MONTH_COUNT = 3

/** Trailing `monthCount` calendar months ending this month, monthly grain - e.g. the Overall
 * Heatmap's 3M/4M/5M range pill. */
export function getTrailingMonthRange(monthCount: number, today: Date = new Date()) {
  const overallTo = toIsoDate(today)
  const months: MonthColumn[] = []
  for (let i = monthCount - 1; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1)
    const lastDayOfMonth = new Date(d.getFullYear(), d.getMonth() + 1, 0)
    months.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      label: formatMonthLabel(d, today),
      from: toIsoDate(d),
      to: lastDayOfMonth < today ? toIsoDate(lastDayOfMonth) : overallTo,
    })
  }
  const from = new Date(today.getFullYear(), today.getMonth() - (monthCount - 1), 1)
  return { months, from: toIsoDate(from), to: overallTo }
}

/** Trailing `weekCount` ISO weeks (Monday-start) ending this week - the Leadership Overall
 * Heatmap's 3W/4W/5W range pill (see OverallHeatmapDemo). Mirrors getTrailingMonthRange's shape
 * exactly, one ISO week at a time instead of one calendar month. */
export function getTrailingWeekRange(weekCount: number, today: Date = new Date()) {
  const overallTo = toIsoDate(today)
  const currentWeekStart = startOfIsoWeek(today)
  const weeks: WeekColumn[] = []
  for (let i = weekCount - 1; i >= 0; i--) {
    const start = new Date(currentWeekStart)
    start.setDate(start.getDate() - i * 7)
    const end = new Date(start)
    end.setDate(start.getDate() + 6)
    const startIso = toIsoDate(start)
    weeks.push({
      key: startIso,
      label: formatIsoWeekTick(startIso),
      from: startIso,
      to: end < today ? toIsoDate(end) : overallTo,
    })
  }
  const from = weeks[0]?.from ?? overallTo
  return { weeks, from, to: overallTo }
}

/** A plain trailing-day-count window ending today (e.g. Leadership's Timeline: T-7/T-30/T-90/T-180
 * filter - see LeadershipFilterBar) - unlike getGrainDateRange's per-grain fixed windows, the day
 * count here is the thing being chosen directly. */
export function trailingDayRange(days: number, today: Date = new Date()): { from: string; to: string } {
  const from = new Date(today)
  from.setDate(from.getDate() - (days - 1))
  return { from: toIsoDate(from), to: toIsoDate(today) }
}

export interface QuarterColumn {
  /** "YYYY-Q#", matches a quarterly-grain row's periodStart (the quarter's first month) once
   * reduced to the same shape - see useHeatmapData's resolveWindow. */
  key: string
  /** "Q1", short year appended only when it isn't `today`'s year - mirrors MonthColumn's label. */
  label: string
  from: string
  to: string
}

/** Trailing `quarterCount` calendar quarters ending this quarter - the Overall Heatmap's 3Q/4Q/5Q
 * range pill (see OverallHeatmapDemo). Mirrors getTrailingMonthRange's shape, one quarter at a time. */
export function getTrailingQuarterRange(quarterCount: number, today: Date = new Date()) {
  const overallTo = toIsoDate(today)
  const currentQuarterStartMonth = Math.floor(today.getMonth() / 3) * 3
  const quarters: QuarterColumn[] = []
  for (let i = quarterCount - 1; i >= 0; i--) {
    const d = new Date(today.getFullYear(), currentQuarterStartMonth - i * 3, 1)
    const quarterIndex = Math.floor(d.getMonth() / 3) + 1
    const lastDayOfQuarter = new Date(d.getFullYear(), d.getMonth() + 3, 0)
    quarters.push({
      key: `${d.getFullYear()}-Q${quarterIndex}`,
      label: withYearIfNotCurrent(`Q${quarterIndex}`, d, today),
      from: toIsoDate(d),
      to: lastDayOfQuarter < today ? toIsoDate(lastDayOfQuarter) : overallTo,
    })
  }
  const from = quarters[0]?.from ?? overallTo
  return { quarters, from, to: overallTo }
}

export interface YearColumn {
  /** "YYYY", matches a yearly-grain row's periodStart's first 4 characters. */
  key: string
  label: string
  from: string
  to: string
}

/** Trailing `yearCount` calendar years ending this year - the Overall Heatmap's 2Y/3Y range pill. */
export function getTrailingYearRange(yearCount: number, today: Date = new Date()) {
  const overallTo = toIsoDate(today)
  const years: YearColumn[] = []
  for (let i = yearCount - 1; i >= 0; i--) {
    const year = today.getFullYear() - i
    const lastDayOfYear = new Date(year, 11, 31)
    years.push({
      key: String(year),
      label: String(year),
      from: toIsoDate(new Date(year, 0, 1)),
      to: lastDayOfYear < today ? toIsoDate(lastDayOfYear) : overallTo,
    })
  }
  const from = years[0]?.from ?? overallTo
  return { years, from, to: overallTo }
}

export interface DayColumn {
  /** "YYYY-MM-DD", matches a daily-grain row's periodStart exactly. */
  key: string
  label: string
  from: string
  to: string
}

/** Trailing `dayCount` calendar days ending today - the Overall Heatmap's fixed daily window (no
 * user-adjustable pill for daily - see OverallHeatmapDemo). */
export function getTrailingDayRange(dayCount: number, today: Date = new Date()) {
  const overallTo = toIsoDate(today)
  const days: DayColumn[] = []
  for (let i = dayCount - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const iso = toIsoDate(d)
    days.push({ key: iso, label: formatPeriodTick(iso, 'daily', today), from: iso, to: iso })
  }
  const from = days[0]?.from ?? overallTo
  return { days, from, to: overallTo }
}
