package com.strides.dto;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.time.LocalDate;
import java.util.List;

/**
 * @BeanParam input shapes for the KPI GET endpoints (Qms/LabPerformance/PeoplePerformance/
 * InstrumentHealth controllers). Every KPI query shares site/grain/from/to, so DateRange is the
 * base every filter shape extends rather than repeating those four fields per endpoint; each
 * subclass adds the one extra filter its group of endpoints needs. Requiredness (grain/from/to)
 * is still enforced by RequiredParams.require in the controller - these classes are just param carriers, not validators.
 */
public final class KpiParams {

    private KpiParams() {
    }

    /** Also read by KpiThresholdResponseFilter, which sees the raw query string without this default applied. */
    public static final String DEFAULT_SITE = "KRSG";

    public static class DateRange {
        @QueryParam("site")
        @DefaultValue(DEFAULT_SITE)
        public String site;
        @QueryParam("grain")
        public String grain;
        @QueryParam("from")
        public LocalDate from;
        @QueryParam("to")
        public LocalDate to;
    }

    /**
     * Shared by every record-level drill-down endpoint (see DrilldownSupport) - sortBy/sortDir/q/
     * page/pageSize, on top of DateRange's site/grain/from/to. sortBy is validated against each
     * repository's own whitelist before ever reaching SQL, never interpolated directly.
     */
    public static class DrilldownBase extends DateRange {
        @QueryParam("sortBy")
        public String sortBy;
        @QueryParam("sortDir")
        @DefaultValue("asc")
        public String sortDir;
        @QueryParam("q")
        public String q;
        @QueryParam("page")
        @DefaultValue("1")
        public int page;
        @QueryParam("pageSize")
        @DefaultValue("" + DrilldownSupport.DEFAULT_PAGE_SIZE)
        public int pageSize;
        /** The drill-down table's own per-column filter row (dropdown for a <=50-distinct-value
         * column, free-text search otherwise, min/max or from/to for a NUMBER/DATE column) and
         * multi-column sort - see DrilldownSupport.parseSort/parseColumnFilters/parseColumnSearches/
         * parseColumnRanges. sort: repeated `column:asc`/`column:dir` pairs, ordered by sort
         * priority. colFilter: repeated `column:value` pairs (same column repeated = a multi-select).
         * colSearch: repeated `column:text` pairs. colRange: repeated `column:min:max` pairs (either
         * side blank for an open range). Declared here (not per-subclass) so every drill-down
         * endpoint gets the same shape for free - a KPI's own service method still decides whether to
         * actually use them (see GenericDrilldownService.fetch's two overloads) or keep the older
         * single-sort/no-filter-row behavior. */
        @QueryParam("sort")
        public List<String> sort = List.of();
        @QueryParam("colFilter")
        public List<String> colFilter = List.of();
        @QueryParam("colSearch")
        public List<String> colSearch = List.of();
        @QueryParam("colRange")
        public List<String> colRange = List.of();
    }

    /** Sample TAT's own drill-down filters, matching its existing card filters (see WithSampleTypeAndBatchCategory). */
    public static class SampleTatDrilldown extends DrilldownBase {
        @QueryParam("sampleType")
        public String sampleType;
        @QueryParam("batchCategory")
        public String batchCategory;
    }

    // Everything below mirrors one KPI's own existing card filters (see the matching With* class
    // above), just extending DrilldownBase instead of DateRange - see DrilldownSpecs for how each
    // field maps to its real SQL column.

    public static class SlaAdherenceDrilldown extends DrilldownBase {
        @QueryParam("sampleCategory")
        public String sampleCategory;
    }

    public static class PrioritySampleComplianceDrilldown extends DrilldownBase {
        @QueryParam("sampleType")
        public String sampleType;
    }

    public static class TestsPerAnalystDrilldown extends DrilldownBase {
        @QueryParam("sampleCategory")
        public String sampleCategory;
        @QueryParam("analyst")
        public String analyst;
    }

    public static class InjectionStatusDrilldown extends DrilldownBase {
        @QueryParam("injectionStatus")
        public String injectionStatus;
    }

    public static class InstrumentUtilizationDrilldown extends DrilldownBase {
        @QueryParam("instGroup")
        public String instGroup;
    }

    public static class ValidOosRateDrilldown extends DrilldownBase {
        @QueryParam("productType")
        public String productType;
    }

    public static class InitiatedThroughDrilldown extends DrilldownBase {
        @QueryParam("initiatedThrough")
        public String initiatedThrough;
    }

    public static class RecordTypeDrilldown extends DrilldownBase {
        @QueryParam("recordType")
        public String recordType;
    }

