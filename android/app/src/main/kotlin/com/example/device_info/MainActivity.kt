package com.example.device_info

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.os.AsyncTask
import android.content.pm.ApplicationInfo



class MainActivity: FlutterActivity() {
    private val CHANNEL = "device_info"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getDeviceInfo" -> {
                    val deviceId = fetchDeviceId()
                    result.success(deviceId)
                }
                "getRamInfo" -> {
                    val ramInfo = fetchRamInfo()
                    result.success(ramInfo)
                }
                "getCameraInfo" -> {
                    val cameraInfo = fetchCameraInfo()
                    result.success(cameraInfo)
                }
                "getManufacturerInfo" -> {
                    val manufacturerInfo = DeviceManufactureInfoHelper.fetchManufacturerInfo(contentResolver)
                    result.success(manufacturerInfo)
                }
                "versionInfo" -> {
                    val versionInfo = fetchBuildVersionInfo()
                    result.success(versionInfo)
                }
                "getBatteryInfo" -> {
                    val batteryInfo = fetchBatteryInfo(this)
                    result.success(batteryInfo)
                }
                "getDeviceModeInfo" -> {
                    val deviceSettingModeInfo = DeviceSettingsHelper.fetchDeviceSettingInfo(contentResolver)
                    result.success(deviceSettingModeInfo)
                }
                "getSystemInfo" -> {
                    val systemInfo =  SystemInfoHelper.fetchSystemInfo(contentResolver)
                    result.success(systemInfo)
                }
                "getPrivateIPAddress" -> {
                    val privateIP = getPrivateIPAddress()
                    result.success(privateIP)
                }
                "getPublicIPAddress" -> {
                    GetPublicIPAddressTask(result).execute()
                }
                "getInstalledApps" -> {
                    val apps = getInstalledApps()
                    result.success(apps)
                }
                else -> result.notImplemented()
            }
        }
    }


    // This is for getting all Installed app list

    private fun getInstalledApps(): List<String> {
        val pm: PackageManager = packageManager
        val packages: List<ApplicationInfo> = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val installedApps = mutableListOf<String>()

        for (packageInfo in packages) {
            if (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                installedApps.add(packageInfo.packageName)
            }
        }
        return installedApps
    }


    // This is for geting Private IP of user
    private fun getPrivateIPAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress) {
                        val ipAddress = inetAddress.hostAddress
                        if (inetAddress.isSiteLocalAddress) {
                            return ipAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // This is for geting Public IP

    private class GetPublicIPAddressTask(private val result: MethodChannel.Result) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg params: Void?): String? {
            return try {
                val url = URL("https://api.ipify.org?format=text")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    response
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: String?) {
            this.result.success(result)
        }
    }


    private fun fetchDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }
    

    private fun fetchRamInfo(): String {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024 * 1024)
        val availRam = memInfo.availMem / (1024 * 1024)
        return "Total RAM: ${totalRam}MB, Available RAM: ${availRam}MB"
    }

    private fun fetchCameraInfo(): String {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList
        val cameras = cameraIds.joinToString(", ") { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "Front Camera" else "Back Camera"
        }
        return "Cameras: $cameras"
    }

    // This is for device build information
    private fun fetchBuildVersionInfo(): String {
        val versionInfo = mapOf(
            "BASE_OS" to Build.VERSION.BASE_OS,
            "CODENAME" to Build.VERSION.CODENAME,
            "INCREMENTAL" to Build.VERSION.INCREMENTAL,
            "PREVIEW_SDK_INT" to Build.VERSION.PREVIEW_SDK_INT.toString(),
            "RELEASE" to Build.VERSION.RELEASE,
            "SDK" to Build.VERSION.SDK,
            "SDK_INT" to Build.VERSION.SDK_INT.toString(),
            "SECURITY_PATCH" to Build.VERSION.SECURITY_PATCH
        )
        return JSONObject(versionInfo).toString()
    }

    
}
