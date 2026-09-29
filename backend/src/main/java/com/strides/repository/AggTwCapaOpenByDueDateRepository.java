package com.strides.repository;

import jakarta.enterprise.context.ApplicationScoped;

/** period_start is the CAPA's due-date period (mostly in the future) - see AbstractOpenCapaRepository for the shared queries. */
@ApplicationScoped
public class AggTwCapaOpenByDueDateRepository extends AbstractOpenCapaRepository {

    @Override
    protected String table() {
        return "agg_tw_capa_open_by_due_date";
    }
}
