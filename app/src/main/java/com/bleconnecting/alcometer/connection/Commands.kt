package com.bleconnecting.alcometer.connection

import android.util.Log
import java.util.*
import kotlin.experimental.inv

import com.bleconnecting.alcometer.connection.Parse.*
import java.lang.Exception
import kotlin.collections.ArrayList

class Commands {

    companion object {

        val writeUuid: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        val notifyUuid: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")

        fun parseAlcoResponse(bytes: ByteArray): Parse.Companion.AlcoResponse {
            val unpacked = unpackCmd(bytes)
            val cmd = unpacked.copyOfRange(0, 3).decodeToString()
            val data = unpacked.copyOfRange(3, 16)
            val bat = unpacked[16].toInt()

            Log.e("cmd", cmd)

            return Parse.parseResponse(
                when (cmd[0]) {
                    'T' ->
                        DeviceResponse.Device(DeviceCommand.values().first { t -> t.code == cmd })
                    'B' ->
                        DeviceResponse.DeviceApp(DeviceAppCommand.values().first { t -> t.code == cmd })
                              else ->
                                  throw Exception("no such command")
                              },
                data,
                bat
            )
        }

        fun packCommand(cmd: AppDeviceCommand, data: ByteArray): ByteArray {
            if (data.size <= 14) {
                val missingBytesArray = ByteArray(14 - data.size)
                missingBytesArray.fill(0)
                return packCmd(cmd.code.toByteArray()
                    .plus(fillEmptyEntries(
                        data.plus(missingBytesArray)
                    )))
            }
            else
                throw Exception("ByteArray size must be 14 or lower")
        }

        private fun unpackCmd(data: ByteArray): ByteArray {
            if (data.size == 20) {
                return data.copyOfRange(1, 18)
            }
            else
                throw Exception("ByteArray size must be 20")
        }

        private fun packCmd(data: ByteArray): ByteArray {
            if (data.size == 17) {
                val stx = 0x02.toByte()
                val etx = 0x03.toByte()

                val checkSum = calcCheckSum(data)
                val bcc = calcAddition(checkSum)

                return byteArrayOf(stx)
                    .plus(data)
                    .plus(bcc)
                    .plus(etx)
            }
            else
                throw Exception("ByteArray size must be 17")
        }

        private fun fillEmptyEntries(bytes: ByteArray) : ByteArray =
            bytes.onEachIndexed { index, bt ->
                if (bt == 0.toByte())
                    bytes[index] = '#'.code.toByte()
            }

        private fun calcAddition(byte: Byte): Byte {
            return (byte.inv() + 0x00000001).toByte()
        }

        private fun calcCheckSum(bytes: ByteArray): Byte {
            var crc: Byte = 0
            for (i in bytes) {
                crc = (crc + i).toByte()
            }
            return crc
        }
    }

    sealed class DeviceResponse {
        class Device(val command: DeviceCommand): DeviceResponse()
        class DeviceApp(val command: DeviceAppCommand): DeviceResponse()
    }

    /**
     * Describes app commands to device. (те, что помечены как не использованные, на тестах не работали, CURRENT_DATE_AND_TIME надо ещё проверить)
     */
    enum class AppDeviceCommand(val code: String)
    {
        CHECK_DEVICE_INFORMATION("A01"),
        CHECK_USAGE_COUNT_OF_DEVICE("A03"),
        CHECK_CALIBRATION_INFO("A04"),
        CONTROL_BUZZER_VOLUME("A19"),
        START_TO_TEST("A20"),
        CURRENT_DATE_AND_TIME("A21"),
        MOVE_TO_STANDBY_MODE_OF_APP("A22")
    }

    /**
     * Describes device info receiving while connected.
     */
    enum class DeviceCommand(val code: String)
    {
        /**
         * Подготовка к записи данных
         */
        SENSOR_STABILIZATION("T03"),
        POWER_OFF("T04"),
        BATTERY_ERROR("T05"),
        /**
         * Устройство готово к работе, ожидается использование
         */
        WAIT_FOR_BLOW("T06"),
        /**
         * Записываются данные с сенсора
         */
        TRIGGER("T07"),
        FLOW_ERROR("T08"),
        SENSOR_ERROR("T09"),
        /**
         * Данные анализируются
         */
        ANALYZING("T10"),
        /**
         * Данные проанализированны, пересылаются в приложение
         */
        TEST_RESULT("T11"),
        /**
         * Отключение режима bluetooth
         */
        STAND_BY_MODE_FOR_SWITCH("T12"),
        TEMPERATURE_ERROR("T17"),
        PRESSURE_SENSOR_ERROR("T18"),
        ENTER_INTO_MENU("T19"),
        OFF("T20")
    }

    /**
     * Describes device answers to app commands
     */
    enum class DeviceAppCommand(val code: String)
    {
        CHECK_DEVICE_INFORMATION("B01"),
        CHECK_USAGE_COUNT_OF_DEVICE("B03"),
        CHECK_CALIBRATION_INFO("B04"),
        CONTROL_BUZZER_VOLUME("B19"),
        START_TO_TEST("B20"),
        CURRENT_DATE_AND_TIME("B21"),
        MOVE_TO_STANDBY_MODE_OF_APP("B22")
    }
}