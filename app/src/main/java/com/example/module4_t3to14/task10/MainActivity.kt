package com.example.module4_t3to14.task10

import com.example.module4_t3to14.R

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var addressText: TextView
    private lateinit var coordinatesText: TextView
    private lateinit var errorText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var getLocationButton: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task10_activity_main)

        addressText = findViewById(R.id.addressText)
        coordinatesText = findViewById(R.id.coordinatesText)
        errorText = findViewById(R.id.errorText)
        progressBar = findViewById(R.id.progressBar)
        getLocationButton = findViewById(R.id.getLocationButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLocationButton.setOnClickListener {
            checkLocationPermissionAndGetLocation()
        }
    }

    private fun checkLocationPermissionAndGetLocation() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLocation()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) -> {

                showPermissionRationale()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun showPermissionRationale() {
        Toast.makeText(
            this,
            "Для определения вашего адреса необходимо разрешение на геолокацию",
            Toast.LENGTH_LONG
        ).show()

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    getLocation()
                } else {
                    showError("Разрешение на геолокацию не получено")
                }
            }
        }
    }

    private fun getLocation() {
        showLoading(true)

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    coordinatesText.text = String.format("Координаты: %.6f, %.6f", latitude, longitude)

                    getAddressFromLocation(latitude, longitude)
                } else {
                    getCurrentLocation()
                }
            }.addOnFailureListener { e ->
                showError("Ошибка получения локации: ${e.message}")
                showLoading(false)
            }
        } catch (e: SecurityException) {
            showError("Ошибка безопасности: ${e.message}")
            showLoading(false)
        }
    }

    private fun getCurrentLocation() {
        // Для Android 12+ используем getCurrentLocation
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).build()

        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    coordinatesText.text = String.format("Координаты: %.6f, %.6f", latitude, longitude)
                    getAddressFromLocation(latitude, longitude)
                } else {
                    showError("Не удалось получить текущее местоположение")
                    showLoading(false)
                }
            }.addOnFailureListener { e ->
                showError("Ошибка: ${e.message}")
                showLoading(false)
            }
        } catch (e: SecurityException) {
            showError("Ошибка безопасности: ${e.message}")
            showLoading(false)
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())

            // Используем асинхронный метод для Android 11+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, object : android.location.Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val fullAddress = getFullAddress(address)

                            runOnUiThread {
                                addressText.text = fullAddress
                                showLoading(false)
                                errorText.visibility = android.view.View.GONE
                            }
                        } else {
                            runOnUiThread {
                                showError("Адрес не найден для указанных координат")
                                showLoading(false)
                            }
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        runOnUiThread {
                            showError("Ошибка геокодирования: $errorMessage")
                            showLoading(false)
                        }
                    }
                })
            } else {
                // Для старых версий используем синхронный метод
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val fullAddress = getFullAddress(address)

                    addressText.text = fullAddress
                    showLoading(false)
                    errorText.visibility = android.view.View.GONE
                } else {
                    showError("Адрес не найден для указанных координат")
                    showLoading(false)
                }
            }
        } catch (e: IOException) {
            showError("Ошибка сети: ${e.message}")
            showLoading(false)
        } catch (e: Exception) {
            showError("Неизвестная ошибка: ${e.message}")
            showLoading(false)
        }
    }

    private fun getFullAddress(address: android.location.Address): String {
        val parts = mutableListOf<String>()

        // Собираем адрес по частям
        address.getAddressLine(0)?.let { parts.add(it) }

        if (parts.isEmpty()) {
            address.countryName?.let { parts.add(it) }
            address.adminArea?.let { parts.add(it) }
            address.locality?.let { parts.add(it) }
            address.thoroughfare?.let { parts.add(it) }
            address.featureName?.let { parts.add(it) }
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            "Адрес не определен"
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = android.view.View.VISIBLE
            getLocationButton.isEnabled = false
            addressText.text = "Определяем местоположение..."
            coordinatesText.text = ""
            errorText.visibility = android.view.View.GONE
        } else {
            progressBar.visibility = android.view.View.GONE
            getLocationButton.isEnabled = true
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = android.view.View.VISIBLE
        addressText.text = "Не удалось определить адрес"
    }
}