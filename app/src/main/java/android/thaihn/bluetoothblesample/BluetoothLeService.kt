package android.thaihn.bluetoothblesample

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

class BluetoothLeService : Service() {

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName

        private val PACKAGE_NAME = BluetoothLeService::class.java.`package`
        val ACTION_GATT_CONNECCTED = "$PACKAGE_NAME.GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "$PACKAGE_NAME.GATT_DISCONNECTED"
        val ACTION_GATT_SERVICE_DISCOVERED = "$PACKAGE_NAME.GATT_SERVICE_DISCOVERED"
        val ACTION_GATT_DATA_AVAILABLE = "$PACKAGE_NAME.GATT_DATA_AVAILABLE"
        val EXTRA_DATA = "$PACKAGE_NAME.EXTRA_DATA"

        val STATE_DISCONNECTED = 0
        val STATE_CONNECTING = 1
        val STATE_CONNECTED = 2

        val UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)
    }

    val mBluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    var mBluetoothGatt: BluetoothGatt? = null
    var mBluetoothDeviceAddress: String? = null

    var mConnectionState = STATE_DISCONNECTED

    private val mBinder = BluetoothBinder()

    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            var action = ACTION_GATT_DISCONNECTED
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    action = ACTION_GATT_CONNECCTED
                    mConnectionState = STATE_CONNECTED
                    Log.i(TAG, "Connected to GATT server. ${mBluetoothGatt?.discoverServices()}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    action = ACTION_GATT_DISCONNECTED
                    mConnectionState = STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                }
            }
            broadcastUpdate(action)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICE_DISCOVERED)
            } else {
                Log.d(TAG, "onServicesDiscovered received: $status")
            }

        }

        override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.let {
                    broadcastUpdate(ACTION_GATT_DATA_AVAILABLE, it)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            characteristic?.let {
                broadcastUpdate(ACTION_GATT_DATA_AVAILABLE, it)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

    }

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

    fun connect(address: String): Boolean {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth adapter is null")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            mBluetoothGatt?.connect()?.let {
                return if (it) {
                    mConnectionState = STATE_CONNECTING
                    true
                } else {
                    false
                }
            }
        }

        mBluetoothAdapter?.getRemoteDevice(address).let {
            return if (it == null) {
                Log.d(TAG, "Device not found.  Unable to connect.")
                false
            } else {
                // We want to directly connect to the device, so we are setting the autoConnect
                // parameter to false.
                mBluetoothGatt = it?.connectGatt(this, false, mGattCallback)
                mBluetoothDeviceAddress = address
                mConnectionState = STATE_CONNECTING
                true
            }
        }
    }

    private fun broadcastUpdate(action: String) {
        sendBroadcast(Intent(action))
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = characteristic.properties
            val format = when (flag and 0x01) {
                0x01 -> {
                    Log.d(TAG, "Heart rate format UINT16.")
                    BluetoothGattCharacteristic.FORMAT_UINT16
                }
                else -> {
                    Log.d(TAG, "Heart rate format UINT8.")
                    BluetoothGattCharacteristic.FORMAT_UINT8
                }
            }
            val heartRate = characteristic.getIntValue(format, 1)
            Log.d(TAG, String.format("Received heart rate: %d", heartRate))
            intent.putExtra(EXTRA_DATA, (heartRate).toString())
        } else {
            // For all other profiles, writes the data formatted in HEX.
            val data: ByteArray? = characteristic.value
            if (data?.isNotEmpty() == true) {
                val hexString: String = data.joinToString(separator = " ") {
                    String.format("%02X", it)
                }
                intent.putExtra(EXTRA_DATA, "$data\n$hexString")
            }
        }
        sendBroadcast(intent)
    }
}
