import { z } from 'zod'
import { getValidated } from './apiClient'
import type { KpiCatalogueEntry, KpiCatalogueSection } from './kpiCatalogue'

// Mirrors backend com.strides.service.kpi.KpiCardResponse / KpiStatus.
export const kpiStatusSchema = z.enum(['RED', 'AMBER', 'GREEN']).nullable()

export const kpiCardResponseSchema = z.object({
  kpiId: z.string(),
  name: z.string(),
  value: z.number(),
  unit: z.string(),
  // Null when the KPI's catalogue threshold doesn't apply to this column's scale/unit -
  // see backend/CLAUDE.md. The real value is still returned; only the RAG color is withheld.
  status: kpiStatusSchema,
  grain: z.string(),
  periodStart: z.string(),
})
export type KpiCardResponse = z.infer<typeof kpiCardResponseSchema>

export const kpiCardListSchema = z.array(kpiCardResponseSchema)

export interface KpiFilters {
  site: string
  grain: string
  from: string
  to: string
}

export async function fetchKpiSeries(
  section: KpiCatalogueSection,
  kpi: KpiCatalogueEntry,
  filters: KpiFilters,
): Promise<KpiCardResponse[]> {
  if (kpi.blockedReason) {
    return []
  }
  const params = new URLSearchParams({
    site: filters.site,
    grain: filters.grain,
    from: filters.from,
    to: filters.to,
    ...kpi.extraParams,
  })
  return getValidated(kpiCardListSchema, `${section.basePath}/${kpi.endpoint}?${params.toString()}`)
}
