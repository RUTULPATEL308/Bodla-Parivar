package com.bodla.parivar.features.offerings

import com.bodla.parivar.domain.model.OfferingBid

fun replaceBidHistory(serverBids: List<OfferingBid>): List<OfferingBid> =
    serverBids.sortedByDescending { bid -> bid.createdAt }
