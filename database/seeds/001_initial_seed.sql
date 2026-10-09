-- ==============================================================================
-- Seed: 001_initial_seed.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Standard roles, default village settings, taxonomy categories,
--              and clearly marked DEMO DATA for initial testing.
-- ==============================================================================

-- 1. Standard Roles
INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Complete system administration and settings control'),
    ('ADMIN', 'General administrative management of village content and users'),
    ('MODERATOR', 'Community moderation, complaint resolution, and verification'),
    ('CONTENT_MANAGER', 'Editorial management of notices, events, and gallery'),
    ('BUSINESS_OWNER', 'Verified village business and job lister'),
    ('USER', 'Registered village community member')
ON CONFLICT (name) DO NOTHING;

-- 2. Default Village Configuration (Bodla, Mehsana, Gujarat)
INSERT INTO village_settings (
    village_name_gu,
    village_name_en,
    district,
    state,
    country,
    description_gu,
    description_en,
    latitude,
    longitude
) VALUES (
    'બોદલા',
    'Bodla',
    'Mehsana',
    'Gujarat',
    'India',
    'બોદલા એ ગુજરાત રાજ્યના મહેસાણા જિલ્લામાં આવેલું એક ગૌરવશાળી અને આદર્શ ગામ છે.',
    'Bodla is a distinguished village located in the Mehsana district of Gujarat, India.',
    23.5880000,
    72.3693000
) ON CONFLICT DO NOTHING;

-- 3. Notice Categories
INSERT INTO notice_categories (name_gu, name_en, display_order) VALUES
    ('સામાન્ય સૂચના', 'General Notice', 1),
    ('ગ્રામ પંચાયત', 'Gram Panchayat', 2),
    ('ધાર્મિક / ઉત્સવ', 'Religious / Festivals', 3),
    ('આરોગ્ય અને સુખાકારી', 'Health & Wellness', 4),
    ('શિક્ષણ અને રમતગમત', 'Education & Sports', 5),
    ('કૃષિ અને હવામાન', 'Agriculture & Weather', 6)
ON CONFLICT DO NOTHING;

-- 4. Event Categories
INSERT INTO event_categories (name_gu, name_en) VALUES
    ('સાંસ્કૃતિક ઉત્સવ', 'Cultural Festival'),
    ('ધાર્મિક કાર્યક્રમ', 'Religious Event'),
    ('ગ્રામસભા', 'Gram Sabha'),
    ('રમતગમત સ્પર્ધા', 'Sports Tournament'),
    ('તબીબી કેમ્પ', 'Medical Camp'),
    ('સામાજિક મેળાવડો', 'Community Gathering')
ON CONFLICT DO NOTHING;

-- 5. Business Categories
INSERT INTO business_categories (name_gu, name_en, display_order) VALUES
    ('ડેરી અને પશુપાલન', 'Dairy & Animal Husbandry', 1),
    ('કરિયાણું અને જનરલ સ્ટોર', 'Grocery & General Store', 2),
    ('કૃષિ સેવાઓ અને સાધન', 'Agro Services & Tools', 3),
    ('બાંધકામ અને મકાન સામગ્રી', 'Construction & Hardware', 4),
    ('ટેક્સટાઇલ અને કપડાં', 'Textiles & Tailoring', 5),
    ('ઇલેક્ટ્રિકલ અને ઇલેક્ટ્રોનિક્સ', 'Electrical & Repairs', 6),
    ('વાહન સેવા અને ગેરેજ', 'Automobile & Garage', 7),
    ('વ્યવસાયિક સેવાઓ', 'Professional Services', 8)
ON CONFLICT DO NOTHING;

-- 6. Job Categories
INSERT INTO job_categories (name_gu, name_en) VALUES
    ('ખેતીકામ અને વાડીકામ', 'Agriculture & Farm Work'),
    ('ડ્રાઇવિંગ અને પરિવહન', 'Driving & Transport'),
    ('બાંધકામ અને મજૂરી', 'Construction & Masonry'),
    ('વેચાણ અને દુકાન સહાયક', 'Retail & Shop Assistant'),
    ('શિક્ષણ અને ટ્યુશન', 'Teaching & Tutoring'),
    ('ઓફિસ અને એકાઉન્ટ્સ', 'Office & Accounts')
ON CONFLICT DO NOTHING;

-- 7. Offering Categories (ચઢાવો કેટેગરી - NOT AUCTION)
INSERT INTO offering_categories (name_gu, name_en, display_order) VALUES
    ('મંદિર પૂજા સેવા', 'Temple Puja Seva', 1),
    ('ધ્વજારોહણ ચઢાવો', 'Temple Flag Hoisting (Dhwajarohan)', 2),
    ('ઉત્સવ પ્રસાદ સેવા', 'Festival Prasad Seva', 3),
    ('ગૌશાળા ઘાસચારો', 'Gaushala Fodder Offering', 4),
    ('ધાર્મિક પુસ્તક / સાધન', 'Religious Literature / Articles', 5),
    ('સામાજિક ઉત્સવ સેવા', 'Community Celebration Seva', 6)
