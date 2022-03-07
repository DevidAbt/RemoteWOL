package com.example.remotewol

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*


class WolService : Service() {
    private val TAG = WolService::class.qualifiedName
    private val notificationId = 1
    private val notificationChannelId = "1"
    private lateinit var api: Api
    private var lastValue: String? = null

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        api = RetrofitHelper.getInstance().create(Api::class.java)

        Timer().scheduleAtFixedRate(object :TimerTask() {
            override fun run() {
                    Log.i(TAG, "request")
                    val result = api.getValue()
                    result.enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            if (response.code() == 200 && response.body() != null) {
                                val value = response.body().toString()
                                Log.i(TAG, "value: $value")
                                Toast.makeText(applicationContext, value, Toast.LENGTH_LONG).show()


                                if (lastValue != null && lastValue != value) {
                                    Toast.makeText(applicationContext, "sending WOL", Toast.LENGTH_LONG).show()
                                    val thread = Thread {
                                        try {
                                            sendWol()
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    thread.start()
                                }

                                lastValue = value
                            }
                            else {
                                Log.w(TAG, "unsuccessful: ${response.body().toString()}")
                            }
                        }
                        override fun onFailure(call: Call<String>, t: Throwable) {
                            Log.w(TAG, "failure: $t")
                        }
                    })
                }
        }, 3000, 10000)

        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        startForeground(
            notificationId, NotificationCompat.Builder(
                this,
                notificationChannelId
            )
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    @Throws(IllegalArgumentException::class)
    private fun getMacBytes(macStr: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macStr.split(":").toTypedArray()
        require(hex.size == 6) { "Invalid MAC address." }
        try {
            for (i in 0..5) {
                bytes[i] = hex[i].toInt(16).toByte()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex digit in MAC address.")
        }
        return bytes
    }

    private fun sendWol() {
        val ipStr: String = "192.168.0.255"
        val macStr: String = "f0:79:59:5a:81:b6"

        try {
            val macBytes: ByteArray = getMacBytes(macStr)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0..5) {
                bytes[i] = 0xff.toByte()
            }
            var i = 6
            while (i < bytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                i += macBytes.size
            }
            val address: InetAddress = InetAddress.getByName(ipStr)
            val packet = DatagramPacket(bytes, bytes.size, address, 9)
            val socket = DatagramSocket()
            socket.send(packet)
            socket.close()
            println("Wake-on-LAN packet sent.")
        } catch (e: Exception) {
            println("Failed to send Wake-on-LAN packet: $e")
            System.exit(1)
        }
    }
}