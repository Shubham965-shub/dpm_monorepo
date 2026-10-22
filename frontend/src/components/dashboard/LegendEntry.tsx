/** A short dashed swatch + label, e.g. "Alert limit" / "Action limit" - SeverityKpiCard's own chart
 * legend. */
export function LegendEntry({ colorHex, label }: { colorHex: string; label: string }) {
  return (
    <span className="flex items-center gap-1">
      <span className="h-px w-3 shrink-0 border-t border-dashed" style={{ borderColor: colorHex }} />
      <span className="text-sm font-medium" style={{ color: colorHex }}>
        {label}
      </span>
    </span>
  )
}
