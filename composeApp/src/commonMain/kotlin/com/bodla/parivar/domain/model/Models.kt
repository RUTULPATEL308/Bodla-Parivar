package com.bodla.parivar.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ==============================================================================
// Domain Models: બોદલા પરિવાર
// ==============================================================================

@Serializable
data class Profile(
    val id: String,
    val authUserId: String,
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val profilePhotoUrl: String? = null,
    val address: String? = null,
    val bio: String? = null,
    val preferredLanguage: String = "gu",
    val isVerified: Boolean = false,
    val status: String = "ACTIVE",
    val isStaff: Boolean = false,
    val isAdmin: Boolean = false
)

@Serializable
data class VillageInfo(
    val id: String,
    val nameGu: String = "બોદલા",
    val nameEn: String = "Bodla",
    val district: String = "Mehsana",
    val state: String = "Gujarat",
    val country: String = "India",
    val descriptionGu: String? = null,
    val descriptionEn: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val latitude: Double = 23.5880,
    val longitude: Double = 72.3693
)

@Serializable
data class Notice(
    val id: String,
    val titleGu: String,
    val titleEn: String,
    val descriptionGu: String? = null,
    val descriptionEn: String? = null,
    val categoryNameGu: String? = null,
    val categoryNameEn: String? = null,
    val imageUrl: String? = null,
    val attachmentUrl: String? = null,
    val isPinned: Boolean = false,
    val status: String = "PUBLISHED",
    val publishAt: String? = null,
    val createdAt: String
)

@Serializable
data class Event(
    val id: String,
    val titleGu: String,
    val titleEn: String,
    val descriptionGu: String? = null,
    val descriptionEn: String? = null,
    val categoryNameGu: String? = null,
    val categoryNameEn: String? = null,
    val startAt: String,
    val endAt: String? = null,
    val locationGu: String? = null,
    val locationEn: String? = null,
    val imageUrl: String? = null,
    val status: String = "PUBLISHED"
)

@Serializable
data class Business(
    val id: String,
    val nameGu: String,
    val nameEn: String,
    val descriptionGu: String? = null,
    val descriptionEn: String? = null,
    val categoryNameGu: String? = null,
    val categoryNameEn: String? = null,
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val addressGu: String? = null,
    val addressEn: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val logoUrl: String? = null,
    val isVerified: Boolean = false,
    val status: String = "APPROVED"
)

@Serializable
data class Job(
    val id: String,
    val titleGu: String,
    val titleEn: String,
    val descriptionGu: String? = null,
    val descriptionEn: String? = null,
    val companyName: String? = null,
    val locationGu: String? = null,
    val locationEn: String? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val employmentType: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val expiresAt: String? = null,
    val status: String = "PUBLISHED"
)

// CRITICAL: Offering / ચઢાવો is NOT an auction
@Serializable
data class Offering(
    val id: String,
    val titleGu: String,
    val titleEn: String,
    val descriptionGu: String? = null,
    val descriptionEn: String? = null,
    val categoryNameGu: String? = null,
    val categoryNameEn: String? = null,
    val amount: Double? = null,
    val quantity: Double? = null,
    val locationGu: String? = null,
    val locationEn: String? = null,
    val imageUrl: String? = null,
    val status: String = "ACTIVE",
    val startAt: String? = null,
    val endAt: String? = null,
    val bidCallCount: Int = 0,
    val winningBidderName: String? = null,
    val winningBidAmount: Double? = null
)

@Serializable
data class OfferingBid(
    val id: String,
    @SerialName("offering_id") val offeringId: String,
    @SerialName("bidder_name") val bidderName: String,
    @SerialName("bidder_phone") val bidderPhone: String? = null,
    val amount: Double,
    val status: String = "PENDING",
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class Complaint(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val categoryNameGu: String? = null,
    val categoryNameEn: String? = null,
    val photoUrl: String? = null,
    val status: String = "SUBMITTED",
    val priority: String = "NORMAL",
    val resolutionNote: String? = null,
    val createdAt: String
)

@Serializable
data class EmergencyContact(
    val id: String,
    val nameGu: String,
    val nameEn: String,
    val organization: String? = null,
    val phone: String,
    val alternatePhone: String? = null,
    val category: String,
    val displayOrder: Int = 0
)

@Serializable
data class Place(
    val id: String,
    val nameGu: String,
    val nameEn: String,
    val descriptionGu: String? = null,
    val descriptionEn: String? = null,
    val categoryNameGu: String? = null,
    val categoryNameEn: String? = null,
    val addressGu: String? = null,
    val addressEn: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null
)
