package com.bodla.parivar.core.navigation

sealed class Screen(val route: String) {
    // 5-item Bottom Navigation
    data object Home : Screen("home")
    data object Notices : Screen("notices")
    data object Events : Screen("events")
    data object Directory : Screen("directory")
    data object Profile : Screen("profile")
    data object AdminHome : Screen("admin_home")
    data object AdminUsers : Screen("admin_users")

    // Sub-screens & Feature Details
    data class NoticeDetail(val noticeId: String) : Screen("notice_detail/$noticeId")
    data class EventDetail(val eventId: String) : Screen("event_detail/$eventId")
    data class BusinessDetail(val businessId: String) : Screen("business_detail/$businessId")
    data class OfferingDetail(val offeringId: String) : Screen("offering_detail/$offeringId")
    data object Offerings : Screen("offerings")
    data object AddOffering : Screen("add_offering")
    data object Complaints : Screen("complaints")
    data object CreateComplaint : Screen("create_complaint")
    data object Emergency : Screen("emergency")
    data object Places : Screen("places")
    data object Gallery : Screen("gallery")
    data object EditProfile : Screen("edit_profile")
    data object Login : Screen("login")
    data object Register : Screen("register")
}
