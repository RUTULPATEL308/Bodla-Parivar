package com.bodla.parivar.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.bodla.parivar.core.network.Endpoints
import com.bodla.parivar.core.security.TokenStorage
import com.bodla.parivar.core.sync.SyncEngine
import com.bodla.parivar.data.mapper.*
import com.bodla.parivar.data.remote.dto.*
import com.bodla.parivar.database.AppDatabase
import com.bodla.parivar.domain.model.*
import com.bodla.parivar.domain.repository.*
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private fun friendlyAuthError(status: Int, body: String): String {
    val fields = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
    val code = fields?.get("code")?.jsonPrimitive?.contentOrNull.orEmpty().lowercase()
    val message = listOf("msg", "message", "error_description")
        .firstNotNullOfOrNull { fields?.get(it)?.jsonPrimitive?.contentOrNull }
        .orEmpty()
        .lowercase()
    val details = "$code $message"

    return when {
        "invalid_credentials" in details || "invalid login credentials" in details ->
            "Email or password is incorrect. Check both and try again."
        "email_not_confirmed" in details || "email not confirmed" in details ->
            "This email is not confirmed yet. Check your inbox or ask the project admin to disable email confirmation."
        "user_already_exists" in details || "already registered" in details || "already been registered" in details ->
            "An account already exists for this email. Sign in or use password recovery."
        "weak_password" in details || "password should" in details ->
            "Choose a stronger password with at least 6 characters."
        "over_email_send_rate_limit" in details || status == 429 ->
            "Too many attempts. Wait a few minutes, then try again."
        "signup_disabled" in details ->
            "New account registration is currently disabled. Contact the project admin."
        status >= 500 ->
            "The sign-in service is temporarily unavailable. Try again shortly."
        status == 400 || status == 422 ->
            "Some sign-in details were rejected. Check the email and password, then try again."
        else ->
            "We couldn't complete sign-in. Check your connection and try again."
    }
}

