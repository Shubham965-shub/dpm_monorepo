/** A short dashed swatch + label, e.g. "Alert limit" / "Action limit" - shared by SeverityKpiCard's
 * chart legend and TimelineChartControls' page-level legend, so the two never drift apart. */
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
