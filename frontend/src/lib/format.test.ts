import { describe, expect, it } from 'vitest'
import { formatAxisTick, formatCurrency } from './format'

describe('formatCurrency', () => {
  it('formats cents as a dollar amount', () => {
    expect(formatCurrency(1050)).toBe('$10.50')
  })

  it('supports other currencies', () => {
    expect(formatCurrency(500, 'EUR')).toBe('€5.00')
  })
})

describe('formatAxisTick', () => {
  it('keeps small-range ticks distinct', () => {
    expect(formatAxisTick(0.08, [0, 0.25])).toBe('0.08')
    expect(formatAxisTick(0.12, [0, 0.25])).toBe('0.12')
  })

  it('shows one decimal on a percent-scale axis', () => {
    expect(formatAxisTick(92.666, [47, 101])).toBe('92.7')
  })

  it('drops decimals on large counts', () => {
    expect(formatAxisTick(1608.33, [0, 1800])).toBe('1,608')
  })
})
