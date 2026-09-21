import { z } from 'zod'

// Mirrors backend com.strides.dto.KpiStatus - fixed 3-value enum, nullable when a KPI's
// catalogue threshold has no matching column/scale to evaluate against (see backend/README.md).
export const kpiStatusSchema = z.enum(['RED', 'AMBER', 'GREEN']).nullable()
export type KpiStatus = z.infer<typeof kpiStatusSchema>

// Mirrors backend com.strides.dto.KpiCardResponse - the one uniform shape every KPI GET
// endpoint returns, regardless of domain.
export const kpiCardResponseSchema = z.object({
  kpiId: z.string(),
  name: z.string(),
  value: z.coerce.number().nullable(),
  unit: z.string(),
  status: kpiStatusSchema,
  grain: z.string(),
  periodStart: z.string(),
  // Caveat the UI must surface as-is, e.g. departmentScope=QC's narrowed scope on Investigation
  // TAT (see backend/README.md's "Department scope" section) - null when a KPI has none.
  note: z.string().nullable(),
})
export type KpiCardResponse = z.infer<typeof kpiCardResponseSchema>

export const kpiCardListSchema = z.array(kpiCardResponseSchema)

// The 5 grains every agg_*/fact_* table is rolled up to (confirmed live against the dpm DB).
export const GRAINS = ['daily', 'weekly', 'monthly', 'quarterly', 'yearly'] as const
export type Grain = (typeof GRAINS)[number]
