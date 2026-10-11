package com.routeforge.coredata

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** A list of domain values persisted as JSON in a dedicated SharedPreferences file — the shared
 *  read/write/decode skeleton behind every "list of favorite X" repository in this app, so each
 *  only supplies its own record type, domain<->record mapping, and a tag for diagnostics. A
 *  corrupted or schema-mismatched stored value is discarded (logged, not thrown) rather than
 *  crashing the app at construction time. */
class JsonPrefsListStore<Domain, Record : Any>(
    context: Context,
    prefsName: String,
    private val key: String,
    private val recordListSerializer: KSerializer<List<Record>>,
    private val toRecord: (Domain) -> Record,
    private val toDomain: (Record) -> Domain,
    private val tag: String,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(read())
    val items: StateFlow<List<Domain>> = _items.asStateFlow()

    fun write(items: List<Domain>) {
        _items.value = items
        val records = items.map(toRecord)
        prefs.edit().putString(key, json.encodeToString(recordListSerializer, records)).apply()
    }

    private fun read(): List<Domain> {
        val text = prefs.getString(key, null) ?: return emptyList()
        val records =
            try {
                json.decodeFromString(recordListSerializer, text)
            } catch (e: SerializationException) {
                Log.w(tag, "Discarding corrupted prefs entry for key \"$key\"", e)
                return emptyList()
            }
        return records.map(toDomain)
    }
}
