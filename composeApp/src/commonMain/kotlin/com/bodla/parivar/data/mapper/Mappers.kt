package com.bodla.parivar.data.mapper

import com.bodla.parivar.data.remote.dto.*
import com.bodla.parivar.database.*
import com.bodla.parivar.domain.model.*

// --- Notice Mappers ---
fun NoticeDto.toDomain(): Notice = Notice(
    id = id,
    titleGu = titleGu,
    titleEn = titleEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = category?.nameGu,
    categoryNameEn = category?.nameEn,
    imageUrl = imageUrl,
    attachmentUrl = attachmentUrl,
    isPinned = isPinned,
    status = status,
    publishAt = publishAt,
    createdAt = createdAt
)

fun CachedNotice.toDomain(): Notice = Notice(
    id = id,
    titleGu = titleGu,
    titleEn = titleEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = categoryNameGu,
    categoryNameEn = categoryNameEn,
    imageUrl = imageUrl,
    attachmentUrl = attachmentUrl,
    isPinned = isPinned == 1L,
    status = status,
    publishAt = publishAt,
    createdAt = createdAt
)

// --- Event Mappers ---
fun EventDto.toDomain(): Event = Event(
    id = id,
    titleGu = titleGu,
    titleEn = titleEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = category?.nameGu,
    categoryNameEn = category?.nameEn,
    startAt = startAt,
    endAt = endAt,
    locationGu = locationGu,
    locationEn = locationEn,
    imageUrl = imageUrl,
    status = status
)

fun CachedEvent.toDomain(): Event = Event(
    id = id,
    titleGu = titleGu,
    titleEn = titleEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = categoryNameGu,
    categoryNameEn = categoryNameEn,
    startAt = startAt,
    endAt = endAt,
    locationGu = locationGu,
    locationEn = locationEn,
    imageUrl = imageUrl,
    status = status
)

// --- Business Mappers ---
fun BusinessDto.toDomain(): Business = Business(
    id = id,
    nameGu = nameGu,
    nameEn = nameEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = category?.nameGu,
    categoryNameEn = category?.nameEn,
    phone = phone,
    whatsapp = whatsapp,
    email = email,
    addressGu = addressGu,
    addressEn = addressEn,
    latitude = latitude,
    longitude = longitude,
    logoUrl = logoUrl,
    isVerified = isVerified,
    status = status
)

fun CachedBusiness.toDomain(): Business = Business(
    id = id,
    nameGu = nameGu,
    nameEn = nameEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = categoryNameGu,
    categoryNameEn = categoryNameEn,
    phone = phone,
    whatsapp = whatsapp,
    email = email,
    addressGu = addressGu,
    addressEn = addressEn,
    latitude = latitude,
    longitude = longitude,
    logoUrl = logoUrl,
    isVerified = isVerified == 1L,
    status = status
)

// --- Offering Mappers ---
fun OfferingDto.toDomain(): Offering = Offering(
    id = id,
    titleGu = titleGu,
    titleEn = titleEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = category?.nameGu,
    categoryNameEn = category?.nameEn,
    amount = amount,
    locationGu = locationGu,
    locationEn = locationEn,
    imageUrl = imageUrl,
    status = status,
    startAt = startAt,
    endAt = endAt,
    bidCallCount = bidCallCount,
    winningBidderName = winningBidderName,
    winningBidAmount = winningBidAmount
)

fun CachedOffering.toDomain(): Offering = Offering(
    id = id,
    titleGu = titleGu,
    titleEn = titleEn,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    categoryNameGu = categoryNameGu,
    categoryNameEn = categoryNameEn,
    amount = amount,
    locationGu = locationGu,
    locationEn = locationEn,
    imageUrl = imageUrl,
    status = status,
    startAt = startAt,
    endAt = endAt,
    bidCallCount = bidCallCount.toInt(),
    winningBidderName = winningBidderName,
    winningBidAmount = winningBidAmount
)

// --- Emergency Contact Mappers ---
fun EmergencyContactDto.toDomain(): EmergencyContact = EmergencyContact(
    id = id,
    nameGu = nameGu,
    nameEn = nameEn,
    organization = organization,
    phone = phone,
    alternatePhone = alternatePhone,
    category = category,
    displayOrder = displayOrder
)

fun CachedEmergencyContact.toDomain(): EmergencyContact = EmergencyContact(
    id = id,
    nameGu = nameGu,
    nameEn = nameEn,
    organization = organization,
    phone = phone,
    alternatePhone = alternatePhone,
    category = category,
    displayOrder = displayOrder.toInt()
)

// --- Village Info Mappers ---
fun VillageSettingsDto.toDomain(): VillageInfo = VillageInfo(
    id = id,
    nameGu = villageNameGu,
    nameEn = villageNameEn,
    district = district,
    state = state,
    country = country,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    contactPhone = contactPhone,
    contactEmail = contactEmail,
    latitude = latitude ?: 23.5880,
    longitude = longitude ?: 72.3693
)

fun CachedVillageInfo.toDomain(): VillageInfo = VillageInfo(
    id = id,
    nameGu = villageNameGu,
    nameEn = villageNameEn,
    district = district,
    state = state,
    country = country,
    descriptionGu = descriptionGu,
    descriptionEn = descriptionEn,
    contactPhone = contactPhone,
    contactEmail = contactEmail,
    latitude = latitude ?: 23.5880,
    longitude = longitude ?: 72.3693
)

// --- Profile Mappers ---
fun ProfileDto.toDomain(): Profile = Profile(
    id = id,
    authUserId = authUserId,
    fullName = fullName,
    phone = phone,
    email = email,
    profilePhotoUrl = profilePhotoUrl,
    address = address,
    bio = bio,
    preferredLanguage = preferredLanguage,
    isVerified = isVerified,
    status = status
)
