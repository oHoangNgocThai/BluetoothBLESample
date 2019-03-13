package android.thaihn.bluetoothblesample

import android.bluetooth.BluetoothDevice
import android.content.*
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.thaihn.bluetoothblesample.databinding.ActivityControlBinding

class ControlActivity : AppCompatActivity() {

    companion object {
        private val TAG = ControlActivity::class.java.simpleName
    }

    private var mBluetoothLeService: BluetoothLeService? = null
    private var mBound = false

    private var mDevice: BluetoothDevice? = null

    private var connected = false

    private lateinit var controlBinding: ActivityControlBinding

    private val mConnectionService = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.BluetoothBinder
            mBluetoothLeService = binder.service

            mBluetoothLeService?.mBluetoothAdapter

            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBluetoothLeService = null
            mBound = false
        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent.takeIf { it != null }?.apply {
                when (action) {
                    BluetoothLeService.ACTION_GATT_CONNECCTED -> {
                        connected = true
                        updateConnectionState(getString(R.string.str_state_connected))
                    }
                    BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                        connected = false
                        updateConnectionState(getString(R.string.str_state_disconnected))
                    }
                    BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERED -> {
//                        displayGattServices
                    }
                    BluetoothLeService.ACTION_GATT_DATA_AVAILABLE -> {

                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, BluetoothLeService::class.java), mConnectionService, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        mDevice?.address?.let {
            mBluetoothLeService?.connect(it)
        }
        registerReceiver(mBroadcastReceiver,makeGattUpdateIntentFilter())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlBinding = DataBindingUtil.setContentView(this, R.layout.activity_control)

        mDevice = intent.getParcelableExtra(MainActivity.EXTRA_BLUETOOTH_DEVICE)

        controlBinding.tvAddress.text = mDevice?.address
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onStop() {
        super.onStop()

        if (mBound) {
            unbindService(mConnectionService)
            mBound = false
        }
    }

    private fun updateConnectionState(resource: String) {
        runOnUiThread {
            controlBinding.tvState.text = resource
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECCTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DATA_AVAILABLE)
        return intentFilter
    }
}
