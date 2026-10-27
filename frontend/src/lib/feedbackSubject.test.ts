import { describe, expect, it } from 'vitest'
import type { FeedbackCreatePayload } from '@strides-pharma-science-ltd/feedback-widget'
import { QA_DOMAINS } from './qaKpiConfig'
import { describeSubject, resolveSubject, WHOLE_DASHBOARD, withSubject, type FeedbackScope } from './feedbackSubject'

const scope: FeedbackScope = { dashboard: 'QA Dashboard', persona: 'Supervisor', domains: QA_DOMAINS }
const payload: FeedbackCreatePayload = { app_key: 'helios', type: 'BUG', summary: 'Value is wrong', context: { url: 'http://x' } }

describe('feedback subject', () => {
  it('names the dashboard alone when nothing more is picked', () => {
    expect(describeSubject(resolveSubject(scope, WHOLE_DASHBOARD))).toBe('QA Dashboard')
  })

  it('a KPI alone names its own module too', () => {
    const resolved = resolveSubject(scope, { moduleKey: null, kpiId: 'qa-batch-reject' })
    expect(resolved.kpi?.label).toBe('Lot Acceptance Rate')
    expect(resolved.module?.key).toBe('qa-batch-disposition')
    expect(describeSubject(resolved)).toContain('Lot Acceptance Rate')
  })

  it('drops a pick that is no longer on the dashboard', () => {
    const resolved = resolveSubject(scope, { moduleKey: 'gone', kpiId: 'gone-kpi' })
    expect(resolved.module).toBeNull()
    expect(resolved.kpi).toBeNull()
  })

  it('stamps where it was raised on the payload without losing what the widget sent', () => {
    const stamped = withSubject(payload, resolveSubject(scope, { moduleKey: null, kpiId: 'qa-batch-reject' }))
    expect(stamped.summary).toBe('Value is wrong')
    expect(stamped.details).toMatch(/^Raised on: QA Dashboard › .* › Lot Acceptance Rate \(Supervisor view\)$/)
    expect(stamped.context).toMatchObject({ url: 'http://x', helios: { dashboard: 'QA Dashboard', kpi_id: 'qa-batch-reject', persona: 'Supervisor' } })
  })

  it('puts the line above anything the user wrote in details', () => {
    const stamped = withSubject({ ...payload, details: 'It shows 3.72' }, resolveSubject(scope, WHOLE_DASHBOARD))
    expect(stamped.details).toBe('Raised on: QA Dashboard (Supervisor view)\n\nIt shows 3.72')
  })
})
