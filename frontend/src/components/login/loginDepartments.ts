/**
 * Data + hand-drawn SVG motifs for the rotating login hero (ported from the
 * dpm-login-rotating-hero.html design mockup, kept as string-built SVG markup rather than JSX so
 * the ported path/loop math stays exactly as designed - see LoginHero.tsx for how it's rendered).
 */

export interface DeptKpi {
  /** Name, already uppercase per the design's mono-caps callout style. */
  n: string
  v: string
  u: string
  /** 0-1, drives the KPI's progress bar fill width. */
  p: number
  s: 'ok' | 'warn'
}

export interface Department {
  id: string
  name: string
  code: string
  blurb: string
  kpis: DeptKpi[]
  motif: () => string
}

export const DEPARTMENTS: Department[] = [
  {
    id: 'QC',
    name: 'Quality Control',
    code: 'QC · LABORATORY',
    blurb: 'HPLC bank in elevation with the LIMS queue running beneath it. The accent trace is the pending-test backlog burning down.',
    kpis: [
      { n: 'VALID OOS / 1000 TESTS', v: '1.8', u: '', p: 0.72, s: 'ok' },
      { n: 'LAB TAT / SLA ADHERENCE', v: '93.4', u: '%', p: 0.93, s: 'ok' },
      { n: 'ANALYST ERROR iOOS', v: '4', u: 'MTD', p: 0.4, s: 'warn' },
      { n: 'INSTRUMENT AVAILABILITY', v: '96.2', u: '%', p: 0.96, s: 'ok' },
    ],
    motif: qcMotif,
  },
  {
    id: 'QA',
    name: 'Quality Assurance',
    code: 'QA · BATCH RELEASE',
    blurb: 'Batch record stack in axonometric with an approval ring. The trace is deviation aging crossing the 30-day line.',
    kpis: [
      { n: 'RIGHT FIRST TIME', v: '97.1', u: '%', p: 0.97, s: 'ok' },
      { n: 'OPEN CRITICAL DEVIATIONS', v: '3', u: '', p: 0.3, s: 'warn' },
      { n: 'CAPA EFFECTIVENESS PASS', v: '91.5', u: '%', p: 0.91, s: 'ok' },
      { n: 'RELEASE CYCLE TIME', v: '6.4', u: 'DAYS', p: 0.64, s: 'ok' },
    ],
    motif: qaMotif,
  },
  {
    id: 'MFG',
    name: 'Manufacturing Operations',
    code: 'MFG · COMPRESSION',
    blurb: 'Compression rotor in section with an OEE dial struck through it. The trace is the needle sweeping to the live figure.',
    kpis: [
      { n: 'OEE', v: '78.4', u: '%', p: 0.784, s: 'warn' },
      { n: 'YIELD', v: '98.3', u: '%', p: 0.983, s: 'ok' },
      { n: 'UPTIME OF EQUIPMENT', v: '94.7', u: '%', p: 0.947, s: 'ok' },
      { n: 'SERIAL CHANGEOVER TIME', v: '3.6', u: 'HRS', p: 0.6, s: 'ok' },
    ],
    motif: mfgMotif,
  },
  {
    id: 'ENGG',
    name: 'Engineering',
    code: 'ENGG · UTILITIES & ASSETS',
    blurb: 'Pump-and-motor skid with a vibration trace and the PM calendar grid alongside. The trace is the live vibration signature.',
    kpis: [
      { n: 'BREAKDOWN FREQUENCY RATE', v: '0.42', u: '/ 1000 H', p: 0.42, s: 'ok' },
      { n: 'PM & CALIBRATION ON TIME', v: '89.0', u: '%', p: 0.89, s: 'warn' },
      { n: 'EQUIPMENT WITH LIVE MONITORING', v: '72', u: '%', p: 0.72, s: 'warn' },
      { n: 'ENERGY USAGE VS NORM', v: '104', u: '% NORM', p: 0.7, s: 'warn' },
    ],
    motif: engMotif,
  },
  {
    id: 'SCP',
    name: 'Supply Chain & Planning',
    code: 'SC · WAREHOUSE & PLAN',
    blurb: 'Racking in axonometric with flow arrows in and out, and an inventory-turn ring. The trace is stock moving through the aisle.',
    kpis: [
      { n: 'INVENTORY TURNS · RM', v: '9.4', u: 'TIMES', p: 0.78, s: 'ok' },
      { n: 'INVENTORY DAYS IN HAND · RM', v: '38', u: 'DAYS', p: 0.55, s: 'warn' },
      { n: 'SLOW MOVING INVENTORY', v: '4.1', u: '%', p: 0.41, s: 'ok' },
      { n: 'CONFIRMED SO FOR MONTHLY PLAN', v: '86', u: '%', p: 0.86, s: 'ok' },
    ],
    motif: scMotif,
  },
]

