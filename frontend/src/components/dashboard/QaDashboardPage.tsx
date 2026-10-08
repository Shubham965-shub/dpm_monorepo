import { useState } from 'react'
import { cn, pluralize } from '@/lib/utils'
import { DEFAULT_SITE } from '@/lib/kpiConfig'
import { QA_DOMAINS } from '@/lib/qaKpiConfig'
import { QA_KPI_VISIBILITY } from '@/lib/qaKpiVisibility'
import { generateMockSectionScore } from '@/lib/qaSectionScore'
import type { KpiVisibilityLens } from '@/lib/roleAccess'
import { SearchKpiBar } from './SearchKpiBar'
import { SectionSummaryList } from './SectionSummaryList'
import { DemoDataBadge } from './DemoDataBadge'
import { QaScoreSummaryCard } from './QaScoreSummaryCard'

// No real QA `/api/qa` backend exists yet (see qaKpiConfig.ts's `mock: true`) - there's no logged-
// in QA persona to resolve a lens from, so an admin previewing this tab picks one directly, same as
// ADMIN already free-toggles QC's own 4 lenses on the main Dashboard tab (see DashboardPage).
const LENS_OPTIONS: { value: KpiVisibilityLens; label: string }[] = [
  { value: 'cxo', label: 'CXO' },
  { value: 'leadership', label: 'Leadership' },
  { value: 'supervisor', label: 'Supervisor' },
  { value: 'analyst', label: 'Analyst' },
]

// Fixed fallback grain for a KPI/lens pair with no cell in the kept rows (see qaDefaultGrainFor) -
// matches DashboardPage's own CXO/unfiltered branch default.
const FALLBACK_GRAIN = 'monthly' as const

function LensToggle({ value, onChange }: { value: KpiVisibilityLens; onChange: (value: KpiVisibilityLens) => void }) {
  return (
    <div className="flex overflow-hidden rounded-md border border-input text-xs font-medium">
      {LENS_OPTIONS.map((option) => (
        <button
          key={option.value}
          onClick={() => onChange(option.value)}
          className={cn('px-3 py-1', value === option.value ? 'bg-foreground text-background' : 'text-muted-foreground hover:text-foreground')}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}

/**
 * Admin-only preview of a QA dashboard built from the same section/persona-visibility/default-grain
 * structure as the real QC dashboard (see SectionSummaryList), applied to the 55 buildable KPIs from
 * QA_KPI_Sheet_for_Front_End (61 rows minus 6 marked "Fix logic first"/"Needs clarification - hold"
 * - see qaKpiConfig.ts). Every KPI card and section score is demo data (qaKpiConfig.ts's `mock:
 * true`, mockKpiSeries.ts / qaSectionScore.ts) - no real QA agg_ / fact_ tables or thresholds exist
 * yet (see backend) - so there's no Overall Heatmap/Pending Actions here either, since those either
 * have no QA analogue or hold QC-specific mock copy that would be misleading relabeled QA.
 */
export function QaDashboardPage() {
  const [lens, setLens] = useState<KpiVisibilityLens>('cxo')
  const [searchQuery, setSearchQuery] = useState('')
  const visibleKpiIds = QA_KPI_VISIBILITY[lens]
  // Cross-Functional Context has `scored: false` (qaKpiConfig.ts) - returning undefined for it here
  // (rather than a generated score) is what makes SectionSummaryList render it with no score at all.
  const sectionScores = (sectionKey: string) => {
    const domain = QA_DOMAINS.find((d) => d.key === sectionKey)
    if (!domain || domain.scored === false) return undefined
    return generateMockSectionScore(domain)
  }

  return (
    <main className="flex flex-col gap-4 px-6 py-4 md:px-10 lg:px-20">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-semibold">QA KPI Dashboard (Preview)</h1>
            <DemoDataBadge title="Preview - demo data, not wired to a live QA data source yet" />
          </div>
          <p className="text-sm text-muted-foreground">
            {DEFAULT_SITE} · {pluralize(visibleKpiIds.size, 'KPI')} across {QA_DOMAINS.length} sections · every value below is synthetic
          </p>
        </div>
        <LensToggle value={lens} onChange={setLens} />
      </div>

      <QaScoreSummaryCard domains={QA_DOMAINS} />

      <SearchKpiBar value={searchQuery} onChange={setSearchQuery} />

      <SectionSummaryList
        site={DEFAULT_SITE}
        section="all"
        grain={FALLBACK_GRAIN}
        searchQuery={searchQuery}
        lens={lens}
        domains={QA_DOMAINS}
        sectionScores={sectionScores}
      />
    </main>
  )
}
