package rocks.crownstone.dev_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.encryption.MeshKeySet
import rocks.crownstone.bluenet.packets.UuidPacket
import rocks.crownstone.bluenet.packets.behaviour.*
import rocks.crownstone.bluenet.packets.other.IbeaconConfigIdPacket
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Conversion
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.bluenet.util.toUint8
import java.util.*


class MainActivity : AppCompatActivity() {
	private val TAG = this.javaClass.simpleName

	private val REQUEST_CODE_LOGIN = 1

	val deviceList = ArrayList<ScannedDevice>()
	val deviceMap = HashMap<DeviceAddress, ScannedDevice>()
	private lateinit var adapter: DeviceListAdapter
	private lateinit var buttonRefresh: Button
	private var lastUpdateTime: Long = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Log.i(TAG, "onCreate")
		setContentView(R.layout.activity_main)
		setSupportActionBar(findViewById(R.id.my_toolbar))


		// Set the adapter
		val listView = findViewById<RecyclerView>(R.id.list)
		if (listView is RecyclerView) {
			listView.layoutManager = LinearLayoutManager(this)
			adapter = DeviceListAdapter(deviceList, ::onDeviceClick)
			listView.adapter = adapter
//			// See: https://stackoverflow.com/questions/45977011/example-of-when-should-we-use-run-let-apply-also-and-with-on-kotlin
//			with(listView) {
//				layoutManager = LinearLayoutManager(context)
//				adapter = DeviceListAdapter(deviceList, ::onClick)
//			}
//			adapter = view.adapter as DeviceListAdapter
		}

		buttonRefresh = findViewById(R.id.buttonRefresh)
		buttonRefresh.setOnClickListener {
			MainApp.instance.bluenet.filterForCrownstones(true)
			MainApp.instance.bluenet.filterForIbeacons(true)
			MainApp.instance.bluenet.setScanInterval(ScanMode.BALANCED)
			MainApp.instance.bluenet.startScanning()
			deviceList.clear()
			adapter.notifyDataSetChanged()
		}

		findViewById<Button>(R.id.buttonSort).setOnClickListener {
			deviceList.sortByDescending { it.rssi }
			adapter.notifyDataSetChanged()
		}

		MainApp.instance.bluenet.subscribe(BluenetEvent.SCAN_RESULT, { data -> onScannedDevice(data as ScannedDevice)})
//		MainApp.instance.bluenet.subscribe(BluenetEvent.INITIALIZED, ::onBluenetInitialized)
//		MainApp.instance.bluenet.subscribe(BluenetEvent.SCANNER_READY, ::onScannerReady)

		Log.i(TAG, "init bluenet")
//		MainApp.instance.bluenet.init(this)
		MainApp.instance.bluenet.init(applicationContext)
				.then {
					MainApp.instance.bluenet.runInForeground(MainApp.instance.NOTIFICATION_ID, MainApp.instance.getNotification())
				}
				.then {
					Log.i(TAG, "make scanner ready")
					MainApp.instance.bluenet.makeScannerReady(this)
				}.unwrap()
				.then {
					MainApp.instance.bluenet.filterForIbeacons(true)
					MainApp.instance.bluenet.filterForCrownstones(true)
					Log.i(TAG, "start scanning")
					MainApp.instance.bluenet.startScanning()
				}

		if (MainApp.instance.user.loadLogin(this)) {
			MainApp.instance.sphere.getSpheres(MainApp.instance.user)
					.success {
						MainApp.instance.showResult("Logged in", this)
						onLogin(0)
					}
					.fail {
						MainApp.instance.showResult("Failed to log in", this)
					}
		}

