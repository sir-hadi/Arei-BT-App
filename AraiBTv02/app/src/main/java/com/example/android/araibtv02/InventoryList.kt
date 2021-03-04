package com.example.android.araibtv02

import android.bluetooth.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ToggleButton
import kotlinx.android.synthetic.main.activity_inventory_list.*
import kotlinx.coroutines.*
import org.jetbrains.anko.toast
import java.util.*


class InventoryList : AppCompatActivity() {

    companion object{
        lateinit var deviceAddress:String
        private const val GATT_MAX_MTU_SIZE = 517
        lateinit var bluetoothGatt: BluetoothGatt
    }

    val STATE_LISTENING = 1
    val STATE_CONNECTING = 2
    val STATE_CONNECTED = 3
    val STATE_CONNECTION_FAILED = 4
    val STATE_MESSAGE_RECEIVED = 5
    val STATE_MESSAGE_ALREADY_RECEIVED = 6
    val STATE_MESSAGE_EMPTY = 7
    val STATE_DISCONNECTED = 8
    val STATE_READABLE = 9
    val STATE_NOT_READABLE = 10
    val STATE_NOTIFY_ERROR = 11
    val STATE_NOTIFY_AMAN = 12
    val connectionStatusText = "Status : "

    lateinit var items : ArrayList<String>
    lateinit var adapter: ArrayAdapter<String>
    var dummyNum = 1
//    val serviceUUID = "ab0828b1-198e-4351-b779-901fa0e0371e"
//    val charUuid = "0972EF8C-7613-4075-AD52-756F33D4DA91"
//    val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    val serviceUUID = "0000fff0-0000-1000-8000-00805f9b34fb"
    val charUuid = "0000fff1-0000-1000-8000-00805f9b34fb"
    val charWrite = "0000fff2-0000-1000-8000-00805f9b34fb"
    val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    var toggleListeningMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_list)
        deviceAddress = intent.getStringExtra(ScanDevice.EXTRA_ADDRESS)

        items = ArrayList()
        adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,items
        )
        item_list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        var device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
        toast("selected device : ${device.name ?: "Unnamed"} - ${device.address}")
        device.connectGatt(this,false,gattCallback)
        refresh_connection.setOnClickListener { device.connectGatt(this,false,gattCallback)}

