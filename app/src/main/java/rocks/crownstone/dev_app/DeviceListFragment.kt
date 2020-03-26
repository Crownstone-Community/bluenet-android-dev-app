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
import rocks.crownstone.bluenet.packets.UuidPacket
import rocks.crownstone.bluenet.packets.behaviour.*
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.util.*
import kotlin.collections.ArrayList


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
		UicrData,
		Switch,
		Toggle,
		SetTime,
		SetIbeaconUUID,
		SetBehaviour,
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
						DeviceOption.UicrData -> {
							MainApp.instance.bluenet.deviceInfo.getUicrData()
									.success { MainApp.instance.showResult("uicr: $it", activity) }
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
						DeviceOption.SetTime -> {
							val timeZoneGmt = TimeZone.getTimeZone("GMT")
							val calendarGmt = Calendar.getInstance(timeZoneGmt)
							val gmtTimestamp = calendarGmt.time.time / 1000

							val calendar = Calendar.getInstance()
							val timeZone = calendar.timeZone
							val secondsFromGmt = (timeZone.rawOffset / 1000).toLong()
							val correctedTimestamp = gmtTimestamp + secondsFromGmt

							MainApp.instance.bluenet.control.setTime(correctedTimestamp.toUint32())
									.success { MainApp.instance.showResult("Set time to: $correctedTimestamp", activity) }
						}
						DeviceOption.SetIbeaconUUID -> {
							val uuid = UUID.randomUUID()
							MainApp.instance.showResult("Set ibeaconUuid: $uuid", activity)
							MainApp.instance.bluenet.config.setIbeaconUuid(uuid, PersistenceModeSet.TEMPORARY)
									.then {
										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.CURRENT)
									}.unwrap()
									.then {
										MainApp.instance.showResult("Current ibeaconUuid: $it", activity)
										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.STORED)
									}.unwrap()
									.then {
										MainApp.instance.showResult("Stored ibeaconUuid: $it", activity)
										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.FIRMWARE_DEFAULT)
									}.unwrap()
									.then {
										MainApp.instance.showResult("Default ibeaconUuid: $it", activity)
									}
//							val statePacket = StatePacketV5(StateTypeV4.IBEACON_PROXIMITY_UUID, 0U, PersistenceModeSet.TEMPORARY.num, UuidPacket(uuid))
//							MainApp.instance.bluenet.mesh.setState(statePacket, 217U)
//							MainApp.instance.bluenet.mesh.setState(statePacket, 83U)

						}
						DeviceOption.SetBehaviour -> {
							val daysOfWeek = DaysOfWeekPacket(true, true, true, true, true, true, true)
							val startTime = TimeOfDayPacket(BaseTimeType.MIDNIGHT, 6*3600)
							val endTime = TimeOfDayPacket(BaseTimeType.MIDNIGHT, 23*3600)
							val presence = PresencePacket(PresenceType.ALWAYS_TRUE, ArrayList(), 5U * 60U)
							val behaviourPacket = SwitchBehaviourPacket(0U, 0U, daysOfWeek, startTime, endTime, presence)
							MainApp.instance.bluenet.control.addBehaviour(behaviourPacket)
									.success {
										MainApp.instance.showResult("Added behaviour at index=${it.index} hash=${it.hash}", activity)
									}
						}
					}
				}.unwrap()
				.success {
					MainApp.instance.showResult("Success", activity)
				}
				.fail {
					Log.e(TAG, "failed: ${it.message}")
					it.printStackTrace()
					MainApp.instance.showResult("Failed: ${it.message}", activity)
				}
				.always { MainApp.instance.bluenet.disconnect() }
	}
}
