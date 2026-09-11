import { describe, expect, it } from 'vitest'
import { formatCurrency } from './format'

describe('formatCurrency', () => {
  it('formats cents as a dollar amount', () => {
    expect(formatCurrency(1050)).toBe('$10.50')
  })

  it('supports other currencies', () => {
    expect(formatCurrency(500, 'EUR')).toBe('€5.00')
  })
})
