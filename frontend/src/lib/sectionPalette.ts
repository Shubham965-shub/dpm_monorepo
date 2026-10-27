export interface SectionPalette {
  bg: string
  border: string
}

const SECTION_PALETTE: Record<string, SectionPalette> = {
  'lab-performance': { bg: 'rgba(255,179,186,0.15)', border: '#ffb3ba' },
  'instrument-health': { bg: 'rgba(255,200,162,0.15)', border: '#ffc8a2' },
  qms: { bg: 'rgba(255,224,178,0.15)', border: '#ffe0b2' },
  'people-performance': { bg: 'rgba(186,225,255,0.15)', border: '#bae1ff' },
  // QA's own 5 sections (qaKpiConfig.ts) - distinct from QC's 4 above so the two never collide if
  // ever shown on the same page, same "one color per section" rule as QC's own palette.
  'qa-batch-disposition': { bg: 'rgba(186,255,201,0.15)', border: '#baffc9' },
  'qa-quality-event': { bg: 'rgba(255,223,186,0.15)', border: '#ffdfba' },
  'qa-compliance-audit': { bg: 'rgba(201,201,255,0.15)', border: '#c9c9ff' },
  'qa-people-performance': { bg: 'rgba(224,187,255,0.15)', border: '#e0bbff' },
  'qa-cross-functional': { bg: 'rgba(217,217,217,0.15)', border: '#d9d9d9' },
}
// A section with no entry above (the admin-only MFG / Engg / SC&P dashboards' sections - see adminDepartments.ts) takes
// one of these, picked from its key so it keeps the same colour across renders.
const FALLBACK_SECTION_PALETTES: SectionPalette[] = [
  { bg: 'rgba(186,255,243,0.15)', border: '#bafff3' },
  { bg: 'rgba(255,186,243,0.15)', border: '#ffbaf3' },
  { bg: 'rgba(243,255,186,0.15)', border: '#f3ffba' },
  { bg: 'rgba(186,208,255,0.15)', border: '#bad0ff' },
  { bg: 'rgba(255,214,186,0.15)', border: '#ffd6ba' },
]
export function sectionPalette(sectionKey: string): SectionPalette {
  const known = SECTION_PALETTE[sectionKey]
  if (known) return known
  const hash = [...sectionKey].reduce((sum, char) => sum + char.charCodeAt(0), 0)
  return FALLBACK_SECTION_PALETTES[hash % FALLBACK_SECTION_PALETTES.length]
}
