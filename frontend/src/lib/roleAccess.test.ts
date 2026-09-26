import { describe, expect, it } from 'vitest'
import { resolveRoleTab, resolveOperatorSupervisorView } from './roleAccess'

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

describe('resolveOperatorSupervisorView', () => {
  it('locks ANALYST to operator', () => {
    expect(resolveOperatorSupervisorView(['ANALYST'])).toEqual({ view: 'operator', locked: true })
  })

  it('locks SUPERVISOR to supervisor', () => {
    expect(resolveOperatorSupervisorView(['SUPERVISOR'])).toEqual({ view: 'supervisor', locked: true })
  })

  it('gives ADMIN the free toggle, defaulting to supervisor', () => {
    expect(resolveOperatorSupervisorView(['ADMIN'])).toEqual({ view: 'supervisor', locked: false })
  })
})