function vials(n: number, r: number): string {
  let s = ''
  for (let i = 0; i < n; i++) {
    const a = (i * 2 * Math.PI) / n
    s += `<circle cx="${(Math.cos(a) * r).toFixed(1)}" cy="${(Math.sin(a) * r).toFixed(1)}" r="4.2" class="${i < 7 ? 'ls' : 'lf'}"${i < 7 ? ' fill="var(--line-faint)"' : ''}/>`
  }
  return s
}

function qcMotif(): string {
  return (
    '<g class="ls">' +
    '<rect x="46" y="28" width="96" height="44" rx="3"/><rect x="46" y="76" width="96" height="44" rx="3"/>' +
    '<rect x="46" y="124" width="96" height="52" rx="3"/>' +
    '<path d="M60 40h30M60 50h48M60 88h40M60 98h22"/>' +
    '<circle cx="126" cy="46" r="4"/><circle cx="126" cy="94" r="4"/>' +
    '</g>' +
    '<g class="ln"><rect x="42" y="24" width="104" height="156" rx="4"/></g>' +
    '<text x="150" y="34" class="lbl">DETECTOR</text>' +
    '<text x="150" y="82" class="lbl">PUMP</text>' +
    '<text x="150" y="134" class="lbl">COLUMN OVEN</text>' +
    `<g transform="translate(262,84)"><circle r="42" class="ln"/><circle r="30" class="ls dash"/>${vials(12, 36)}<circle r="4" class="ls"/></g>` +
    '<text x="228" y="142" class="lbl">AUTOSAMPLER · 12 V</text>' +
    '<path d="M42 206h316" class="lf"/>' +
    '<text x="42" y="198" class="lbl">PENDING TESTS · LIMS</text>' +
    '<path d="M42 222 L88 218 L134 224 L180 210 L226 214 L272 198 L318 192 L358 186" class="acc trace" pathLength="1"/>' +
    '<circle cx="358" cy="186" r="3" fill="var(--accent)"/>'
  )
}

function qaMotif(): string {
  let s = ''
  for (let i = 0; i < 5; i++) {
    const y = 150 - i * 22
    s += `<g transform="translate(40,${y})" class="${i === 4 ? 'ln' : 'ls'}"><path d="M0 0 L104 -34 L208 0 L104 34 Z"/></g>`
  }
  s += '<g class="lf"><path d="M40 150 L40 62M248 150 L248 62"/></g>'
  s += '<text x="266" y="58" class="lbl">BMR / BPR · 5 LOTS</text>'
  s += '<text x="266" y="72" class="lbl2">ISSUED · RECEIPT BACK PENDING 1</text>'
  s +=
    '<g transform="translate(314,150)"><circle r="34" class="ln"/><circle r="26" class="ls dash"/>' +
    '<path d="M-11 0 l8 9 l16 -19" class="acc trace" pathLength="1"/></g>'
  s += '<text x="274" y="198" class="lbl">RELEASED</text>'
  s += '<text x="40" y="186" class="lbl">DEVIATION AGING · DAYS</text>'
  s +=
    '<g><rect x="40" y="196" width="120" height="7" rx="2" class="fs"/><rect x="40" y="196" width="78" height="7" rx="2" fill="var(--accent)" class="bar"/>' +
    '<rect x="40" y="210" width="120" height="7" rx="2" class="fs"/><rect x="40" y="210" width="112" height="7" rx="2" fill="var(--warn)" class="bar" style="animation-delay:.15s"/>' +
    '<path d="M148 190v34" class="lf dash"/><text x="152" y="232" class="lbl2">30 D</text></g>'
  return s
}

function punches(n: number, r1: number, r2: number): string {
  let s = ''
  for (let i = 0; i < n; i++) {
    const a = (i * 2 * Math.PI) / n - Math.PI / 2
    s += `<path d="M${(Math.cos(a) * r1).toFixed(1)} ${(Math.sin(a) * r1).toFixed(1)} L${(Math.cos(a) * r2).toFixed(1)} ${(Math.sin(a) * r2).toFixed(1)}" class="ls"/>`
    s += `<circle cx="${(Math.cos(a) * r1).toFixed(1)}" cy="${(Math.sin(a) * r1).toFixed(1)}" r="4" class="${i < 8 ? 'ls' : 'lf'}"/>`
  }
  return s
}

