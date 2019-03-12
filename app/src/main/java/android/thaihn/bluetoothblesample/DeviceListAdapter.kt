package android.thaihn.bluetoothblesample

import android.bluetooth.BluetoothDevice
import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.thaihn.bluetoothblesample.databinding.ItemBluetoothDeviceBinding
import android.view.LayoutInflater
import android.view.ViewGroup

class DeviceListAdapter(
        private val items: ArrayList<BluetoothDevice>,
        private val listener: DeviceListener
) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    interface DeviceListener {
        fun onItemClick(item: BluetoothDevice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflate = LayoutInflater.from(parent.context)
        return ViewHolder(DataBindingUtil.inflate(layoutInflate, R.layout.item_bluetooth_device, parent, false), listener)
    }

    override fun onBindViewHolder(viewholder: ViewHolder, position: Int) {
        viewholder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(
            private val binding: ItemBluetoothDeviceBinding,
            private val listener: DeviceListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BluetoothDevice) {
            binding.device = item
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                listener.onItemClick(item)
            }
        }
    }

    fun addDevice(item: BluetoothDevice) {
        val index = items.indexOfFirst {
            it.uuids.contentEquals(item.uuids)
        }

        if (index == -1) {

        }
    }
}
