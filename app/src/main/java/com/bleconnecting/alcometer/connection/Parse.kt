package com.bleconnecting.alcometer.connection

import com.bleconnecting.alcometer.connection.Commands.*
import kotlinx.datetime.LocalDate
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class Parse {

    companion object {
        fun parseResponse(cmd: DeviceResponse, data: ByteArray, bat: Int): AlcoResponse {
            return AlcoResponse(
                cmd,
                when (cmd) {
                    is DeviceResponse.Device ->
                        when (cmd.command) {
                            DeviceCommand.SENSOR_STABILIZATION,
                            DeviceCommand.POWER_OFF,
                            DeviceCommand.BATTERY_ERROR,
                            DeviceCommand.WAIT_FOR_BLOW,
                            DeviceCommand.TRIGGER,
                            DeviceCommand.FLOW_ERROR,
                            DeviceCommand.STAND_BY_MODE_FOR_SWITCH,
                            DeviceCommand.TEMPERATURE_ERROR,
                            DeviceCommand.PRESSURE_SENSOR_ERROR,
                            DeviceCommand.SENSOR_ERROR -> parseInt(data, 3)
                            DeviceCommand.TEST_RESULT -> arrayListOf(
                                data[0].toInt(),
                                data[2].toInt(),
                                data.decodeToString(4, 5),
                                data[10].toInt()
                            )
                            DeviceCommand.ANALYZING -> arrayListOf("ANALYZING")
                            DeviceCommand.ENTER_INTO_MENU -> arrayListOf("MANU")
                            DeviceCommand.OFF -> arrayListOf("OFF")
                        }
                    is DeviceResponse.DeviceApp ->
                        when (cmd.command) {
                            DeviceAppCommand.CHECK_DEVICE_INFORMATION -> parseFloat(data, 4)
                            DeviceAppCommand.CONTROL_BUZZER_VOLUME -> parseInt(data, 1)
                            DeviceAppCommand.CHECK_USAGE_COUNT_OF_DEVICE -> arrayListOf(
                                data[0].toInt(),
                                data.decodeToString(2, 6).toInt()
                            )
                            DeviceAppCommand.CHECK_CALIBRATION_INFO -> when (data.count { bt -> bt == '#'.code.toByte() }) {
                                3 -> arrayListOf(kotlinx.datetime.LocalDateTime(
                                    data.decodeToString(0, 4).toInt(),
                                    data.decodeToString(5, 2).toInt(),
                                    data.decodeToString(8, 2).toInt(),
                                    0, 0, 0
                                ))
                                0 -> arrayListOf(
                                    data.decodeToString(0, 6),
                                    data.decodeToString(8, 3),
                                    data.decodeToString(11, 3)
                                )
                                6 -> arrayListOf(
                                    data[0].toInt(),
                                    data.decodeToString(2, 3),
                                    data[6].toInt()
                                )
                                else -> arrayListOf()
                            }
                            DeviceAppCommand.CURRENT_DATE_AND_TIME -> arrayListOf(Date(
                                data.decodeToString(0, 1).toInt(),
                                data.decodeToString(2, 3).toInt(),
                                data.decodeToString(4, 5).toInt(),
                                data.decodeToString(6, 7).toInt(),
                                data.decodeToString(8, 9).toInt(),
                                data.decodeToString(10, 11).toInt()
                            ))
                            DeviceAppCommand.START_TO_TEST,
                            DeviceAppCommand.MOVE_TO_STANDBY_MODE_OF_APP -> arrayListOf("OK")
                        }
                },
                bat
            )
        }

        /**
        *   Describes response from device
         */
        data class AlcoResponse(val cmd: DeviceResponse, val data: ArrayList<Any>, val bat: Int)

        private fun parseInt(bytes: ByteArray, bytesCount: Int): ArrayList<Any> {
            return arrayListOf(bytes.decodeToString(0, bytesCount).toInt())
        }

        private fun parseFloat(bytes: ByteArray, bytesCount: Int): ArrayList<Any> {
            return arrayListOf(bytes.decodeToString(0, bytesCount).toFloat())
        }
    }
}