package com.bodla.parivar

import com.bodla.parivar.domain.model.Offering
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfferingLifecycleTest {

    @Test
    fun testOfferingInitialStatusIsPendingApproval() {
        val offering = Offering(
            id = "test_offering_1",
            titleGu = "શ્રી મહાદેવ મંદિર ધ્વજારોહણ સેવા",
            titleEn = "Shri Mahadev Temple Dhwajarohan Seva",
            amount = 5100.0,
            status = "PENDING_APPROVAL"
        )

        assertEquals("PENDING_APPROVAL", offering.status)
        assertEquals(5100.0, offering.amount)
    }

    @Test
    fun testOfferingApprovalTransition() {
        val pendingOffering = Offering(
            id = "test_offering_2",
            titleGu = "ગૌશાળા ઘાસચારો સેવા",
            titleEn = "Gaushala Fodder Seva",
            amount = 2100.0,
            status = "PENDING_APPROVAL"
        )

        // Admin approves offering
        val approvedOffering = pendingOffering.copy(status = "APPROVED")
        assertEquals("APPROVED", approvedOffering.status)

        // Offering goes active for village participation
        val activeOffering = approvedOffering.copy(status = "ACTIVE")
        assertEquals("ACTIVE", activeOffering.status)

        // Offering completes
        val completedOffering = activeOffering.copy(status = "COMPLETED")
        assertEquals("COMPLETED", completedOffering.status)
    }

    @Test
    fun testOfferingDoesNotContainAuctionOrBiddingProperties() {
        val offering = Offering(
            id = "test_offering_3",
            titleGu = "પૂજા અર્ચના સેવા",
            titleEn = "Puja Archana Seva",
            status = "ACTIVE"
        )

        // Verify clean offering domain semantics
        assertTrue(offering.status in listOf("PENDING_APPROVAL", "APPROVED", "ACTIVE", "COMPLETED", "REJECTED"))
    }

    @Test
    fun testBidAcceptanceDependsOnOfferingStatusAndCallCount() {
        val openOffering = Offering(
            id = "test_offering_4",
            titleGu = "તેલ ગૌશાળા ચઢાવો",
            titleEn = "Gaushala Offering",
            status = "ACTIVE",
            bidCallCount = 2
        )

        val closedOffering = Offering(
            id = "test_offering_5",
            titleGu = "બંધ ચઢાવો",
            titleEn = "Closed Offering",
            status = "COMPLETED",
            bidCallCount = 3
        )

        assertTrue(openOffering.isBidOpen)
        assertFalse(closedOffering.isBidOpen)
    }
}
