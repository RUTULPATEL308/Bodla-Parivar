package com.bodla.parivar

import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.domain.model.OfferingBid
import com.bodla.parivar.features.offerings.calculateMinimumBidAmount
import com.bodla.parivar.features.offerings.filterVisibleBidsForUser
import com.bodla.parivar.features.offerings.getWinnerNameForUser
import com.bodla.parivar.features.offerings.resolveCurrentHighestBid
import com.bodla.parivar.features.offerings.replaceBidHistory
import com.bodla.parivar.features.offerings.validateBidAmount
import kotlin.test.Test
import kotlin.test.assertEquals

class OfferingBidHistoryTest {
    @Test
    fun replaceBidHistory_removesRowsDeletedOnServer() {
        val serverBids = listOf(
            OfferingBid(
                id = "a",
                offeringId = "offer-1",
                bidderName = "Asha",
                amount = 1700.0,
                status = "APPROVED",
                createdAt = "2026-10-05T09:10:00Z"
            ),
            OfferingBid(
                id = "c",
                offeringId = "offer-1",
                bidderName = "Chand",
                amount = 2000.0,
                status = "PENDING",
                createdAt = "2026-10-05T09:15:00Z"
            )
        )

        val replaced = replaceBidHistory(serverBids)

        assertEquals(listOf("c", "a"), replaced.map { it.id })
        assertEquals(1700.0, replaced.first { it.id == "a" }.amount)
    }

    @Test
    fun filterVisibleBidsForUser_shows_only_current_users_bid_history() {
        val currentUser = Profile(
            id = "user-1",
            authUserId = "auth-1",
            fullName = "Nisha Patel",
            phone = "9999999999"
        )

        val bids = listOf(
            OfferingBid(
                id = "mine",
                offeringId = "offer-1",
                bidderName = "Nisha Patel",
                bidderPhone = "9999999999",
                amount = 1500.0,
                status = "PENDING",
                createdAt = "2026-10-05T09:10:00Z"
            ),
            OfferingBid(
                id = "other",
                offeringId = "offer-1",
                bidderName = "Amit Shah",
                bidderPhone = "8888888888",
                amount = 1800.0,
                status = "APPROVED",
                createdAt = "2026-10-05T09:20:00Z"
            )
        )

        val visible = filterVisibleBidsForUser(bids, currentUser)

        assertEquals(listOf("mine"), visible.map { it.id })
    }

    @Test
    fun getWinnerNameForUser_hides_name_until_winner_confirms() {
        val winner = Profile(
            id = "user-1",
            authUserId = "auth-1",
            fullName = "Amit Shah"
        )

        assertEquals(null, getWinnerNameForUser(winner, "Amit Shah", false))
        assertEquals("Amit Shah", getWinnerNameForUser(winner, "Amit Shah", true))
        assertEquals(null, getWinnerNameForUser(Profile(id = "user-2", authUserId = "auth-2", fullName = "Riya"), "Amit Shah", true))
    }

    @Test
    fun calculateMinimumBidAmount_rejects_lower_bids_and_tracks_current_highest() {
        assertEquals(5000.0, calculateMinimumBidAmount(5000.0, null))
        assertEquals(2101.0, calculateMinimumBidAmount(2000.0, 2100.0))

        val validBid = validateBidAmount(2101.0, 2000.0, 2100.0)
        assertEquals(true, validBid.first)

        val invalidBid = validateBidAmount(2100.0, 2000.0, 2100.0)
        assertEquals(false, invalidBid.first)
        assertEquals("Bid must be at least ₹2101. The current highest bid is ₹2100.", invalidBid.second)

        val invalidBidGujarati = validateBidAmount(2100.0, 2000.0, 2100.0, AppLanguage.GUJARATI)
        assertEquals(false, invalidBidGujarati.first)
        assertEquals("બોલી ઓછામાં ઓછી ₹2101 હોવી જોઈએ. હાલમાં સૌથી વધુ બોલી ₹2100 છે.", invalidBidGujarati.second)
    }

    @Test
    fun resolveCurrentHighestBid_prefersLiveBidsOverStaleStoredAmount() {
        val bids = listOf(
            OfferingBid("old", "offer-1", "Asha", amount = 1800.0, status = "APPROVED", createdAt = "1"),
            OfferingBid("live", "offer-1", "Ravi", amount = 2500.0, status = "PENDING", createdAt = "2"),
            OfferingBid("rejected", "offer-1", "Mina", amount = 4000.0, status = "REJECTED", createdAt = "3")
        )

        assertEquals(2500.0, resolveCurrentHighestBid(bids, 1200.0))
        assertEquals(1200.0, resolveCurrentHighestBid(emptyList(), 1200.0))
    }
}
