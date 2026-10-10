package com.rikhotso.studytime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rikhotso.studytime.data.FileStorage
import com.rikhotso.studytime.timer.AndroidAlarmScheduler
import com.rikhotso.studytime.timer.AndroidReminderScheduler
import com.rikhotso.studytime.timer.SAVE_FILE_NAME
import com.rikhotso.studytime.ui.StudytimeApp
import com.rikhotso.studytime.ui.theme.StudytimeTheme
import com.rikhotso.studytime.ui.theme.isDark
import java.io.File

/** The only Activity. It creates the ViewModel and hands everything else to Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val storage = FileStorage(File(filesDir, SAVE_FILE_NAME))
        val alarms = AndroidAlarmScheduler(applicationContext)
        val reminders = AndroidReminderScheduler(applicationContext)
        setContent {
            val viewModel: StudyViewModel = viewModel(factory = StudyViewModel.Factory(storage, alarms, reminders))
            val data by viewModel.state.collectAsStateWithLifecycle()
            val dark = isDark(data.settings.themeMode)
            // The clock and battery icons in the status bar switch between dark and light to stay readable.
            DisposableEffect(dark) {
                val transparent = 0
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            StudytimeTheme(data.settings) {
                StudytimeApp(viewModel)
            }
        }
    }
}
