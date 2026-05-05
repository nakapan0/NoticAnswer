package com.example.noticanswer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }

    var autoEnabled by remember { mutableStateOf(false) }
    var startHourText by remember { mutableStateOf("10") }
    var endHourText by remember { mutableStateOf("22") }
    var countText by remember { mutableStateOf("3") }
    var minIntervalText by remember { mutableStateOf("30") }

    var statusMessage by remember { mutableStateOf("") }

    var quietHoursEnabled by remember { mutableStateOf(false) }
    var quietStartHourText by remember { mutableStateOf("22") }
    var quietEndHourText by remember { mutableStateOf("6") }

    LaunchedEffect(Unit) {
        val settings = SettingsRepository.getSettings(context)

        autoEnabled = settings.autoEnabled
        startHourText = settings.startHour.toString()
        endHourText = settings.endHour.toString()
        countText = settings.count.toString()
        minIntervalText = settings.minIntervalMinutes.toString()
        quietHoursEnabled = settings.quietHoursEnabled
        quietStartHourText = settings.quietStartHour.toString()
        quietEndHourText = settings.quietEndHour.toString()

        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        ExplorerHeader(
            title = "設定",
            onBack = onBack,
            onAdd = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!loaded) {
            Text("読み込み中...")
            return@Column
        }

        Text("自動通知")

        Spacer(modifier = Modifier.height(8.dp))

        Switch(
            checked = autoEnabled,
            onCheckedChange = { checked ->
                autoEnabled = checked
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = startHourText,
            onValueChange = { startHourText = it },
            label = { Text("開始時刻（0〜23）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = endHourText,
            onValueChange = { endHourText = it },
            label = { Text("終了時刻（0〜23）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = countText,
            onValueChange = { countText = it },
            label = { Text("1日の通知回数（0〜10）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = minIntervalText,
            onValueChange = { minIntervalText = it },
            label = { Text("最低間隔（分）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("通知しない時間帯")

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = quietHoursEnabled,
                onCheckedChange = { checked ->
                    quietHoursEnabled = checked
                }
            )

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            Text(if (quietHoursEnabled) "ON" else "OFF")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = quietStartHourText,
            onValueChange = { quietStartHourText = it },
            label = { Text("通知しない開始時刻（0〜23）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = quietEndHourText,
            onValueChange = { quietEndHourText = it },
            label = { Text("通知しない終了時刻（0〜23）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = {
                val settings = parseNotificationSettingsOrNull(
                    startHourText = startHourText,
                    endHourText = endHourText,
                    countText = countText,
                    minIntervalText = minIntervalText,
                    autoEnabled = autoEnabled,
                    quietHoursEnabled = quietHoursEnabled,
                    quietStartHourText = quietStartHourText,
                    quietEndHourText = quietEndHourText
                )

                if (settings == null) {
                    statusMessage = "設定値が不正です"
                    return@Button
                }

                coroutineScope.launch {
                    SettingsRepository.saveSettings(
                        context = context,
                        settings = settings
                    )

                    if (settings.autoEnabled) {
                        startDailyAutoSchedule(
                            context = context,
                            settings = settings
                        )
                        statusMessage = "自動通知をONにして保存しました"
                    } else {
                        stopDailyAutoSchedule(context)
                        statusMessage = "自動通知をOFFにして保存しました"
                    }
                }
            }
        ) {
            Text("設定を保存")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = {
                coroutineScope.launch {
                    try {
                        showNotification(context)
                        statusMessage = "通知を表示しました"
                    } catch (e: Exception) {
                        statusMessage = "出題可能な問題がありません"
                    }
                }
            }
        ) {
            Text("今すぐ通知")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(statusMessage)
    }
}