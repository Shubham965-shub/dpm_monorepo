package com.strides.service;

import com.strides.dto.DrilldownPage;
import com.strides.dto.DrilldownSupport;
import com.strides.dto.KpiCardResponse;
import com.strides.model.PeriodSum;
import com.strides.model.PeriodTotal;
import com.strides.repository.AggTwDeviationsRepository;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.repository.QaAggRepository;
import com.strides.repository.QaDrilldownSpecs;
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

    @Inject
    GenericDrilldownService genericDrilldownService;

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

    // ---- Record-level drill-down for the KPIs with a real qa.fact_* table ----

    public DrilldownPage<Map<String, Object>> getSlaIpDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.SLA_IP, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSlaIpDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.SLA_IP, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSlaIpDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.SLA_IP, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.SLA_IP.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getStabilityTestingPendingDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.STABILITY_TESTING_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getStabilityTestingPendingDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.STABILITY_TESTING_PENDING, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getStabilityTestingPendingDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.STABILITY_TESTING_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.STABILITY_TESTING_PENDING.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getQaSamplingOnTimeDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.QA_SAMPLING_ONTIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getQaSamplingOnTimeDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.QA_SAMPLING_ONTIME, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getQaSamplingOnTimeDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.QA_SAMPLING_ONTIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.QA_SAMPLING_ONTIME.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getIpCvSampleQaApprovalPendingDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getIpCvSampleQaApprovalPendingDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getIpCvSampleQaApprovalPendingDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.IP_CV_SAMPLE_QA_APPROVAL_PENDING.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCqaAdherenceInProcessDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCqaAdherenceInProcessDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCqaAdherenceInProcessDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CQA_ADHERENCE_INPROCESS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getLineClearanceTatDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.LINE_CLEARANCE_TAT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getLineClearanceTatDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.LINE_CLEARANCE_TAT, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getLineClearanceTatDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.LINE_CLEARANCE_TAT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.LINE_CLEARANCE_TAT.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getReceiptBackBmrBprDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getReceiptBackBmrBprDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getReceiptBackBmrBprDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.RECEIPT_BACK_BMR_BPR.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getUndispensedBmrBprBeyond60DaysDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getUndispensedBmrBprBeyond60DaysDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getUndispensedBmrBprBeyond60DaysDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.UNDISPENSED_BMR_BPR_BEYOND_60_DAYS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getSuccessRateValidationBatchesDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSuccessRateValidationBatchesDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSuccessRateValidationBatchesDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.SUCCESS_RATE_VALIDATION_BATCHES.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getBatchRejectDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.BATCH_REJECT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getBatchRejectDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.BATCH_REJECT, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getBatchRejectDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.BATCH_REJECT, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.BATCH_REJECT.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getBatchesErrorIncomingInspectionDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getBatchesErrorIncomingInspectionDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getBatchesErrorIncomingInspectionDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.BATCHES_ERROR_INCOMING_INSPECTION.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getSfgReleaseDelaysDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.SFG_RELEASE_DELAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSfgReleaseDelaysDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.SFG_RELEASE_DELAYS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSfgReleaseDelaysDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.SFG_RELEASE_DELAYS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.SFG_RELEASE_DELAYS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getPlanVsActualPmCalibrationDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getPlanVsActualPmCalibrationDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getPlanVsActualPmCalibrationDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.PM_CALIBRATION_PLAN_VS_ACTUAL.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRightFirstTimeDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.RIGHT_FIRST_TIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRightFirstTimeDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.RIGHT_FIRST_TIME, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRightFirstTimeDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.RIGHT_FIRST_TIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.RIGHT_FIRST_TIME.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getDeviationsShopfloorDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getDeviationsShopfloorDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getDeviationsShopfloorDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.DEVIATIONS_SHOPFLOOR.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getInstrumentErrorDeviationsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInstrumentErrorDeviationsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getInstrumentErrorDeviationsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.INSTRUMENT_ERROR_DEVIATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getSelfReportingQualityEventsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getSelfReportingQualityEventsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getSelfReportingQualityEventsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.SELF_REPORTING_QUALITY_EVENTS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getQEventsClosureTimeDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getQEventsClosureTimeDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getQEventsClosureTimeDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.Q_EVENTS_CLOSURE_TIME.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCapaApprovalWithinSlaDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCapaApprovalWithinSlaDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCapaApprovalWithinSlaDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CAPA_APPROVAL_WITHIN_SLA.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getCcApprovalWithinSlaDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getCcApprovalWithinSlaDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getCcApprovalWithinSlaDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.CC_APPROVAL_WITHIN_SLA.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getOpenNewEquipmentFacilityModCcsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getOpenNewEquipmentFacilityModCcsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getOpenNewEquipmentFacilityModCcsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.OPEN_NEW_EQUIPMENT_FACILITY_MOD_CCS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getOverdueQmsRecordsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.OVERDUE_QMS_RECORDS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getOverdueQmsRecordsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.OVERDUE_QMS_RECORDS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getOverdueQmsRecordsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.OVERDUE_QMS_RECORDS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.OVERDUE_QMS_RECORDS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRegCustomerAuditSuccessRateDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRegCustomerAuditSuccessRateDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRegCustomerAuditSuccessRateDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.REG_CUSTOMER_AUDIT_SUCCESS_RATE.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getInternalAuditObservationSeverityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInternalAuditObservationSeverityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getInternalAuditObservationSeverityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.INTERNAL_AUDIT_OBSERVATION_SEVERITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getInternalAuditOnTimeActionsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getInternalAuditOnTimeActionsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getInternalAuditOnTimeActionsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.INTERNAL_AUDIT_ONTIME_ACTIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getAuditCriticalObservationsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getAuditCriticalObservationsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getAuditCriticalObservationsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.AUDIT_CRITICAL_OBSERVATIONS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getObservationsOpenDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.OBSERVATIONS_OPEN, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getObservationsOpenDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.OBSERVATIONS_OPEN, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getObservationsOpenDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.OBSERVATIONS_OPEN, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.OBSERVATIONS_OPEN.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getAdherenceRegulatoryGuidanceMonographDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getAdherenceRegulatoryGuidanceMonographDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getAdherenceRegulatoryGuidanceMonographDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.ADHERENCE_REGULATORY_GUIDANCE_MONOGRAPH.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getProductComplaintCriticalityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getProductComplaintCriticalityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getProductComplaintCriticalityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.PRODUCT_COMPLAINT_CRITICALITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRepeatComplaintsDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.REPEAT_COMPLAINTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRepeatComplaintsDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.REPEAT_COMPLAINTS, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRepeatComplaintsDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.REPEAT_COMPLAINTS, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.REPEAT_COMPLAINTS.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getMarketComplaintsTrackingDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getMarketComplaintsTrackingDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getMarketComplaintsTrackingDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.MARKET_COMPLAINTS_TRACKING.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getRecallEventDensityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.RECALL_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getRecallEventDensityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.RECALL_EVENT_DENSITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getRecallEventDensityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.RECALL_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.RECALL_EVENT_DENSITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }

    public DrilldownPage<Map<String, Object>> getFarAlertEventDensityDrilldown(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange, int page, int pageSize) {
        return genericDrilldownService.fetch(QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange), page, pageSize);
    }

    public Map<String, List<String>> getFarAlertEventDensityDrilldownColumnOptions(String site, LocalDate from, LocalDate to) {
        return genericDrilldownService.fetchColumnOptions(QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY, site, from, to, Map.of());
    }

    /** CSV text for the drill-down table's Export button - see GenericDrilldownService.fetchAllForExport. */
    public String getFarAlertEventDensityDrilldownExport(String site, LocalDate from, LocalDate to,
            String q, List<String> sort, List<String> colFilter, List<String> colSearch, List<String> colRange) {
        List<Map<String, Object>> rows = genericDrilldownService.fetchAllForExport(QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY, site, from, to, Map.of(), q,
                DrilldownSupport.parseSort(sort), DrilldownSupport.parseColumnFilters(colFilter),
                DrilldownSupport.parseColumnSearches(colSearch), DrilldownSupport.parseColumnRanges(colRange));
        List<String> headers = QaDrilldownSpecs.FAR_ALERT_EVENT_DENSITY.selectColumns().stream().map(GenericDrilldownRepository.Column::jsonKey).toList();
        return DrilldownSupport.toCsv(headers, rows);
    }
}
