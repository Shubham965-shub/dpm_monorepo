import { describe, it, expect } from 'vitest'
import { generateMockKpiSeries } from './mockKpiSeries'
import { kpiCardListSchema } from './kpiSchemas'
import type { KpiDomain, KpiDefinition } from './kpiConfig'
import type { GlobalKpiFilters } from '@/hooks/useKpiData'

const domain: KpiDomain = {
  key: 'qa-test-domain',
  label: 'Test Domain',
  shortLabel: 'Test',
  category: 'Test',
  basePath: '/api/qa',
  mock: true,
  kpis: [],
}

const trendKpi: KpiDefinition = { id: 'qa-test-kpi', label: 'Test KPI', path: 'qa-test-kpi', extraFilters: [] }
const snapshotKpi: KpiDefinition = { ...trendKpi, id: 'qa-test-snapshot-kpi', snapshot: true }
const filters: GlobalKpiFilters = { site: 'KRSG', grain: 'monthly', from: '2026-01-01', to: '2026-04-01' }

describe('generateMockKpiSeries', () => {
  it('is deterministic for the same (domain, kpi, filters)', () => {
    const a = generateMockKpiSeries(domain, trendKpi, filters)
    const b = generateMockKpiSeries(domain, trendKpi, filters)
    expect(a).toEqual(b)
  })

  it('produces one row per period across the requested range', () => {
    const rows = generateMockKpiSeries(domain, trendKpi, filters)
    expect(rows.map((r) => r.periodStart)).toEqual(['2026-01-01', '2026-02-01', '2026-03-01', '2026-04-01'])
  })

  it('returns exactly one row for a snapshot KPI, dated `to`', () => {
    const rows = generateMockKpiSeries(domain, snapshotKpi, filters)
    expect(rows).toHaveLength(1)
    expect(rows[0].periodStart).toBe(filters.to)
  })

  it('never fabricates a RAG status - every row is status: null', () => {
    const rows = generateMockKpiSeries(domain, trendKpi, filters)
    expect(rows.every((r) => r.status === null)).toBe(true)
  })

  it('validates against the same schema a real backend response must satisfy', () => {
    expect(() => kpiCardListSchema.parse(generateMockKpiSeries(domain, trendKpi, filters))).not.toThrow()
  })
})
