import { describe, expect, it } from 'vitest'
import { ageShadeOpacity } from './ageShade'

describe('ageShadeOpacity', () => {
  const TODAY = new Date(2026, 9, 21)

  it('saturates the oldest period and fades the newest', () => {
    expect(ageShadeOpacity('2026-09-21', '2026-09-21', TODAY)).toBe(1)
    expect(ageShadeOpacity('2026-10-21', '2026-09-21', TODAY)).toBeCloseTo(0.25)
  })

  it('grows with age', () => {
    expect(ageShadeOpacity('2026-10-01', '2026-09-21', TODAY)).toBeLessThan(ageShadeOpacity('2026-09-25', '2026-09-21', TODAY))
  })

  it('is fully opaque when every period is today', () => {
    expect(ageShadeOpacity('2026-10-21', '2026-10-21', TODAY)).toBe(1)
  })
})
