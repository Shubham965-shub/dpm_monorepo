export { cn } from "cn"

/** "1 KPI" vs "3 KPIs" - `plural` defaults to `${singular}s`, only needed when that's not correct. */
export function pluralize(count: number, singular: string, plural: string = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`
}

/** "Ravi Shankar" -> "RS" - shared by any avatar-style initials badge (TopNav's user avatar,
 * TeamWorkloadOverview's analyst roster). */
export function initials(fullName: string): string {
  const parts = fullName.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts.at(-1)?.[0] ?? '')).toUpperCase()
}

