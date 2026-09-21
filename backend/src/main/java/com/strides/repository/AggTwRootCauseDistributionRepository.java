package com.strides.repository;

import com.strides.model.RootCauseTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AggTwRootCauseDistributionRepository extends AbstractAggRepository<RootCauseTotal> {

    /** recordTypes empty = no filter (Deviation + OOS + OOT all combined). All 14 dimensions + total summed in Postgres. */
    public List<RootCauseTotal> findTotals(String site, Set<String> recordTypes, String grain, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
                SELECT period_start, grain,
                       SUM(by_instrument) AS by_instrument, SUM(by_human) AS by_human,
                       SUM(by_sop) AS by_sop, SUM(by_stp) AS by_stp,
                       SUM(by_environment) AS by_environment, SUM(by_material) AS by_material,
                       SUM(by_software) AS by_software, SUM(by_data_integrity) AS by_data_integrity,
                       SUM(by_vendor) AS by_vendor, SUM(by_utility) AS by_utility,
                       SUM(by_bmr_bpr) AS by_bmr_bpr, SUM(by_equipment) AS by_equipment,
                       SUM(by_facility) AS by_facility, SUM(by_medical_product) AS by_medical_product,
                       SUM(total_count) AS total_count
                FROM agg_tw_root_cause_distribution
                WHERE site = ? AND grain = ? AND period_start BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(site, grain, from, to));
        if (!recordTypes.isEmpty()) {
            sql.append(" AND record_type IN (").append(placeholders(recordTypes)).append(")");
            params.addAll(recordTypes);
        }
        sql.append(" GROUP BY period_start, grain ORDER BY period_start");

        return query(sql.toString(), rs -> new RootCauseTotal(
                rs.getObject("period_start", LocalDate.class),
                rs.getString("grain"),
                rs.getLong("by_instrument"),
                rs.getLong("by_human"),
                rs.getLong("by_sop"),
                rs.getLong("by_stp"),
                rs.getLong("by_environment"),
                rs.getLong("by_material"),
                rs.getLong("by_software"),
                rs.getLong("by_data_integrity"),
                rs.getLong("by_vendor"),
                rs.getLong("by_utility"),
                rs.getLong("by_bmr_bpr"),
                rs.getLong("by_equipment"),
                rs.getLong("by_facility"),
                rs.getLong("by_medical_product"),
                rs.getLong("total_count")),
                params.toArray());
    }
}
