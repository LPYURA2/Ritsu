package dev.ritsu.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ritsu.android.ui.RitsuApp
import dev.ritsu.android.ui.RitsuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RitsuTheme {
                val vm: RitsuViewModel = viewModel(factory = RitsuViewModel.Factory((application as RitsuApplication).gateway))
                val snapshot by vm.snapshot.collectAsStateWithLifecycle()
                RitsuApp(snapshot, vm)
            }
        }
    }
}
