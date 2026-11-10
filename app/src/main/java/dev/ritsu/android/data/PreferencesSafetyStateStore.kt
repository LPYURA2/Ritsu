package dev.ritsu.android.data

import android.content.Context
import dev.ritsu.core.*
import org.json.JSONArray
import org.json.JSONObject

/** One synchronous atomic preference transaction. A failed write must reach the core. */
class PreferencesSafetyStateStore(context: Context) : SafetyStateStore {
    private val preferences = context.getSharedPreferences("demo_safety", Context.MODE_PRIVATE)

    override fun load(): SafetyState {
        val raw = preferences.getString("state", null) ?: return SafetyState()
        val json = JSONObject(raw)
        check(json.getInt("version") == 1) { "Unsupported safety state" }
        val grants = json.getJSONArray("grants")
        val events = json.getJSONArray("events")
        return SafetyState(
            mode = CoreMode.valueOf(json.getString("mode")),
            grants = List(grants.length()) { index ->
                val grant = grants.getJSONObject(index)
                PermissionGrant(
                    Capability.valueOf(grant.getString("capability")),
                    grant.getLong("expiresAtMillis"),
                    grant.getInt("remainingActions"),
                )
            },
            events = List(events.length()) { index ->
                val event = events.getJSONObject(index)
                AuditEvent(
                    event.getString("id"), event.getLong("timestampMillis"),
                    AuditKind.valueOf(event.getString("kind")), event.getString("title"),
                    event.getString("detail"), event.getBoolean("allowed"),
                )
            },
        )
    }

    // KTX edit(commit=true) discards the Boolean result needed for fail-closed handling.
    @android.annotation.SuppressLint("UseKtx")
    override fun save(state: SafetyState) {
        val json = JSONObject().put("version", 1).put("mode", state.mode.name)
            .put("grants", JSONArray().apply {
                state.grants.forEach { grant ->
                    put(JSONObject().put("capability", grant.capability.name)
                        .put("expiresAtMillis", grant.expiresAtMillis)
                        .put("remainingActions", grant.remainingActions))
                }
            }).put("events", JSONArray().apply {
                state.events.forEach { event ->
                    put(JSONObject().put("id", event.id).put("timestampMillis", event.timestampMillis)
                        .put("kind", event.kind.name).put("title", event.title)
                        .put("detail", event.detail).put("allowed", event.allowed))
                }
            })
        check(preferences.edit().putString("state", json.toString()).commit()) {
            "Safety state could not be persisted"
        }
    }
}
