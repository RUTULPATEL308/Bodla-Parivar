BEGIN;

GRANT SELECT ON TABLE public.offering_winner_private TO authenticated;

COMMIT;