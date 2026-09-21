export interface MonthColumn {
  /** "YYYY-MM", matches the first 7 chars of a KpiCardResponse.periodStart. */
  key: string
  label: string
}

function toIsoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Trailing 3 calendar months ending this month, monthly grain - matches the wireframe's 3-column layout. */
export function getTrailing3MonthRange(today: Date = new Date()) {
  const months: MonthColumn[] = []
  for (let i = 2; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1)
    months.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      label: d.toLocaleString('en-US', { month: 'short' }),
    })
  }
  const from = new Date(today.getFullYear(), today.getMonth() - 2, 1)
  return { months, from: toIsoDate(from), to: toIsoDate(today) }
}