function ticks(n: number, r1: number, r2: number): string {
  let s = ''
  for (let i = 0; i <= n; i++) {
    const a = ((-225 + i * (270 / n)) * Math.PI) / 180
    const mj = i % 5 === 0
    s += `<path d="M${(Math.cos(a) * (mj ? r1 - 6 : r1)).toFixed(1)} ${(Math.sin(a) * (mj ? r1 - 6 : r1)).toFixed(1)} L${(Math.cos(a) * r2).toFixed(1)} ${(Math.sin(a) * r2).toFixed(1)}" stroke="var(--line-soft)" stroke-width="${mj ? 1.2 : 0.8}"/>`
  }
  return s
}

function mfgMotif(): string {
  return (
    '<g transform="translate(120,118)">' +
    '<circle r="78" class="ln"/><circle r="64" class="ls dash"/><circle r="14" class="ls"/>' +
    punches(10, 64, 78) +
    '<path d="M-78 0h156" class="lf"/><path d="M0 -78v156" class="lf"/>' +
    '</g>' +
    '<text x="60" y="216" class="lbl">TURRET · 10 STATION</text>' +
    '<path d="M42 200h156" class="lf"/>' +
    '<g transform="translate(300,112)">' +
    '<circle r="74" class="lf"/><circle r="62" class="ln"/>' +
    ticks(26, 52, 62) +
    '<path d="M-53.7 31 A62 62 0 1 1 46 41" class="acc trace" pathLength="1"/>' +
    '<path d="M0 0 L38 -44" class="ls" stroke-width="2"/><circle r="5" class="ln" fill="var(--bg-2)"/>' +
    '<text y="34" text-anchor="middle" class="lblg">OEE 78.4</text>' +
    '</g>' +
    '<text x="252" y="216" class="lbl">A × P × Q · LIVE</text>' +
    '<path d="M240 200h120" class="lf"/>'
  )
}

function cal(): string {
  let s = ''
  let i = 0
  for (let r = 0; r < 4; r++) {
    for (let c = 0; c < 6; c++) {
      i++
      const done = i <= 17
      s += `<rect x="${c * 11}" y="${r * 11}" width="8" height="8" rx="1.5" ${done ? 'fill="var(--accent)" opacity=".45" stroke="none"' : 'class="ls"'}/>`
    }
  }
  return s
}

function engMotif(): string {
  return (
    '<path d="M40 176h230" class="ln"/><path d="M52 176v12M258 176v12" class="ls"/>' +
    '<g class="ln"><rect x="60" y="120" width="78" height="56" rx="4"/></g>' +
    '<g class="ls"><path d="M68 132h62M68 142h44M68 152h54"/></g>' +
    '<text x="62" y="112" class="lbl">MOTOR 45 KW</text>' +
    '<g transform="translate(200,146)"><circle r="30" class="ln"/><circle r="18" class="ls"/>' +
    '<path d="M0 -30 A30 30 0 0 1 26 15" class="ls"/><path d="M-30 30h60" class="ls"/>' +
    '<circle r="3" class="ls"/></g>' +
    '<text x="176" y="106" class="lbl">CENTRIFUGAL PUMP</text>' +
    '<path d="M138 146h32M230 146h28" class="ls"/>' +
    '<g transform="translate(258,110)"><circle r="12" class="ls"/><path d="M0 0 L6 -8" class="ls"/><path d="M0 12v10" class="ls"/></g>' +
    '<text x="40" y="212" class="lbl">VIBRATION · MM/S RMS</text>' +
    '<path d="M40 234 q10 -16 20 0 t20 0 t20 0 t20 -2 t20 4 t20 -10 t20 12 t20 -6 t20 2 t20 0 t20 0 t20 0 t20 0" class="acc trace" pathLength="1"/>' +
    '<path d="M40 222h320" class="lf dash"/><text x="330" y="218" class="lbl2">ALERT</text>' +
    `<g transform="translate(296,140)">${cal()}</g>` +
    '<text x="292" y="132" class="lbl">PM PLAN VS ACTUAL</text>'
  )
}

