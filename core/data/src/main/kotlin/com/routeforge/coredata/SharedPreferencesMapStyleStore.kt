package com.routeforge.coredata

import android.content.Context
import com.routeforge.coredomain.MapStyleStore
import com.routeforge.coredomain.model.MapStyle
import kotlinx.coroutines.flow.MutableStateFlow

private const val PREFS_NAME = "routeforge_map_style"
private const val KEY_MAP_STYLE = "map_style"

internal class SharedPreferencesMapStyleStore(
    context: Context,
) : MapStyleStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mapStyle = MutableStateFlow(readMapStyle())

    override fun observeMapStyle() = mapStyle

    override fun setMapStyle(mapStyle: MapStyle) {
        prefs.edit().putString(KEY_MAP_STYLE, mapStyle.name).apply()
        this.mapStyle.value = mapStyle
    }

    /** Falls back to [MapStyle.DEFAULT] for a missing or no-longer-recognized stored name (e.g. an
     *  enum constant renamed/removed in a later version), rather than crashing on startup. */
    private fun readMapStyle(): MapStyle {
        val stored = prefs.getString(KEY_MAP_STYLE, null) ?: return MapStyle.DEFAULT
        return runCatching { MapStyle.valueOf(stored) }.getOrDefault(MapStyle.DEFAULT)
    }
}
