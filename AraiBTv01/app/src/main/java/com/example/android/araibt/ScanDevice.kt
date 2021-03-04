package com.example.android.araibt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.util.*


class ScanDevice : AppCompatActivity() {

    var mLeDeviceListAdapter: LeDeviceListAdapter? = null
    var mBluetoothAdapter: BluetoothAdapter? = null
    var mScanning = false
    var mHandler: Handler? = null

    val REQUEST_ENABLE_BLUETOOTH = 1

    // Stops scanning after 10 seconds.
    val SCAN_PERIOD: Long = 10000

    lateinit var deviceName : ArrayList<String>
    lateinit var device : ArrayList<BluetoothDevice>
    lateinit var adapter:ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mHandler = Handler()

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast("Does Not support BLE")
            finish()
        }


        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter


        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            toast("error bluetooth not supported")
            finish()
            return
        }

        checkCoarseLocationPermission()
        refresh_scan.setOnClickListener { scanLeDevice(true) }



    }

    fun checkCoarseLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("checkCoarseLocation: ", "Has to request permissions to ACCESS_COARSE_LOCATION")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
            false
        } else {
            Log.i("checkCoarseLocation: ", "ACCESS_COARSE_LOCATION is already GRANTED")
            true
        }
    }

    override fun onResume() {
        super.onResume()

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter!!.isEnabled){
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        scanLeDevice(true)
        setListAdapter(mLeDeviceListAdapter!!)
    }

    fun setListAdapter(mLeDeviceListAdapter: ScanDevice.LeDeviceListAdapter) {
        device = mLeDeviceListAdapter.getListDevice()
        deviceName = mLeDeviceListAdapter.getListDeviceName()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceName)
        list_device.adapter = adapter
    }

    fun updateListAdapter(item: BluetoothDevice){
        device.add(item)
        deviceName.add(item.name)
        adapter.notifyDataSetChanged()
    }



    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                invalidateOptionsMenu()
            }, 10000.toLong())
            mScanning = true
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        }
        invalidateOptionsMenu()
    }

    // Device scan callback.
    private val mLeScanCallback =
        LeScanCallback { device, rssi, scanRecord ->
            runOnUiThread {
                val deviceName = device.name
//                val condition: Boolean
//                condition = try {
//                    deviceName == "JDY-08"
//                } catch (e: NullPointerException) {
//                    false
//                }
//                if (condition) {
//                    autoConnect(device)
//                } else {
                    mLeDeviceListAdapter!!.addDevice(device)
                    toast(deviceName)
                    updateListAdapter(device)
                    mLeDeviceListAdapter!!.notifyDataSetChanged()
//                }
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                if (mBluetoothAdapter!!.isEnabled) {
                    toast("Bluetooth has been enabled")
                    scanLeDevice(true)

                } else {
                    toast("Bluetooth has been disabled")
                }
            } else if (resultCode == RESULT_CANCELED) {
                toast("Bluetooth enabling has been canceled")
            }
        }
    }


//    Class For LE Device List which is wrap in an adapter
    class LeDeviceListAdapter : BaseAdapter() {
        val mLeDevices: ArrayList<BluetoothDevice>
        val mLeDevicesName: ArrayList<String>

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
                mLeDevicesName.add((device.name))
            }
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        fun getListDevice(): ArrayList<BluetoothDevice> {
            return this.mLeDevices
        }

        fun getListDeviceName(): ArrayList<String> {
            return this.mLeDevicesName
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            TODO("Not yet implemented")
        }


        init {
            mLeDevices = ArrayList()
            mLeDevicesName = ArrayList()
        }
    }



}