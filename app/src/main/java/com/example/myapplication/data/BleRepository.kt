package com.example.myapplication.data


import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleRepository(context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val appContext = context.applicationContext

    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private var currentGatt: BluetoothGatt? = null
    private var readJob: Job? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = getDeviceName(device)
            if (!deviceName.isNullOrBlank()) {
                println("Найдено устройство: $deviceName - ${device.address}")

                val current = _devices.value.toMutableList()
                if (current.none { it.address == device.address }) {
                    current.add(device)
                    _devices.value = current.toList()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            println("Сканирование провалилось: errorCode = $errorCode")
        }
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    fun startScan() {
        if (!adapter.isEnabled) return
        val scanner = adapter.bluetoothLeScanner ?: return
        if (!hasBluetoothScanPermission()) return

        _devices.value = emptyList()  // очищаем список

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            _isScanning.value = true
        } catch (_: SecurityException) {
            _isScanning.value = false
        }
    }

    fun stopScan() {
        if (hasBluetoothScanPermission()) {
            try {
                adapter.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: SecurityException) {
                // Permission can be revoked while scanning.
            }
        }
        _isScanning.value = false
    }

    fun connect(device: BluetoothDevice) {
        if (!hasBluetoothConnectPermission()) return
        stopScan()
        try {
            currentGatt = device.connectGatt(appContext, false, gattCallback)
            _connectionState.value = "Connecting"
        } catch (_: SecurityException) {
            _connectionState.value = "Disconnected"
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        if (hasBluetoothConnectPermission()) {
            try {
                currentGatt?.disconnect()
                currentGatt?.close()
            } catch (_: SecurityException) {
                // Permission can be revoked while connected.
            }
        }
        currentGatt = null
        _connectionState.value = "Disconnected"
        _heartRate.value = null
    }

    fun refreshData() {
        if (!hasBluetoothConnectPermission()) return
        currentGatt?.let { gatt ->
            val service = gatt.getService(HEART_RATE_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            characteristic?.let {
                try {
                    val success = gatt.readCharacteristic(it)
                    println("Ручное обновление данных: $success")
                } catch (_: SecurityException) {
                    println("Нет разрешения на чтение характеристики")
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = "Connected"
                if (!hasBluetoothConnectPermission()) {
                    _connectionState.value = "Disconnected"
                    return
                }
                try {
                    gatt.discoverServices()
                } catch (_: SecurityException) {
                    _connectionState.value = "Disconnected"
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                readJob?.cancel()
                readJob = null
                _connectionState.value = "Disconnected"
                _heartRate.value = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            println("onServicesDiscovered: status = $status")

            if (!hasBluetoothConnectPermission()) {
                println("Нет разрешения на работу с GATT")
                return
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(HEART_RATE_SERVICE_UUID)
                if (service == null) {
                    println("Heart Rate Service НЕ НАЙДЕН!")
                    return
                }

                val characteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                if (characteristic == null) {
                    println("Heart Rate Measurement НЕ НАЙДЕНА!")
                    return
                }

                enableNotifications(gatt, characteristic)

                readJob?.cancel()
                readJob = scope.launch {
                    delay(500)
                    if (!hasBluetoothConnectPermission()) return@launch
                    try {
                        val readSuccess = gatt.readCharacteristic(characteristic)
                        println("Первое чтение после задержки: $readSuccess")
                    } catch (_: SecurityException) {
                        println("Нет разрешения на первое чтение характеристики")
                        return@launch
                    }

                    while (true) {
                        delay(5000)
                        if (!hasBluetoothConnectPermission()) return@launch
                        try {
                            val cycleSuccess = gatt.readCharacteristic(characteristic)
                            println("Циклическое чтение: $cycleSuccess")
                        } catch (_: SecurityException) {
                            println("Нет разрешения на циклическое чтение характеристики")
                            return@launch
                        }
                    }
                }
            } else {
                println("Обнаружение сервисов провалилось: status = $status")
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            println("onCharacteristicRead: status = $status, uuid = ${characteristic.uuid}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value
                println("Значение получено (hex): ${value?.joinToString(" ") { "%02x".format(it) } ?: "null"}")
                if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                    parseHeartRateData(value)
                }
            } else {
                println("Ошибка чтения: status = $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            println("onCharacteristicRead: status = $status, uuid = ${characteristic.uuid}")
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                parseHeartRateData(value)
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                println("Ошибка чтения: status = $status")
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value
            println("onCharacteristicChanged: uuid = ${characteristic.uuid}")
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                parseHeartRateData(value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            println("onCharacteristicChanged: uuid = ${characteristic.uuid}")
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                parseHeartRateData(value)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            println("onDescriptorWrite: status = $status, uuid = ${descriptor.uuid}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("Дескриптор CCC успешно записан — уведомления должны работать")
            } else {
                println("Ошибка записи дескриптора: $status")
            }
        }

        private fun parseHeartRateData(value: ByteArray?) {
            val bytes = value ?: run {
                println("Значение характеристики пустое (null)")
                return
            }

            if (bytes.isEmpty()) {
                println("Значение характеристики пустое (0 байт)")
                return
            }

            println("Получено значение (hex): ${bytes.joinToString(" ") { "%02x".format(it) }}")
            println("Длина: ${bytes.size} байт")

            val flags = bytes[0].toInt() and 0xFF
            println("Flags: $flags")

            val isHeartRateUint16 = (flags and HEART_RATE_VALUE_FORMAT_UINT16_FLAG) != 0

            val heartRate = if (isHeartRateUint16) {
                if (bytes.size < 3) {
                    println("Слишком короткое значение для Heart Rate Measurement UINT16")
                    return
                }
                ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
            } else {
                if (bytes.size < 2) {
                    println("Слишком короткое значение для Heart Rate Measurement UINT8")
                    return
                }
                bytes[1].toInt() and 0xFF
            }

            _heartRate.value = heartRate
            println("Пульс прочитан: $heartRate bpm")
        }

        private fun enableNotifications(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (!hasBluetoothConnectPermission()) {
                println("Нет разрешения на включение уведомлений")
                return
            }

            val notificationEnabled = try {
                gatt.setCharacteristicNotification(characteristic, true)
            } catch (_: SecurityException) {
                println("Нет разрешения на локальное включение уведомлений")
                return
            }
            println("Локальное включение уведомлений: $notificationEnabled")

            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (descriptor == null) {
                println("CCC-дескриптор НЕ НАЙДЕН!")
                return
            }

            val writeStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    gatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    ) == BluetoothStatusCodes.SUCCESS
                } catch (_: SecurityException) {
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                try {
                    gatt.writeDescriptor(descriptor)
                } catch (_: SecurityException) {
                    false
                }
            }

            println("Запись CCC-дескриптора отправлена: $writeStarted")
        }
    }

    private fun getDeviceName(device: BluetoothDevice): String? {
        if (!hasBluetoothConnectPermission()) return null
        return try {
            device.name
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasBluetoothScanPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val HEART_RATE_VALUE_FORMAT_UINT16_FLAG = 0x01
        private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
