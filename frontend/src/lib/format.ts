export function formatCurrency(amountInCents: number, currency = 'USD') {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amountInCents / 100)
}
