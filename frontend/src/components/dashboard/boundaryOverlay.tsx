import { ReferenceArea, ReferenceLine } from 'recharts'
import { boundaryHex, type BoundaryLine, type BoundaryZone } from './types'

/**
 * The shaded zones + labelled dashed boundary lines behind a KPI chart - one definition shared by
 * SeverityKpiCard (line and bar views) and KpiDetailModal. A plain function returning chart
 * children (not a component) so Recharts sees the ReferenceArea/ReferenceLine elements directly.
 * Zones render first so the lines and series sit on top.
 */
export function renderBoundaryOverlay(
  zones: BoundaryZone[],
  lines: BoundaryLine[],
  { strokeWidth, labelFontSize }: { strokeWidth: number; labelFontSize: number },
) {
  return [
    ...zones.map((zone) => (
      <ReferenceArea
        key={`zone-${zone.y1}-${zone.y2}`}
        y1={zone.y1}
        y2={zone.y2}
        fill={zone.fillHex}
        fillOpacity={zone.fillOpacity}
        ifOverflow="hidden"
      />
    )),
    ...lines.map((line) => (
      <ReferenceLine
        key={`line-${line.name}`}
        y={line.y}
        stroke={boundaryHex(line.tone)}
        strokeDasharray="4 3"
        strokeWidth={strokeWidth}
        label={{ value: line.label, position: line.labelPosition, fill: boundaryHex(line.tone), fontSize: labelFontSize, fontWeight: 700 }}
      />
    )),
  ]
}
