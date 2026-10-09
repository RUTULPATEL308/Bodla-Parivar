package com.bodla.parivar.core.di

import com.bodla.parivar.core.database.DatabaseDriverFactory
import com.bodla.parivar.core.network.KtorClientFactory
import com.bodla.parivar.core.security.TokenStorage
import com.bodla.parivar.core.sync.SyncEngine
import com.bodla.parivar.data.repository.*
import com.bodla.parivar.database.AppDatabase
import com.bodla.parivar.domain.repository.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule: Module = module {
    single { DatabaseDriverFactory() }
    single { AppDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { TokenStorage() }
    single { KtorClientFactory.create { get<TokenStorage>().getToken() } }
    single { SyncEngine(get(), get(), get()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<NoticeRepository> { NoticeRepositoryImpl(get(), get()) }
    single<EventRepository> { EventRepositoryImpl(get(), get()) }
    single<BusinessRepository> { BusinessRepositoryImpl(get(), get()) }
    single<OfferingRepository> { OfferingRepositoryImpl(get(), get(), get()) }
    single<ComplaintRepository> { ComplaintRepositoryImpl(get(), get(), get()) }
    single<EmergencyRepository> { EmergencyRepositoryImpl(get(), get()) }
    single<VillageRepository> { VillageRepositoryImpl(get(), get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(appModule)
    }

/**
 * Direct AppContainer for Compose Multiplatform components
 */
object AppContainer {
    val databaseDriverFactory by lazy { DatabaseDriverFactory() }
    val database by lazy { AppDatabase(databaseDriverFactory.createDriver()) }
    val tokenStorage by lazy { TokenStorage() }
    val networkObserver by lazy { com.bodla.parivar.core.network.NetworkObserver() }
    val httpClient by lazy { KtorClientFactory.create { tokenStorage.getToken() } }
    val syncEngine by lazy { SyncEngine(database, httpClient, tokenStorage) }

    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(httpClient, tokenStorage) }
    val noticeRepository: NoticeRepository by lazy { NoticeRepositoryImpl(database, httpClient) }
    val eventRepository: EventRepository by lazy { EventRepositoryImpl(database, httpClient) }
    val businessRepository: BusinessRepository by lazy { BusinessRepositoryImpl(database, httpClient) }
    val offeringRepository: OfferingRepository by lazy { OfferingRepositoryImpl(database, httpClient, syncEngine) }
    val complaintRepository: ComplaintRepository by lazy { ComplaintRepositoryImpl(database, httpClient, syncEngine) }
    val emergencyRepository: EmergencyRepository by lazy { EmergencyRepositoryImpl(database, httpClient) }
    val villageRepository: VillageRepository by lazy { VillageRepositoryImpl(database, httpClient) }
}
