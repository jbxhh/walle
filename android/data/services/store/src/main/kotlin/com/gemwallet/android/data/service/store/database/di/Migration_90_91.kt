package com.gemwallet.android.data.service.store.database.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_90_91 : Migration(90, 91) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `fake_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `walletId` TEXT NOT NULL,
                `assetId` TEXT NOT NULL,
                `fromAddress` TEXT NOT NULL,
                `toAddress` TEXT NOT NULL,
                `amount` TEXT NOT NULL,
                `fee` TEXT NOT NULL DEFAULT '0',
                `memo` TEXT NOT NULL DEFAULT '',
                `timestamp` INTEGER NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'confirmed',
                `type` TEXT NOT NULL DEFAULT 'transfer',
                `swapFromAssetId` TEXT,
                `swapFromAmount` TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `fake_balances` (
                `walletId` TEXT NOT NULL,
                `assetId` TEXT NOT NULL,
                `adjustment` TEXT NOT NULL,
                `isOverride` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`walletId`, `assetId`)
            )
            """.trimIndent()
        )
    }
}
