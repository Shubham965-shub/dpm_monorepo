import { useDataFreshness } from '@/hooks/useDataFreshness'
import { formatLastUpdatedIst } from '@/lib/format'

/** "Last updated on 12 Oct 2026 at 7:20 AM IST" - the upstream pipeline's own last successful run
 * (see useDataFreshness), shown once in SubNav so it's visible across every dashboard view (QC, QA,
 * Performance) rather than duplicated per page. Renders nothing while loading, on a fetch error, or
 * before the pipeline's run-log table has ever recorded a successful run - there's no good partial
 * state for "we don't know yet" here, so it's silent rather than showing a placeholder. */
export function DataFreshnessLabel() {
  const { data } = useDataFreshness()
  const formatted = data?.lastUpdatedIst ? formatLastUpdatedIst(data.lastUpdatedIst) : null
  if (!formatted) return null
  return <span className="text-muted-foreground text-xs whitespace-nowrap">Last updated on {formatted}</span>
}