//        readingsThread().start()

    }


    var handler: Handler = Handler { msg ->
        when (msg.what) {
            STATE_LISTENING -> connection_status?.setText(connectionStatusText+"Listening")
            STATE_CONNECTING -> connection_status?.setText(connectionStatusText+"Connecting")
            STATE_CONNECTED -> connection_status?.setText(connectionStatusText+"Connected")
            STATE_CONNECTION_FAILED -> connection_status?.setText(connectionStatusText+"Connection Failed")
            STATE_MESSAGE_RECEIVED -> {
                toast( "item receive : ${ msg.obj.toString() }")
                addItem(msg.obj.toString())
            }
//            STATE_MESSAGE_ALREADY_RECEIVED -> toast("Message is Already In")
//            STATE_MESSAGE_EMPTY -> toast("Message is empty")
            STATE_DISCONNECTED -> connection_status?.setText(connectionStatusText+"Disconnected")
            STATE_READABLE -> reading_status.setText("Readable")
            STATE_NOT_READABLE-> reading_status.setText("Not Readable")
            STATE_NOTIFY_ERROR-> notify_status.setText(msg.obj.toString())
            STATE_NOTIFY_AMAN-> notify_status.setText("Notify Connected")
        }
        true
    }

    inner class readingsThread : Thread() {
        override fun run() {
            super.run()
            while (true){
                try {
                    readDataRecive(bluetoothGatt)
                    Log.i("ReadingsThread","Thread Is being used")
                }catch (e: Exception){
                    Log.w("ExceptionThread", "bluetoothGatt is Null")
                }

                sleep(1500)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            var message = Message.obtain()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatt = gatt
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)

                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt.discoverServices()

                    }



                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    message.what = STATE_DISCONNECTED
                    handler.sendMessage(message)
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                printGattTable()
                readDataRecive(gatt)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.w("MTUChange","ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
        }

        override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
        ) {

            with(characteristic) {
                var deskriptoLIst = characteristic.getDescriptors()
                for (desk in deskriptoLIst){
                    Log.d("getDescriptors","UUID : ${desk.uuid.toString()} , Valeu : ${desk}, DescribeContens : ${desk.describeContents()}")
                }
                enableNotifications(characteristic)
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        val readBytes: ByteArray = value
                        val sValue = readBytes.decodeToString()
                        var message = Message.obtain()
                        if (sValue !in items) {
                            if(sValue.isNotBlank() || sValue.isNotEmpty()){
                                message.obj = sValue
                                message.what = STATE_MESSAGE_RECEIVED
                                handler.sendMessage(message)

                                Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${sValue}")
                            } else {
                                message.what = STATE_MESSAGE_EMPTY
                                handler.sendMessage(message)
                            }
                        }else{
                            Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${sValue} is already in")
                            message.what = STATE_MESSAGE_ALREADY_RECEIVED
                            handler.sendMessage(message)
                        }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Wrote to characteristic $uuid | value: ${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic write failed for $uuid, error: $status")
                    }
                }
            }
        }


        override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                var message = Message.obtain()
                message.obj = value.decodeToString()
                message.what = STATE_MESSAGE_RECEIVED
                handler.sendMessage(message)
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: ${value.decodeToString()}")
            }
        }

    }

    fun addItem(item: String){
        items.add(item)
        adapter.notifyDataSetChanged()
    }

    fun readDataRecive(gatt: BluetoothGatt) {
        val batteryServiceUuid = UUID.fromString(serviceUUID)
        val batteryLevelCharUuid = UUID.fromString(charUuid)
        val batteryLevelChar = gatt?.getService(batteryServiceUuid)?.getCharacteristic(batteryLevelCharUuid)
        var message = Message.obtain()
        try {
            gatt?.readCharacteristic(batteryLevelChar)
        }catch (e: Exception){
                Log.e("readCharacteristic","Characteristic ${e.toString()}")
        }

        if (batteryLevelChar?.isReadable() == true) {
            if(!reading_status.text.equals("Readable")){
                message.what = STATE_READABLE
                handler.sendMessage(message)
            }
            gatt?.readCharacteristic(batteryLevelChar)
            Log.i("BluetoothGatt","Characteristic ${batteryLevelCharUuid} is readable")
            toast("Characteristic ${batteryLevelCharUuid} is readable")

        }else{
            if(!reading_status.text.equals("Not Readable")){
                message.what = STATE_NOT_READABLE
                handler.sendMessage(message)
            }
            toast("Characteristic ${batteryLevelCharUuid} is not readable")
        }


//        if (toggleListeningMode) {
//            Log.e("NotificationManager", "${batteryLevelChar?.uuid} doesn't support notifications/indications, starting readingsThread")
//            readingsThread().start()
//            toast("Using System Readings")
//            toggleListeningMode = false
//        }
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        var message = Message.obtain()
        if ( characteristic.isNotifiable() ){
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                message.what = STATE_NOTIFY_ERROR
                message.obj = "setCharacteristicNotification failed for ${characteristic.uuid}"
                handler.sendMessage(message)
                return
            }else{
                Log.i("ConnectionManager", "setCharacteristicNotification successful for ${characteristic.uuid}")
                message.what = STATE_NOTIFY_AMAN
                message.obj = "setCharacteristicNotification successful for ${characteristic.uuid}"
                handler.sendMessage(message)
                if (readingsThread().isAlive) readingsThread().stop()
            }
        }else{
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
            message.what = STATE_NOTIFY_ERROR
            message.obj = "${characteristic.uuid} doesn't support notifications/indications"
            handler.sendMessage(message)
            if (!(readingsThread().isAlive)) readingsThread().start()

        }
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, payload: ByteArray) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic.uuid} cannot be written to")
        }

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = writeType
            characteristic.value = payload
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }


    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun ByteArray.toHexString(): String =
            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    fun BluetoothGattDescriptor.isReadable(): Boolean =
            containsPermission(BluetoothGattDescriptor.PERMISSION_READ) ||
                    containsPermission(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) ||
                    containsPermission(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM)
    fun BluetoothGattDescriptor.isWritable(): Boolean =
            containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE) ||
                    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) ||
                    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) ||
                    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) ||
                    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM)

    fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
            permissions and permission != 0


}