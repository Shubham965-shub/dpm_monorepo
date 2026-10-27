import { describe, expect, it } from 'vitest'
import { DRILLDOWN_CONFIG } from '@/components/dashboard/drilldownColumns'
import { ADMIN_DEPARTMENTS, adminDepartmentById, type AdminDepartmentId } from './adminDepartments'
import { KPI_DEFAULT_GRAIN } from './kpiDefaultGrain'
import { KPI_VISIBILITY } from './kpiVisibility'
import { QA_DOMAINS } from './qaKpiConfig'
import { KPI_DOMAINS } from './kpiConfig'
import { isAdmin, type KpiVisibilityLens } from './roleAccess'

const LENSES: KpiVisibilityLens[] = ['analyst', 'supervisor', 'leadership', 'cxo', 'clusterHead']

// KPIs per section, as listed on the source workbook's Sections sheet.
const SECTION_KPI_COUNTS: Record<AdminDepartmentId, number[]> = {
  mfg: [9, 15, 28, 4, 2], // 28: QMS also carries Product Complaint Criticality (Final), which the sheet lacks
  engg: [13, 5, 4],
  scp: [22, 22, 10, 3, 2],
}

describe('admin department scaffolds', () => {
  it('carry every section and KPI the source sheet lists', () => {
    for (const department of ADMIN_DEPARTMENTS) {
      expect(department.domains.map((d) => d.kpis.length)).toEqual(SECTION_KPI_COUNTS[department.id])
    }
  })

  it('prefix every KPI id with its department, with no id repeated anywhere', () => {
    const all = ADMIN_DEPARTMENTS.flatMap((department) => {
      const ids = department.domains.flatMap((d) => d.kpis.map((k) => k.id))
      expect(ids.every((id) => id.startsWith(`${department.id}-`))).toBe(true)
      return ids
    })
    expect(new Set(all).size).toBe(all.length)
    const existing = [...KPI_DOMAINS, ...QA_DOMAINS].flatMap((d) => d.kpis.map((k) => k.id))
    expect(all.filter((id) => existing.includes(id))).toEqual([])
  })

  it('only list KPIs that exist in visibility and default grain', () => {
    for (const department of ADMIN_DEPARTMENTS) {
      const ids = new Set(department.domains.flatMap((d) => d.kpis.map((k) => k.id)))
      for (const lens of LENSES) expect([...department.visibility[lens]].filter((id) => !ids.has(id))).toEqual([])
      expect(Object.keys(department.defaultGrain).filter((id) => !ids.has(id))).toEqual([])
    }
  })

  it('read real data only where the KPI has an endpoint, and keep the rest as marked demo cards', () => {
    for (const department of ADMIN_DEPARTMENTS) {
      for (const domain of department.domains) {
        expect(domain.mock).toBe(true)
        for (const kpi of domain.kpis) {
          if (kpi.mock === false) {
            // Wired cards address a real endpoint under the shared /api base: mfg/... or qa/...
            expect(domain.basePath).toBe('/api')
            expect(kpi.path).toMatch(/^(mfg|eng|qa)\/(mfg|eng|qa)-/)
          } else {
            expect(kpi.mock).toBeUndefined()
          }
        }
      }
    }
  })

  it('wire the KPIs whose data exists today', () => {
    const wired = (id: AdminDepartmentId) =>
      adminDepartmentById(id)!.domains.flatMap((d) => d.kpis).filter((k) => k.mock === false).length
    expect([wired('mfg'), wired('engg'), wired('scp')]).toEqual([53, 22, 16])
  })

  it('give every drill-down card a drill-down column config', () => {
    for (const department of ADMIN_DEPARTMENTS) {
      for (const kpi of department.domains.flatMap((d) => d.kpis)) {
        if (kpi.drilldownPath) expect(DRILLDOWN_CONFIG[kpi.id]).toBeDefined()
      }
    }
  })

  it('leave the Cross-Functional Context sections unscored and visible to every lens', () => {
    for (const department of ADMIN_DEPARTMENTS) {
      for (const domain of department.domains.filter((d) => d.label === 'Cross-Functional Context')) {
        expect(domain.scored).toBe(false)
        for (const lens of LENSES) expect(domain.kpis.every((k) => department.visibility[lens].has(k.id))).toBe(true)
      }
    }
  })

  it('merge into the shared visibility and default-grain lookups', () => {
    const mfg = adminDepartmentById('mfg')!
    expect(KPI_VISIBILITY.supervisor.has('mfg-serial-changeover-time')).toBe(true)
    expect(mfg.visibility.supervisor.has('mfg-serial-changeover-time')).toBe(true)
    expect(KPI_DEFAULT_GRAIN['mfg-serial-changeover-time']?.supervisor).toBe('daily')
  })

  it('are only reachable by an admin', () => {
    expect(isAdmin(['ADMIN'])).toBe(true)
    for (const role of ['CXO', 'HOD', 'MANAGER', 'SUPERVISOR', 'ANALYST', 'CLUSTER_HEAD']) expect(isAdmin([role])).toBe(false)
  })
})
