package rocks.crownstone.dev_app

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
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

	enum class DeviceOption {
		Setup,
		FactoryReset,
		PutInDfu,
		HardwareVersion,
		FirmwareVersion,
		BootloaderVersion,
		ResetCount,
		Switch,
		Toggle,
		SetIbeaconUUID,
	}

	private fun onDeviceClick(device: ScannedDevice, longClick: Boolean) {
		Log.i(TAG, "onDeviceClick ${device.address}")
		val activity = activity ?: return

		val optionList = DeviceOption.values()
		val optionNames = ArrayList<String>()
		for (opt in optionList) {
			optionNames.add(opt.name)
		}
		var optionInd = 0
		MainApp.instance.selectOptionAlert(activity, "Select an option", optionNames)
				.then {
					optionInd = it
					MainApp.instance.bluenet.connect(device.address)
				}.unwrap()
				.then {
					when (optionList[optionInd]) {
						DeviceOption.Setup -> {
							MainApp.instance.setup(device, activity)
						}
						DeviceOption.FactoryReset -> {
							MainApp.instance.factoryReset(device, activity)
						}
						DeviceOption.PutInDfu -> {
							MainApp.instance.bluenet.control.goToDfu()
						}
						DeviceOption.HardwareVersion -> {
							MainApp.instance.bluenet.deviceInfo.getHardwareVersion()
									.success { MainApp.instance.showResult("hw: $it", activity) }
						}
						DeviceOption.FirmwareVersion -> {
							MainApp.instance.bluenet.deviceInfo.getFirmwareVersion()
									.success { MainApp.instance.showResult("fw: $it", activity) }
						}
						DeviceOption.BootloaderVersion -> {
							MainApp.instance.bluenet.deviceInfo.getBootloaderVersion()
									.success { MainApp.instance.showResult("bl: $it", activity) }
						}
						DeviceOption.ResetCount -> {
							MainApp.instance.bluenet.state.getResetCount()
									.success { MainApp.instance.showResult("resetCount: $it", activity) }
						}
						DeviceOption.Switch -> {
							if (MainApp.instance.switchCmd != 0) {
								MainApp.instance.switchCmd = 0
							}
							else {
								MainApp.instance.switchCmd = 255
							}
							val sphereId: SphereId = device.sphereId ?: "unknown"
							val stoneId: Uint8 = device.serviceData?.crownstoneId ?: 255.toUint8()
							MainApp.instance.bluenet.broadCast.switch(sphereId, stoneId, MainApp.instance.switchCmd.toUint8())
						}
						DeviceOption.Toggle -> {
							MainApp.instance.bluenet.control.toggleSwitch(255.toUint8())
						}
						DeviceOption.SetIbeaconUUID -> {
							val uuid = UUID.randomUUID()
							Log.i(TAG, "Set ibeaconUuid: $uuid")
							MainApp.instance.showResult("Set uuid: $uuid", activity)
							MainApp.instance.bluenet.config.setIbeaconUuid(uuid, PersistenceModeSet.TEMPORARY)
									.then {
										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.CURRENT)
									}.unwrap()
									.then {
										Log.i(TAG, "Current ibeaconUuid: $it")
										MainApp.instance.showResult("Current uuid: $it", activity)
										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.STORED)
									}.unwrap()
									.then {
										Log.i(TAG, "Stored ibeaconUuid: $it")
										MainApp.instance.showResult("Stored uuid: $it", activity)
										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.FIRMWARE_DEFAULT)
									}.unwrap()
									.then {
										Log.i(TAG, "Default ibeaconUuid: $it")
										MainApp.instance.showResult("Default uuid: $it", activity)
									}

						}
					}
				}.unwrap()
				.fail {
					Log.e(TAG, "failed: ${it.message}")
					MainApp.instance.showResult("Failed: ${it.message}", activity)
				}
				.always { MainApp.instance.bluenet.disconnect() }
	}
}
