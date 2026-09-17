import { useState } from 'react'
import { KPI_DOMAINS, DEFAULT_SITE } from '@/lib/kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'
import { KpiFilterBar } from './KpiFilterBar'
import { KpiChartCard } from './KpiChartCard'

const DEFAULT_FILTERS: GlobalKpiFilters = {
  site: DEFAULT_SITE,
  grain: 'monthly',
  from: '2025-09-01',
  to: '2026-09-17',
}

export function KpiValidationDashboard() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS)

  return (
    <div className="flex min-h-svh flex-col">
      <KpiFilterBar value={filters} onChange={setFilters} />
      <div className="flex flex-col gap-8 p-4">
        {KPI_DOMAINS.map((domain) => (
          <section key={domain.key} className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold">{domain.label}</h2>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              {domain.kpis.map((kpi) => (
                <KpiChartCard key={kpi.id} domain={domain} kpi={kpi} globalFilters={filters} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  )
}