function scMotif(): string {
  let s = ''
  for (let i = 0; i < 3; i++) {
    const y = 170 - i * 36
    s += `<g transform="translate(60,${y})" class="${i === 0 ? 'ln' : 'ls'}"><path d="M0 0 L90 -30 L180 0 L90 30 Z"/></g>`
  }
  s += '<g class="lf"><path d="M60 170V98M240 170V98M150 200V128"/></g>'
  s +=
    '<g class="ls"><path d="M96 122 L120 114 L144 122 L120 130 Z"/><path d="M120 114v-14M96 122v-14M144 122v-14"/></g>'
  s += '<g class="lf"><path d="M150 140 L174 132 L198 140 L174 148 Z"/></g>'
  s += '<path d="M30 152 L54 152" class="acc"/><path d="M48 148l7 4l-7 4z" fill="var(--accent)"/>'
  s += '<text x="22" y="142" class="lbl">RM IN</text>'
  s += '<path d="M246 152 L272 152" class="ls"/><path d="M266 148l7 4l-7 4z" fill="var(--line-soft)"/>'
  s += '<text x="248" y="142" class="lbl">FG OUT</text>'
  s +=
    '<g transform="translate(324,96)"><circle r="40" class="lf"/><circle r="32" class="ls dash"/>' +
    '<path d="M0 -32 A32 32 0 1 1 -22 23" class="acc trace" pathLength="1"/>' +
    '<text y="4" text-anchor="middle" class="lblg" font-size="11">9.4×</text>' +
    '<text y="18" text-anchor="middle" class="lbl" font-size="7">TURNS</text></g>'
  s += '<text x="40" y="212" class="lbl">DAYS IN HAND · RM</text>'
  s += '<rect x="40" y="222" width="250" height="8" rx="2" class="fs"/>'
  s += '<rect x="40" y="222" width="138" height="8" rx="2" fill="var(--warn)" class="bar"/>'
  s += '<path d="M212 216v20" class="lf dash"/><text x="216" y="240" class="lbl2">TARGET 30 D</text>'
  return s
}

/** Builds one department's full plate SVG markup (frame, motif, KPI column, department rail). */
export function renderPlateSvg(d: Department): string {
  const W = 880
  const H = 470
  let s = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="${d.name} plate">`
  s += '<rect x="20" y="20" width="840" height="430" rx="4" class="lf dash"/>'
  s += '<path d="M20 46V20h26M834 20h26v26M860 424v26h-26M46 450H20v-26" class="ls"/>'
  s += `<text x="46" y="14" class="lbl">${d.code}</text>`
  s += '<text x="700" y="14" class="lbl">SHIFT A · 06:00 IST</text>'
  s += `<g class="m-in" transform="translate(48,74)">${d.motif()}</g>`
  s += '<path d="M498 60v330" class="lf"/>'
  for (let i = 0; i < d.kpis.length; i++) {
    const k = d.kpis[i]
    const y = 96 + i * 78
    const col = k.s === 'warn' ? 'var(--warn)' : 'var(--accent)'
    s += `<g class="kpi" style="animation-delay:${(0.35 + i * 0.12).toFixed(2)}s">`
    s += `<text x="536" y="${y - 16}" class="lbl">${k.n}</text>`
    s += `<text x="536" y="${y + 18}" class="num" font-size="32">${k.v}</text>`
    s += `<text x="${536 + String(k.v).length * 20 + 8}" y="${y + 18}" class="lbl" font-size="9">${k.u}</text>`
    s += `<rect x="536" y="${y + 30}" width="270" height="4" rx="2" class="fs"/>`
    s += `<rect x="536" y="${y + 30}" width="${(270 * Math.max(0.08, Math.min(1, k.p))).toFixed(0)}" height="4" rx="2" fill="${col}" class="bar" style="animation-delay:${(0.45 + i * 0.12).toFixed(2)}s"/>`
    s += `<circle cx="820" cy="${y + 8}" r="3.2" fill="${col}"${k.s === 'warn' ? ' class="blip"' : ''}/>`
    s += '</g>'
  }
  s += '<path d="M48 418h764" class="lf"/>'
  for (let i = 0; i < DEPARTMENTS.length; i++) {
    const x = 48 + i * 152
    const on = DEPARTMENTS[i].id === d.id
    s += `<path d="M${x} 412v12" stroke="${on ? 'var(--accent)' : 'var(--line-faint)'}" stroke-width="${on ? 2 : 1}"/>`
    s += `<text x="${x + 9}" y="${on ? 437 : 436}" class="${on ? 'lblg' : 'lbl'}" font-size="8">${DEPARTMENTS[i].id}</text>`
    if (on) {
      s +=
        `<rect x="${x}" y="417" width="0" height="2.6" fill="var(--accent)" opacity=".55">` +
        '<animate attributeName="width" from="0" to="140" dur="6s" fill="freeze"/></rect>'
    }
  }
  s += '<text x="536" y="404" class="lbl2" font-size="7.5">FIGURES ILLUSTRATIVE</text>'
  s += '</svg>'
  return s
}
