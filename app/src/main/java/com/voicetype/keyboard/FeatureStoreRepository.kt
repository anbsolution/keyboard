package com.voicetype.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local-first package manager foundation.
 * It installs data-only theme packages; it never loads or executes downloaded code.
 */
class FeatureStoreRepository(context: Context) {
    private val prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
    private val packageDir = File(context.filesDir, "feature_packages").apply { mkdirs() }

    fun catalog(): List<FeaturePackage> = builtInCatalog()

    fun installedIds(): Set<String> =
        prefs.getStringSet(KEY_INSTALLED, emptySet())?.toSet() ?: emptySet()

    fun isInstalled(id: String): Boolean = id in installedIds()

    fun install(pkg: FeaturePackage): Boolean {
        if (!PackageIntegrity.isValidPackageId(pkg.id)) return false
        val bytes = pkg.payload.toByteArray(Charsets.UTF_8)
        if (!PackageIntegrity.verify(bytes, pkg.sha256)) return false
        val json = try { JSONObject(pkg.payload) } catch (_: Exception) { return false }
        if (json.optString("type") != "theme") return false
        val palette = try {
            ThemePalette(
                id = pkg.id,
                name = json.getString("name"),
                background = color(json.getString("background")),
                keyBackground = color(json.getString("keyBackground")),
                foreground = color(json.getString("foreground"))
            )
        } catch (_: Exception) { return false }

        val target = File(packageDir, "${pkg.id}.json")
        val temp = File(packageDir, "${pkg.id}.tmp")
        return try {
            temp.writeText(json.toString(), Charsets.UTF_8)
            if (!temp.renameTo(target)) {
                temp.delete()
                false
            } else {
                prefs.edit().putStringSet(KEY_INSTALLED, installedIds() + pkg.id).apply()
                true
            }
        } catch (_: Exception) {
            temp.delete()
            false
        }
    }

    fun remove(id: String): Boolean {
        if (!PackageIntegrity.isValidPackageId(id)) return false
        val target = File(packageDir, "$id.json")
        val deleted = !target.exists() || target.delete()
        if (deleted) prefs.edit().putStringSet(KEY_INSTALLED, installedIds() - id).apply()
        return deleted
    }

    fun applyTheme(id: String): Boolean {
        if (!isInstalled(id)) return false
        val file = File(packageDir, "$id.json")
        val json = try { JSONObject(file.readText(Charsets.UTF_8)) } catch (_: Exception) { return false }
        val palette = try {
            ThemePalette(id, json.getString("name"), color(json.getString("background")),
                color(json.getString("keyBackground")), color(json.getString("foreground")))
        } catch (_: Exception) { return false }
        prefs.edit()
            .putString("custom_theme_id", palette.id)
            .putString("custom_theme_name", palette.name)
            .putString("custom_theme_background", palette.background)
            .putString("custom_theme_key_background", palette.keyBackground)
            .putString("custom_theme_foreground", palette.foreground)
            .putString("theme", "Custom")
            .apply()
        return true
    }

    private fun color(value: String): String {
        require(Regex("^#[0-9A-Fa-f]{6}$").matches(value))
        return value.uppercase()
    }

    private fun builtInCatalog(): List<FeaturePackage> = listOf(
        theme("theme.royal-blue", "Royal Blue", "#102B50", "#FFFFFF", "#173B70"),
        theme("theme.royal-gold", "Royal Gold", "#30230B", "#F5D88A", "#FFFFFF"),
        theme("theme.mint-light", "Mint Light", "#E8F7F0", "#FFFFFF", "#174B38"),
        theme("theme.graphite", "Graphite", "#252A32", "#414955", "#FFFFFF"),
        theme("theme.violet", "Violet", "#302044", "#60417C", "#FFFFFF")
    )

    private fun theme(id: String, name: String, bg: String, key: String, fg: String): FeaturePackage {
        val payload = JSONObject()
            .put("type", "theme").put("name", name).put("background", bg)
            .put("keyBackground", key).put("foreground", fg).toString()
        val bytes = payload.toByteArray(Charsets.UTF_8)
        return FeaturePackage(id, name, "Theme", "Install this color theme offline", 1, payload, PackageIntegrity.sha256(bytes))
    }

    companion object { private const val KEY_INSTALLED = "feature_store_installed_ids" }
}
