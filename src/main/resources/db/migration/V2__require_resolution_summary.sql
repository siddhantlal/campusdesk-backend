-- PostgreSQL CHECK treats NULL as passing; require the summary explicitly.
ALTER TABLE tickets DROP CONSTRAINT resolution_consistent;
ALTER TABLE tickets ADD CONSTRAINT resolution_consistent CHECK (
  (status IN ('RESOLVED','CLOSED') AND resolved_at IS NOT NULL
    AND resolution_summary IS NOT NULL AND length(trim(resolution_summary)) > 0)
  OR (status IN ('OPEN','IN_PROGRESS') AND resolved_at IS NULL AND resolution_summary IS NULL)
);
