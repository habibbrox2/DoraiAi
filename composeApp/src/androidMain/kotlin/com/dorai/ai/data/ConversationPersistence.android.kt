package com.dorai.ai.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.dorai.ai.db.DoraIDatabase
import org.koin.java.KoinJavaComponent.inject

actual fun createConversationSqlDriver(): SqlDriver? {
    val context: Context by inject(Context::class.java)
    return AndroidSqliteDriver(DoraIDatabase.Schema, context, "dorai.db")
}
