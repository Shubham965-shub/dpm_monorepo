import { GRAINS } from '@/lib/kpiSchemas'
import { useSiteOptions } from '@/hooks/useSiteOptions'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'

export function KpiFilterBar({
  value,
  onChange,
}: {
  value: GlobalKpiFilters
  onChange: (value: GlobalKpiFilters) => void
}) {
  const siteOptionsQuery = useSiteOptions()
  // The current site may not be in the live list yet (still loading, or a stale value from before
  // a site was retired) - keep it selectable so the select's value never goes out of sync with `value.site`.
  const siteOptions = siteOptionsQuery.data?.includes(value.site)
    ? siteOptionsQuery.data
    : [value.site, ...(siteOptionsQuery.data ?? [])]

  return (
    <div className="border-border bg-card sticky top-0 z-10 flex flex-wrap items-end gap-4 border-b p-4">
      <label className="flex flex-col gap-1">
        <span className="text-muted-foreground text-[11px] font-medium">site</span>
        <select
          className="rounded-md border border-input bg-background px-2 py-1 text-sm"
          value={value.site}
          onChange={(e) => onChange({ ...value, site: e.target.value })}
        >
          {siteOptions.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-muted-foreground text-[11px] font-medium">grain</span>
        <select
          className="rounded-md border border-input bg-background px-2 py-1 text-sm"
          value={value.grain}
          onChange={(e) => onChange({ ...value, grain: e.target.value })}
        >
          {GRAINS.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-muted-foreground text-[11px] font-medium">from</span>
        <input
          type="date"
          className="rounded-md border border-input bg-background px-2 py-1 text-sm"
          value={value.from}
          onChange={(e) => onChange({ ...value, from: e.target.value })}
        />
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-muted-foreground text-[11px] font-medium">to</span>
        <input
          type="date"
          className="rounded-md border border-input bg-background px-2 py-1 text-sm"
          value={value.to}
          onChange={(e) => onChange({ ...value, to: e.target.value })}
        />
      </label>
      <p className="text-muted-foreground max-w-xs text-[11px]">
        Applies to every card below. Each card also has its own KPI-specific filter (site/grain/from/to are shared, matching every backend endpoint's KpiParams.DateRange base).
      </p>
    </div>
  )
}