ON CONFLICT DO NOTHING;

-- 8. Complaint Categories
INSERT INTO complaint_categories (name_gu, name_en) VALUES
    ('પીવાનું પાણી', 'Drinking Water'),
    ('ગટર અને સફાઈ', 'Drainage & Sanitation'),
    ('શેરી લાઈટ', 'Street Lighting'),
    ('રસ્તા અને ખાડા', 'Roads & Pavements'),
    ('કચરા વ્યવસ્થાપન', 'Waste Management'),
    ('અન્ય ગ્રામીણ સમસ્યા', 'Other Village Grievance')
ON CONFLICT DO NOTHING;

-- 9. Place Categories
INSERT INTO place_categories (name_gu, name_en) VALUES
    ('મંદિર / ધાર્મિક સ્થળ', 'Temple / Religious Site'),
    ('ગ્રામ પંચાયત ભવન', 'Gram Panchayat Office'),
    ('પ્રાથમિક શાળા', 'Primary School'),
    ('આરોગ્ય કેન્દ્ર (PHC)', 'Primary Health Center'),
    ('ગૌશાળા', 'Gaushala'),
    ('ગામનું તળાવ / બગીચો', 'Village Lake / Park')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------------------------
-- DEMO DATA (Clearly flagged as DEMO DATA for initial preview & testing)
-- ------------------------------------------------------------------------------

-- Demo Emergency Contacts (Official Standard National/State Helplines - Safe Real Numbers)
INSERT INTO emergency_contacts (name_gu, name_en, organization, phone, alternate_phone, category, display_order, is_active) VALUES
    ('ઈમરજન્સી એમ્બ્યુલન્સ', 'Emergency Ambulance', '108 GVK EMRI', '108', NULL, 'AMBULANCE', 1, true),
    ('પોલીસ કંટ્રોલ રૂમ', 'Police Emergency', 'Gujarat Police', '100', '112', 'POLICE', 2, true),
    ('મહેસાણા ફાયર બ્રિગેડ', 'Fire Services', 'Fire Department', '101', NULL, 'FIRE', 3, true),
    ('મહિલા હેલ્પલાઇન', 'Women Helpline', 'Abhayam Helpline', '181', NULL, 'HELPLINE', 4, true),
    ('બાળ સહાય હેલ્પલાઇન', 'Child Helpline', 'Childline India', '1098', NULL, 'HELPLINE', 5, true)
ON CONFLICT DO NOTHING;

-- Demo Notice (Clearly marked as DEMO DATA)
INSERT INTO notices (
    title_gu,
    title_en,
    description_gu,
    description_en,
    status,
    is_pinned,
    publish_at
) VALUES (
    '[DEMO DATA] ગ્રામ પંચાયત સામાન્ય સભાનું આયોજન',
    '[DEMO DATA] Gram Panchayat General Meeting Scheduled',
    'આ ડેમો સૂચના છે. બોદલા ગામના તમામ ગ્રામજનોને જણાવવાનું કે આગામી રવિવારે પંચાયત હોલ ખાતે બેઠક મળશે.',
    'This is demo data. Notice to all Bodla residents regarding upcoming community meeting at Panchayat hall.',
    'PUBLISHED',
    true,
    NOW()
);

-- Demo Event (Clearly marked as DEMO DATA)
INSERT INTO events (
    title_gu,
    title_en,
    description_gu,
    description_en,
    start_at,
    end_at,
    location_gu,
    location_en,
    status
) VALUES (
    '[DEMO DATA] વાર્ષિક સાંસ્કૃતિક ઉત્સવ',
    '[DEMO DATA] Annual Cultural Celebration',
    'આ ડેમો કાર્યક્રમ છે. ગામના તમામ પરિવારો માટે સાંસ્કૃતિક મહોત્સવનું આયોજન.',
    'This is demo data. Annual cultural celebration for all Bodla Parivar families.',
    NOW() + INTERVAL '7 days',
    NOW() + INTERVAL '7 days 4 hours',
    'બોદલા પ્રાથમિક શાળા મેદાન',
    'Bodla Primary School Ground',
    'PUBLISHED'
);

-- Demo Offering (Clearly marked as DEMO DATA - ચઢાવો, NO AUCTION)
INSERT INTO offerings (
    title_gu,
    title_en,
    description_gu,
    description_en,
    amount,
    location_gu,
    location_en,
    status
) VALUES (
    '[DEMO DATA] શ્રી મહાદેવ મંદિર ધ્વજારોહણ સેવા',
    '[DEMO DATA] Shri Mahadev Temple Dhwajarohan Seva',
    'આ ડેમો ચઢાવો છે. શ્રાવણ માસ નિમિત્તે મંદિર ધ્વજારોહણ ચઢાવા સેવા.',
    'This is demo data. Temple flag hoisting offering seva for the holy month of Shravan.',
    5100.00,
    'બોદલા મંદિર સંકુલ',
    'Bodla Temple Complex',
    'ACTIVE'
);
