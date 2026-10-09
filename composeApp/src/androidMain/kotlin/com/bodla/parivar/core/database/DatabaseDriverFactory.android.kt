package com.bodla.parivar.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.bodla.parivar.core.AndroidPlatform
import com.bodla.parivar.database.AppDatabase

actual class DatabaseDriverFactory {
    private val context: Context = AndroidPlatform.appContext
        ?: throw IllegalStateException("AndroidPlatform.appContext must be initialized before creating DatabaseDriverFactory")

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, context, "bodla_parivar.db")
    }
}
