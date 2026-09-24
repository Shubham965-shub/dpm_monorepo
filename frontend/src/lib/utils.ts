export { cn } from "cn"

/** "1 KPI" vs "3 KPIs" - `plural` defaults to `${singular}s`, only needed when that's not correct. */
export function pluralize(count: number, singular: string, plural: string = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`
}

