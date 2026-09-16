package com.strides.dto;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.time.LocalDate;
import java.util.List;

/**
 * @BeanParam input shapes for the KPI GET endpoints (Qms/LabPerformance/PeoplePerformance/
 * InstrumentHealth controllers). Every KPI query shares site/grain/from/to, so DateRange is the
 * base every filter shape extends rather than repeating those four fields per endpoint; each
 * subclass adds the one extra filter its group of endpoints needs. Requiredness (grain/from/to,
 * and instGroup on InstrumentHealth) is still enforced by RequiredParams.require in the
 * controller - these classes are just param carriers, not validators.
 */
public final class KpiParams {

    private KpiParams() {
    }

    public static class DateRange {
        @QueryParam("site")
        @DefaultValue("KRSG")
        public String site;
        @QueryParam("grain")
        public String grain;
        @QueryParam("from")
        public LocalDate from;
        @QueryParam("to")
        public LocalDate to;
    }

    public static class WithInstGroup extends DateRange {
        @QueryParam("instGroup")
        public String instGroup;
    }

    public static class WithSampleType extends DateRange {
        @QueryParam("sampleType")
        public String sampleType;
    }

    public static class WithSampleCategory extends DateRange {
        @QueryParam("sampleCategory")
        public String sampleCategory;
    }

    public static class WithRootCauseBucket extends DateRange {
        @QueryParam("rootCauseBucket")
        public String rootCauseBucket;
    }

    public static class WithRespDept extends DateRange {
        @QueryParam("respDept")
        public String respDept;
    }

    public static class WithRecordType extends DateRange {
        @QueryParam("recordType")
        public String recordType;
    }

    public static class WithOriginatedFrom extends DateRange {
        @QueryParam("originatedFrom")
        public String originatedFrom;
    }

    public static class WithMaterialType extends DateRange {
        @QueryParam("materialType")
        public String materialType;
    }

    /** Repeated params (?material=A&material=B), not comma-joined - see backend/README.md's "Filter syntax" note. */
    public static class WithMaterialList extends DateRange {
        @QueryParam("material")
        public List<String> material;
    }

    public static class CapaAgingStatus extends DateRange {
        @QueryParam("recordStatus")
        public String recordStatus;
        @QueryParam("level")
        public String level;
        @QueryParam("minAgingDays")
        public Double minAgingDays;
        @QueryParam("maxAgingDays")
        public Double maxAgingDays;
    }
}
