package com.strides.service;

import com.strides.dto.DrilldownPage;
import com.strides.dto.DrilldownSupport;
import com.strides.repository.GenericDrilldownRepository;
import com.strides.repository.GenericDrilldownRepository.TableSpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared by every domain service's own thin per-KPI drill-down method (e.g.
 * LabPerformanceService.getSlaAdherenceDrilldown) - each one only has to resolve its own specific
 * extraFilters into the generic Map&lt;String, Set&lt;String&gt;&gt; shape GenericDrilldownRepository
 * expects, then delegate the fetch+count+page-wrap here, rather than repeating that 3 times per KPI. */
@ApplicationScoped
public class GenericDrilldownService {

    @Inject
    GenericDrilldownRepository repository;

    public DrilldownPage<Map<String, Object>> fetch(TableSpec spec, String site, LocalDate from, LocalDate to,
            Map<String, Set<String>> extraFilters, String q, String sortBy, String sortDir, int page, int pageSize) {
        int clampedPage = DrilldownSupport.clampPage(page);
        int clampedSize = DrilldownSupport.clampPageSize(pageSize);
        List<Map<String, Object>> rows = repository.findRows(spec, site, from, to, extraFilters, q, sortBy, sortDir, clampedPage, clampedSize);
        long totalRows = repository.countRows(spec, site, from, to, extraFilters, q);
        return new DrilldownPage<>(rows, clampedPage, clampedSize, totalRows);
    }
}
