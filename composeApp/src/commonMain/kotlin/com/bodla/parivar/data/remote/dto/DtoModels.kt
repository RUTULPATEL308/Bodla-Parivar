package com.bodla.parivar.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    @SerialName("name_gu") val nameGu: String? = null,
    @SerialName("name_en") val nameEn: String? = null
)

@Serializable
data class NoticeDto(
    val id: String,
    @SerialName("title_gu") val titleGu: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("description_gu") val descriptionGu: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    val status: String = "PUBLISHED",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("publish_at") val publishAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    val category: CategoryDto? = null
)

@Serializable
data class EventDto(
    val id: String,
    @SerialName("title_gu") val titleGu: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("description_gu") val descriptionGu: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("location_gu") val locationGu: String? = null,
    @SerialName("location_en") val locationEn: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String = "PUBLISHED",
    val category: CategoryDto? = null
)

@Serializable
data class BusinessDto(
    val id: String,
    @SerialName("name_gu") val nameGu: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("description_gu") val descriptionGu: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    @SerialName("address_gu") val addressGu: String? = null,
    @SerialName("address_en") val addressEn: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    val status: String = "APPROVED",
    val category: CategoryDto? = null
)

@Serializable
data class OfferingDto(
    val id: String,
    @SerialName("title_gu") val titleGu: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("description_gu") val descriptionGu: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    val amount: Double? = null,
    @SerialName("location_gu") val locationGu: String? = null,
    @SerialName("location_en") val locationEn: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String = "ACTIVE",
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("end_at") val endAt: String? = null,
    @SerialName("bid_call_count") val bidCallCount: Int = 0,
    @SerialName("winning_bidder_name") val winningBidderName: String? = null,
    @SerialName("winning_bid_amount") val winningBidAmount: Double? = null,
    val category: CategoryDto? = null
)

@Serializable
data class EmergencyContactDto(
    val id: String,
    @SerialName("name_gu") val nameGu: String,
    @SerialName("name_en") val nameEn: String,
    val organization: String? = null,
    val phone: String,
    @SerialName("alternate_phone") val alternatePhone: String? = null,
    val category: String,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class VillageSettingsDto(
    val id: String,
    @SerialName("village_name_gu") val villageNameGu: String = "બોદલા",
    @SerialName("village_name_en") val villageNameEn: String = "Bodla",
    val district: String = "Mehsana",
    val state: String = "Gujarat",
    val country: String = "India",
    @SerialName("description_gu") val descriptionGu: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
    val latitude: Double? = 23.5880,
    val longitude: Double? = 72.3693,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, String>? = null
)

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: AuthUserDto? = null
)

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("auth_user_id") val authUserId: String,
    @SerialName("full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    val address: String? = null,
    val bio: String? = null,
    @SerialName("preferred_language") val preferredLanguage: String = "gu",
    @SerialName("is_verified") val isVerified: Boolean = false,
    val status: String = "ACTIVE"
)
