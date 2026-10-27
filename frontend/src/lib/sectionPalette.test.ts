import { describe, expect, it } from 'vitest'
import { ADMIN_DEPARTMENTS } from './adminDepartments'
import { KPI_DOMAINS } from './kpiConfig'
import { QA_DOMAINS } from './qaKpiConfig'
import { sectionPalette } from './sectionPalette'

describe('sectionPalette', () => {
  it('gives every section on every dashboard a colour - the heatmap crashed on one without', () => {
    for (const domain of [...KPI_DOMAINS, ...QA_DOMAINS, ...ADMIN_DEPARTMENTS.flatMap((d) => d.domains)]) {
      const palette = sectionPalette(domain.key)
      expect(palette.bg).toBeTruthy()
      expect(palette.border).toBeTruthy()
    }
  })

  it('keeps a section on the same colour every time', () => {
    expect(sectionPalette('mfg-equipment-and-process-reliability')).toEqual(sectionPalette('mfg-equipment-and-process-reliability'))
  })
})
