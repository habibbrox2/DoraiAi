package com.dorai.ai.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.dorai.ai.db.DoraIDatabase

actual fun createConversationSqlDriver(): SqlDriver? = NativeSqliteDriver(DoraIDatabase.Schema, "dorai.db")
