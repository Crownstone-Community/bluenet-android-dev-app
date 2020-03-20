package rocks.crownstone.dev_app

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
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
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
			MainApp.instance.bluenet.filterForCrownstones(true)
			MainApp.instance.bluenet.filterForIbeacons(true)
			MainApp.instance.bluenet.setScanInterval(ScanMode.BALANCED)
			MainApp.instance.bluenet.startScanning()
			deviceList.clear()
			adapter.notifyDataSetChanged()
		}


		MainApp.instance.bluenet.subscribe(BluenetEvent.SCAN_RESULT, { data -> onScannedDevice(data as ScannedDevice)})
//		MainApp.instance.bluenet.startScanning()
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

//		if (device.operationMode == OperationMode.SETUP) {
//			MainApp.instance.setup(device, activity)
//		}
//
//		if (device.operationMode == OperationMode.NORMAL) {
//			if (longClick) {
//				MainApp.instance.factoryReset(device, activity)
//			}
//			else {
//				val timestamp = 0x123456.toLong()
//				val keepAlivePacket = KeepAliveSameTimeout(10)
//				val ids = arrayOf(1,2,3,17,24)
////				for (i in 0 until 5) {
//				for (i in ids) {
//					keepAlivePacket.add(KeepAliveSameTimeoutItem(Conversion.toUint8(i), KeepAliveActionSwitch(100)))
//				}
//				val multiSwitchPacket = MultiSwitchListPacket()
//
//				if (MainApp.instance.switchCmd > 0) {
//					MainApp.instance.switchCmd = 0
//				}
//				else {
//					MainApp.instance.switchCmd = 100
//				}
//				for (i in ids) {
//					multiSwitchPacket.add(MultiSwitchListItemPacket(Conversion.toUint8(i), Conversion.toUint8(MainApp.instance.switchCmd), 0, MultiSwitchIntent.MANUAL))
//				}
				MainApp.instance.bluenet.connect(device.address)
//						.then {
//							if (device.operationMode == OperationMode.DFU) {
//								MainApp.instance.bluenet.dfu.reset()
//							}
//							else {
//								MainApp.instance.bluenet.control.goToDfu()
//							}
//						}.unwrap()
//						.then { MainApp.instance.bluenet.deviceInfo.getBootloaderVersion() }.unwrap()
//						.then {
//							MainApp.instance.bluenet.state.getResetCount()
//									.success { Log.i(TAG, "reset count = $it") }
//						}.unwrap()
						.then {
							val uuid = UUID.randomUUID()
							MainApp.instance.bluenet.config.setIbeaconUuid(uuid, PersistenceMode.RAM)
									.success {
										Log.i(TAG, "set uuid $uuid")
										MainApp.instance.showResult("set $uuid", activity)
									}
						}.unwrap()
						.then {
							MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceMode.FLASH)
									.success {
										Log.i(TAG, "flash uuid $it")
										MainApp.instance.showResult("flash $it", activity)
									}
						}.unwrap()
						.then {
							MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceMode.RAM)
									.success {
										Log.i(TAG, "ram uuid $it")
										MainApp.instance.showResult("ram $it", activity)
									}
						}.unwrap()
//						.then { MainApp.instance.bluenet.control.meshCommand(MeshControlPacket(ControlPacket(ControlType.NOOP))) }.unwrap()
//						.then { Util.waitPromise(100, MainApp.instance.handler) }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshAction(MultiKeepAlivePacket(keepAlivePacket)) }.unwrap()
//						.then { Util.waitPromise(100, MainApp.instance.handler) }.unwrap()
//						.then { MainApp.instance.bluenet.control.meshCommand(MeshControlPacket(ControlPacket(ControlType.SET_TIME, Conversion.toByteArray(timestamp)))) }.unwrap()
//						.then { Util.waitPromise(100, MainApp.instance.handler) }.unwrap()
//						.then { MainApp.instance.bluenet.control.multiSwtich(MultiSwitchPacket(multiSwitchPacket)) }.unwrap()
//						.then { Util.waitPromise(100, MainApp.instance.handler) }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshRepeat() }.unwrap()
//						.then { Util.waitPromise(100, MainApp.instance.handler) }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshRepeat() }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshRepeat() }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshRepeat() }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshRepeat() }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshRepeat() }.unwrap()
//						.then { MainApp.instance.bluenet.control.keepAliveMeshRepeat() }.unwrap()
//						.then { MainApp.instance.bluenet.state.getSwitchCraftBuffers() }.unwrap()
//						.success { Log.i(TAG, "buf: ${Conversion.bytesToString(it)}") }
//						.success { Log.i(TAG, "buf: $it") }
//						.fail {	Log.e(TAG, "failed to get switchcraft buffers: ${it.message}") }
						.fail { Log.e(TAG, "failed: ${it.message}") }
						.always { MainApp.instance.bluenet.disconnect() }
//			}
//		}
	}
}
