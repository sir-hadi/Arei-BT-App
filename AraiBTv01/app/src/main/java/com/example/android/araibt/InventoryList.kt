package com.example.android.araibt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import com.example.android.araibt.MainActivity.Companion.targetDevice
import kotlinx.android.synthetic.main.activity_inventory_list.*
import org.jetbrains.anko.toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList


class InventoryList : AppCompatActivity() {

    var bluetoothAdapter: BluetoothAdapter? = null
    lateinit var btArray: Array<BluetoothDevice?>

    lateinit var items : ArrayList<String>
    lateinit var adapter:ArrayAdapter<String>

    var sendReceive: SendReceive? = null

    val STATE_LISTENING = 1
    val STATE_CONNECTING = 2
    val STATE_CONNECTED = 3
    val STATE_CONNECTION_FAILED = 4
    val STATE_MESSAGE_RECEIVED = 5
    val STATE_DISCONNECTED = 5
    val connectionStatusText = "Status : "

    var REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        lateinit var m_address: String
        var m_bluetoothSocket: BluetoothSocket? = null
    }

    private val APP_NAME = "HC-05"
    private val MY_UUID = UUID.fromString("ab0828b1-198e-4351-b779-901fa0e0371e")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_list)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter!!.isEnabled()) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        m_address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)
        val device:BluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(m_address)
        toast("selected device : "+ device.name+" - "+device.address)


//        var targetDevice: BluetoothDevice? = MainActivity.targetDevice
//        toast(targetDevice?.name.toString())

        items = ArrayList()
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,items
        )
        item_list.adapter = adapter
        addItem("test")



        refresh_connection.setOnClickListener { refreshClientConnection() }
//        val server = ServerClass()
//        server.start()


    }



    fun refreshClientConnection(){
        val device:BluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(m_address)
        toast(device.address+" : "+device.name)
//        val socket: BluetoothSocket =  device.createInsecureRfcommSocketToServiceRecord(MY_UUID)
//        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
//        socket.connect()
//        var sendreceive: SendReceive = SendReceive(socket)
//        sendreceive.start()
        val clientClass = ClientClass(device)
        clientClass.start()

//        val server = ServerClass()
//        server.start()
    }

    fun addItem(item: String){
        items.add(item)
        adapter.notifyDataSetChanged()
        toast("added Item")

    }



    var handler: Handler = Handler { msg ->
        when (msg.what) {
            STATE_LISTENING -> connection_status?.setText(connectionStatusText+"Listening")
            STATE_CONNECTING -> connection_status?.setText(connectionStatusText+"Connecting")
            STATE_CONNECTED -> connection_status?.setText(connectionStatusText+"Connected")
            STATE_CONNECTION_FAILED -> connection_status?.setText(connectionStatusText+"Connection Failed")
            STATE_MESSAGE_RECEIVED -> {
                val readBuff = msg.obj as ByteArray
                val tempMsg = String(readBuff, 0, msg.arg1)
                addItem(tempMsg)
            }
            STATE_DISCONNECTED -> connection_status?.setText(connectionStatusText+"Disconnected")
        }
        true
    }



    inner class ServerClass : Thread() {
        private var serverSocket: BluetoothServerSocket? = null
        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)
                    socket = serverSocket!!.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }
                if (socket != null) {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)
                    sendReceive = SendReceive(socket)
                    sendReceive!!.start()
                    break
                }
            }
        }

        init {
            try {
                serverSocket =
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ClientClass(private val device: BluetoothDevice?) : Thread() {

        override fun run() {
            try {
                m_bluetoothSocket!!.connect()
                val message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)
                sendReceive = SendReceive(m_bluetoothSocket)
                sendReceive!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }

//            try {
//                if (!m_bluetoothSocket?.isConnected!!){
//                    val message = Message.obtain()
//                    message.what = STATE_DISCONNECTED
//                    handler.sendMessage(message)
//                }
//            } catch (e : Exception) {
//                e.printStackTrace()
//                toast(e.toString())
//            }


        }

        init {
            try {
                m_bluetoothSocket = device!!.createRfcommSocketToServiceRecord(MY_UUID)
                val message = Message.obtain()
                message.what = STATE_CONNECTING
                handler.sendMessage(message)
            } catch (e: IOException) {
                e.printStackTrace()
                toast(e.toString())
            }
        }

    }


    inner class SendReceive(private val bluetoothSocket: BluetoothSocket?) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                outputStream!!.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null
            try {
                tempIn = bluetoothSocket!!.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            inputStream = tempIn
            outputStream = tempOut
        }
    }




}
