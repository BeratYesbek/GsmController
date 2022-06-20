package com.example.gsmcontroller

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.ktor.http.*
import io.ktor.websocket.*
import okhttp3.*
import okio.ByteString
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mqttClient: MqttAndroidClient
    private var status = false
    private var engineStatus = false
    private var closeStatus = false
    private lateinit var buttonOn: Button
    private lateinit var buttonClose: Button
    private lateinit var buttonRunEngine: Button
    private lateinit var buttonStopEngine: Button
    private lateinit var buttonUp : Button
    private lateinit var buttonDown : Button
    private var successCount = 0
    private var engineOn =  0
    private var engineOff = 0
    private var state = 0;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonOn = findViewById(R.id.btnOn)
        buttonClose = findViewById(R.id.btnClose)
        buttonRunEngine = findViewById(R.id.btnRunEngine)
        buttonStopEngine = findViewById(R.id.btnStopEngine)
        buttonUp = findViewById(R.id.btnOutPutUp);
        buttonDown = findViewById(R.id.btnOutPutDown);

        buttonUp.setOnClickListener {
            engine("OUTPUT1","on")
        }

        buttonDown.setOnClickListener {
            engine("OUTPUT1","off")
        }

        buttonRunEngine.setOnClickListener {
            engineStatus = false
            engineOn = 1
            runEngine("OUTPUT2","on")

        }
        buttonStopEngine.setOnClickListener {
            engineStatus = false
            engineOff = 1
            runEngine("OUTPUT2","off")

        }


        buttonOn.setOnClickListener {

            on("LED", "on")
            state = 0
            successCount = 1
            status = false
            startTimeCounter()

        }
        buttonClose.setOnClickListener {
            close("LED", "off")
            successCount = 1
            state = 1
            closeStatus = false

            closeTimeCounter()
        }
        connect(this)


    }


    private fun runEngine(topic: String,msg: String) {

        val timer = object : CountDownTimer(2000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                if (engineStatus)
                    return
                engine(topic,msg)
                runEngine(topic,msg)
            }
        }
        timer.start()
    }
    private fun closeTimeCounter() {
        val timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                if (closeStatus != true) {
                    close("LED", "off")
                    closeTimeCounter()
                } else {
                    buttonOn.isEnabled = true
                    closeStatus = false
                    return
                }
            }
        }
        timer.start()
    }

    private fun startTimeCounter() {
        val timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                if (status != true) {
                    on("LED", "on")
                    startTimeCounter()
                    status = false

                } else {
                    buttonClose.isEnabled = true
                    return
                }
            }
        }
        timer.start()

    }


    companion object {
        const val TAG = "AndroidMqttClient"
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, this, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
    fun engineSuccess(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, this, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun engineOff(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, this, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribeOff(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, this, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun engine(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun connect(context: Context) {
        val serverURI = "ws://164.92.187.54:3000"
        mqttClient = MqttAndroidClient(context, serverURI, "kotlin_client")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")
                if (topic.equals("SUCCESS") && successCount == 1 && state == 0) {
                    status = true
                    successCount = 0
                    Toast.makeText(
                        this@MainActivity,
                        "LED has been opened successfully ",
                        Toast.LENGTH_LONG
                    ).show()

                } else if (topic.equals("OFF_SUCCESS") && successCount == 1 && state == 1) {
                    closeStatus = true
                    successCount = 0
                    Toast.makeText(
                        this@MainActivity,
                        "LED has been closed successfully ",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (topic.equals("OUTPUT2_SUCCESS") && engineOn == 1) {
                    Toast.makeText(
                        this@MainActivity,
                        "Engine has been started successfully ",
                        Toast.LENGTH_LONG
                    ).show()
                    engineOn = 0
                    engineStatus = true

                } else if (topic.equals("OUTPUT2_OFF_SUCCESS") && engineOff == 1) {
                    Toast.makeText(
                        this@MainActivity,
                        "Engine has been closed successfully ",
                        Toast.LENGTH_LONG
                    ).show()
                    engineOff = 0
                    engineStatus = true
                }
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options, this, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    subscribe("SUCCESS")
                    subscribeOff("OFF_SUCCESS")
                    engineSuccess("OUTPUT2_SUCCESS")
                    engineOff("OUTPUT2_OFF_SUCCESS")

                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    private fun on(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun close(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun disconnect() {
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }


}