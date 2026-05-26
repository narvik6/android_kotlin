package com.example.myapplication.presentation.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BleScannerScreen() {
    val context = LocalContext.current
    val viewModel = remember { BleViewModel(context) }

    val devices by viewModel.devices.collectAsState()
    val heartRate by viewModel.heartRate.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted && !isScanning) {
            viewModel.startScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BLE Scanner", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (permissionState.allPermissionsGranted) {
                if (isScanning) viewModel.stopScan() else viewModel.startScan()
            } else {
                permissionState.launchMultiplePermissionRequest()
            }
        }) {
            Text(if (isScanning) "Перезапустить сканирование" else "Начать сканирование")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Heart Rate: ${heartRate?.let { "$it bpm" } ?: "—"}",
            style = MaterialTheme.typography.titleLarge
        )
        Text("Статус: $connectionState", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        if (devices.isEmpty() && !isScanning) {
            Text("Устройства не найдены", style = MaterialTheme.typography.bodyLarge)
        }

        LazyColumn {
            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (context.hasBluetoothConnectPermission()) {
                                viewModel.connect(device)
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(device.displayName(context), style = MaterialTheme.typography.titleMedium)
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (connectionState == "Connected") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.refreshData() }) {
                    Text("Обновить данные")
                }
                Button(onClick = { viewModel.disconnect() }) {
                    Text("Отключиться")
                }
            }
        }
    }
}

private fun BluetoothDevice.displayName(context: Context): String {
    return if (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            name?.takeIf { it.isNotBlank() } ?: "Без имени"
        } catch (_: SecurityException) {
            "Без имени"
        }
    } else {
        "Без имени"
    }
}

private fun Context.hasBluetoothConnectPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
}
