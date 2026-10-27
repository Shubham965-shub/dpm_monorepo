import type { FeedbackCreatePayload } from '@strides-pharma-science-ltd/feedback-widget'
import type { KpiDomain } from './kpiConfig'

/** What the feedback button is sitting on: the dashboard being viewed and its sections/KPIs, for the "what is this about"
 * picker (see FeedbackLauncher). `persona` is the role view in use (CXO / Leadership / Supervisor …), when the page has one. */
export interface FeedbackScope {
  dashboard: string
  persona: string | null
  domains: KpiDomain[]
}

/** The picker's choice: a section (module) and/or one of its KPIs; both null = the dashboard as a whole. */
export interface FeedbackSubject {
  moduleKey: string | null
  kpiId: string | null
}

export const WHOLE_DASHBOARD: FeedbackSubject = { moduleKey: null, kpiId: null }

export interface ResolvedSubject {
  dashboard: string
  persona: string | null
  module: { key: string; label: string } | null
  kpi: { id: string; label: string } | null
}

/** Looks the picked ids up in the scope; an id no longer on the dashboard (it switched under the picker) drops out. A
 * KPI implies its own section, so picking a KPI alone still names the module. */
export function resolveSubject(scope: FeedbackScope, subject: FeedbackSubject): ResolvedSubject {
  const kpiDomain = subject.kpiId ? scope.domains.find((d) => d.kpis.some((k) => k.id === subject.kpiId)) : undefined
  const kpi = kpiDomain?.kpis.find((k) => k.id === subject.kpiId)
  const moduleDomain = kpiDomain ?? scope.domains.find((d) => d.key === subject.moduleKey)
  return {
    dashboard: scope.dashboard,
    persona: scope.persona,
    module: moduleDomain ? { key: moduleDomain.key, label: moduleDomain.label } : null,
    kpi: kpi ? { id: kpi.id, label: kpi.label } : null,
  }
}

/** "QA Dashboard › Batch Disposition & Release Performance › Right First Time (RFT)" */
export function describeSubject(subject: ResolvedSubject): string {
  return [subject.dashboard, subject.module?.label, subject.kpi?.label].filter(Boolean).join(' › ')
}

/** The payload with where it was raised stamped on: structured under `context.helios` (for the feedback service) and as
 * a first line of `details` (readable wherever the feedback is listed). The summary is left alone - the widget caps it. */
export function withSubject(payload: FeedbackCreatePayload, subject: ResolvedSubject): FeedbackCreatePayload {
  const raisedOn = `Raised on: ${describeSubject(subject)}${subject.persona ? ` (${subject.persona} view)` : ''}`
  return {
    ...payload,
    details: payload.details ? `${raisedOn}\n\n${payload.details}` : raisedOn,
    context: {
      ...payload.context,
      helios: {
        dashboard: subject.dashboard,
        persona: subject.persona,
        module: subject.module?.label ?? null,
        module_key: subject.module?.key ?? null,
        kpi: subject.kpi?.label ?? null,
        kpi_id: subject.kpi?.id ?? null,
      },
    },
  }
}
