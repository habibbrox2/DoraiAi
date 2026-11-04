package com.dorai.ai.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.dorai.ai.db.DoraIDatabase
import com.dorai.ai.getAppFilesDirectory
import java.io.File

actual fun createConversationSqlDriver(): SqlDriver? {
    val file = File(getAppFilesDirectory(), "dorai.db")
    file.parentFile?.mkdirs()
    return JdbcSqliteDriver(url = "jdbc:sqlite:${file.absolutePath}", schema = DoraIDatabase.Schema)
}
