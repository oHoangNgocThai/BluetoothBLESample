package android.thaihn.bluetoothblesample

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.thaihn.bluetoothblesample.databinding.ActivityControlBinding
import android.util.Log

class ControlActivity : AppCompatActivity() {

    companion object {
        private val TAG = ControlActivity::class.java.simpleName
    }

    private var mBluetoothLeService: BluetoothLeService? = null
    private var mBound = false

    private var mDevice: BluetoothDevice? = null

    private lateinit var controlBinding: ActivityControlBinding

    private val mConnectionService = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.BluetoothBinder
            mBluetoothLeService = binder.service

            Log.d(TAG, "device when connection success $mDevice")

            mBluetoothLeService?.mBluetoothAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlBinding = DataBindingUtil.setContentView(this, R.layout.activity_control)

        mDevice = intent.getParcelableExtra<BluetoothDevice>(MainActivity.EXTRA_BLUETOOTH_DEVICE)
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            unbindService(mConnectionService)
            mBound = false
        }
    }
}
