/** Matches TopNav's ROLE_TABS - kept here (not re-exported from TopNav) so this stays a plain,
 * framework-agnostic module the tests below can import without pulling in React. */
export type RoleTab = 'CXO' | 'Leadership' | 'Operations'

/** One KPI-visibility list per KPI Framework spec role column (see SectionSummaryList's
 * KPI_VISIBILITY) - 'leadership' covers both HOD and MANAGER, which the spec never distinguishes
 * (same column for both). Every tab/role resolves to exactly one of these - there is no unfiltered
 * "all KPIs" state. */
export type KpiVisibilityLens = 'operator' | 'supervisor' | 'leadership' | 'cxo'
/** The two lenses a locked SUPERVISOR/ANALYST (and ADMIN, via its own free-toggle Operations
 * choice) switch between within Operations. */
export type OperatorSupervisorView = Extract<KpiVisibilityLens, 'operator' | 'supervisor'>

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
 * Within the Operations tab: ANALYST maps to the Operator KPI list, SUPERVISOR to the broader
 * Supervisor list (see SectionSummaryList's OPERATOR_VISIBLE_KPI_IDS/SUPERVISOR_VISIBLE_KPI_IDS) -
 * both locked, no toggle to the other. The `view`/`locked: false` returned for ADMIN here is a
 * placeholder DashboardPage never actually reads - ADMIN's real Operations choice lives in its own
 * free-toggle local state instead.
 */
export function resolveOperatorSupervisorView(roles: string[]): { view: OperatorSupervisorView; locked: boolean } {
  const role = roles[0]
  if (role === 'ADMIN') return { view: 'supervisor', locked: false }
  if (role === 'ANALYST') return { view: 'operator', locked: true }
  return { view: 'supervisor', locked: true }
}
