package com.example.android.araibt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity() {

    var m_bluetoothAdapter: BluetoothAdapter? = null
    lateinit var m_pairedDevices: Set<BluetoothDevice>

    val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        val EXTRA_ADDRESS: String = "Device_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (m_bluetoothAdapter == null){
            toast("this deivce does not support bluetooth")
            return
        }

        // '!!' will say that the var will not be null, dan if ini intinya ngecek apakah dia udah aktif bluetoothnya? jika belum ngepromt untuk mengaktifkan
        if (!m_bluetoothAdapter!!.isEnabled){
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        btSetting.setOnClickListener { goToBluetoothSettings() }

        setUpConnection()
//        continueOnDevice()



    }

//    fun continueOnDevice(){
//        val invList: Intent = Intent(this, InventoryList::class.java)
//        if (targetDevice != null){
//            startActivity(invList)
//        }
//    }

    private fun setUpConnection() {
        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices
        val list : ArrayList<BluetoothDevice> = ArrayList()
        val listName : ArrayList<String> = ArrayList()

        if (!m_pairedDevices.isEmpty()) {
            for (device: BluetoothDevice in m_pairedDevices) {
                list.add(device)
                listName.add(device.name)
//                if (device.name.equals(targetDeviceName)){
//                    val intent = Intent(this, InventoryList::class.java)
//                    intent.putExtra(EXTRA_ADDRESS, device.address)
//                    startActivity(intent)
//                    toast("Target Device Is Found")
//                }
                Log.i("device", "" + device)
            }
        } else {
            toast("no paired bluetooth devices found, please pair the device on you phone bluetooth settings")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listName)
        list_device.adapter = adapter
        list_device.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = list[position]
            val address: String = device.address

            val intent = Intent(this, InventoryList::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)
        }

    }


    private fun goToBluetoothSettings() {
        val intentOpenBluetoothSettings = Intent()
        intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
        startActivity(intentOpenBluetoothSettings)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                if (m_bluetoothAdapter!!.isEnabled) {
                    toast("Bluetooth has been enabled")
                    setUpConnection()


                } else {
                    toast("Bluetooth has been disabled")
                }
            } else if (resultCode == RESULT_CANCELED) {
                toast("Bluetooth enabling has been canceled")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        setUpConnection()
    }

    override fun onPause() {
        super.onPause()
        setUpConnection()
    }

    override fun onStop() {
        super.onStop()
        setUpConnection()
    }

}