		// Always call onLogin, so the dev sphere settings are loaded.
		onLogin(0)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		Log.i(TAG, "onCreateOptionsMenu")
		val inflater: MenuInflater = menuInflater
		inflater.inflate(R.menu.menu_main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		Log.i(TAG, "onOptionsItemSelected")
		if (item == null) { return false }
		when (item.itemId) {
			R.id.action_login -> {
				val intent = Intent(this, LoginActivity::class.java)
				this.startActivityForResult(intent, REQUEST_CODE_LOGIN)
				return true
			}
			R.id.action_localization -> {
				MainApp.instance.showResult("This does nothing!", this)
				return true
			}
			R.id.action_test -> {
				val deviceListCopy = ArrayList<ScannedDevice>()
				deviceListCopy.addAll(deviceList)
				MainApp.instance.test(deviceListCopy, this)
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	fun onBluenetInitialized(data: Any) {
//		Log.i(TAG, "onBluenetInitialized")
//		MainApp.instance.bluenet.makeScannerReady(this)
//				.success {
//					Log.i(TAG, "start scanning")
//					MainApp.instance.bluenet.startScanning()
//				}
//				.fail {
//					Log.w(TAG, "unable to start scanning: $it")
//				}
//		MainApp.instance.bluenet.tryMakeScannerReady(this)
	}

	fun onScannerReady(data: Any) {
		Log.i(TAG, "onScannerReady")
//		MainApp.instance.bluenet.startScanning()
	}

	fun onLogin(resultCode: Int) {
		Log.i(TAG, "onLogin result=$resultCode")
		setSphereSettings()
	}

	fun setSphereSettings() {
		val spheres = MainApp.instance.sphere.spheres
		val sphereSettings = SphereSettingsMap()
		for (sphere in spheres.values) {
			val keySet = KeySet(sphere.keySet?.adminKey, sphere.keySet?.memberKey, sphere.keySet?.guestKey, sphere.keySet?.serviceDataKey, sphere.keySet?.localizationKey)
			val meshKeySet = MeshKeySet(null, sphere.keySet?.meshAppKey, sphere.keySet?.meshNetKey)
			val iBeaconUuid = UUID.fromString(sphere.iBeaconUUID)
			val deviceToken = 0.toUint8()
			val sphereSetting = SphereSettings(keySet, meshKeySet, iBeaconUuid, Conversion.toUint8(sphere.uid), deviceToken)
			sphereSettings.put(sphere.id, sphereSetting)
			MainApp.instance.bluenet.iBeaconRanger.track(iBeaconUuid, sphere.id)
		}
		sphereSettings.put(MainApp.instance.devSphereId, MainApp.instance.devSphereSetting)
		MainApp.instance.bluenet.setSphereSettings(sphereSettings)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		Log.i(TAG, "onActivityResult $requestCode $resultCode")
		if (MainApp.instance.bluenet.handleActivityResult(requestCode, resultCode, data)) {
			return
		}
		if (requestCode == REQUEST_CODE_LOGIN) {
			onLogin(resultCode)
			return
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		Log.i(TAG, "onRequestPermissionsResult $requestCode")
		if (MainApp.instance.bluenet.handlePermissionResult(requestCode, permissions, grantResults)) {
			return
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}


	fun onScannedDevice(device: ScannedDevice) {
//		if (!device.validated) {
//			return
//		}
		if (device.serviceData == null) {
			return
		}

		if (device.serviceData?.unique == false) {
			return
		}
		Log.v(TAG, "Add or update $device")

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
		Toggle,
	}

	private fun onDeviceClick(device: ScannedDevice, longClick: Boolean) {
		rocks.crownstone.bluenet.util.Log.i(TAG, "onDeviceClick ${device.address}")
		val activity = this ?: return

		if (!longClick) {
			val intent = Intent(this, ControlActivity::class.java)
			intent.putExtra("deviceAddress", device.address)
			MainApp.instance.selectedDevice = device
			this.startActivity(intent)
//			this.startActivityForResult(intent, REQUEST_CODE_LOGIN)
			return
		}

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
							MainApp.instance.bluenet.control(device.address).reset()
						}
						DeviceOption.PutInDfu -> {
							MainApp.instance.bluenet.control(device.address).goToDfu()
						}
						DeviceOption.HardwareVersion -> {
							MainApp.instance.bluenet.deviceInfo(device.address).getHardwareVersion()
									.success { MainApp.instance.showResult("hw: $it", activity) }
						}
						DeviceOption.FirmwareVersion -> {
							MainApp.instance.bluenet.deviceInfo(device.address).getFirmwareVersion()
									.success { MainApp.instance.showResult("fw: $it", activity) }
						}
						DeviceOption.BootloaderVersion -> {
							MainApp.instance.bluenet.deviceInfo(device.address).getBootloaderVersion()
									.success { MainApp.instance.showResult("bl: $it", activity) }
						}
						DeviceOption.ResetCount -> {
							MainApp.instance.bluenet.state(device.address).getResetCount()
									.success { MainApp.instance.showResult("resetCount: $it", activity) }
						}
						DeviceOption.UicrData -> {
							MainApp.instance.bluenet.deviceInfo(device.address).getUicrData()
									.success { MainApp.instance.showResult("uicr: $it", activity) }
						}
						DeviceOption.Toggle -> {
							MainApp.instance.bluenet.control(device.address).toggleSwitch(255.toUint8())
						}
					}
				}.unwrap()
				.success {
					MainApp.instance.showResult("Success", activity)
				}
				.fail {
					rocks.crownstone.bluenet.util.Log.e(TAG, "failed: ${it.message}")
					it.printStackTrace()
					MainApp.instance.showResult("Failed: ${it.message}", activity)
				}
				.always { MainApp.instance.bluenet.disconnect(device.address) }
	}
}
