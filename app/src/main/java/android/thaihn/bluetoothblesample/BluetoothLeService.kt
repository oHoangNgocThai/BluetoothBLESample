package android.thaihn.bluetoothblesample

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BluetoothLeService : Service() {

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName

        private val PACKAGE_NAME = BluetoothLeService::class.java.`package`
        val ACTION_GATT_CONNECCTED = "$PACKAGE_NAME.GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "$PACKAGE_NAME.GATT_DISCONNECTED"
        val ACTION_GATT_SERVICE_DISCOVERED = "$PACKAGE_NAME.GATT_SERVICE_DISCOVERED"
        val ACTION_GATT_DATA_AVAILABLE = "$PACKAGE_NAME.GATT_DATA_AVAILABLE"
        val EXTRA_DATA = "$PACKAGE_NAME.EXTRA_DATA"
    }

    val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val mBinder = BluetoothBinder()

    inner class BluetoothBinder : Binder() {
        internal val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
