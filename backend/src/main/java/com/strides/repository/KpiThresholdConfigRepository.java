package com.strides.repository;

import com.strides.model.KpiThresholdConfigRow;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MOCK - serves the KPI threshold config table from memory until the real table exists in the
 * dpm database. Replace this class's body with a JDBC query against that table; its method
 * signatures are the contract the service layer relies on, so nothing above this layer changes.
 *
 * Source_System / Description (not in the export yet) come from the DPM QC KPI Data & Logic
 * Validation sheet: its Source System, and its "What it measures / logic used" lines joined.
 *
 * KPI-QC-003 and KPI-QC-007 come from the table export (2026-09-23; 007's band converted from
 * fractions to percent). Every other row's
 * band values are placeholders, not agreed limits (alert -> green boundary, action -> red
 * boundary; target dropped), from one of two sources:
 *  - the original catalogue/Figma limits, where real performance simply misses them - so a bad
 *    KPI reads red: 002, 010, 023, 028, 032, 036, 038;
 *  - typical pharma QC / GMP practice for compliance-type KPIs expected at or near 100%
 *    (calibration, PM, training, RFT, CAPA effectiveness) and the 30-day investigation closure
 *    limit: 012, 013, 018, 027, 033, 037;
 *  - the frontend's former rescaled placeholders, where the original was in a different unit or
 *    scale (e.g. a monthly count vs the API's per-day value, ~10x off): every other row.
 */
@ApplicationScoped
public class KpiThresholdConfigRepository {

    private static final String BETTER = "higher_is_better";
    private static final String WORSE = "higher_is_worse";
    private static final String BAND = "band";
    private static final String NO_AUTO_MODE = "none";
    private static final String MOCK_UPDATED_BY = "mock";

    private static final String LIMS = "LIMS";
    private static final String TRACKWISE = "TrackWise";
    private static final String TRACKWISE_AND_LIMS = "TrackWise + LIMS";
    private static final String CHROMELEON = "Chromeleon (Chromatography Data System)";
    private static final String LMS = "Learning Management System";
    private static final String EXPORT_UPDATED_BY = "mckinseyuser";

    private static final List<KpiThresholdConfigRow> ROWS = List.of(
            higherIsWorse("KPI-QC-001", "Sample TAT (Turnaround Time)",
                    LIMS, "Looks at completed samples in LIMS. Turnaround = days from sampling date to test completion date (only samples with a positive turnaround time are counted). Shown as 3 separate averages by default: RM, IP, and PV samples. User can select any other sample type instead.", "28", "34"),
            higherIsBetter("KPI-QC-002", "SLA Adherence (RM, IP, FP, Stability)",
                    LIMS, "Looks at completed samples in LIMS (status Approved or Completed). Each sample gets a working-day SLA target by type: RM = 15 working days, Stability = 20 working days, FP/IP/FG = 10 working days. Due date = sampled date + SLA working days. Sundays are always off; the first Saturday of the month is also off, but only for KRSG. 'On time' = completed on or before the due date. Result = on-time samples divided by total samples (shown as a %).", "90", "82"),
            exported("KPI-QC-003", "Priority Sample Compliance",
                    LIMS, "Looks at completed samples in LIMS (excludes PM, CB, VER and EXC sample types). Each sample has an SLA target in days by sample type: CV = 0, HT/PV/ST = 6, RM/SFG = 10, all others (AQ/PC/MIS/PAC/RA/Stability/VS/WS/PW) = 30 days. CV and IP samples are always labelled P1. Other samples: P2 if completed with under 24 hours to spare before the SLA deadline, P3 if between 24 and 72 hours to spare, P4 if 72+ hours to spare. Reports the % of samples landing in each band - P1, P2, and P4 - each as that band's count divided by total samples.", BETTER, NO_AUTO_MODE,
                    null, null, null, null, "2026-09-23T10:04:04.752095Z"),
            higherIsBetter("KPI-QC-004", "Tests per Analyst per Day (RM / IP / FP / Stability)",
                    LIMS, "Looks at completed tests in LIMS. Result = distinct tests divided by total analyst-days worked (the number of distinct analysts active each day, added up across every day in the period).", "9", "6"),
            higherIsBetter("KPI-QC-005", "HPLC Sequences Executed per Analyst",
                    CHROMELEON, "Looks at HPLC sequence runs in Chromeleon where the injection status is Finished. Result = distinct sequences divided by distinct analysts (operator name; excludes 'Unknown' operator).", "70", "50"),
            higherIsBetter("KPI-QC-006", "HPLC Samples Injected per Analyst",
                    CHROMELEON, "Looks at HPLC injections in Chromeleon where the injection status is Finished. Result = distinct injections divided by distinct analysts (excludes 'Unknown' operator).", "9", "6"),
            // The export's real auto band is 0.85-0.98 (fractions); held here in percent because the
            // utilization API reports percent (e.g. 59.79). The real table must use the same unit.
            // Full band, all four columns set: red < 65 | amber 65-85 | green 85-95 | amber 95-98 | red > 98.
            // Green starts at the export's real auto band floor (85, converted from 0.85); red < 65 is
            // the original catalogue utilization action limit. The upper side is a mock (the export has
            // no red bounds): above ~95% an instrument is a bottleneck, and past its real 98 ceiling red.
            exported("KPI-QC-007", "HPLC Utilization %",
                    LIMS, "Looks at instrument usage-hours logged in LIMS for instruments in the HPLC group. Result = used hours divided by available hours (shown as a %).", BAND, "auto",
                    new BigDecimal("65"), new BigDecimal("85"), new BigDecimal("95"), new BigDecimal("98"),
                    "2026-09-23T10:39:52.67391Z"),
            band("KPI-QC-008", "GC Utilization %",
                    LIMS, "Looks at instrument usage-hours logged in LIMS for instruments in the GC group. Result = used hours divided by available hours (shown as a %).", "65", "85", "95", "98"),
            band("KPI-QC-009", "Dissolution Apparatus Utilization %",
                    LIMS, "Looks at instrument usage-hours logged in LIMS for instruments in the Dissolution group. Result = used hours divided by available hours (shown as a %).", "65", "85", "95", "98"),
            higherIsBetter("KPI-QC-010", "Instrument Availability %",
                    LIMS, "Looks at all instruments logged in LIMS. Result = actual available hours divided by (24 hours x number of instrument-days), shown as a %.", "93", "88"),
            higherIsWorse("KPI-QC-011", "Instrument Downtime %",
                    LIMS, "Looks at all instruments logged in LIMS. Result = breakdown hours divided by (24 hours x number of instrument-days), shown as a %.", "5", "8"),
            higherIsBetter("KPI-QC-012", "Calibration Compliance %",
                    LIMS, "Looks at calibration activities logged in LIMS. Denominator excludes activities not yet due (status 'Within Due Date'). Result = on-time calibrations divided by (on-time + overdue/late calibrations), shown as a %.", "98", "95"),
            higherIsBetter("KPI-QC-013", "PM Adherence %",
                    LIMS, "Looks at Preventive Maintenance (PM) activities logged in LIMS. Denominator excludes activities not yet due (status 'Within Due Date'). Result = on-time PM activities divided by (on-time + overdue/late PM activities), shown as a %.", "95", "90"),
            higherIsWorse("KPI-QC-014", "Breakdown Frequency",
                    LIMS, "Looks at instrument breakdown events in LIMS. Result = number of breakdown events divided by number of days in the period (breakdowns per day).", "1.2", "1.8"),
            higherIsWorse("KPI-QC-015", "Instrument Failure Rate",
                    LIMS, "Looks at instrument breakdown events and usage hours in LIMS. Result = breakdown count divided by effective usage hours, multiplied by 100.", "0.10", "0.20"),
            higherIsWorse("KPI-QC-016", "Invalid OOS Rate",
                    TRACKWISE_AND_LIMS, "Numerator: Invalid OOS records in TrackWise (investigation attributed to lab/analyst cause), excluding Raw Material and Packing Material. Denominator: tests performed in LIMS for IP, SFG, Stability and FG sample types (RM tests excluded). Result = invalid OOS divided by tests, multiplied by 1000.", "1.5", "2.5"),
            higherIsWorse("KPI-QC-017", "Confirmed OOS Rate",
                    TRACKWISE_AND_LIMS, "Numerator: Confirmed (valid) OOS records in TrackWise, excluding Raw Material and Packing Material. Denominator: tests performed in LIMS for IP, SFG, Stability and FG sample types. Result = confirmed OOS divided by tests, multiplied by 1000.", "1.0", "2.0"),
            higherIsWorse("KPI-QC-018", "Investigation Closure TAT",
                    TRACKWISE, "Looks at closed root-cause investigations in TrackWise. Closure time = days from date opened to date closed. Reported as the median closure time.", "25", "30"),
            higherIsWorse("KPI-QC-019", "RCIs Beyond Timeline",
                    TRACKWISE, "Looks at root-cause investigations in TrackWise. Counts investigations whose due date has passed and which are either still open, or were closed after their due date.", "480", "550"),
            higherIsWorse("KPI-QC-020", "Repeat Failure Rate",
                    TRACKWISE, "Looks at Deviation and OOS records in TrackWise (OOT is not included - it has no recurring flag in TrackWise). An event is 'repeat' when its recurring flag = Yes. Result = repeat events divided by total events (shown as a %).", "10", "15"),
            // Per root-cause category per month: the busiest categories (Instrument, Human) run ~15-50.
            higherIsWorse("KPI-QC-021", "Root-Cause View",
                    TRACKWISE, "Looks at Deviation, OOS and OOT records in TrackWise. Each record is assigned to one root-cause bucket (By Instrument / Human / SOP / STP / Environment / Material / Sample / Software / Data Integrity / Vendor / Utility) based on its Causal Factor, Causal Factor Subcategory, Overall Category, and Record Category. Individual views filter to one bucket at a time; can be sliced further by site, date, etc.", "10", "20"),
            unconfigured("KPI-QC-022", "Consolidated Root-Cause View",
                    TRACKWISE, "Same table and bucketing as the Root-Cause View, shown with all buckets together (unfiltered).", WORSE),
            higherIsWorse("KPI-QC-023", "Human Error Rate",
                    TRACKWISE_AND_LIMS, "Numerator: Deviation, OOS and OOT records in TrackWise whose root cause is By Human. Denominator: total distinct tests performed, from LIMS. Result = human-error records divided by tests, multiplied by 1000.", "4", "6"),
            higherIsWorse("KPI-QC-024", "Repeat Human Error Rate",
                    TRACKWISE_AND_LIMS, "Same population as Human Error Rate, restricted to records flagged as recurring. Note: the recurring flag is only available for OOS and Deviation records closed at KRSG - it is blank (not zero) for other sites, and OOT has no recurring flag at all. Result = recurring human-error records divided by tests, multiplied by 1000.", "0.1", "0.2"),
            higherIsWorse("KPI-QC-025", "Human Error Severity",
                    TRACKWISE, "Looks at the same By-Human records in TrackWise. Reported as two separate counts: records classified Critical, and records classified Major (no Minor category tracked here).", "6", "9"),
            unconfigured("KPI-QC-026", "Human Error by Product",
                    TRACKWISE, "Looks at the same By-Human records in TrackWise. Counts records grouped by the affected material/product.", WORSE),
            higherIsBetter("KPI-QC-027", "CAPA Effectiveness",
                    TRACKWISE, "Looks at CAPA and Change-Control effectiveness checks in TrackWise (Closed - Done, with a conclusion recorded). A check 'passes' when its conclusion is CAPA Effective or Task Effective. Result = passed checks divided by total checks (shown as a %).", "90", "80"),
            higherIsWorse("KPI-QC-028", "CAPA Recurrence Rate",
                    TRACKWISE, "Looks at all CAPAs in TrackWise. A CAPA is 'recurring' when its 'CAPA for recurring QMS event' field = Yes. Result = recurring CAPAs divided by total CAPAs (shown as a %).", "10", "15"),
            // The former "Open CAPA by Date Opened" placeholder - the dashboard's CAPA status trend.
            higherIsWorse("KPI-QC-029", "CAPA Status / Aging",
                    TRACKWISE, "Looks at all CAPAs in TrackWise. Shows each CAPA's current status, and its aging (days from open date to completion, or to today if still open).", "20", "30"),
            higherIsWorse("KPI-QC-030", "OOS Trend",
                    TRACKWISE, "Counts all OOS records logged in TrackWise, over time.", "48", "55"),
            higherIsWorse("KPI-QC-031", "OOT Trend",
                    TRACKWISE, "Counts all OOT records logged in TrackWise, over time.", "90", "110"),
            higherIsWorse("KPI-QC-032", "Deviations",
                    TRACKWISE, "Counts all deviations logged in TrackWise, over time (any classification).", "35", "50"),
            higherIsBetter("KPI-QC-033", "Right First Time (RFT) Documentation",
                    LIMS, "Looks at completed tests in LIMS, across all sample types (including RM). A test is 'Right First Time' when its result was never modified after entry. Result = unmodified tests divided by total tests (shown as a %).", "95", "90"),
            higherIsWorse("KPI-QC-034", "Method Failure Rate",
                    TRACKWISE, "Looks at Deviation, OOS and OOT records in TrackWise whose overall category is Laboratory Control, causal factor is Method, and causal factor subcategory is Analytical Method Deficiencies or Specification/STP. Reported as a count of records (not a %).", "10", "15"),
            unconfigured("KPI-QC-035", "Product-wise Failure",
                    TRACKWISE, "Looks at closed, batch-linked quality records in TrackWise where the affected material is Finished Goods or Semi-Finished Goods (Raw Materials excluded by material code). Counts records grouped by material type (FG/SFG) and batch disposition.", WORSE),
            higherIsWorse("KPI-QC-036", "Qualification Cycle Time",
                    LIMS, "Looks at analyst qualification records in LIMS. Result = average number of days to first successful qualification completion.", "30", "45"),
            higherIsBetter("KPI-QC-037", "Training Completion Rate",
                    LMS, "Looks at training assignments in the learning system, for the Quality Control department. 'Completed on time' assignments are counted as the numerator. Denominator excludes assignments that are Pending - Not Yet Due. Result shown as a %.", "95", "90"),
            higherIsWorse("KPI-QC-038", "Repeat Analysis Rate",
                    LIMS, "Looks at tests performed in LIMS, across all sample types. A test is 'repeated' when it has more than one replicate recorded (replicate count > 1). Result = repeated tests divided by total tests (shown as a %).", "10", "15"));

    /**
     * Which config row each API series (KpiCardResponse.kpiId) is judged against. Not a column of
     * the table - lives here until the real table (or a mapping table) carries it. A KPI's
     * threshold is fixed per graph and doesn't change with filters, so every series a filter can
     * switch a card to links to the same row (e.g. Invalid OOS's All tests / RISE / Commercial).
     * Unlinked on purpose: instrument-utilization (the combined view has no row),
     * human-error-severity-overall (a synthetic per-period summary row whose value is always null -
     * there is nothing to colour; the Critical/Major category rows carry KPI-QC-025),
     * reviewer-productivity (not among the 38 KPIs), capa-aging-status (a category ranking).
     */
    private static final Map<String, String> SERIES_TO_KPI_ID = Map.ofEntries(
            Map.entry("sample-tat", "KPI-QC-001"),
            Map.entry("sla-adherence", "KPI-QC-002"),
            Map.entry("priority-sample-compliance", "KPI-QC-003"),
            Map.entry("tests-per-analyst", "KPI-QC-004"),
            Map.entry("hplc-sequences-per-analyst", "KPI-QC-005"),
            Map.entry("hplc-injections-per-analyst", "KPI-QC-006"),
            Map.entry("hplc-utilization", "KPI-QC-007"),
            Map.entry("gc-utilization", "KPI-QC-008"),
            Map.entry("dissolution-utilization", "KPI-QC-009"),
            Map.entry("instrument-availability", "KPI-QC-010"),
            Map.entry("instrument-downtime", "KPI-QC-011"),
            Map.entry("calibration-compliance", "KPI-QC-012"),
            Map.entry("pm-adherence", "KPI-QC-013"),
            Map.entry("breakdown-frequency", "KPI-QC-014"),
            Map.entry("instrument-failure-rate", "KPI-QC-015"),
            Map.entry("invalid-oos-rate", "KPI-QC-016"),
            Map.entry("invalid-oos-rate-rise", "KPI-QC-016"),
            Map.entry("invalid-oos-rate-commercial", "KPI-QC-016"),
            Map.entry("confirmed-oos-rate", "KPI-QC-017"),
            Map.entry("investigation-closure-tat", "KPI-QC-018"),
            Map.entry("rci-beyond-timeline", "KPI-QC-019"),
            Map.entry("repeat-failure-rate", "KPI-QC-020"),
            Map.entry("human-error-rate", "KPI-QC-023"),
            Map.entry("repeat-human-error-rate", "KPI-QC-024"),
            Map.entry("human-error-severity", "KPI-QC-025"),
            Map.entry("human-error-by-product", "KPI-QC-026"),
            Map.entry("capa-effectiveness", "KPI-QC-027"),
            Map.entry("capa-recurrence-rate", "KPI-QC-028"),
            Map.entry("capa-open-by-date-opened", "KPI-QC-029"),
            Map.entry("oos-trend", "KPI-QC-030"),
            Map.entry("oot-trend", "KPI-QC-031"),
            Map.entry("deviations", "KPI-QC-032"),
            Map.entry("rft-documentation", "KPI-QC-033"),
            Map.entry("method-failure-rate", "KPI-QC-034"),
            Map.entry("product-wise-failure", "KPI-QC-035"),
            Map.entry("qualification-cycle-time", "KPI-QC-036"),
            Map.entry("training-completion-rate", "KPI-QC-037"),
            Map.entry("repeat-analysis-rate", "KPI-QC-038"));

    /** Root-cause series ids carry their dimension as a suffix (e.g. root-cause-by-man). */
    private static final Map<String, String> SERIES_PREFIX_TO_KPI_ID = Map.of(
            "root-cause-by-", "KPI-QC-021",
            "root-cause-consolidated-", "KPI-QC-022");

    private static KpiThresholdConfigRow higherIsBetter(String kpiId, String kpiName, String source, String description,
            String greenLow, String redLow) {
        return mock(kpiId, kpiName, source, description, BETTER, new BigDecimal(redLow), new BigDecimal(greenLow), null, null);
    }

    private static KpiThresholdConfigRow higherIsWorse(String kpiId, String kpiName, String source, String description,
            String greenHigh, String redHigh) {
        return mock(kpiId, kpiName, source, description, WORSE, null, null, new BigDecimal(greenHigh), new BigDecimal(redHigh));
    }

    private static KpiThresholdConfigRow band(String kpiId, String kpiName, String source, String description,
            String redLow, String greenLow, String greenHigh, String redHigh) {
        return mock(kpiId, kpiName, source, description, BAND, decimalOrNull(redLow), decimalOrNull(greenLow),
                decimalOrNull(greenHigh), decimalOrNull(redHigh));
    }

    private static KpiThresholdConfigRow unconfigured(String kpiId, String kpiName, String source, String description,
            String direction) {
        return mock(kpiId, kpiName, source, description, direction, null, null, null, null);
    }

    private static KpiThresholdConfigRow mock(String kpiId, String kpiName, String source, String description, String direction,
            BigDecimal redLow, BigDecimal greenLow, BigDecimal greenHigh, BigDecimal redHigh) {
        return new KpiThresholdConfigRow(kpiId, kpiName, source, description, direction, NO_AUTO_MODE,
                null, null, null, null, null, null, null, null,
                redLow, greenLow, greenHigh, redHigh, MOCK_UPDATED_BY, null);
    }

    private static KpiThresholdConfigRow exported(String kpiId, String kpiName, String source, String description,
            String direction, String mode, BigDecimal redLow, BigDecimal greenLow, BigDecimal greenHigh, BigDecimal redHigh,
            String updatedAt) {
        return new KpiThresholdConfigRow(kpiId, kpiName, source, description, direction, mode,
                null, null, null, null, null, null, null, null,
                redLow, greenLow, greenHigh, redHigh, EXPORT_UPDATED_BY, OffsetDateTime.parse(updatedAt));
    }

    private static BigDecimal decimalOrNull(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    public List<KpiThresholdConfigRow> findAll() {
        return ROWS;
    }

    public Optional<KpiThresholdConfigRow> findByKpiId(String kpiId) {
        return ROWS.stream().filter(row -> row.kpiId().equals(kpiId)).findFirst();
    }

    public Optional<KpiThresholdConfigRow> findBySeriesKpiId(String seriesKpiId) {
        String kpiId = SERIES_TO_KPI_ID.get(seriesKpiId);
        if (kpiId == null) {
            kpiId = SERIES_PREFIX_TO_KPI_ID.entrySet().stream()
                    .filter(entry -> seriesKpiId.startsWith(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return kpiId == null ? Optional.empty() : findByKpiId(kpiId);
    }
}
