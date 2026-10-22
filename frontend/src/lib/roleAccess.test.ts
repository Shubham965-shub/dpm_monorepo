import { describe, expect, it } from 'vitest'
import { resolveRoleTab, resolveAnalystSupervisorView, resolveQaLens, hasDepartmentAccess } from './roleAccess'

describe('resolveRoleTab', () => {
  it('locks CXO to the CXO tab', () => {
    expect(resolveRoleTab(['CXO'])).toEqual({ tab: 'CXO', locked: true })
  })

  it('locks HOD and MANAGER to Leadership', () => {
    expect(resolveRoleTab(['HOD'])).toEqual({ tab: 'Leadership', locked: true })
    expect(resolveRoleTab(['MANAGER'])).toEqual({ tab: 'Leadership', locked: true })
  })

  it('locks SUPERVISOR and ANALYST to Operations', () => {
    expect(resolveRoleTab(['SUPERVISOR'])).toEqual({ tab: 'Operations', locked: true })
    expect(resolveRoleTab(['ANALYST'])).toEqual({ tab: 'Operations', locked: true })
  })

  it('gives ADMIN every tab, unlocked', () => {
    expect(resolveRoleTab(['ADMIN'])).toEqual({ tab: 'Operations', locked: false })
  })

  it('falls back to Operations for no recognized role', () => {
    expect(resolveRoleTab([])).toEqual({ tab: 'Operations', locked: true })
  })
})

describe('resolveAnalystSupervisorView', () => {
  it('locks ANALYST to analyst', () => {
    expect(resolveAnalystSupervisorView(['ANALYST'])).toEqual({ view: 'analyst', locked: true })
  })

  it('locks SUPERVISOR to supervisor', () => {
    expect(resolveAnalystSupervisorView(['SUPERVISOR'])).toEqual({ view: 'supervisor', locked: true })
  })

  it('gives ADMIN the free toggle, defaulting to supervisor', () => {
    expect(resolveAnalystSupervisorView(['ADMIN'])).toEqual({ view: 'supervisor', locked: false })
  })
})

describe('resolveQaLens', () => {
  it('locks CXO to cxo and CLUSTER_HEAD to its own clusterHead lens - QA\'s CXO tier has both', () => {
    expect(resolveQaLens(['CXO'])).toEqual({ lens: 'cxo', locked: true })
    expect(resolveQaLens(['CLUSTER_HEAD'])).toEqual({ lens: 'clusterHead', locked: true })
  })

  it('locks HOD and MANAGER to leadership, SUPERVISOR to supervisor', () => {
    expect(resolveQaLens(['HOD'])).toEqual({ lens: 'leadership', locked: true })
    expect(resolveQaLens(['MANAGER'])).toEqual({ lens: 'leadership', locked: true })
    expect(resolveQaLens(['SUPERVISOR'])).toEqual({ lens: 'supervisor', locked: true })
  })

  it('gives ADMIN the free toggle, defaulting to cxo', () => {
    expect(resolveQaLens(['ADMIN'])).toEqual({ lens: 'cxo', locked: false })
  })

  it('falls back to supervisor for ANALYST or an unrecognized role - QA has no Analyst-level column', () => {
    expect(resolveQaLens(['ANALYST'])).toEqual({ lens: 'supervisor', locked: true })
    expect(resolveQaLens([])).toEqual({ lens: 'supervisor', locked: true })
  })
})

describe('hasDepartmentAccess', () => {
  it('grants access when the department is in the user\'s own list', () => {
    expect(hasDepartmentAccess(['SUPERVISOR'], ['QC'], 'QC')).toBe(true)
    expect(hasDepartmentAccess(['SUPERVISOR'], ['QC'], 'QA')).toBe(false)
  })

  it('a user can hold more than one department (e.g. the shared Mfg/Engg/SC&P CXO)', () => {
    expect(hasDepartmentAccess(['CXO'], ['MFG', 'ENGG', 'SCP'], 'ENGG')).toBe(true)
  })

  it('ADMIN bypasses the gate regardless of their own departments', () => {
    expect(hasDepartmentAccess(['ADMIN'], [], 'QA')).toBe(true)
    expect(hasDepartmentAccess(['ADMIN'], ['QC'], 'QA')).toBe(true)
  })
})
