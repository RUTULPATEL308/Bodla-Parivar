package com.bodla.parivar.core.network

object Endpoints {
    // Supabase project root URL (e.g. "https://your-project-id.supabase.co")
    var SUPABASE_URL: String = "https://kjoykvxrnyykdnzektoj.supabase.co"
    var SUPABASE_ANON_KEY: String = "sb_publishable_PNb4_Epkaqv-J1KdC0x7dQ_-vxdAt-d"

    val isConfigured: Boolean
        get() = !SUPABASE_URL.contains("your-project-id") && SUPABASE_ANON_KEY.isNotBlank()

    val REST_BASE_URL: String
        get() = "${SUPABASE_URL.trimEnd('/')}/rest/v1"

    val AUTH_BASE_URL: String
        get() = "${SUPABASE_URL.trimEnd('/')}/auth/v1"

    // Auth Endpoints
    val AUTH_SIGN_UP: String
        get() = "$AUTH_BASE_URL/signup"

    val AUTH_SIGN_IN: String
        get() = "$AUTH_BASE_URL/token?grant_type=password"

    val AUTH_REFRESH: String
        get() = "$AUTH_BASE_URL/token?grant_type=refresh_token"

    val AUTH_USER: String
        get() = "$AUTH_BASE_URL/user"

    val IS_STAFF: String
        get() = "$REST_BASE_URL/rpc/is_staff"

    val IS_ADMIN: String
        get() = "$REST_BASE_URL/rpc/is_admin"

    val AUTH_LOGOUT: String
        get() = "$AUTH_BASE_URL/logout"

    // REST Table Endpoints
    val PROFILES: String
        get() = "$REST_BASE_URL/profiles"

    val NOTICES: String
        get() = "$REST_BASE_URL/notices?select=*,category:notice_categories(name_gu,name_en)&status=eq.PUBLISHED&order=is_pinned.desc,created_at.desc"

    val EVENTS: String
        get() = "$REST_BASE_URL/events?select=*,category:event_categories(name_gu,name_en)&status=eq.PUBLISHED&order=start_at.asc"

    val BUSINESSES: String
        get() = "$REST_BASE_URL/businesses?select=*,category:business_categories(name_gu,name_en)&status=eq.APPROVED&order=name_gu.asc"

    val OFFERINGS: String
        get() = "$REST_BASE_URL/offerings?select=*,category:offering_categories(name_gu,name_en)&status=in.(APPROVED,ACTIVE,COMPLETED)&order=created_at.desc"

    val EMERGENCY_CONTACTS: String
        get() = "$REST_BASE_URL/emergency_contacts?is_active=eq.true&order=display_order.asc"

    val PLACES: String
        get() = "$REST_BASE_URL/places?select=*,category:place_categories(name_gu,name_en)&status=eq.PUBLISHED&order=name_gu.asc"

    val COMPLAINTS: String
        get() = "$REST_BASE_URL/complaints"

    val VILLAGE_SETTINGS: String
        get() = "$REST_BASE_URL/village_settings?limit=1"

    val OFFERING_BIDS_BASE: String
        get() = "$REST_BASE_URL/offering_bids"

    fun offeringBidsForOffering(offeringId: String): String =
        "$OFFERING_BIDS_BASE?offering_id=eq.$offeringId&order=created_at.desc"

    // Latest notifications (for in-app toasts)
    val NOTIFICATIONS_LATEST: String
        get() = "$REST_BASE_URL/notifications?order=created_at.desc&limit=5"
}
