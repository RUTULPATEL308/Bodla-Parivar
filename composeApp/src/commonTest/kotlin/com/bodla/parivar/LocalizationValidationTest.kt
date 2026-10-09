package com.bodla.parivar

import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.Strings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalizationValidationTest {

    @Test
    fun testAppNameExactSpelling() {
        assertEquals("બોદલા પરિવાર", Strings.APP_NAME_GU)
        assertEquals("Bodla Parivar", Strings.APP_NAME_EN)
    }

    @Test
    fun testVillageNameExactSpelling() {
        assertTrue(Strings.VILLAGE_SUBTITLE_GU.contains("બોદલા"))
        assertTrue(Strings.VILLAGE_SUBTITLE_EN.contains("Bodla"))
    }

    @Test
    fun testBilingualStringsSwitching() {
        // Gujarati
        assertEquals("મુખ્ય", Strings.navHome(AppLanguage.GUJARATI))
        assertEquals("સૂચનાઓ", Strings.navNotices(AppLanguage.GUJARATI))
        assertEquals("ચઢાવો", Strings.offeringsTitle(AppLanguage.GUJARATI))
        assertEquals("ફરિયાદ નિવારણ", Strings.complaintsTitle(AppLanguage.GUJARATI))

        // English
        assertEquals("Home", Strings.navHome(AppLanguage.ENGLISH))
        assertEquals("Notices", Strings.navNotices(AppLanguage.ENGLISH))
        assertEquals("Offerings", Strings.offeringsTitle(AppLanguage.ENGLISH))
        assertEquals("Complaints", Strings.complaintsTitle(AppLanguage.ENGLISH))
    }

    @Test
    fun testOfferingsDoesNotContainAuctionTerminology() {
        val guOffering = Strings.offeringsTitle(AppLanguage.GUJARATI)
        val enOffering = Strings.offeringsTitle(AppLanguage.ENGLISH)

        assertFalse(guOffering.contains("Auction", ignoreCase = true))
        assertFalse(enOffering.contains("Auction", ignoreCase = true))
        assertFalse(enOffering.contains("Bid", ignoreCase = true))
    }
}
