import type { SectionKey } from './mockData'

// Each dashboard section's short name (QC Score breakdown) and category line (section bar) -
// fixed display labels, not data. The scores themselves come from GET /api/qc-score (useQcScore).
export const SECTION_LABELS: Record<SectionKey, { shortLabel: string; category: string }> = {
  'lab-performance': { shortLabel: 'Lab', category: 'Operational Efficiency' },
  'instrument-health': { shortLabel: 'Instrument', category: 'Asset Reliability' },
  qms: { shortLabel: 'QMS', category: 'Compliance System' },
  'people-performance': { shortLabel: 'People', category: 'Workforce' },
}
