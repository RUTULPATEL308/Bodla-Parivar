package com.bodla.parivar.features.offerings

import com.bodla.parivar.domain.model.OfferingBid
import com.bodla.parivar.domain.model.Profile
import com.bodla.parivar.core.localization.AppLanguage
import kotlin.math.max
import kotlin.math.roundToLong

fun calculateMinimumBidAmount(baseAmount: Double?, currentHighestBid: Double?): Double {
    val baseBid = baseAmount?.takeIf { it > 0 } ?: 0.0
    val highestBid = currentHighestBid?.takeIf { it > 0 } ?: 0.0
    return if (highestBid <= 0.0) {
        baseBid
    } else {
        max(baseBid, highestBid + 1.0)
    }
}

fun validateBidAmount(
    amount: Double,
    baseAmount: Double?,
    currentHighestBid: Double?,
    lang: AppLanguage = AppLanguage.ENGLISH
): Pair<Boolean, String?> {
    if (amount <= 0.0) {
        return false to if (lang == AppLanguage.GUJARATI) "શૂન્યથી વધુ માન્ય રકમ દાખલ કરો." else "Enter a valid amount greater than zero."
    }

    val minimumAllowed = calculateMinimumBidAmount(baseAmount, currentHighestBid)
    if (amount < minimumAllowed) {
        val formattedMinimum = minimumAllowed.roundToLong().toString()
        val formattedHighest = currentHighestBid?.roundToLong()?.toString()
        val message = if (lang == AppLanguage.GUJARATI) {
            if (currentHighestBid != null && currentHighestBid > 0.0) {
                "બોલી ઓછામાં ઓછી ₹$formattedMinimum હોવી જોઈએ. હાલમાં સૌથી વધુ બોલી ₹$formattedHighest છે."
            } else {
                "આ ચઢાવા માટે ઓછામાં ઓછી રકમ ₹$formattedMinimum હોવી જોઈએ."
            }
        } else if (currentHighestBid != null && currentHighestBid > 0.0) {
            "Bid must be at least ₹$formattedMinimum. The current highest bid is ₹$formattedHighest."
        } else {
            "Bid must be at least ₹$formattedMinimum for this offering."
        }
        return false to message
    }

    return true to null
}

fun replaceBidHistory(serverBids: List<OfferingBid>): List<OfferingBid> =
    serverBids.sortedByDescending { bid -> bid.createdAt }

fun resolveCurrentHighestBid(bids: List<OfferingBid>, storedWinningBidAmount: Double?): Double? =
    bids.asSequence()
        .filter { bid -> bid.status != "REJECTED" }
        .maxOfOrNull { bid -> bid.amount }
        ?: storedWinningBidAmount?.takeIf { amount -> amount > 0.0 }

fun filterVisibleBidsForUser(serverBids: List<OfferingBid>, currentUser: Profile?): List<OfferingBid> {
    if (currentUser == null) return emptyList()
    if (currentUser.isStaff) {
        return serverBids.filter { bid -> bid.status != "REJECTED" }
    }

    val normalizedUserName = currentUser.fullName.trim()
    val normalizedPhone = currentUser.phone?.trim()

    return serverBids.filter { bid ->
        bid.status != "REJECTED" && (
            (!normalizedPhone.isNullOrBlank() && bid.bidderPhone?.trim() == normalizedPhone) ||
                bid.bidderName.equals(normalizedUserName, ignoreCase = true)
            )
    }
}

fun getWinnerNameForUser(currentUser: Profile?, winningBidderName: String?, winnerAccepted: Boolean = false): String? {
    if (winningBidderName.isNullOrBlank()) return null
    if (currentUser == null) return null
    if (currentUser.isStaff) return winningBidderName
    if (!winnerAccepted) return null
    return if (currentUser.fullName.equals(winningBidderName.trim(), ignoreCase = true)) winningBidderName else null
}
