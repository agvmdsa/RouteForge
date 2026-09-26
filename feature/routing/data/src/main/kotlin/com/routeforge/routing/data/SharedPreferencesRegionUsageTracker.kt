package com.routeforge.routing.data

import android.content.Context
import com.routeforge.routing.domain.RegionUsageTracker
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "routeforge_region_usage"
private const val KEY_USAGE_JSON = "usage_json"

@Serializable
data class RegionUsageRecord(
    val lastUsedAtMillis: Long,
    val useCount: Int,
)

class SharedPreferencesRegionUsageTracker(
    private val context: Context,
) : RegionUsageTracker {
    private val json = Json { ignoreUnknownKeys = true }

    override fun recordUsed(
        regionIds: Set<String>,
        atMillis: Long,
    ) {
        val records = readRecords().toMutableMap()
        for (regionId in regionIds) {
            val existing = records[regionId]
            records[regionId] = RegionUsageRecord(lastUsedAtMillis = atMillis, useCount = (existing?.useCount ?: 0) + 1)
        }
        writeRecords(records)
    }

    override fun lastUsedAt(regionId: String): Long? = readRecords()[regionId]?.lastUsedAtMillis

    override fun clear(regionId: String) {
        val records = readRecords().toMutableMap()
        records.remove(regionId)
        writeRecords(records)
    }

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readRecords(): Map<String, RegionUsageRecord> {
        val text = prefs().getString(KEY_USAGE_JSON, null) ?: return emptyMap()
        return json.decodeFromString(text)
    }

    private fun writeRecords(records: Map<String, RegionUsageRecord>) {
        prefs().edit().putString(KEY_USAGE_JSON, json.encodeToString(records)).apply()
    }
}
