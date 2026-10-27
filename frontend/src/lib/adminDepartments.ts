import type { KpiDomain } from './kpiConfig'
import type { Grain } from './kpiSchemas'
import type { KpiVisibilityLens } from './roleAccess'
import { ENGG_DOMAINS, ENGG_DRILLDOWN_SOURCE, ENGG_KPI_DEFAULT_GRAIN, ENGG_KPI_VISIBILITY } from './enggDepartment'
import { MFG_DOMAINS, MFG_DRILLDOWN_SOURCE, MFG_KPI_DEFAULT_GRAIN, MFG_KPI_VISIBILITY } from './mfgDepartment'
import { SCP_DOMAINS, SCP_DRILLDOWN_SOURCE, SCP_KPI_DEFAULT_GRAIN, SCP_KPI_VISIBILITY } from './scpDepartment'

export type AdminDepartmentId = 'mfg' | 'engg' | 'scp'

/** A department dashboard that exists only as an admin-visible scaffold: its nav tab, page, sections and KPI
 * cards render for ADMIN alone (see DashboardPage). Each one's KPI ids carry its own prefix, so they stay
 * disjoint from QC's, QA's and each other's - which is what lets kpiVisibility.ts and kpiDefaultGrain.ts merge
 * them into their single lookups. */
export interface AdminDepartment {
  id: AdminDepartmentId
  navLabel: string
  title: string
  /** The score card's heading. */
  scoreTitle: string
  domains: KpiDomain[]
  visibility: Record<KpiVisibilityLens, Set<string>>
  defaultGrain: Partial<Record<string, Partial<Record<KpiVisibilityLens, Grain>>>>
  /** Card id -> the QA card whose drill-down columns it reuses (see DRILLDOWN_CONFIG). */
  drilldownSource: Record<string, string>
}

export const ADMIN_DEPARTMENTS: AdminDepartment[] = [
  { id: 'mfg', navLabel: 'MFG Dashboard', title: 'Manufacturing Dashboard', scoreTitle: 'Manufacturing Score', domains: MFG_DOMAINS, visibility: MFG_KPI_VISIBILITY, defaultGrain: MFG_KPI_DEFAULT_GRAIN, drilldownSource: MFG_DRILLDOWN_SOURCE },
  { id: 'engg', navLabel: 'Engg Dashboard', title: 'Engineering Dashboard', scoreTitle: 'Engineering Score', domains: ENGG_DOMAINS, visibility: ENGG_KPI_VISIBILITY, defaultGrain: ENGG_KPI_DEFAULT_GRAIN, drilldownSource: ENGG_DRILLDOWN_SOURCE },
  { id: 'scp', navLabel: 'SC&P Dashboard', title: 'Supply Chain & Planning Dashboard', scoreTitle: 'Supply Chain & Planning Score', domains: SCP_DOMAINS, visibility: SCP_KPI_VISIBILITY, defaultGrain: SCP_KPI_DEFAULT_GRAIN, drilldownSource: SCP_DRILLDOWN_SOURCE },
]

export function adminDepartmentById(id: string): AdminDepartment | undefined {
  return ADMIN_DEPARTMENTS.find((department) => department.id === id)
}

/** Every admin department's KPI ids for one lens - the slice kpiVisibility.ts merges into KPI_VISIBILITY. */
export function adminVisibleKpiIds(lens: KpiVisibilityLens): string[] {
  return ADMIN_DEPARTMENTS.flatMap((department) => [...department.visibility[lens]])
}

export const ADMIN_KPI_DEFAULT_GRAIN: Partial<Record<string, Partial<Record<KpiVisibilityLens, Grain>>>> = Object.assign(
  {},
  ...ADMIN_DEPARTMENTS.map((department) => department.defaultGrain),
)

export const ADMIN_DRILLDOWN_SOURCE: Record<string, string> = Object.assign({}, ...ADMIN_DEPARTMENTS.map((department) => department.drilldownSource))