    public static class FinalClassificationDrilldown extends DrilldownBase {
        @QueryParam("finalClassification")
        public String finalClassification;
    }

    public static class MaterialDrilldown extends DrilldownBase {
        @QueryParam("material")
        public String material;
    }

    public static class SampleTypeDrilldown extends DrilldownBase {
        @QueryParam("sampleType")
        public String sampleType;
    }

    public static class OriginatedFromDrilldown extends DrilldownBase {
        @QueryParam("originatedFrom")
        public String originatedFrom;
    }

    public static class LevelDrilldown extends DrilldownBase {
        @QueryParam("level")
        public String level;
    }

    public static class DeviationsDrilldown extends DrilldownBase {
        @QueryParam("respDept")
        public String respDept;
        @QueryParam("level")
        public String level;
        @QueryParam("initialClassification")
        public String initialClassification;
        @QueryParam("finalClassification")
        public String finalClassification;
    }

    public static class ProductWiseFailureDrilldown extends DrilldownBase {
        @QueryParam("materialType")
        public String materialType;
        @QueryParam("disposition")
        public String disposition;
    }

    public static class DesignationDrilldown extends DrilldownBase {
        @QueryParam("designation")
        public String designation;
    }

    public static class TrainingCompletionRateDrilldown extends DrilldownBase {
        @QueryParam("department")
        public String department;
        @QueryParam("trainingType")
        public String trainingType;
    }

    /** instGroup: comma-separated list to combine (HPLC, GC, Dissolution), or null/blank for ALL. */
    public static class WithInstGroup extends DateRange {
        @QueryParam("instGroup")
        public String instGroup;
    }

    public static class WithSampleType extends DateRange {
        @QueryParam("sampleType")
        public String sampleType;
    }

    /** batchCategory: comma-separated list to combine (commercial, other, exhibit, raw material, commercial validation), or null/blank for ALL. sample-tat only. */
    public static class WithSampleTypeAndBatchCategory extends WithSampleType {
        @QueryParam("batchCategory")
        public String batchCategory;
    }

    public static class WithSampleCategory extends DateRange {
        @QueryParam("sampleCategory")
        public String sampleCategory;
    }

    /** analyst: comma-separated list of analyst codes to combine, or null/blank for ALL. */
    public static class WithSampleCategoryAndAnalyst extends WithSampleCategory {
        @QueryParam("analyst")
        public String analyst;
    }

    public static class WithInjectionStatus extends DateRange {
        @QueryParam("injectionStatus")
        public String injectionStatus;
    }

    public static class WithDesignation extends DateRange {
        @QueryParam("designation")
        public String designation;
    }

    /**
     * department/trainingType: comma-separated combine filters, each independent, or null/blank
     * for ALL. departmentScope: QC | ALL (default ALL) - additive pattern, see
     * backend/README.md's "Department scope" section; QC takes priority over an explicit
     * department selection when both are given.
     */
    public static class WithDepartmentAndTrainingType extends DateRange {
        @QueryParam("department")
        public String department;
        @QueryParam("trainingType")
        public String trainingType;
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    /** productType: comma-separated list to combine (Commercial, Exhibit, Process Validation, ...), or null/blank for ALL. confirmed-oos-rate only. */
    public static class WithProductType extends DateRange {
        @QueryParam("productType")
        public String productType;
    }

    /** testScope: RISE or Commercial, or null/blank for All tests (see InvalidOosTestScope). invalid-oos-rate only. */
    public static class WithTestScope extends DateRange {
        @QueryParam("testScope")
        public String testScope;
    }

    public static class WithRootCauseBucket extends DateRange {
        @QueryParam("rootCauseBucket")
        public String rootCauseBucket;
    }

    /** oos-trend / oot-trend only - conclusion is part of their agg grain, so it filters independently of rootCauseBucket. */
    public static class WithRootCauseBucketAndConclusion extends WithRootCauseBucket {
        @QueryParam("conclusion")
        public String conclusion;
    }

    /**
     * initiatedThrough: comma-separated combine filter, or null/blank for ALL. respDept: repeated
     * params, not comma-joined - same comma-in-names reason as WithInitiatedThroughAndDepartmentScope.
     * rci-beyond-timeline only - true partition, no department-scope split.
     */
    public static class WithRespDept extends DateRange {
        @QueryParam("respDept")
        public List<String> respDept;
        @QueryParam("initiatedThrough")
        public String initiatedThrough;
    }

