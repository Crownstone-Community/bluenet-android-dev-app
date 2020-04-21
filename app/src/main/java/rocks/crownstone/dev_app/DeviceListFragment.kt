package rocks.crownstone.dev_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.packets.UuidPacket
import rocks.crownstone.bluenet.packets.behaviour.*
import rocks.crownstone.bluenet.packets.multiSwitch.MultiSwitchItemPacket
import rocks.crownstone.bluenet.packets.multiSwitch.MultiSwitchPacket
import rocks.crownstone.bluenet.packets.other.IbeaconConfigIdPacket
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import java.util.*
import kotlin.collections.ArrayList


/**
 * A fragment representing a list of Items.
 */
class DeviceListFragment : androidx.fragment.app.Fragment() {
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
		val listView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.list)
		if (listView is androidx.recyclerview.widget.RecyclerView) {
			listView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
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
		Reset,
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
						DeviceOption.Reset -> {
							MainApp.instance.bluenet.control.reset()
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
//							val multiSwitchPacket = MultiSwitchPacket()
//							multiSwitchPacket.add(MultiSwitchItemPacket(217U, MainApp.instance.switchCmd.toUint8()))
//							MainApp.instance.bluenet.control.multiSwitch(multiSwitchPacket)
						}
						DeviceOption.Toggle -> {
							MainApp.instance.bluenet.control.toggleSwitch(255.toUint8())
						}
						DeviceOption.SetTime -> {
							val timestamp = Util.getLocalTimestamp()
							MainApp.instance.bluenet.control.setTime(timestamp)
									.success { MainApp.instance.showResult("Set time to: $timestamp", activity) }
						}
						DeviceOption.SetIbeaconUUID -> {
							val uuid = UUID.randomUUID()
							MainApp.instance.showResult("Set ibeaconUuid: $uuid", activity)
//							MainApp.instance.bluenet.config.setIbeaconUuid(uuid, PersistenceModeSet.TEMPORARY)
//									.then {
//										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.CURRENT)
//									}.unwrap()
//									.then {
//										MainApp.instance.showResult("Current ibeaconUuid: $it", activity)
//										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.STORED)
//									}.unwrap()
//									.then {
//										MainApp.instance.showResult("Stored ibeaconUuid: $it", activity)
//										MainApp.instance.bluenet.config.getIbeaconUuid(PersistenceModeGet.FIRMWARE_DEFAULT)
//									}.unwrap()
//									.then {
//										MainApp.instance.showResult("Default ibeaconUuid: $it", activity)
//									}
							val statePacket = StatePacketV5(StateTypeV4.IBEACON_PROXIMITY_UUID, 1U, PersistenceModeSet.TEMPORARY.num, UuidPacket(uuid))
							val ids = listOf<Uint8>(217U, 83U)
							MainApp.instance.bluenet.mesh.setState(statePacket, ids[0])
									.then {
										MainApp.instance.bluenet.mesh.setState(statePacket, ids[1])
									}.unwrap()
									.then {
										val timestamp: Uint32 = 0U
										val interval: Uint16 = 4U
//										MainApp.instance.bluenet.control.setIbeaconConfigId(IbeaconConfigIdPacket(0U, timestamp, interval))
										MainApp.instance.bluenet.mesh.setIbeaconConfigId(IbeaconConfigIdPacket(0U, timestamp, interval), ids)
									}.unwrap()
									.then {
										val timestamp: Uint32 = 2U
										val interval: Uint16 = 4U
//										MainApp.instance.bluenet.control.setIbeaconConfigId(IbeaconConfigIdPacket(1U, timestamp, interval))
										MainApp.instance.bluenet.mesh.setIbeaconConfigId(IbeaconConfigIdPacket(1U, timestamp, interval), ids)
									}.unwrap()

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
