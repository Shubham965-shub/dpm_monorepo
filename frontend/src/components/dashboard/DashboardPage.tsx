import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { TopNav, SubNav, type SubNavView } from './TopNav'
import { FilterBar, type DashboardFilters } from './FilterBar'
import { OverallHeatmapDemo } from './OverallHeatmapDemo'
import { QcScoreCard } from './QcScoreCard'
import { PositiveSignals } from './PositiveSignals'
import { NeedsAttentionPanel } from './NeedsAttentionPanel'
import { SearchKpiBar } from './SearchKpiBar'
import { SectionSummaryList } from './SectionSummaryList'
import { PerformanceWorkInProgress } from './PerformanceWorkInProgress'
import { KPI_DOMAINS } from '@/lib/kpiConfig'
import { pluralize } from '@/lib/utils'

export function DashboardPage() {
  const { user, logout } = useAuth()
  const [view, setView] = useState<SubNavView>('dashboard')
  const [filters, setFilters] = useState<DashboardFilters>({ section: 'all', grain: 'monthly' })
  const [searchQuery, setSearchQuery] = useState('')
  if (!user) return null

  const totalKpis = KPI_DOMAINS.reduce((sum, d) => sum + d.kpis.length, 0)

  return (
    <div className="min-h-svh bg-muted/40">
      <TopNav user={user} onLogout={() => void logout()} />
      <SubNav view={view} onViewChange={setView} />

      {view === 'performance' ? (
        <PerformanceWorkInProgress />
      ) : (
        <>
          <FilterBar value={filters} onChange={setFilters} />

          <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
            <div>
              <h1 className="text-xl font-semibold">KRSG Operations</h1>
              <p className="text-sm text-muted-foreground">
                Composite plant performance · {pluralize(totalKpis, 'KPI')} across {KPI_DOMAINS.length} sections
              </p>
            </div>

            {/* Heatmap:sidebar was minmax(0,1fr)_320px (~2.95:1). 3fr:2fr (1.5:1) narrowed the
            heatmap too much - 2fr:1fr (2:1) is a smaller step down from the original, still giving
            the sidebar more room than the fixed 320px did. */}
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
              <OverallHeatmapDemo section={filters.section} />
              <div className="flex flex-col gap-4">
                <QcScoreCard />
                <PositiveSignals />
              </div>
            </div>

            <NeedsAttentionPanel section={filters.section} />

            <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />

            <SectionSummaryList
              site="KRSG"
              section={filters.section}
              grain={filters.grain}
              searchQuery={searchQuery}
            />
          </main>
        </>

)}
    </div>
  )
}
