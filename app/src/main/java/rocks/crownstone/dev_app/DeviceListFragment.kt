package rocks.crownstone.dev_app

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.dev_app.cloud.SphereData
import rocks.crownstone.dev_app.cloud.StoneData
import rocks.crownstone.dev_app.cloud.UserData
import java.util.*


/**
 * A fragment representing a list of Items.
 */
class DeviceListFragment : Fragment() {
	private val TAG = this.javaClass.simpleName
	private val GUI_UPDATE_INTERVAL_MS = 500

	val deviceList = ArrayList<ScannedDevice>()
	val deviceMap = HashMap<DeviceAddress, ScannedDevice>()
	private lateinit var adapter: DeviceListAdapter
	private lateinit var buttonRefresh: Button
	private var lastUpdateTime: Long = 0


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.device_list_fragment, container, false)

		// Set the adapter
		val listView = view.findViewById<RecyclerView>(R.id.list)
		if (listView is RecyclerView) {
			listView.layoutManager = LinearLayoutManager(context)
			adapter = DeviceListAdapter(deviceList, ::onDeviceClick)
			listView.adapter = adapter
//			// See: https://stackoverflow.com/questions/45977011/example-of-when-should-we-use-run-let-apply-also-and-with-on-kotlin
//			with(listView) {
//				layoutManager = LinearLayoutManager(context)
//				adapter = DeviceListAdapter(deviceList, ::onClick)
//			}
//			adapter = view.adapter as DeviceListAdapter
		}

		buttonRefresh = view.findViewById(R.id.buttonRefresh)
		buttonRefresh.setOnClickListener {
			deviceList.clear()
			adapter.notifyDataSetChanged()
		}


		MainApp.instance.bluenet.subscribe(BluenetEvent.SCAN_RESULT, {data -> onScannedDevice(data as ScannedDevice)})
		MainApp.instance.bluenet.startScanning()
		return view
	}


	fun onScannedDevice(device: ScannedDevice) {
		if (!device.validated) {
			return
		}

//		deviceMap[device.address] = device
		var found = false
		for (i in deviceList.indices) {
			if (deviceList[i].address == device.address) {
				deviceList[i] = device
				adapter.notifyItemChanged(i, device)
				found = true
				break
			}
		}
		if (!found) {
			deviceList.add(device)
			adapter.notifyItemInserted(deviceList.size-1)
		}


//		if (System.currentTimeMillis() > lastUpdateTime + GUI_UPDATE_INTERVAL_MS) {
//			lastUpdateTime = System.currentTimeMillis()
//			updateDeviceList()
//		}
	}

//	private fun updateDeviceList() {
//		deviceList = ArrayList(deviceMap.values)
//	}

	private fun onDeviceClick(device: ScannedDevice, longClick: Boolean) {
		Log.i(TAG, "onDeviceClick ${device.address}")
		val activity = activity ?: return

		if (device.operationMode == OperationMode.SETUP) {
			MainApp.instance.setup(device, activity)
		}

		if (device.operationMode == OperationMode.NORMAL) {
			if (longClick) {
				MainApp.instance.factoryReset(device, activity)
			}
		}
	}
}
