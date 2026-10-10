package com.strides.service;

import com.strides.dto.KpiCardResponse;
import com.strides.model.PeriodSum;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggTwDeviationsRepository;
import com.strides.repository.QaAggRepository;
import com.strides.repository.QaKpiSpec;
import com.strides.service.kpi.PeriodCombiner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One generic fetch behind every QA KPI endpoint (see controller.QaController) - QaKpiSpec already
 * reduces every KPI to a numerator/denominator-over-period shape (see QaAggRepository), which is
 * exactly PeriodCombiner.fromTotals' own input, so no QA-specific combiner is needed.
 *
 * QA cards never carry a RAG status today (see qaKpiConfig.ts's own doc comment: the KPI
 * Framework's thresholds are unconfirmed proposals, same reasoning the original mock QA dashboard
 * was built on) - status stays null here exactly like every mock card did, only the values are now
 * real. KpiThresholdResponseFilter never touches /api/qa/* (it only resolves kpi_threshold_snapshot
 * rows for ids its catalogue knows, and no QA kpi_id is in that catalogue), so this is automatic,
 * not something this service has to suppress itself.
 */
@ApplicationScoped
public class QaKpiService {

    @Inject
    QaAggRepository repository;

    @Inject
    AggTwDeviationsRepository deviationsRepository;

    public List<KpiCardResponse> fetch(QaKpiSpec spec, String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to) {
        return fetch(spec, kpiId, name, site, grain, from, to, Map.of(), Map.of());
    }

    public List<KpiCardResponse> fetch(QaKpiSpec spec, String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to, Map<String, String> fixedFilters, Map<String, Set<String>> extraFilters) {
        List<PeriodTotal> totals = repository.findTotals(spec, site, grain, from, to, fixedFilters, extraFilters);
        return PeriodCombiner.fromTotals(totals, spec.multiplier(), kpiId, name, spec.unit());
    }

    /**
     * Instrument error deviations has no qa.agg_tw_instrument_error_deviations table live (confirmed
     * against information_schema - only 41 of the handover doc's ~42 qa tables actually exist), so
     * this reads the doc's own fallback instead: public.agg_tw_deviations filtered to
     * root_cause_bucket = 'By Instrument', via the same repository QC's own Deviations KPI already
     * uses (reuse, not a new one-off query path).
     */
    public List<KpiCardResponse> fetchInstrumentErrorDeviations(String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to) {
        List<PeriodSum> sums = deviationsRepository.findTotals(site, Set.of("By Instrument"), Set.of(), Set.of(),
                Set.of(), Set.of(), Set.of(), grain, from, to);
        return PeriodCombiner.fromSums(sums, kpiId, name, "deviations");
    }

    /** Doc: "Procedural error deviations" - served from public.agg_tw_deviations with its own
     * dedup (see AggTwDeviationsRepository.findProceduralErrorDeviationTotals). */
    public List<KpiCardResponse> fetchProceduralErrorDeviations(String kpiId, String name, String site, String grain,
            LocalDate from, LocalDate to) {
        List<PeriodSum> sums = deviationsRepository.findProceduralErrorDeviationTotals(site, grain, from, to);
        return PeriodCombiner.fromSums(sums, kpiId, name, "deviations");
    }
}
