-- Audit history must be written by trusted database functions or server code.
-- The previous authenticated INSERT policy let any resident forge log entries.
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "System can insert audit logs" ON public.audit_logs;
REVOKE INSERT ON TABLE public.audit_logs FROM anon, authenticated;

-- SECURITY DEFINER database procedures continue to write as their owner.
-- Trusted Edge Functions may write with the server-only service-role key after
-- verifying the caller's identity and staff role.

-- Rollback guidance: do not restore the former unrestricted policy. If a
-- rollback is required, first deploy and verify a narrower INSERT policy for
-- the specific trusted actor or workflow that needs to write audit records.
