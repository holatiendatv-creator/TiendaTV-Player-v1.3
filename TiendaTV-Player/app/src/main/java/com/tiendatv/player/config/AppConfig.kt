package com.tiendatv.player.config

import com.tiendatv.player.BuildConfig

/**
 * Central branding and default server configuration for TiendaTV Player.
 * To pre-configure a default Xtream Codes server for all customers, set either
 * DEFAULT_XTREAM_SERVER in app/build.gradle.kts or override DEFAULT_SERVER_URL below.
 */
object AppConfig {
    const val APP_NAME = "TiendaTV"
    const val APP_VERSION = BuildConfig.VERSION_NAME
    const val USER_AGENT = "TiendaTVPlayer/1.0 (AndroidTV; ExoPlayer)"

    /**
     * Optional pre-configured Xtream server URL (e.g., "http://tv.tiendatv.example:8080").
     * When non-blank, the Xtream login screen pre-fills this server URL automatically.
     */
    val DEFAULT_SERVER_URL: String
        get() = BuildConfig.DEFAULT_XTREAM_SERVER

    /** Batch size for streaming catalog inserts into Room (keeps RAM low on Firestick). */
    const val DB_INSERT_BATCH_SIZE = 500

    /** Page size for fast incremental grid loading across 80,000+ items. */
    const val CATALOG_PAGE_SIZE = 120

    /** Maximum automatic reconnect attempts on dropped live or VOD streams. */
    const val MAX_PLAYER_RETRIES = 5

    /** Keywords used to auto-flag adult/restricted categories for Parental Control PIN. */
    val ADULT_CATEGORY_KEYWORDS = listOf(
        "xxx", "adult", "adulto", "adultos", "+18", "18+", "porn", "erotic", "onlyfans"
    )
}
