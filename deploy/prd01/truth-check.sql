-- Row count of every copied table. Run on QA `dpm` right after the dump (save as truth-check.expected)
-- and on PRD01 `helios` after the restore; the diff must be empty:
--   psql "<conn>" -X -A -t -f deploy/prd01/truth-check.sql > truth-check.<env>
-- refresh_tokens (structure only) and the one-off dim_kpi_lite_bak_* backup are not copied.
SELECT table_schema,
       table_name,
       (xpath('/row/c/text()',
              query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name),
                           false, true, '')))[1]::text::bigint AS row_count
FROM information_schema.tables
WHERE table_schema IN ('public', 'qa', 'manufacturing')
  AND table_type = 'BASE TABLE'
  AND table_name <> 'refresh_tokens'
  AND table_name NOT LIKE 'dim_kpi_lite_bak_%'
ORDER BY table_schema, table_name;
