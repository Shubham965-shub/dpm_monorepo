// Standard ISO 8601 week numbering (week 1 = the week containing the year's first Thursday, weeks
// start Monday) - not an invented scheme, so no product decision needed for the numbering itself.

function isoWeekInfo(date: Date): { year: number; week: number } {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = d.getUTCDay() || 7
  d.setUTCDate(d.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
  const week = Math.ceil(((d.getTime() - yearStart.getTime()) / 86400000 + 1) / 7)
  return { year: d.getUTCFullYear(), week }
}

export function startOfIsoWeek(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay() || 7 // Mon=1 .. Sun=7
  d.setDate(d.getDate() - day + 1)
  return d
}

export function formatIsoWeekTick(periodStart: string): string {
  const date = new Date(`${periodStart}T00:00:00`)
  return `W${isoWeekInfo(date).week}`
}

/** "Week 38, 2026 (Sep 14 - Sep 20)" - the full context a bare "W38" axis tick doesn't have room for. */
export function formatIsoWeekRangeLabel(periodStart: string): string {
  const date = new Date(`${periodStart}T00:00:00`)
  const { year, week } = isoWeekInfo(date)
  const start = startOfIsoWeek(date)
  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  const fmt = (d: Date) => d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  return `Week ${week}, ${year} (${fmt(start)} - ${fmt(end)})`
}
