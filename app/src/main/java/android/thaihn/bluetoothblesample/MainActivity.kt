package android.thaihn.bluetoothblesample

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.thaihn.bluetoothblesample.databinding.ActivityMainBinding
import android.util.Log
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity(), DeviceListAdapter.DeviceListener {

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val REQUEST_ENABLE_BLUETOOTH = 99
        private const val SCAN_PERIOD: Long = 1000 * 5
    }

    private lateinit var mainBinding: ActivityMainBinding

    private val BluetoothAdapter.isDisable: Boolean get() = !isEnabled

    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    private var mBluetoothLeService: BluetoothLeService? = null
    private var mBound = false

    // BLE
    private var mScanning: Boolean = false
    private var mHandler: Handler = Handler()

    // UI
    private val mDeviceAdapter: DeviceListAdapter = DeviceListAdapter(arrayListOf(), this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mainBinding.rvDevice.apply {
            adapter = mDeviceAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        mainBinding.btnScan.setOnClickListener {
            if (!mScanning) {
                scanBLEDevice(true)
            }
        }
    }

    private val mConnectionService = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.BluetoothBinder
            mBluetoothLeService = binder.service

            enableBluetooth()

            packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
                Toast.makeText(applicationContext, "Bluetooth Low Energy not support!", Toast.LENGTH_SHORT).show()
                onStop()
            }

            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBluetoothLeService = null
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, BluetoothLeService::class.java), mConnectionService, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            unbindService(mConnectionService)
            mBound = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            Log.d(TAG, "result code $resultCode")
            if (resultCode == Activity.RESULT_OK) {
                // Do something
                if (!mScanning) {
                    scanBLEDevice(true)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Show again
            }
        }
    }

    override fun onItemClick(item: BluetoothDevice) {
        if (mScanning) {
            scanBLEDevice(false)
        }
    }

    // Enable Bluetooth when user disable
    private fun enableBluetooth() {
        mBluetoothLeService?.bluetoothAdapter?.takeIf { it.isDisable }.apply {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH)
        }
    }

    private fun scanBLEDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stop scan after a pre-degined scan period
                mHandler.postDelayed({
                    mScanning = false
                    mainBinding.progress.visibility = View.GONE
                    mBluetoothLeService?.bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                }, SCAN_PERIOD)

                mScanning = true
                mainBinding.progress.visibility = View.VISIBLE
                val scanFilter = ScanFilter.Builder()
                        .build()
                val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                        .build()
                mBluetoothLeService?.bluetoothAdapter?.bluetoothLeScanner?.startScan(
                        listOf(scanFilter),
                        settings,
                        scanCallback
                )
            }
            false -> {
                mScanning = false
                mainBinding.progress.visibility = View.GONE
                mBluetoothLeService?.bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        }
    }

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "ScanCallback: device:$result")
            result?.device?.let {
                mDeviceAdapter.addDevice(it)
            }
        }
    }

}
