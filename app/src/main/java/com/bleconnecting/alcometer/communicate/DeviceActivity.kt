package com.bleconnecting.alcometer.communicate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bleconnecting.alcometer.R
import com.bleconnecting.alcometer.connection.Client
import com.bleconnecting.alcometer.connection.Commands
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.exceptions.BleException
import java.lang.Exception
import kotlin.collections.ArrayList

class DeviceActivity : AppCompatActivity(),
    Client.ConnectionCallback,
Client.StateCallback{

    companion object {
        val MAC_ADDRESS = "MAC_ADDRESS"
        val NAME = "NAME"
    }

    private val TAG = this.javaClass.name

    private lateinit var recycler: RecyclerView
    private val logArray: ArrayList<LogAdapter.Log> = ArrayList()
    private val logAdapter: LogAdapter = LogAdapter(logArray)

    private lateinit var connectBtn: Button
    private lateinit var disconnectBtn: Button
    private lateinit var buzzVolumeBtn: Button
    private lateinit var versionBtn: Button
    private lateinit var startMeasurementBtn: Button
    private lateinit var syncDateTimeBtn: Button

    private lateinit var mac: String

    private lateinit var alcoClient: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_communication)

        alcoClient = Client(this, this, connectionCallback = this)
        if (intent.getStringExtra(NAME) != null && intent.getStringExtra(MAC_ADDRESS) != null) {
            title = intent.getStringExtra(NAME)
            mac = intent.getStringExtra(MAC_ADDRESS)!!
        }
        else {
            throw Exception("intent without mac or name")
        }

        initLayout()
    }

    private fun initLayout() {

        connectBtn = findViewById(R.id.connect)
        disconnectBtn = findViewById(R.id.disconnect)
        buzzVolumeBtn = findViewById(R.id.buzz_volume)
        versionBtn = findViewById(R.id.device_info)
        startMeasurementBtn = findViewById(R.id.start_measure)
        syncDateTimeBtn = findViewById(R.id.sync_date_time)

        connectBtn.setOnClickListener {
            alcoClient.checkState()
        }

        disconnectBtn.setOnClickListener {
            alcoClient.sendCommand(
                Commands.AppDeviceCommand.MOVE_TO_STANDBY_MODE_OF_APP,
                "SOFT,RESET".toByteArray()
            )
        }

        buzzVolumeBtn.setOnClickListener {
            alcoClient.sendCommand(
                Commands.AppDeviceCommand.CONTROL_BUZZER_VOLUME,
                "R".toByteArray()
            )
        }

        versionBtn.setOnClickListener {
            alcoClient.sendCommand(
                Commands.AppDeviceCommand.CHECK_DEVICE_INFORMATION,
                "INFORMATION".toByteArray()
            )
        }

        startMeasurementBtn.setOnClickListener {
            alcoClient.sendCommand(
                Commands.AppDeviceCommand.START_TO_TEST,
                "TEST,START".toByteArray()
            )
        }

        syncDateTimeBtn.setOnClickListener { }

        recycler = findViewById(R.id.logRecycler)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = logAdapter
        recycler.itemAnimator = DefaultItemAnimator()

        val dividerItemDecoration = DividerItemDecoration(recycler.context, RecyclerView.VERTICAL)
        dividerItemDecoration.setDrawable(
            ContextCompat.getDrawable(baseContext,
                R.drawable.list_divider)!!)
        recycler.addItemDecoration(dividerItemDecoration)
    }

    override fun onDestroy() {
        alcoClient.disconnect()
        super.onDestroy()
    }

    override fun onConnectionError(error: BleException) {
        error.printStackTrace()
    }

    override fun onConnectionState(state: RxBleConnection.RxBleConnectionState) {
        when (state) {
            RxBleConnection.RxBleConnectionState.CONNECTING -> {
                addLog("CONNECTING", "")
            }
            RxBleConnection.RxBleConnectionState.CONNECTED -> {
                addLog("CONNECTED", "")
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                addLog("DISCONNECTED", "")
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTING -> {
                addLog("DISCONNECTING", "")
            }
        }
    }

    override fun onSendSuccess(cmd: Commands.AppDeviceCommand, data: ByteArray) {
        // событие, возникающее при успешном записывании команды
    }

    override fun onData(cmd: Commands.DeviceResponse, data: ArrayList<Any>, bat: Int) {
        // событие получения данных с устройства
        when (cmd) {
            is Commands.DeviceResponse.Device ->
                addLog(cmd.command.name, "data = $data, bat = $bat")
            is Commands.DeviceResponse.DeviceApp ->
                addLog(cmd.command.name, "data = $data, bat = $bat")
        }
    }

    override fun onError(msg: String) {
        addLog("Error", msg)
    }

    override fun onLocationPermRequired() {
        Log.e(TAG, "onLocationPermRequired()")
    }

    override fun onLocationRequired() {
        Log.e(TAG, "onLocationRequired()")
    }

    override fun onBluetoothRequired() {
        Log.e(TAG, "onBluetoothRequired()")
    }

    override fun onReady() {
        alcoClient.connect(mac)
    }

    private fun addLog(log: String, data: String) {
        runOnUiThread {
            logArray.add(LogAdapter.Log(log, data))
            logAdapter.notifyItemInserted(logArray.lastIndex)
            recycler.scrollToPosition(logArray.lastIndex)
        }
    }

    private fun makeToast(msg: String) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}