package com.bodla.parivar.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.bodla.parivar.core.AndroidPlatform
import com.bodla.parivar.database.AppDatabase

actual class DatabaseDriverFactory {
    private val context: Context = AndroidPlatform.appContext
        ?: throw IllegalStateException("AndroidPlatform.appContext must be initialized before creating DatabaseDriverFactory")

    actual fun createDriver(): SqlDriver {
        val dbFile = context.getDatabasePath("bodla_parivar.db")
        if (dbFile.exists()) {
            try {
                val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                var hasPublishedById = false
                try {
                    val cursor = db.rawQuery("PRAGMA table_info(CachedNotice);", null)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        if (name == "publishedById") {
                            hasPublishedById = true
                            break
                        }
                    }
                    cursor.close()
                } catch (_: Exception) {}
                db.close()
                if (!hasPublishedById) {
                    context.deleteDatabase("bodla_parivar.db")
                }
            } catch (_: Exception) {
                context.deleteDatabase("bodla_parivar.db")
            }
        }
        return AndroidSqliteDriver(AppDatabase.Schema, context, "bodla_parivar.db")
    }
}
