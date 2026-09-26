import { describe, it, expect } from 'vitest'
import { defaultGrainFor } from './kpiDefaultGrain'

describe('kpiDefaultGrain - confirmed exceptions to the naive "one grain step per role" pattern', () => {
  // Re-verified against the source PDF's rendered page images (not just text extraction) before
  // locking these in - each is a real, distinct cell in the framework doc, not a transcription slip.

  it('CXO drops to quarterly (not monthly) for 3 instrument-reliability KPIs', () => {
    expect(defaultGrainFor('downtime', 'cxo')).toBe('quarterly')
    expect(defaultGrainFor('calibration-compliance', 'cxo')).toBe('quarterly')
    expect(defaultGrainFor('breakdown-frequency', 'cxo')).toBe('quarterly')
    // Every other CXO-visible KPI stays monthly - these 3 aren't part of a blanket "CXO = quarterly" rule.
    expect(defaultGrainFor('availability', 'cxo')).toBe('monthly')
  })

  it('Supervisor matches Leadership\'s weekly cadence (not its own usual daily) for investigation/cycle-time KPIs', () => {
    const weeklyForSupervisor = [
      'invalid-oos-rate',
      'confirmed-oos-rate',
      'investigation-closure-tat',
      'repeat-failure-rate',
      'root-cause',
      'human-error-rate',
      'repeat-human-error-rate',
      'human-error-severity',
      'human-error-by-product',
      'qualification-cycle-time',
    ]
    for (const kpiId of weeklyForSupervisor) {
      expect(defaultGrainFor(kpiId, 'supervisor')).toBe('weekly')
    }
    // Sibling KPIs in the very same subsections stay daily for Supervisor - confirms this is a
    // real per-KPI exception, not a domain-wide (QMS vs instrument-health) rule:
    // - "RCIs Beyond Timeline" sits directly below "Investigation Closure TAT" in the doc's own
    //   "3a. Investigation Metrics" subsection, yet stays daily.
    expect(defaultGrainFor('rci-beyond-timeline', 'supervisor')).toBe('daily')
    // - qualification-cycle-time is filed under the instrument-health domain in kpiConfig.ts, but
    //   behaves like its QMS cycle-time/investigation siblings above, not like its own domain's
    //   other KPIs (availability/downtime/calibration-compliance), which all stay daily.
    expect(defaultGrainFor('availability', 'supervisor')).toBe('daily')
  })

  it('Training Completion Rate breaks the usual Operator/Supervisor daily lockstep', () => {
    // Every other Operator-visible KPI has Operator=daily AND Supervisor=daily together.
    expect(defaultGrainFor('training-completion-rate', 'operator')).toBe('daily')
    expect(defaultGrainFor('training-completion-rate', 'supervisor')).toBe('weekly')
    // Contrast: a typical Operator-visible KPI keeps Supervisor in lockstep at daily.
    expect(defaultGrainFor('sample-tat', 'operator')).toBe('daily')
    expect(defaultGrainFor('sample-tat', 'supervisor')).toBe('daily')
  })

  it('Priority Sample Compliance: Manager/HOD disagree (Manager=daily, HOD=weekly) - merged Leadership lens picked HOD\'s weekly', () => {
    expect(defaultGrainFor('priority-sample-compliance', 'leadership')).toBe('weekly')
  })
})