    /**
     * initiatedThrough: comma-separated combine filter, or null/blank for ALL.
     * respDept: repeated params (?respDept=A&respDept=B), not comma-joined - at least one real
     * value ("Manufacturing, Science & Technology") contains a comma, see backend/README.md's
     * "Filter syntax" note; options sourced live from the DB, not hardcoded - takes priority over
     * departmentScope when given.
     * departmentScope: QC | ALL (default ALL), used only when respDept is blank - see
     * backend/README.md's "Department scope" section. investigation-closure-tat only.
     */
    public static class WithInitiatedThroughAndDepartmentScope extends DateRange {
        @QueryParam("initiatedThrough")
        public String initiatedThrough;
        @QueryParam("respDept")
        public List<String> respDept;
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    /**
     * respDept: repeated params (?respDept=A&respDept=B), not comma-joined - same comma-in-names
     * reason as WithInitiatedThroughAndDepartmentScope; options sourced live from the DB. Several
     * selected departments are summed (see AggTwCapaRecurrenceRateRepository.find) - takes priority
     * over departmentScope when given. departmentScope: QC | ALL (default ALL), used only when
     * respDept is blank - see backend/README.md's "Department scope" section. capa-recurrence-rate only.
     */
    public static class WithRespDeptAndDepartmentScope extends DateRange {
        @QueryParam("respDept")
        public List<String> respDept;
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    /**
     * All five extra dimensions on agg_tw_deviations, each a comma-separated combine filter, or
     * null/blank for ALL - except respDept, which is repeated params (same comma-in-names reason as
     * WithInitiatedThroughAndDepartmentScope). departmentScope: QC | ALL (default ALL) - additive pattern, see
     * backend/README.md's "Department scope" section; QC takes priority over an explicit respDept
     * selection when both are given.
     */
    public static class Deviations extends WithRootCauseBucket {
        @QueryParam("respDept")
        public List<String> respDept;
        @QueryParam("level")
        public String level;
        @QueryParam("initialClassification")
        public String initialClassification;
        @QueryParam("finalClassification")
        public String finalClassification;
        @QueryParam("recurringByCause")
        public String recurringByCause;
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    /** departmentScope: QC | ALL (default ALL), repeat-failure-rate only - see backend/README.md's "Department scope" section. */
    public static class WithRecordType extends DateRange {
        @QueryParam("recordType")
        public String recordType;
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    public static class WithOriginatedFrom extends DateRange {
        @QueryParam("originatedFrom")
        public String originatedFrom;
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    /** departmentScope: QC | ALL (default ALL) - human-error-rate, repeat-human-error-rate, human-error-severity. */
    public static class WithDepartmentScope extends DateRange {
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    /** human-error-severity: one final_classification (Critical/Major/...), or null/blank for every class. */
    public static class WithFinalClassification extends WithDepartmentScope {
        @QueryParam("finalClassification")
        public String finalClassification;
    }

    public static class WithMaterialType extends DateRange {
        @QueryParam("materialType")
        public String materialType;
    }

    /**
     * product: repeated params (?product=A&product=B) of product_slice values to combine, or
     * empty/null for ALL - same comma-in-names problem as WithMaterialList, see backend/README.md's
     * "Filter syntax" note.
     * disposition: comma-separated list to combine (Recall, Unknown, Release, Field Alert, Alert,
     * Reject, Not Applicable), or null/blank for ALL.
     */
    public static class WithMaterialTypeAndProduct extends WithMaterialType {
        @QueryParam("product")
        public List<String> product;
        @QueryParam("disposition")
        public String disposition;
    }

    /**
     * material: repeated params (?material=A&material=B), not comma-joined - see
     * backend/README.md's "Filter syntax" note.
     * departmentScope: QC | ALL (default ALL) - human-error-by-product, see backend/README.md's
     * "Department scope" section.
     */
    public static class WithMaterialList extends DateRange {
        @QueryParam("material")
        public List<String> material;
        @QueryParam("departmentScope")
        public String departmentScope;
    }

    /**
     * departmentScope: QC | ALL (default ALL) - slice pattern on resp_dept_slice, see backend/README.md's "Department scope" section.
     * from/to are optional here (omit both = every open CAPA as of today). dateBasis: capa-aging
     * only - Date Opened (default) | Due Date.
     */
    public static class CapaAgingStatus extends DateRange {
        @QueryParam("dateBasis")
        public String dateBasis;
        /** capa-aging only - one department's resp_dept_slice; overrides departmentScope. */
        @QueryParam("respDeptSlice")
        public String respDeptSlice;
        @QueryParam("recordStatus")
        public String recordStatus;
        @QueryParam("level")
        public String level;
        @QueryParam("minAgingDays")
        public Double minAgingDays;
        @QueryParam("maxAgingDays")
        public Double maxAgingDays;
        @QueryParam("departmentScope")
        public String departmentScope;
    }
}
