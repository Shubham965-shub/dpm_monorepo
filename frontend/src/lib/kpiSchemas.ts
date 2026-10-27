import { z } from 'zod'

// Mirrors backend com.strides.dto.KpiStatus - fixed 3-value enum, nullable when a KPI's
// catalogue threshold has no matching column/scale to evaluate against (see backend/README.md).
export const kpiStatusSchema = z.enum(['RED', 'AMBER', 'GREEN']).nullable()
export type KpiStatus = z.infer<typeof kpiStatusSchema>

// Mirrors backend com.strides.dto.KpiThresholds - the kpi_threshold_snapshot limits the row's
// status was computed against (a null limit = unbounded side). configKpiId is the KPI's
// dim_kpi_lite id.
// multiSelectHint: several values of a threshold filter were picked, so the "All" limits were used.
export const kpiThresholdsSchema = z.object({
  configKpiId: z.string(),
  // Nullable in kpi_threshold_snapshot - a null must not fail the whole card's validation.
  direction: z.string().nullable(),
  redLow: z.coerce.number().nullable(),
  greenLow: z.coerce.number().nullable(),
  greenHigh: z.coerce.number().nullable(),
  redHigh: z.coerce.number().nullable(),
  multiSelectHint: z.boolean().optional(),
})
export type KpiThresholds = z.infer<typeof kpiThresholdsSchema>

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
  // The two already-aggregated quantities `value` was divided from (e.g. on-time count / sample
  // count), for a KPI whose value is a rate/ratio - null for every non-ratio KPI, and also null
  // for a handful of ratio KPIs where the source table doesn't unambiguously identify the pair
  // (see backend InstrumentHealthService.getInstrumentFailureRate). Shown as a tooltip breakdown.
  numerator: z.coerce.number().nullable(),
  denominator: z.coerce.number().nullable(),
  // What numerator/denominator actually count (e.g. "On-Time Samples"/"Total Samples" for SLA
  // Adherence) - null wherever numerator/denominator are null, and also null for a ratio KPI not
  // labeled yet (see backend PeriodCombiner.fromTotals's call sites); the UI falls back to a
  // generic "Numerator / Denominator" heading in that case. nullish since a backend deployed before
  // this field existed simply omits it.
  numeratorLabel: z.string().nullish(),
  denominatorLabel: z.string().nullish(),
  // Per-category counts in display order (e.g. observations per severity), shown in the tooltip
  // instead of numerator / denominator - null for every KPI without one.
  breakdown: z.record(z.string(), z.coerce.number()).nullish(),
  // The bar this row's segment (`name`) stacks into, for a card plotted by category instead of by period.
  category: z.string().nullish(),
  // nullish, not nullable: a backend deployed before this field existed simply omits it.
  thresholds: kpiThresholdsSchema.nullish(),
  // The period hasn't finished yet, so its value (and colour) is still moving.
  inProgress: z.boolean().nullish(),
})
export type KpiCardResponse = z.infer<typeof kpiCardResponseSchema>

export const kpiCardListSchema = z.array(kpiCardResponseSchema)


// Mirrors backend com.strides.dto.QcScoreDtos - GET /api/qc-score. Scores are unrounded 0-100
// (round only for display); null = no data (grey), never 0.
export const scoreBandSchema = z.enum(['GREEN', 'AMBER', 'RED']).nullable()
export type ScoreBand = z.infer<typeof scoreBandSchema>

const kpiScoreSchema = z.object({
  seriesId: z.string(),
  name: z.string().nullable(),
  configKpiId: z.string().nullable(),
  periodStart: z.string().nullable(),
  actual: z.coerce.number().nullable(),
  score: z.number().nullable(),
  previousScore: z.number().nullable(),
  band: scoreBandSchema,
})

const sectionScoreSchema = z.object({
  sectionId: z.string(),
  score: z.number().nullable(),
  previousScore: z.number().nullable(),
  band: scoreBandSchema,
  kpis: z.array(kpiScoreSchema),
})
export type SectionScore = z.infer<typeof sectionScoreSchema>

export const qcScoreSchema = z.object({
  score: z.number().nullable(),
  previousScore: z.number().nullable(),
  band: scoreBandSchema,
  sections: z.array(sectionScoreSchema),
})
export type QcScore = z.infer<typeof qcScoreSchema>

// The 5 grains every agg_*/fact_* table is rolled up to (confirmed live against the dpm DB).
export const GRAINS = ['daily', 'weekly', 'monthly', 'quarterly', 'yearly'] as const
export type Grain = (typeof GRAINS)[number]

// Mirrors a KPI's {basePath}/{kpiPath}/filter-options response - one live-queried option list per
// extraFilter param name (see backend AbstractAggRepository.distinctValues / each repository's
// filterOptions method).
export const filterOptionsSchema = z.record(z.string(), z.array(z.string()))
export type FilterOptions = z.infer<typeof filterOptionsSchema>

// Mirrors GET /api/filters/sites - the union of every distinct site/site_name value across every
// agg_* table (see backend SiteOptionsRepository).
export const siteListSchema = z.array(z.string())

// Mirrors backend com.strides.model.KpiThresholdConfigRow - one KPI catalogue entry
// (dim_kpi_lite + kpi_info). sourceSystem/summary are null when kpi_info has no row for the KPI.
export const kpiInfoSchema = z.object({
  kpiId: z.string(),
  kpiName: z.string().nullable(),
  sourceSystem: z.string().nullable(),
  summary: z.string().nullable(),
})
export type KpiInfo = z.infer<typeof kpiInfoSchema>
