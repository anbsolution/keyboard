package com.voicetype.keyboard

data class FeaturePackage(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val version: Int,
    val payload: String,
    val sha256: String
)

data class ThemePalette(
    val id: String,
    val name: String,
    val background: String,
    val keyBackground: String,
    val foreground: String
)
