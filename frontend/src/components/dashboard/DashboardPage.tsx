import { useState } from 'react'
import { useAuth } from '@/hooks/useAuth'
import { TopNav, SubNav, type SubNavView } from './TopNav'
import { FilterBar } from './FilterBar'
import { OverallHeatmap } from './OverallHeatmap'
import { QcScoreCard } from './QcScoreCard'
import { PositiveSignals } from './PositiveSignals'
import { NeedsAttentionPanel } from './NeedsAttentionPanel'
import { SectionSummaryList } from './SectionSummaryList'
import { KpiValidationDashboard } from '@/components/kpi/KpiValidationDashboard'
import { KPI_DOMAINS } from '@/lib/kpiConfig'

export function DashboardPage() {
  const { user, logout } = useAuth()
  const [view, setView] = useState<SubNavView>('dashboard')
  if (!user) return null

  const totalKpis = KPI_DOMAINS.reduce((sum, d) => sum + d.kpis.length, 0)

  return (
    <div className="min-h-svh bg-muted/40">
      <TopNav user={user} onLogout={() => void logout()} />
      <SubNav view={view} onViewChange={setView} />

      {view === 'performance' ? (
        <KpiValidationDashboard />
      ) : (
        <>
          <FilterBar />

          <main className="flex flex-col gap-4 px-6 py-4">
            <div>
              <h1 className="text-xl font-semibold">KRSG Operations</h1>
              <p className="text-sm text-muted-foreground">
                Composite plant performance · {totalKpis} KPIs across {KPI_DOMAINS.length} sections
              </p>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
              <OverallHeatmap site="KRSG" />
              <div className="flex flex-col gap-4">
                <QcScoreCard />
                <PositiveSignals />
              </div>
            </div>

            <NeedsAttentionPanel />

            <SectionSummaryList site="KRSG" />
          </main>
        </>
      )}
    </div>
  )
}
