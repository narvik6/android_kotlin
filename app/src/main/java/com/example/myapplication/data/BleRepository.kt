package com.example.myapplication.data


import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleRepository(context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices

    private val _bloodPressure = MutableStateFlow<String?>(null)  // "120/80 mmHg, pulse 72"
    val bloodPressure: StateFlow<String?> = _bloodPressure

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private var currentGatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!device.name.isNullOrBlank()) {
                println("Найдено устройство: ${device.name} - ${device.address}")

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

        _devices.value = emptyList()  // очищаем список

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        _isScanning.value = true
    }

    fun stopScan() {
        adapter.bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        currentGatt = device.connectGatt(null, false, gattCallback)
        _connectionState.value = "Connecting"
    }

    fun disconnect() {
        currentGatt?.disconnect()
        currentGatt?.close()
        currentGatt = null
        _connectionState.value = "Disconnected"
        _bloodPressure.value = null
    }

    fun refreshData() {
        currentGatt?.let { gatt ->
            val service = gatt.getService(BLOOD_PRESSURE_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_UUID)
            characteristic?.let {
                val success = gatt.readCharacteristic(it)
                println("Ручное обновление данных: $success")
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = "Connected"
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = "Disconnected"
                _bloodPressure.value = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            println("onServicesDiscovered: status = $status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(BLOOD_PRESSURE_SERVICE_UUID)
                if (service == null) {
                    println("Blood Pressure Service НЕ НАЙДЕН!")
                    return
                }

                val characteristic = service.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_UUID)
                if (characteristic == null) {
                    println("Blood Pressure Measurement НЕ НАЙДЕНА!")
                    return
                }

                // Включаем уведомления
                gatt.setCharacteristicNotification(characteristic, true)

                val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    println("Запись CCC-дескриптора отправлена")
                }

                scope.launch {
                    delay(500)
                    val readSuccess = gatt.readCharacteristic(characteristic)
                    println("Первое чтение после задержки: $readSuccess")

                    // Цикл чтения
                    while (true) {
                        delay(5000)
                        val cycleSuccess = gatt.readCharacteristic(characteristic)
                        println("Циклическое чтение: $cycleSuccess")
                    }
                }
            } else {
                println("Обнаружение сервисов провалилось: status = $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            println("onCharacteristicRead: status = $status, uuid = ${characteristic.uuid}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value
                println("Значение получено (hex): ${value?.joinToString(" ") { "%02x".format(it) } ?: "null"}")
                parseBloodPressureData(characteristic)
            } else {
                println("Ошибка чтения: status = $status")
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

        private fun parseBloodPressureData(characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: run {
                println("Значение характеристики пустое (null)")
                return
            }

            if (value.isEmpty()) {
                println("Значение характеристики пустое (0 байт)")
                return
            }

            println("Получено значение (hex): ${value.joinToString(" ") { "%02x".format(it) }}")
            println("Длина: ${value.size} байт")

            if (value.size < 7) {
                println("Слишком короткое значение для Blood Pressure Measurement")
                return
            }

            val flags = value[0].toInt() and 0xFF
            println("Flags: $flags")

            var offset = 1

            val systolic = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
            offset += 2

            val diastolic = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
            offset += 2

            val mean = ((value[offset + 1].toInt() and 0xFF) shl 8) or (value[offset].toInt() and 0xFF)
            offset += 2

            val bpText = "$systolic/$diastolic mmHg"
            _bloodPressure.value = bpText

            println("Давление прочитано: $bpText")
        }
    }

    companion object {
        private val BLOOD_PRESSURE_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
        private val BLOOD_PRESSURE_MEASUREMENT_UUID = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}