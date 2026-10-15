-- Configurable Navratri services, public cash bookings, and admin-managed report content.
CREATE TABLE IF NOT EXISTS public.navratri_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug TEXT UNIQUE NOT NULL,
    title_gu TEXT NOT NULL,
    title_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    report_url TEXT,
    option_type TEXT NOT NULL CHECK (option_type IN ('BOOKING', 'REPORT')),
    cash_only BOOLEAN NOT NULL DEFAULT TRUE,
    form_fields JSONB NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(form_fields) = 'array'),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.navratri_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    option_id UUID NOT NULL REFERENCES public.navratri_options(id) ON DELETE RESTRICT,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    customer_name TEXT NOT NULL CHECK (length(trim(customer_name)) BETWEEN 2 AND 100),
    phone TEXT CHECK (phone IS NULL OR length(trim(phone)) BETWEEN 7 AND 20),
    amount NUMERIC(12, 2) CHECK (amount IS NULL OR amount >= 0),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity BETWEEN 1 AND 500),
    notes TEXT,
    form_data JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(form_data) = 'object'),
    payment_method TEXT NOT NULL DEFAULT 'CASH' CHECK (payment_method = 'CASH'),
    payment_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID')),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_navratri_options_active_order
    ON public.navratri_options(is_active, display_order);
CREATE INDEX IF NOT EXISTS idx_navratri_bookings_option_created
    ON public.navratri_bookings(option_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_navratri_bookings_status
    ON public.navratri_bookings(status, payment_status);

INSERT INTO public.navratri_options
    (slug, title_gu, title_en, description_gu, description_en, option_type, cash_only, display_order, form_fields)
VALUES
    ('prasad', 'પ્રસાદ બુકિંગ', 'Prasad Booking', 'પ્રસાદ માટે બુકિંગ નોંધાવો.', 'Reserve prasad for Navratri.', 'BOOKING', TRUE, 1, '[]'::jsonb),
    ('mandavi', 'મંડવી બુકિંગ', 'Mandavi Booking', 'મંડવી બુકિંગ માટે વિનંતી મોકલો.', 'Submit a Mandavi booking request.', 'BOOKING', TRUE, 2,
     '[{"key":"phone","label_gu":"ફોન નંબર","label_en":"Phone number","type":"PHONE","required":true}]'::jsonb),
    ('khardo', 'ખરડો બુકિંગ', 'Khardo Booking', 'ખરડા માટે બુકિંગ નોંધાવો. ચુકવણી રોકડમાં જ.', 'Reserve a Khardo. Cash payment only.', 'BOOKING', TRUE, 3,
     '[{"key":"phone","label_gu":"ફોન નંબર","label_en":"Phone number","type":"PHONE","required":true},{"key":"amount","label_gu":"રકમ","label_en":"Amount","type":"AMOUNT","required":true}]'::jsonb),
    ('aheval', 'નવરાત્રી અહેવાલ', 'Navratri Aheval Report', 'નવરાત્રીનો અહેવાલ અહીં મળશે.', 'The Navratri event report will be published here.', 'REPORT', FALSE, 4, '[]'::jsonb)
ON CONFLICT (slug) DO NOTHING;

ALTER TABLE public.navratri_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.navratri_bookings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Active Navratri options are public" ON public.navratri_options;
CREATE POLICY "Active Navratri options are public"
    ON public.navratri_options FOR SELECT TO anon, authenticated
    USING (is_active = TRUE);

DROP POLICY IF EXISTS "Staff manage Navratri options" ON public.navratri_options;
CREATE POLICY "Staff manage Navratri options"
    ON public.navratri_options FOR ALL TO authenticated
    USING (public.is_staff()) WITH CHECK (public.is_staff());

DROP POLICY IF EXISTS "Public can submit cash Navratri bookings" ON public.navratri_bookings;
CREATE POLICY "Public can submit cash Navratri bookings"
    ON public.navratri_bookings FOR INSERT TO anon, authenticated
    WITH CHECK (
        payment_method = 'CASH'
        AND payment_status = 'PENDING'
        AND status = 'PENDING'
        AND (user_id IS NULL OR user_id = auth.uid())
        AND EXISTS (
            SELECT 1 FROM public.navratri_options option
            WHERE option.id = option_id
              AND option.option_type = 'BOOKING'
              AND option.is_active = TRUE
        )
    );

DROP POLICY IF EXISTS "Staff manage Navratri bookings" ON public.navratri_bookings;
CREATE POLICY "Staff manage Navratri bookings"
    ON public.navratri_bookings FOR ALL TO authenticated
    USING (public.is_staff()) WITH CHECK (public.is_staff());

GRANT SELECT ON public.navratri_options TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.navratri_options TO authenticated;
GRANT INSERT ON public.navratri_bookings TO anon, authenticated;
GRANT SELECT, UPDATE, DELETE ON public.navratri_bookings TO authenticated;

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'navratri', 'navratri', TRUE, 104857600,
    ARRAY['application/pdf', 'image/jpeg', 'image/png', 'image/webp', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
)
ON CONFLICT (id) DO UPDATE SET
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS "Public Read Navratri Reports" ON storage.objects;
CREATE POLICY "Public Read Navratri Reports"
    ON storage.objects FOR SELECT TO anon, authenticated
    USING (bucket_id = 'navratri');
