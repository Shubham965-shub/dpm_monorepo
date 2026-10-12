import { z } from 'zod'

// lastUpdatedIst is the pipeline's own IST wall-clock run timestamp (see backend's
// DataFreshnessDtos) - a plain "YYYY-MM-DDTHH:mm:ss[.ffffff]" string with no UTC offset. null
// fields mean the pipeline's own run-log table had no successful run yet, not a fetch error.
export const dataFreshnessSchema = z.object({
  lastUpdatedIst: z.string().nullable(),
  runStatus: z.string().nullable(),
})

export type DataFreshness = z.infer<typeof dataFreshnessSchema>
