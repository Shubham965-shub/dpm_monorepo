// Confirmed against the backend controller/service layer (LabPerformanceController,
// InstrumentHealthController, PeoplePerformanceController, QmsController) - not guessed.
export interface KpiCatalogueEntry {
  slug: string
  name: string
  /** Path segment under the section's base path, e.g. "sample-tat" -> /api/lab-performance/sample-tat. */
  endpoint: string
  /** Extra query params this specific KPI needs beyond site/grain/from/to. */
  extraParams?: Record<string, string>
  /**
   * Set when a required query param has no agreed value yet (e.g. InstrumentHealthController's
   * "availability"/"downtime" require `instGroup` with no default, and no instrument group has
   * been decided for these two). The heatmap renders these as "no data" instead of guessing one.
   */
  blockedReason?: string
}

export interface KpiCatalogueSection {
  key: 'lab-performance' | 'instrument-health' | 'qms' | 'people-performance'
  title: string
  basePath: string
  kpis: KpiCatalogueEntry[]
}

export const KPI_CATALOGUE: KpiCatalogueSection[] = [
  {
    key: 'lab-performance',
    title: 'Lab Performance',
    basePath: '/api/lab-performance',
    kpis: [
      { slug: 'sample-tat', name: 'Sample TAT', endpoint: 'sample-tat' },
      { slug: 'sla-adherence', name: 'SLA Adherence', endpoint: 'sla-adherence' },
      {
        slug: 'priority-sample-compliance',
        name: 'Priority Sample Compliance',
        endpoint: 'priority-sample-compliance',
      },
      { slug: 'tests-per-analyst', name: 'Tests per Analyst per Day', endpoint: 'tests-per-analyst' },
      {
        slug: 'hplc-sequences-per-analyst',
        name: 'HPLC Sequences Executed per Analyst',
        endpoint: 'hplc-sequences-per-analyst',
      },
      {
        slug: 'hplc-injections-per-analyst',
        name: 'HPLC Samples Injected per Analyst',
        endpoint: 'hplc-injections-per-analyst',
      },
    ],
  },
  {
    key: 'instrument-health',
    title: 'Instrument Health',
    basePath: '/api/instrument-health',
    kpis: [
      {
        slug: 'utilization-hplc',
        name: 'HPLC Utilization %',
        endpoint: 'utilization',
        extraParams: { instGroup: 'HPLC' },
      },
      {
        slug: 'availability',
        name: 'Instrument Availability %',
        endpoint: 'availability',
        blockedReason: 'Requires an instGroup filter - no instrument group decided for this KPI yet.',
      },
      {
        slug: 'downtime',
        name: 'Instrument Downtime %',
        endpoint: 'downtime',
        blockedReason: 'Requires an instGroup filter - no instrument group decided for this KPI yet.',
      },
      { slug: 'calibration-compliance', name: 'Calibration Compliance %', endpoint: 'calibration-compliance' },
      { slug: 'pm-adherence', name: 'PM Adherence %', endpoint: 'pm-adherence' },
      { slug: 'breakdown-frequency', name: 'Breakdown Frequency', endpoint: 'breakdown-frequency' },
      { slug: 'failure-rate', name: 'Instrument Failure Rate', endpoint: 'failure-rate' },
      { slug: 'qualification-cycle-time', name: 'Qualification Cycle Time', endpoint: 'qualification-cycle-time' },
    ],
  },
  {
    key: 'qms',
    title: 'QMS Dashboard',
    basePath: '/api/qms',
    kpis: [
      { slug: 'invalid-oos-rate', name: 'Invalid OOS Rate', endpoint: 'invalid-oos-rate' },
      { slug: 'confirmed-oos-rate', name: 'Confirmed OOS Rate', endpoint: 'confirmed-oos-rate' },
      { slug: 'oos-trend', name: 'OOS Trend', endpoint: 'oos-trend' },
      { slug: 'oot-trend', name: 'OOT Trend', endpoint: 'oot-trend' },
      { slug: 'deviations', name: 'Deviations', endpoint: 'deviations' },
      { slug: 'investigation-closure-tat', name: 'Investigation Closure TAT', endpoint: 'investigation-closure-tat' },
      { slug: 'rci-beyond-timeline', name: 'RCIs Beyond Timeline', endpoint: 'rci-beyond-timeline' },
      { slug: 'repeat-failure-rate', name: 'Repeat Failure Rate', endpoint: 'repeat-failure-rate' },
      { slug: 'root-cause', name: 'Root Cause Distribution', endpoint: 'root-cause' },
      { slug: 'capa-aging-status', name: 'CAPA Status/Aging', endpoint: 'capa-aging-status' },
      { slug: 'capa-effectiveness', name: 'CAPA Effectiveness', endpoint: 'capa-effectiveness' },
      { slug: 'capa-recurrence-rate', name: 'CAPA Recurrence Rate', endpoint: 'capa-recurrence-rate' },
      { slug: 'human-error-rate', name: 'Human Error Rate', endpoint: 'human-error-rate' },
      { slug: 'repeat-human-error-rate', name: 'Repeat Human Error Rate', endpoint: 'repeat-human-error-rate' },
      { slug: 'human-error-severity', name: 'Human Error Severity', endpoint: 'human-error-severity' },
      { slug: 'human-error-by-product', name: 'Human Error by Product', endpoint: 'human-error-by-product' },
      { slug: 'method-failure-rate', name: 'Method Failure Rate', endpoint: 'method-failure-rate' },
      { slug: 'product-wise-failure', name: 'Product-wise Failure', endpoint: 'product-wise-failure' },
    ],
  },
  {
    key: 'people-performance',
    title: 'People Performance',
    basePath: '/api/people-performance',
    kpis: [
      { slug: 'repeat-analysis-rate', name: 'Repeat Analysis Rate', endpoint: 'repeat-analysis-rate' },
      { slug: 'reviewer-productivity', name: 'Reviewer Productivity Index', endpoint: 'reviewer-productivity' },
      { slug: 'rft-documentation', name: 'Right First Time (RFT) Documentation', endpoint: 'rft-documentation' },
      { slug: 'training-completion-rate', name: 'Training Completion Rate', endpoint: 'training-completion-rate' },
    ],
  },
]
