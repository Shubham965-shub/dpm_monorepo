/** Matches TopNav's ROLE_TABS - kept here (not re-exported from TopNav) so this stays a plain,
 * framework-agnostic module the tests below can import without pulling in React. */
export type RoleTab = 'CXO' | 'Leadership' | 'Operations'

/** One KPI-visibility list per KPI Framework spec role column (see SectionSummaryList's
 * KPI_VISIBILITY) - 'leadership' covers both HOD and MANAGER, which the spec never distinguishes
 * (same column for both). Every tab/role resolves to exactly one of these - there is no unfiltered
 * "all KPIs" state. 'clusterHead' sits alongside 'cxo' within a department's CXO tier (see the DPM
 * role ladder) - currently only meaningful for QA (qaKpiVisibility.ts/qaDefaultGrain.ts carry real,
 * distinct Cluster Head data); QC has no Cluster Head tier, so its own KPI_VISIBILITY/
 * KPI_DEFAULT_GRAIN just reuse the 'cxo' entry for 'clusterHead' (see kpiVisibility.ts). */
export type KpiVisibilityLens = 'analyst' | 'supervisor' | 'leadership' | 'cxo' | 'clusterHead'
/** The two lenses a locked SUPERVISOR/ANALYST (and ADMIN, via its own free-toggle Operations
 * choice) switch between within Operations. */
export type AnalystSupervisorView = Extract<KpiVisibilityLens, 'analyst' | 'supervisor'>
/** The two lenses within a department's CXO tier (see resolveQaLens) - CLUSTER_HEAD and CXO. */
export type CxoClusterHeadView = Extract<KpiVisibilityLens, 'cxo' | 'clusterHead'>

/**
 * Maps the backend's fixed Role enum (CXO/HOD/MANAGER/SUPERVISOR/ANALYST/ADMIN - see
 * backend/src/main/java/com/strides/domain/Role.java; a user holds exactly one) onto which of the
 * 3 role tabs they're confined to. ADMIN sees every tab, switchable (locked: false) - every other
 * role is locked to exactly one tab, with no nav to the others. A user with none of these
 * (shouldn't happen given the backend's fixed enum) falls back to Operations, the
 * least-privileged tab.
 */
export function resolveRoleTab(roles: string[]): { tab: RoleTab; locked: boolean } {
  const role = roles[0]
  if (role === 'ADMIN') return { tab: 'Operations', locked: false }
  if (role === 'CXO') return { tab: 'CXO', locked: true }
  if (role === 'HOD' || role === 'MANAGER') return { tab: 'Leadership', locked: true }
  return { tab: 'Operations', locked: true }
}

/**
 * Within the Operations tab: ANALYST maps to the Analyst KPI list, SUPERVISOR to the broader
 * Supervisor list (see SectionSummaryList's ANALYST_VISIBLE_KPI_IDS/SUPERVISOR_VISIBLE_KPI_IDS) -
 * both locked, no toggle to the other. The `view`/`locked: false` returned for ADMIN here is a
 * placeholder DashboardPage never actually reads - ADMIN's real Operations choice lives in its own
 * free-toggle local state instead.
 */
export function resolveAnalystSupervisorView(roles: string[]): { view: AnalystSupervisorView; locked: boolean } {
  const role = roles[0]
  if (role === 'ADMIN') return { view: 'supervisor', locked: false }
  if (role === 'ANALYST') return { view: 'analyst', locked: true }
  return { view: 'supervisor', locked: true }
}

/** True if any of the user's roles is ADMIN (a user can hold more than one - see
 * backend/src/main/java/com/strides/domain/Role.java). ADMIN bypasses both the QC/QA tab gate (see
 * hasDepartmentAccess) and the role-tab locking above - unlike resolveRoleTab/
 * resolveAnalystSupervisorView, this isn't about which of the existing QC lenses a role is confined
 * to, just a plain admin check. */
export function isAdmin(roles: string[]): boolean {
  return roles.includes('ADMIN')
}

/** True if the user can access `department`'s dashboard - either ADMIN (bypasses this gate
 * entirely, same as it already bypasses role-tab locking) or the department is in their own list
 * (a user can hold more than one - see backend/src/main/java/com/strides/domain/Department.java,
 * e.g. the one CXO shared across Mfg/Engg/SC&P). Mirrors RequiresDepartment's server-side check -
 * see TopNav's tab visibility / DashboardPage. */
export function hasDepartmentAccess(roles: string[], departments: string[], department: string): boolean {
  return isAdmin(roles) || departments.includes(department)
}

/**
 * QA's own role→lens resolution (there's no separate "tab" concept for QA - just one page, see
 * QaDashboardPage) - mirrors resolveRoleTab/resolveAnalystSupervisorView's combined shape for QC,
 * adapted to QA's flatter ladder (no Analyst-level column in the sheet - see qaKpiVisibility.ts) and
 * its CXO/Cluster Head split (see CxoClusterHeadView). ADMIN previews freely (locked: false);
 * every other role is locked to exactly one lens. A role with no QA-specific mapping (ANALYST, or
 * none) falls back to 'supervisor', QA's least-privileged real lens.
 */
export function resolveQaLens(roles: string[]): { lens: KpiVisibilityLens; locked: boolean } {
  const role = roles[0]
  if (role === 'ADMIN') return { lens: 'cxo', locked: false }
  if (role === 'CXO') return { lens: 'cxo', locked: true }
  if (role === 'CLUSTER_HEAD') return { lens: 'clusterHead', locked: true }
  if (role === 'HOD' || role === 'MANAGER') return { lens: 'leadership', locked: true }
  if (role === 'SUPERVISOR') return { lens: 'supervisor', locked: true }
  return { lens: 'supervisor', locked: true }
}

/** ADMIN's QA lens preview is driven by TopNav's own CXO/Leadership/Operations tabs (see
 * DashboardPage), not a separate toggle - QA has no Analyst tier (unlike QC's Operations tab, which
 * splits further into Analyst/Supervisor), so Operations maps straight to 'supervisor'. The CXO tab
 * itself still splits further into CXO/Cluster Head (see SubNav's secondaryToggle, the same
 * mechanism QC's Operations tab already uses for Analyst/Supervisor) - `cxoClusterHeadView` is
 * that sub-choice, meaningful only when `tab === 'CXO'`. */
export function qaLensForRoleTab(tab: RoleTab, cxoClusterHeadView: CxoClusterHeadView = 'cxo'): KpiVisibilityLens {
  if (tab === 'CXO') return cxoClusterHeadView
  if (tab === 'Leadership') return 'leadership'
  return 'supervisor'
}

/** Shared display label for a lens value - used both by QaDashboardPage's read-only lens badge and
 * by DashboardPage's CXO/Cluster Head and Analyst/Supervisor toggle option labels, so the wording
 * stays in one place instead of being duplicated per call site. */
export const LENS_LABEL: Record<KpiVisibilityLens, string> = {
  cxo: 'CXO',
  clusterHead: 'Cluster Head',
  leadership: 'Leadership',
  supervisor: 'Supervisor',
  analyst: 'Analyst',
}
