package com.bodla.parivar.core.localization

import androidx.compose.runtime.Composable

// ==============================================================================
// Bilingual Localization: બોદલા પરિવાર
// Single source of truth for UI strings (Gujarati first-class & English)
// ==============================================================================

object Strings {
    // App Identity
    const val APP_NAME_GU = "બોદલા પરિવાર"
    const val APP_NAME_EN = "Bodla Parivar"
    const val VILLAGE_SUBTITLE_GU = "બોદલા, મહેસાણા"
    const val VILLAGE_SUBTITLE_EN = "Bodla, Mehsana"

    // Navigation Items
    fun navHome(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "મુખ્ય" else "Home"
    fun navNotices(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સૂચનાઓ" else "Notices"
    fun navEvents(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "કાર્યક્રમો" else "Events"
    fun navDirectory(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ડિરેક્ટરી" else "Directory"
    fun navProfile(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "પ્રોફાઇલ" else "Profile"

    // Home Screen
    fun greeting(name: String, lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "નમસ્કાર, $name" else "Namaskar, $name"
    fun welcomeMessage(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "બોદલા પરિવારમાં આપનું સ્વાગત છે" else "Welcome to Bodla Parivar"
    fun villageHistory(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "બોદલા ગામનો ઇતિહાસ" else "History of Bodla Village"
    fun searchPlaceholder(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "🔍 શોધો બોદલા..." else "🔍 Search Bodla..."
    fun importantNotice(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "📢 અગત્યની સૂચના" else "📢 Important Notice"
    fun quickServices(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "ઝડપી સેવાઓ" else "Quick Services"
    fun upcomingEvents(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "આગામી કાર્યક્રમો" else "Upcoming Events"
    fun latestNotices(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "તાજા પરિપત્રો / સૂચનાઓ" else "Latest Notices"
    fun viewMore(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "વધુ જુઓ →" else "View More →"

    // Quick Service Tiles
    fun serviceNotices(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સૂચનાઓ" else "Notices"
    fun serviceEvents(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "કાર્યક્રમો" else "Events"
    fun serviceBusinesses(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "વ્યવસાયો" else "Businesses"
    fun serviceJobs(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નોકરીઓ" else "Jobs"
    fun serviceOfferings(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચઢાવો" else "Offerings"
    fun serviceEmergency(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ઈમરજન્સી" else "Emergency"
    fun servicePlaces(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સ્થળો" else "Places"
    fun serviceGallery(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ગેલેરી" else "Gallery"
    fun serviceComplaints(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ફરિયાદ" else "Complaints"

    // Offerings / ચઢાવો (Explicitly non-auction terminology)
    fun offeringsTitle(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચઢાવો" else "Offerings"
    fun addOffering(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચઢાવો ઉમેરો" else "Add Offering"
    fun myOfferings(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "મારા ચઢાવો" else "My Offerings"
    fun offeringDetails(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચઢાવાની વિગતો" else "Offering Details"
    fun offeringCategories(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચઢાવો કેટેગરી" else "Offering Categories"
    fun offeringAmount(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "રકમ (₹)" else "Amount (₹)"
    fun offeringQuantity(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "જથ્થો" else "Quantity"
    fun offeringLocation(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સ્થળ" else "Location"
    fun submitOffering(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચઢાવો નોંધાવો" else "Submit Offering"

    // Winner Announcement Flow
    fun winnerPromptTitle(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "તમે ચઢાવાના વિજેતા બન્યા છો!" else "You won the offering!"
    fun winnerPromptSubtitle(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચઢાવાની જાહેરાતમાં દર્શાવવા માટે તમારું નામ દાખલ કરો." else "Enter the name you would like displayed publicly."
    fun winnerNameInputLabel(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "પ્રદર્શિત નામ (૨ થી ૬૦ અક્ષરો)" else "Display Name (2 to 60 characters)"
    fun winnerConfirmButton(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નામ કન્ફર્મ કરો" else "Confirm Name"
    fun winnerSubmitting(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સબમિટ થઈ રહ્યું છે..." else "Submitting..."
    fun winnerTimeRemaining(seconds: Long, lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "બાકી સમય: $seconds સેકન્ડ" else "Time remaining: ${seconds}s"
    fun winnerErrorNameTooShort(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નામ ઓછામાં ઓછા ૨ અક્ષરનું હોવું જોઈએ." else "Name must be at least 2 characters."
    fun winnerErrorNameTooLong(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નામ ૬૦ અક્ષરથી વધુ ન હોવું જોઈએ." else "Name must be 60 characters or fewer."
    fun winnerDeadlineExpired(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સમય મર્યાદા પૂરી થઈ ગઈ છે. વહીવટકર્તા ટૂંક સમયમાં નામ દાખલ કરશે." else "Submission window closed. The administrator will enter the display name."
    fun winnerPendingConfirmation(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "વિજેતાનું નામ કન્ફર્મ થઈ રહ્યું છે..." else "Winner is being confirmed..."
    fun winnerCardTitle(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સંપન્ન સેવા વિજેતા" else "Offering Completed"
    fun winnerErrorInvalidLength(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નામ ૨ થી ૬૦ અક્ષરનું હોવું જોઈએ." else "Name must be between 2 and 60 characters."
    fun winnerErrorNotWinner(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "તમે આ ચઢાવાના વિજેતા નથી." else "You are not the winning bidder for this offering."
    fun winnerErrorDeadlinePassed(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સમય મર્યાદા સમાપ્ત થઈ ગઈ છે." else "The submission deadline has passed."
    fun winnerErrorNetwork(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નેટવર્ક સમસ્યા. કૃપા કરીને તમારું ઇન્ટરનેટ કનેક્શન તપાસો." else "Network error. Please check your connection."
    fun winnerErrorGeneric(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નામ સબમિટ કરવામાં ભૂલ આવી. કૃપા કરીને ફરી પ્રયાસ કરો." else "Failed to submit name. Please try again."

    // Complaints
    fun complaintsTitle(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ફરિયાદ નિવારણ" else "Complaints"
    fun newComplaint(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નવી ફરિયાદ નોંધાવો" else "Register Complaint"
    fun myComplaints(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "મારી ફરિયાદો" else "My Complaints"
    fun complaintTitle(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ફરિયાદનું શીર્ષક" else "Complaint Title"
    fun complaintDescription(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "વિગતવાર વર્ણન" else "Detailed Description"
    fun complaintCategory(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "કેટેગરી પસંદ કરો" else "Select Category"
    fun submit(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સબમિટ કરો" else "Submit"

    // Emergency
    fun emergencyTitle(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "તાત્કાલિક સહાય સંપર્કો" else "Emergency Contacts"
    fun emergencyNotice(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "ચકાસાયેલા અધિકૃત હેલ્પલાઇન નંબરો" else "Verified Official Helpline Numbers"
    fun callNow(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "કૉલ કરો" else "Call Now"

    // Businesses & Directory
    fun call(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "કૉલ" else "Call"
    fun whatsapp(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "વોટ્સએપ" else "WhatsApp"
    fun map(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નકશો" else "Map"
    fun verifiedBadge(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ચકાસાયેલ" else "Verified"

    // Auth & Profile
    fun login(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "પ્રવેશ કરો" else "Login"
    fun register(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "નવું ખાતું બનાવો" else "Register"
    fun fullName(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "પૂરું નામ" else "Full Name"
    fun email(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ઈમેઈલ" else "Email"
    fun password(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "પાસવર્ડ" else "Password"
    fun phone(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "મોબાઇલ નંબર" else "Mobile Number"
    fun preferredLanguage(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "પસંદગીની ભાષા" else "Preferred Language"
    fun editProfile(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "પ્રોફાઇલમાં ફેરફાર" else "Edit Profile"
    fun logout(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "લૉગ આઉટ" else "Logout"
    fun settings(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "સેટિંગ્સ" else "Settings"

    // Common UX States
    fun loading(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "લોડ થઈ રહ્યું છે..." else "Loading..."
    fun emptyData(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "હાલમાં કોઈ માહિતી ઉપલબ્ધ નથી" else "Data not available yet"
    fun errorGeneral(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "કંઈક ખોટું થયું છે" else "Something went wrong"
    fun tryAgain(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ફરી પ્રયાસ કરો" else "Try Again"
    fun offlineNotice(lang: AppLanguage) = 
        if (lang == AppLanguage.GUJARATI) "તમે ઑફલાઇન છો. સાચવેલી માહિતી દર્શાવી રહ્યા છીએ." else "You are offline. Showing saved information."
    fun demoDataBadge(lang: AppLanguage) = if (lang == AppLanguage.GUJARATI) "ડેમો માહિતી" else "DEMO DATA"
}

@Composable
fun stringResource(block: Strings.(AppLanguage) -> String): String {
    val currentLang = LocalAppLanguage.current
    return Strings.block(currentLang)
}
