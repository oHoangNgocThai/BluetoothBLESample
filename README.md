# BluetoothBLESample

# Bluetooth Low Energy

## BLE permission
* Để sử dụng các tính năng của Bluetooth trong ứng dụng, bạn cần phải khai báo quyền BLUETOOTH. Chúng ta cần quyền này cho bất kỳ giao tiếp nào nữ yêu cầu kết nối, chấp nhận và truyền dữ liệu.
* Nếu muốn thay đổi các thiết lập cài đặt Bluetooth, bạn cần khai báo thêm quyền BLUETOOTH_ADMIN nữa, quyền này dựa trên quyền BLUETOOTH được cài đặt trước đó.

```
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
```

* Nếu bạn muốn ứng dụng của bạn chỉ khả dụng cho những thiết bị có khả năng BLE, hãy thêm mục sau vào tệp AndroidManifest.xml: 

```
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
```

* Nếu bạn vẫn muốn cung cấp cho những thiết bị không hỗ trợ BLE, bạn vẫn khai báo nhưng đặt `required-false`. Sau đó có thể kiểm tra xem thiết bị có hỗ trợ không sử dụng `PackageManager.hasSystemFeature()`: 

```
private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)
...

packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
    finish()
}
```

> Lưu ý: **LE Beacons** thường được liên kết với vị trí. Để sử dụng **BluetoothLeScanner**, bạn phải thêm quyền **ACCESS_COARSE_LOCATION** hoặc **ACCESS_FINE_LOCATION**. 
 
## Setup BLE

> Nếu trong thiết đặt quyền mà bạn cho phép ứng dụng giao tiếp với BLE, bạn cần check lại xem thiết bị đó có hỗ trợ hay không, và chắc chắn rằng nó đang được bật. Nếu thiết bị hỗ trợ nhưng lại bị tắt thì bạn cần yêu cầu người dùng bật Bluetooth lên, việc này được thực hiện thông qua **BluetoothAdapter**

1. Get `BluetoothAdapter`:

> **BluetoothAdapter** là bắt buộc cho bất kỳ hoạt động nào của Bluetooth. Nó đại diện cho bộ điều khiển Bluetooth cho thiết bị. Chỉ có một bộ điều khiển Bluetooth cho toàn hệ thống và ứng dụng tương tác với Bluetooth qua nó.

```
private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
    val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    bluetoothManager.adapter
}
```

2. Enable Bluetooth:
> Tiếp đến, bạn cần cho người dùng mở cài đặt Bluetooth. Hãy sử dụng hàm **isEnable()** để kiểm tra xem trạng thái của Bluetooth.

```
private val BluetoothAdapter.isDisabled: Boolean
    get() = !isEnabled
...

// Ensures Bluetooth is available on the device and it is enabled. If not,
// displays a dialog requesting user permission to enable Bluetooth.
bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
}
```

* Khi nhận lại kết quả khi người dùng enable bluetooth hay vẫn cancel thì cần check RequestCode(nên lớn hơn 0).

```
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
        Log.d(TAG, "result code $resultCode")
        if (resultCode == Activity.RESULT_OK) {
            // Do something
            Toast.makeText(applicationContext, "Enabled bluetooth", Toast.LENGTH_SHORT).show()
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // Show again
            Toast.makeText(applicationContext, "You must enable Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }
}
```

## Scan BLE devices
> Việc quét các thiết bị xung quanh sẽ được chạy liên tục từ lúc bạn bắt đầu, vì vậy nên dừng quét khi thấy thiết bị mong muốn hoặc là trong 1 khoảng thời gian nhất định.

* Để thực hiện scan, chúng ta sử dụng `bluetoothScanner` trong BluetoothAdapter để tiến hành scan.

```

companion object {
    private const val SCAN_PERIOD: Long = 10000
}
    
private fun scanBLEDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stop scan after a pre-degined scan period
                mHandler.postDelayed({
                    mScanning = false
                    mBluetoothLeService?.bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                }, SCAN_PERIOD)

                mScanning = true
                val scanFilter = ScanFilter.Builder()
                        .build()
                val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                        .build()
                mBluetoothLeService?.bluetoothAdapter?.bluetoothLeScanner?.startScan(listOf(scanFilter), settings, scanCallback)
            }
            false -> {
                mScanning = false
                mBluetoothLeService?.bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        }
    }
```

> Lúc trước trong Java sử dụng `bluetoothAdapter.startLeScan` thì có thể sử dụng việc quét một thiết bị nhất định nào, nhưng trong Kotlin đã không sử dụng nữa nên thay thế vào đó là sử dụng như code trên. Nếu muốn sử dụng trên java, hãy tham khảo tại [đây](https://developer.android.com/guide/topics/connectivity/bluetooth-le?hl=en#kotlin)

* Tiếp theo chúng ta sử dụng listener callback để có thể lấy được các device quanh đây: 

```
private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            mDeviceAdapter.submitList(listOf(result?.device))
        }
    }
```

> Chú ý. Bạn chỉ có thể hoặc là Bluetooth LE device hoặc là Classic Bluetooth Device. Không được phép scan cả 2 loại cùng 1 lúc.

## Connect to a GATT server

### GATT là gì?
* **GATT - Generic Attribute Profile**: 


