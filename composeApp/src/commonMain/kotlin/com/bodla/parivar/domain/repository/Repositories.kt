package com.bodla.parivar.domain.repository

import com.bodla.parivar.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String):Result<Profile>
    suspend fun register(fullName: String, email: String, password: String, phone: String?): Result<Profile>
    suspend fun getCurrentUser(): Profile?
    suspend fun getRegisteredUsers(): Result<List<Profile>>
    suspend fun updateProfile(profile: Profile): Result<Profile>
    suspend fun logout(): Result<Unit>
}

interface NoticeRepository {
    fun getNotices(): Flow<List<Notice>>
    suspend fun refreshNotices(): Result<Unit>
    suspend fun getPinnedNotice(): Notice?
    suspend fun getNoticeById(id: String): Notice?
}

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    fun getUpcomingEvents(): Flow<List<Event>>
    suspend fun refreshEvents(): Result<Unit>
    suspend fun getEventById(id: String): Event?
}

interface BusinessRepository {
    fun getBusinesses(): Flow<List<Business>>
    suspend fun refreshBusinesses(): Result<Unit>
    suspend fun getBusinessById(id: String): Business?
}

// CRITICAL: Offering / ચઢાવો repository - NOT an auction
interface OfferingRepository {
    fun getOfferings(): Flow<List<Offering>>
    suspend fun refreshOfferings(): Result<Unit>
    suspend fun getOfferingById(id: String): Offering?
    suspend fun submitOffering(offering: Offering): Result<Unit>
    suspend fun submitOfferingBid(offeringId: String, bidderName: String, amount: Double, bidderPhone: String? = null): Result<Unit>
    suspend fun getOfferingBids(offeringId: String): Result<List<OfferingBid>>
}

interface ComplaintRepository {
    fun getMyComplaints(): Flow<List<Complaint>>
    suspend fun submitComplaint(title: String, description: String, categoryId: String?, photoUrl: String?): Result<Unit>
}

interface EmergencyRepository {
    fun getEmergencyContacts(): Flow<List<EmergencyContact>>
    suspend fun refreshEmergencyContacts(): Result<Unit>
}

interface VillageRepository {
    fun getVillageInfo(): Flow<VillageInfo>
    suspend fun refreshVillageInfo(): Result<Unit>
}
