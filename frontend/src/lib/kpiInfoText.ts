/** True when a KPI's info summary already carries its own "Source: X" line (the KPI Logic sheets write one into the text),
 * in which case the separate source-system field would repeat it. */
export function summaryHasSourceLine(summary: string | null | undefined): boolean {
  return (summary ?? '').split('\n').some((line) => line.trim().startsWith('Source:'))
}
