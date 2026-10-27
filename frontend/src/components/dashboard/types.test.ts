import { describe, expect, it } from 'vitest'
import { boundaryLines, boundaryZones, SEVERITY_META, statusBarHex } from './types'

const GREEN = SEVERITY_META['in-control'].paleHex
const AMBER = SEVERITY_META.alert.paleHex
const RED = SEVERITY_META.action.paleHex
const DOMAIN: [number, number] = [0, 100]

function bands(redLow: number | null, greenLow: number | null, greenHigh: number | null, redHigh: number | null) {
  return { configKpiId: 'KPI-TEST', direction: 'band', redLow, greenLow, greenHigh, redHigh }
}

describe('boundaryZones', () => {
  it('higher_is_worse: green at the bottom, amber between, red at the top', () => {
    expect(boundaryZones(bands(null, null, 28, 34), DOMAIN)).toMatchObject([
      { y1: 0, y2: 28, fillHex: GREEN },
      { y1: 28, y2: 34, fillHex: AMBER },
      { y1: 34, y2: 100, fillHex: RED },
    ])
  })

  it('higher_is_better: red at the bottom, amber between, green at the top', () => {
    expect(boundaryZones(bands(55, 70, null, null), DOMAIN)).toMatchObject([
      { y1: 70, y2: 100, fillHex: GREEN },
      { y1: 55, y2: 70, fillHex: AMBER },
      { y1: 0, y2: 55, fillHex: RED },
    ])
  })

  it('green band with no red bounds: amber out to both chart edges', () => {
    expect(boundaryZones(bands(null, 85, 98, null), DOMAIN)).toMatchObject([
      { y1: 85, y2: 98, fillHex: GREEN },
      { y1: 0, y2: 85, fillHex: AMBER },
      { y1: 98, y2: 100, fillHex: AMBER },
    ])
  })

  it('every zone runs bottom-to-top (y1 <= y2), so none can render flipped', () => {
    for (const zone of boundaryZones(bands(80, 85, 98, 99), DOMAIN)) {
      expect(zone.y1).toBeLessThanOrEqual(zone.y2)
    }
  })

  it('no thresholds or no bands: no zones', () => {
    expect(boundaryZones(null, DOMAIN)).toEqual([])
    expect(boundaryZones(bands(null, null, null, null), DOMAIN)).toEqual([])
  })
})

describe('boundaryLines', () => {
  it('names the green edges Alert limits and the red edges Action limits', () => {
    expect(boundaryLines(bands(null, null, 28, 34)).map((l) => [l.label, l.name, l.tone])).toEqual([
      ['Alert 28', 'Alert limit', 'alert'],
      ['Action 34', 'Action limit', 'action'],
    ])
  })

  it('qualifies low/high only on a two-sided band', () => {
    expect(boundaryLines(bands(65, 85, 95, 98)).map((l) => l.name)).toEqual([
      'Alert limit (low)',
      'Alert limit (high)',
      'Action limit (low)',
      'Action limit (high)',
    ])
  })

  // Recharts draws "insideBottom" above a horizontal line and "insideTop" below it.
  it('points each alert/action label pair away from each other, lower edges left and upper right', () => {
    const lines = boundaryLines(bands(65, 85, 95, 98))
    const position = (y: number) => lines.find((l) => l.y === y)?.labelPosition
    expect(position(98)).toBe('insideBottomRight') // Action 98 above, right
    expect(position(95)).toBe('insideTopRight') // Alert 95 below, right
    expect(position(85)).toBe('insideBottomLeft') // Alert 85 above, left
    expect(position(65)).toBe('insideTopLeft') // Action 65 below, left
  })
})

describe('boundaryZones opacity', () => {
  it('draws the action zone lighter than the in-control and alert zones', () => {
    const zones = boundaryZones(bands(55, 70, null, null), DOMAIN)
    const action = zones.find((z) => z.fillHex === RED)!
    const alert = zones.find((z) => z.fillHex === AMBER)!
    expect(action.fillOpacity).toBeLessThan(alert.fillOpacity)
  })
})

describe('statusBarHex', () => {
  it('colours a bar by its own period status', () => {
    expect(statusBarHex('GREEN')).toBe(SEVERITY_META['in-control'].solidHex)
    expect(statusBarHex('AMBER')).toBe(SEVERITY_META.alert.solidHex)
    expect(statusBarHex('RED')).toBe(SEVERITY_META.action.solidHex)
  })

  it('leaves the series colour when there is no usable status', () => {
    expect(statusBarHex(null)).toBeNull()
    expect(statusBarHex(undefined)).toBeNull()
    expect(statusBarHex('PURPLE')).toBeNull()
  })
})