// ==============================================================================
// 1. AuthRepository Implementation (Supabase Auth & Profiles)
// ==============================================================================
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    private var cachedProfile: Profile? = null

    override suspend fun login(email: String, password: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val bodyPayload = buildJsonObject {
                put("email", email.trim())
                put("password", password)
            }

            val response = httpClient.post(Endpoints.AUTH_SIGN_IN) {
                contentType(ContentType.Application.Json)
                setBody(bodyPayload.toString())
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.body<String>()
                Napier.e("AuthRepository: Login failed HTTP ${response.status} - $errorBody")
                return@withContext Result.failure(Exception(friendlyAuthError(response.status.value, errorBody)))
            }

            val authDto = response.body<AuthResponseDto>()
            val accessToken = authDto.accessToken
                ?: return@withContext Result.failure(Exception("No access token in response"))

            tokenStorage.saveToken(accessToken)
            authDto.refreshToken?.let(tokenStorage::saveRefreshToken)
            val userId = authDto.user?.id

            val (storedProfile, accessFlags) = if (userId != null) {
                coroutineScope {
                    val profileRequest = async { fetchProfileByAuthId(userId) }
                    val accessRequest = async { fetchAccessFlags() }
                    profileRequest.await() to accessRequest.await()
                }
            } else {
                null to (false to false)
            }

            val (isStaff, isAdmin) = accessFlags
            val profile = storedProfile ?: if (userId != null) {
                Profile(
                    id = userId,
                    authUserId = userId,
                    fullName = authDto.user.userMetadata?.get("full_name") ?: email.substringBefore("@"),
                    email = email,
                    phone = authDto.user.userMetadata?.get("phone"),
                    preferredLanguage = "gu",
                    isVerified = false
                )
            } else {
                Profile(
                    id = "local_user",
                    authUserId = "local_user",
                    fullName = email.substringBefore("@"),
                    email = email
                )
            }

            val profileWithAccess = profile.copy(isStaff = isStaff, isAdmin = isAdmin)
            cachedProfile = profileWithAccess
            runCatching { tokenStorage.saveUserProfile(Json.encodeToString(profileWithAccess)) }
            Result.success(profileWithAccess)
        } catch (e: Exception) {
            Napier.e("AuthRepository: Login exception", e)
            Result.failure(e)
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String?
    ): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val bodyPayload = buildJsonObject {
                put("email", email.trim())
                put("password", password)
                putJsonObject("data") {
                    put("full_name", fullName.trim())
                    phone?.let { put("phone", it.trim()) }
                    put("preferred_language", "gu")
                }
            }

            val response = httpClient.post(Endpoints.AUTH_SIGN_UP) {
                contentType(ContentType.Application.Json)
                setBody(bodyPayload.toString())
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.body<String>()
                Napier.e("AuthRepository: Register failed HTTP ${response.status} - $errorBody")
                return@withContext Result.failure(Exception(friendlyAuthError(response.status.value, errorBody)))
            }

            val authDto = response.body<AuthResponseDto>()
            val accessToken = authDto.accessToken
                ?: return@withContext Result.failure(
                    Exception("Account created. Confirm your email, then log in.")
                )

            val userId = authDto.user?.id
                ?: return@withContext Result.failure(Exception("Registration response did not include a user."))
            tokenStorage.saveToken(accessToken)
            authDto.refreshToken?.let(tokenStorage::saveRefreshToken)
            val profile = Profile(
                id = userId,
                authUserId = userId,
                fullName = fullName,
                email = email,
                phone = phone,
                preferredLanguage = "gu",
                isVerified = false
            )
            cachedProfile = profile
            Result.success(profile)
        } catch (e: Exception) {
            Napier.e("AuthRepository: Registration exception", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Profile? = withContext(Dispatchers.IO) {
        if (cachedProfile != null) return@withContext cachedProfile

        // 1. FAST LOCAL CACHE HIT: Return persisted profile in 0ms so app opens instantly without blocking network delay
        val savedJson = tokenStorage.getUserProfile()
        if (!savedJson.isNullOrBlank()) {
            val localProfile = runCatching { Json.decodeFromString<Profile>(savedJson) }.getOrNull()
            if (localProfile != null) {
                cachedProfile = localProfile
                return@withContext localProfile
            }
        }

        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) return@withContext null

        try {
            val response = httpClient.get(Endpoints.AUTH_USER)
            if (response.status.isSuccess()) {
                val user = response.body<AuthUserDto>()
                val (storedProfile, accessFlags) = coroutineScope {
                    val profileRequest = async { fetchProfileByAuthId(user.id) }
                    val accessRequest = async { fetchAccessFlags() }
                    profileRequest.await() to accessRequest.await()
                }
                val profile = storedProfile ?: Profile(
                    id = user.id,
                    authUserId = user.id,
                    fullName = user.userMetadata?.get("full_name") ?: user.email?.substringBefore("@") ?: "Bodla Resident",
                    email = user.email,
                    phone = user.userMetadata?.get("phone")
                )
                val (isStaff, isAdmin) = accessFlags
                val profileWithAccess = profile.copy(isStaff = isStaff, isAdmin = isAdmin)
                cachedProfile = profileWithAccess
                runCatching { tokenStorage.saveUserProfile(Json.encodeToString(profileWithAccess)) }
                profileWithAccess
            } else {
                null
            }
        } catch (e: Exception) {
            Napier.w("AuthRepository: Offline or error fetching current user", e)
            null
        }
    }

    override suspend fun updateProfile(profile: Profile): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val updatePayload = buildJsonObject {
                put("full_name", profile.fullName)
                profile.phone?.let { put("phone", it) }
                profile.address?.let { put("address", it) }
                profile.bio?.let { put("bio", it) }
                put("preferred_language", profile.preferredLanguage)
            }

            val response = httpClient.patch("${Endpoints.PROFILES}?id=eq.${profile.id}") {
                contentType(ContentType.Application.Json)
                setBody(updatePayload.toString())
            }

            if (response.status.isSuccess()) {
                cachedProfile = profile
                Result.success(profile)
            } else {
                Result.failure(Exception("Failed to update profile HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.e("AuthRepository: Update profile error", e)
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            httpClient.post(Endpoints.AUTH_LOGOUT)
        } catch (_: Exception) {
            // Best effort logout on server
        }
        tokenStorage.clearToken()
        cachedProfile = null
        Result.success(Unit)
    }

    private suspend fun fetchProfileByAuthId(authUserId: String): Profile? {
        return try {
            val response = httpClient.get("${Endpoints.PROFILES}?auth_user_id=eq.$authUserId&limit=1")
            if (response.status.isSuccess()) {
                val profiles = response.body<List<ProfileDto>>()
                profiles.firstOrNull()?.toDomain()
            } else {
                null
            }
        } catch (e: Exception) {
            Napier.w("AuthRepository: Could not fetch profile by auth id", e)
            null
        }
    }

    private suspend fun fetchStaffAccess(): Boolean {
        return try {
            val response = httpClient.post(Endpoints.IS_STAFF) {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            if (response.status.isSuccess()) response.body<Boolean>() else false
        } catch (e: Exception) {
            Napier.w("AuthRepository: Could not check staff access", e)
            false
        }
    }

    private suspend fun fetchAdminAccess(): Boolean {
        return try {
            val response = httpClient.post(Endpoints.IS_ADMIN) {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            if (response.status.isSuccess()) response.body<Boolean>() else false
        } catch (e: Exception) {
            Napier.w("AuthRepository: Could not check administrator access", e)
            false
        }
    }

    private suspend fun fetchAccessFlags(): Pair<Boolean, Boolean> = coroutineScope {
        val staffAccess = async { fetchStaffAccess() }
        val adminAccess = async { fetchAdminAccess() }
        staffAccess.await() to adminAccess.await()
    }

    override suspend fun getRegisteredUsers(): Result<List<Profile>> = withContext(Dispatchers.IO) {
        try {
            if (!fetchAdminAccess()) {
                return@withContext Result.failure(Exception("Administrator access is required to view registered users."))
            }
            val response = httpClient.get("${Endpoints.PROFILES}?select=*&order=created_at.desc")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(Exception("Could not load registered users (${response.status.value})."))
            }
            val profiles = response.body<List<ProfileDto>>().map { it.toDomain() }
            Result.success(profiles)
        } catch (e: Exception) {
            Napier.e("AuthRepository: Could not load registered users", e)
            Result.failure(e)
        }
    }
}

// ==============================================================================
// 2. NoticeRepository Implementation (Offline-First SQLDelight Cache)
// ==============================================================================
class NoticeRepositoryImpl(
    private val database: AppDatabase,
    private val httpClient: HttpClient
) : NoticeRepository {

    override fun getNotices(): Flow<List<Notice>> {
        return database.appDatabaseQueries
            .selectAllNotices()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshNotices(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(Endpoints.NOTICES)
            if (response.status.isSuccess()) {
                val dtos = response.body<List<NoticeDto>>()
                database.transaction {
                    for (dto in dtos) {
                        database.appDatabaseQueries.insertOrReplaceNotice(
                            id = dto.id,
                            titleGu = dto.titleGu,
                            titleEn = dto.titleEn,
                            descriptionGu = dto.descriptionGu,
                            descriptionEn = dto.descriptionEn,
                            categoryNameGu = dto.category?.nameGu,
                            categoryNameEn = dto.category?.nameEn,
                            imageUrl = dto.imageUrl,
                            attachmentUrl = dto.attachmentUrl,
                            isPinned = if (dto.isPinned) 1L else 0L,
                            status = dto.status,
                            publishAt = dto.publishAt,
                            createdAt = dto.createdAt
                        )
                    }
                }
                Napier.i("NoticeRepository: Refreshed ${dtos.size} notices from server")
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status} fetching notices"))
            }
        } catch (e: Exception) {
            Napier.w("NoticeRepository: Offline mode or error refreshing notices", e)
            Result.failure(e)
        }
    }

    override suspend fun getPinnedNotice(): Notice? = withContext(Dispatchers.IO) {
        database.appDatabaseQueries
            .selectPinnedNotices()
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun getNoticeById(id: String): Notice? = withContext(Dispatchers.IO) {
        database.appDatabaseQueries
            .selectNoticeById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }
}

// ==============================================================================
// 3. EventRepository Implementation
// ==============================================================================
class EventRepositoryImpl(
    private val database: AppDatabase,
    private val httpClient: HttpClient
) : EventRepository {

    override fun getEvents(): Flow<List<Event>> {
        return database.appDatabaseQueries
            .selectAllEvents()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getUpcomingEvents(): Flow<List<Event>> {
        return database.appDatabaseQueries
            .selectUpcomingEvents()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshEvents(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(Endpoints.EVENTS)
            if (response.status.isSuccess()) {
                val dtos = response.body<List<EventDto>>()
                database.transaction {
                    for (dto in dtos) {
                        database.appDatabaseQueries.insertOrReplaceEvent(
                            id = dto.id,
                            titleGu = dto.titleGu,
                            titleEn = dto.titleEn,
                            descriptionGu = dto.descriptionGu,
                            descriptionEn = dto.descriptionEn,
                            categoryNameGu = dto.category?.nameGu,
                            categoryNameEn = dto.category?.nameEn,
                            startAt = dto.startAt,
                            endAt = dto.endAt,
                            locationGu = dto.locationGu,
                            locationEn = dto.locationEn,
                            imageUrl = dto.imageUrl,
                            status = dto.status
                        )
                    }
                }
                Napier.i("EventRepository: Refreshed ${dtos.size} events")
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.w("EventRepository: Error refreshing events", e)
            Result.failure(e)
        }
    }

    override suspend fun getEventById(id: String): Event? = withContext(Dispatchers.IO) {
        database.appDatabaseQueries
            .selectEventById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }
}

// ==============================================================================
// 4. BusinessRepository Implementation
// ==============================================================================
class BusinessRepositoryImpl(
    private val database: AppDatabase,
    private val httpClient: HttpClient
) : BusinessRepository {

    override fun getBusinesses(): Flow<List<Business>> {
        return database.appDatabaseQueries
            .selectAllBusinesses()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshBusinesses(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(Endpoints.BUSINESSES)
            if (response.status.isSuccess()) {
                val dtos = response.body<List<BusinessDto>>()
                database.transaction {
                    for (dto in dtos) {
                        database.appDatabaseQueries.insertOrReplaceBusiness(
                            id = dto.id,
                            nameGu = dto.nameGu,
                            nameEn = dto.nameEn,
                            descriptionGu = dto.descriptionGu,
                            descriptionEn = dto.descriptionEn,
                            categoryNameGu = dto.category?.nameGu,
                            categoryNameEn = dto.category?.nameEn,
                            phone = dto.phone,
                            whatsapp = dto.whatsapp,
                            email = dto.email,
                            addressGu = dto.addressGu,
                            addressEn = dto.addressEn,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            logoUrl = dto.logoUrl,
                            isVerified = if (dto.isVerified) 1L else 0L,
                            status = dto.status
                        )
                    }
                }
                Napier.i("BusinessRepository: Refreshed ${dtos.size} businesses")
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.w("BusinessRepository: Error refreshing businesses", e)
            Result.failure(e)
        }
    }

    override suspend fun getBusinessById(id: String): Business? = withContext(Dispatchers.IO) {
        database.appDatabaseQueries
            .selectBusinessById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }
}

// ==============================================================================
// 5. OfferingRepository Implementation (STRICTLY NON-AUCTION)
// ==============================================================================
class OfferingRepositoryImpl(
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val syncEngine: SyncEngine
) : OfferingRepository {

    override fun getOfferings(): Flow<List<Offering>> {
        return database.appDatabaseQueries
            .selectAllOfferings()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshOfferings(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(Endpoints.OFFERINGS)
            if (response.status.isSuccess()) {
                val dtos = response.body<List<OfferingDto>>()
                database.transaction {
                    for (dto in dtos) {
                        database.appDatabaseQueries.insertOrReplaceOffering(
                            id = dto.id,
                            titleGu = dto.titleGu,
                            titleEn = dto.titleEn,
                            descriptionGu = dto.descriptionGu,
                            descriptionEn = dto.descriptionEn,
                            categoryNameGu = dto.category?.nameGu,
                            categoryNameEn = dto.category?.nameEn,
                            amount = dto.amount,
                            locationGu = dto.locationGu,
                            locationEn = dto.locationEn,
                            imageUrl = dto.imageUrl,
                            status = dto.status,
                            startAt = dto.startAt,
                            endAt = dto.endAt,
                            bidCallCount = dto.bidCallCount.toLong(),
                            winningBidderName = dto.winningBidderName,
                            winningBidAmount = dto.winningBidAmount
                        )
                    }
                }
                Napier.i("OfferingRepository: Refreshed ${dtos.size} offerings")
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.w("OfferingRepository: Error refreshing offerings", e)
            Result.failure(e)
        }
    }

    override suspend fun getOfferingById(id: String): Offering? = withContext(Dispatchers.IO) {
        database.appDatabaseQueries
            .selectOfferingById(id)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun submitOffering(offering: Offering): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toString()
            val payload = buildJsonObject {
                put("title_gu", offering.titleGu)
                put("title_en", offering.titleEn)
                offering.descriptionGu?.let { put("description_gu", it) }
                offering.descriptionEn?.let { put("description_en", it) }
                offering.amount?.let { put("amount", it) }
                offering.locationGu?.let { put("location_gu", it) }
                offering.locationEn?.let { put("location_en", it) }
                put("status", "ACTIVE")
            }

            // Write to offline SyncQueue
            database.appDatabaseQueries.insertSyncItem(
                id = offering.id,
                entityType = "OFFERING",
                action = "CREATE",
                payloadJson = payload.toString(),
                status = "PENDING_UPLOAD",
                createdAt = now,
                updatedAt = now
            )

            // Cache locally immediately
            database.appDatabaseQueries.insertOrReplaceOffering(
                id = offering.id,
                titleGu = offering.titleGu,
                titleEn = offering.titleEn,
                descriptionGu = offering.descriptionGu,
                descriptionEn = offering.descriptionEn,
                categoryNameGu = offering.categoryNameGu,
                categoryNameEn = offering.categoryNameEn,
                amount = offering.amount,
                locationGu = offering.locationGu,
                locationEn = offering.locationEn,
                imageUrl = offering.imageUrl,
                status = "ACTIVE",
                startAt = offering.startAt,
                endAt = offering.endAt,
                bidCallCount = offering.bidCallCount.toLong(),
                winningBidderName = offering.winningBidderName,
                winningBidAmount = offering.winningBidAmount
            )

            // Opportunistically trigger background sync
            syncEngine.syncAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("OfferingRepository: Failed to submit offering", e)
            Result.failure(e)
        }
    }

    override suspend fun submitOfferingBid(
        offeringId: String,
        bidderName: String,
        amount: Double,
        bidderPhone: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toString()
            val queueId = "bid_${Clock.System.now().toEpochMilliseconds()}"
            val payload = buildJsonObject {
                put("offering_id", offeringId)
                put("bidder_name", bidderName)
                bidderPhone?.let { put("bidder_phone", it) }
                put("amount", amount)
                put("status", "PENDING")
                put("created_at", now)
                put("updated_at", now)
            }

            database.appDatabaseQueries.insertSyncItem(
                id = queueId,
                entityType = "OFFERING_BID",
                action = "CREATE",
                payloadJson = payload.toString(),
                status = "PENDING_UPLOAD",
                createdAt = now,
                updatedAt = now
            )

            syncEngine.syncAll()
            val isStillQueued = database.appDatabaseQueries
                .countSyncQueueItemsById(queueId)
                .executeAsOne() > 0
            if (!isStillQueued) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Bid is queued but could not be synced. Check your connection and try again."))
            }
        } catch (e: Exception) {
            Napier.e("OfferingRepository: Failed to submit offering bid", e)
            Result.failure(e)
        }
    }

    override suspend fun getOfferingBids(offeringId: String): Result<List<OfferingBid>> = withContext(Dispatchers.IO) {
        try {
            val url = Endpoints.offeringBidsForOffering(offeringId)
            var response = httpClient.get(url)
            if (response.status == HttpStatusCode.Unauthorized && syncEngine.refreshAccessToken() != null) {
                response = httpClient.get(url)
            }
            if (response.status.isSuccess()) {
                val bids = response.body<List<OfferingBid>>()
                Result.success(bids)
            } else {
                val errorBody = response.body<String>()
                Result.failure(Exception("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Napier.w("OfferingRepository: Error fetching offering bids", e)
            Result.failure(e)
        }
    }

}

// ==============================================================================
// 6. ComplaintRepository Implementation
// ==============================================================================
class ComplaintRepositoryImpl(
    private val database: AppDatabase,
    private val httpClient: HttpClient,
    private val syncEngine: SyncEngine
) : ComplaintRepository {

    override fun getMyComplaints(): Flow<List<Complaint>> = flow {
        try {
            val response = httpClient.get(Endpoints.COMPLAINTS)
            if (response.status.isSuccess()) {
                val list = response.body<List<Complaint>>()
                emit(list)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Napier.w("ComplaintRepository: Error or offline fetching complaints", e)
            emit(emptyList())
        }
    }

    override suspend fun submitComplaint(
        title: String,
        description: String,
        categoryId: String?,
        photoUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toString()
            val id = "comp_${Clock.System.now().toEpochMilliseconds()}"
            val payload = buildJsonObject {
                put("id", id)
                put("title", title)
                put("description", description)
                categoryId?.let { put("category_id", it) }
                photoUrl?.let { put("photo_url", it) }
                put("status", "SUBMITTED")
                put("priority", "NORMAL")
            }

            database.appDatabaseQueries.insertSyncItem(
                id = id,
                entityType = "COMPLAINT",
                action = "CREATE",
                payloadJson = payload.toString(),
                status = "PENDING_UPLOAD",
                createdAt = now,
                updatedAt = now
            )

            syncEngine.syncAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("ComplaintRepository: Error enqueuing complaint", e)
            Result.failure(e)
        }
    }
}

// ==============================================================================
// 7. EmergencyRepository Implementation
// ==============================================================================
class EmergencyRepositoryImpl(
    private val database: AppDatabase,
    private val httpClient: HttpClient
) : EmergencyRepository {

    override fun getEmergencyContacts(): Flow<List<EmergencyContact>> {
        return database.appDatabaseQueries
            .selectAllEmergencyContacts()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshEmergencyContacts(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(Endpoints.EMERGENCY_CONTACTS)
            if (response.status.isSuccess()) {
                val dtos = response.body<List<EmergencyContactDto>>()
                database.transaction {
                    for (dto in dtos) {
                        database.appDatabaseQueries.insertOrReplaceEmergencyContact(
                            id = dto.id,
                            nameGu = dto.nameGu,
                            nameEn = dto.nameEn,
                            organization = dto.organization,
                            phone = dto.phone,
                            alternatePhone = dto.alternatePhone,
                            category = dto.category,
                            displayOrder = dto.displayOrder.toLong()
                        )
                    }
                }
                Napier.i("EmergencyRepository: Refreshed ${dtos.size} emergency contacts")
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.w("EmergencyRepository: Error refreshing emergency contacts", e)
            Result.failure(e)
        }
    }
}

// ==============================================================================
// 8. VillageRepository Implementation
// ==============================================================================
class VillageRepositoryImpl(
    private val database: AppDatabase,
    private val httpClient: HttpClient
) : VillageRepository {

    override fun getVillageInfo(): Flow<VillageInfo> {
        return database.appDatabaseQueries
            .getVillageInfo()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() ?: VillageInfo(id = "default_bodla") }
    }

    override suspend fun refreshVillageInfo(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(Endpoints.VILLAGE_SETTINGS)
            if (response.status.isSuccess()) {
                val list = response.body<List<VillageSettingsDto>>()
                val dto = list.firstOrNull()
                if (dto != null) {
                    val now = Clock.System.now().toString()
                    database.appDatabaseQueries.insertOrUpdateVillageInfo(
                        id = dto.id,
                        villageNameGu = dto.villageNameGu,
                        villageNameEn = dto.villageNameEn,
                        district = dto.district,
                        state = dto.state,
                        country = dto.country,
                        descriptionGu = dto.descriptionGu,
                        descriptionEn = dto.descriptionEn,
                        contactPhone = dto.contactPhone,
                        contactEmail = dto.contactEmail,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        updatedAt = dto.updatedAt ?: now
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.status}"))
            }
        } catch (e: Exception) {
            Napier.w("VillageRepository: Error refreshing village info", e)
            Result.failure(e)
        }
    }
}
