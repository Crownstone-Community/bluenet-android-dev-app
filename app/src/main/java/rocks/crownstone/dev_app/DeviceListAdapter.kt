package rocks.crownstone.dev_app

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView



import kotlinx.android.synthetic.main.device_list_item.view.*
import rocks.crownstone.bluenet.OperationMode
import rocks.crownstone.bluenet.scanparsing.ScannedDevice

/**
 * Adapter that can display a list of [ScannedDevice].
 *
 * @param deviceList List of devices to display.
 * @param onClick    Callback function when a device is clicked. True for long clicks.
 */
class DeviceListAdapter(val deviceList: List<ScannedDevice>, onClick: (ScannedDevice, Boolean) -> Unit): RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {
	private val TAG = this.javaClass.simpleName
//	private var deviceList: List<ScannedDevice> = ArrayList()
	private val onClickListener: View.OnClickListener
	private val onLongClickListener: View.OnLongClickListener

	init {
		onClickListener = View.OnClickListener { view ->
			val device = view.tag as ScannedDevice
			onClick(device, false)
		}
		onLongClickListener = View.OnLongClickListener { view ->
			val device = view.tag as ScannedDevice
			onClick(device, true)
			return@OnLongClickListener true
		}
	}


//	fun updateList(deviceList: List<ScannedDevice>) {
//		this.deviceList = deviceList
//		notif
//	}


	// Create a view holder (single list item)
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.device_list_item, parent, false)
		return ViewHolder(view)
	}

	//
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//		Log.i(TAG, "bind $position")
		val device = deviceList[position]
		holder.name.text =    device.name
		holder.address.text = device.address
		holder.rssi.text =    device.rssi.toString()
		val ibeaconData = device.ibeaconData
		if (ibeaconData != null) {
			holder.iBeacon.visibility = View.VISIBLE
			holder.uuid.text =  "uuid: ${ibeaconData.uuid}"
			holder.major.text = "major: ${ibeaconData.major}"
			holder.minor.text = "minor: ${ibeaconData.minor}"
		}
		else {
			holder.iBeacon.visibility = View.GONE
		}

		when (device.operationMode) {
			OperationMode.DFU ->    holder.view.setBackgroundColor(0xFF8000A0.toInt())
			OperationMode.SETUP ->  holder.view.setBackgroundColor(0xFF0080D0.toInt())
			OperationMode.NORMAL -> holder.view.setBackgroundColor(0xFF60A000.toInt())
			else ->                 holder.view.setBackgroundColor(0xFF000000.toInt())
		}

//		holder.view.tag = device
//		holder.view.setOnClickListener(mOnClickListener)
		// See https://stackoverflow.com/questions/45977011/example-of-when-should-we-use-run-let-apply-also-and-with-on-kotlin
		with(holder.view) {
			tag = device // So we can get the associated device when clicked.
			setOnClickListener(onClickListener)
			setOnLongClickListener(onLongClickListener)
		}
	}

	override fun getItemCount(): Int = deviceList.size

	inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
		val name:    TextView = view.name
		val address: TextView = view.address
		val rssi:    TextView = view.rssi
		val iBeacon: LinearLayout = view.layIBeacon
		val uuid:    TextView = view.iBeaconUuid
		val major:   TextView = view.iBeaconMajor
		val minor:   TextView = view.iBeaconMinor
	}
}