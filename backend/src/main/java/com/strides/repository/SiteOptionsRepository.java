package com.strides.repository;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Site is the one filter shared by every KPI card on the dashboard rather than scoped to a
 * single table, so its option list is the union of every distinct site/site_name value across
 * all 31 agg_* tables (see backend/README.md) - the live equivalent of the "confirmed live
 * against every agg_* table" hand-derivation the frontend's previous hardcoded SITE_OPTIONS list
 * needed to redo by hand whenever a new table/site appeared.
 */
@ApplicationScoped
public class SiteOptionsRepository extends AbstractAggRepository<String> {

    private static final String SQL = """
            SELECT DISTINCT site AS site FROM agg_chrm_hplc_sequence_and_injections WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_cw_training_completion_rate WHERE site IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_breakdowns_frequency WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_cal_pm_compliance WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_instrument_failure_rate WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_instrument_utilization WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_lims_priority_sample_compliance WHERE site IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_qualification_cycle_time WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_repeat_analysis_rate WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_reviewer_productivity_index WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_right_first_time WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_sla_adherence WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_tests_per_analyst WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_lims_turn_around_time WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_capa_aging_status WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_capa_effectiveness WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_capa_recurrence_rate WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_deviations WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_human_error_by_product WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_human_error_rate WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_human_error_severity WHERE site IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_tw_invalid_oos_rate WHERE site_name IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_investigation_closure_tat WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_method_failure_rate WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_oos_trend WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_oot_trend WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_product_wise_failure WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_rci_beyond_timeline WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_repeat_failure_rate WHERE site IS NOT NULL
            UNION SELECT DISTINCT site FROM agg_tw_root_cause_distribution WHERE site IS NOT NULL
            UNION SELECT DISTINCT site_name FROM agg_tw_valid_oos_rate WHERE site_name IS NOT NULL
            ORDER BY site""";

    public List<String> findAll() {
        return query(SQL, rs -> rs.getString(1));
    }
}
