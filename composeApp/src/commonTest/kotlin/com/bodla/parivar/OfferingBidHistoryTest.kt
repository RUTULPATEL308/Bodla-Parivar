package com.bodla.parivar

import com.bodla.parivar.domain.model.OfferingBid
import com.bodla.parivar.features.offerings.replaceBidHistory
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
}
