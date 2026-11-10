package dev.ritsu.android

import android.app.Application
import dev.ritsu.android.data.PreferencesSafetyStateStore
import dev.ritsu.core.CoreGateway
import dev.ritsu.core.DemoCoreGateway

class RitsuApplication : Application() {
    // A single gateway per process, shared by all six screens. No production transport.
    val gateway: CoreGateway by lazy { DemoCoreGateway(PreferencesSafetyStateStore(this)) }
}
