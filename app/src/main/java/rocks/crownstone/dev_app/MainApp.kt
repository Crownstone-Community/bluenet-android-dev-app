package rocks.crownstone.dev_app

import android.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.appcompat.app.AlertDialog
import android.widget.ArrayAdapter
import android.widget.Toast
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.promiseOnUi
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.Bluenet
import rocks.crownstone.bluenet.behaviour.BehaviourSyncerFromCrownstone
import rocks.crownstone.bluenet.encryption.KeySet
import rocks.crownstone.bluenet.encryption.MeshKeySet
import rocks.crownstone.bluenet.encryption.RC5
import rocks.crownstone.bluenet.packets.behaviour.IndexedBehaviourPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesType
import rocks.crownstone.bluenet.scanhandling.NearestDeviceListEntry
import rocks.crownstone.bluenet.scanparsing.CrownstoneServiceData
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import rocks.crownstone.dev_app.cloud.*
import java.util.*
import kotlin.math.min


// Singleton class that is accessible in all activities
//object MainApp : Application() {
//	private val TAG = this.javaClass.simpleName
//	lateinit var volleyQueue: RequestQueue
//
//	override fun onCreate() {
//		Log.i(TAG, "onCreate")
//		super.onCreate()
//		volleyQueue = Volley.newRequestQueue(this)
//	}
//}


//class MainApp : Application(), DefaultLifecycleObserver { // Requires min api 24
class MainApp : Application(), LifecycleObserver {
	private val TAG = this.javaClass.simpleName
	lateinit var context: Context
//	val volleyQueue = Volley.newRequestQueue(this)
	lateinit var volleyQueue: RequestQueue
	lateinit var user: User
	lateinit var sphere: Sphere
	lateinit var stone: Stone
	val bluenet = Bluenet()

	val usingDevSphere = true

	val NOTIFICATION_ID = 1

	val deviceMap = HashMap<DeviceAddress, ScannedDevice>()
	var nearestDeviceAddress: DeviceAddress? = null
	var handler = Handler()

	var switchCmd = 100

	// Device selected from list.
	var selectedDevice: ScannedDevice? = null
	var selectedDeviceServiceData: CrownstoneServiceData? = null

	// Dev sphere.
	val devSphereId = "devSphere"
	val devKeySet = KeySet("adminKeyForCrown", "memberKeyForHome", "basicKeyForOther", "MyServiceDataKey", "aLocalizationKey")
	val devMeshKeySet = MeshKeySet("aStoneKeyForMesh", "MyGoodMeshAppKey", "MyGoodMeshNetKey")
	val devMeshAccessAddress = 0.toUint32()
	val devIbeaconUuid = UUID.fromString("1843423e-e175-4af0-a2e4-31e32f729a8a")
	val devSphereShortId = 123.toUint8()
	val devDeviceToken = 12.toUint8()
	val devSphereSetting = SphereSettings(devKeySet, devMeshKeySet, devIbeaconUuid, devSphereShortId, devDeviceToken)

	val behaviours = ArrayList<IndexedBehaviourPacket>()
	val behaviourSyncer = BehaviourSyncerFromCrownstone(bluenet)

	override fun onCreate() {
		super<Application>.onCreate()
		Log.i(TAG, "onCreate")
		instance = this
		context = this.applicationContext
		startKovenant() // Start thread(s)
		volleyQueue = Volley.newRequestQueue(this)
		user = User(this, volleyQueue)
		sphere = Sphere(this, volleyQueue)
		stone = Stone(this, volleyQueue)

		ProcessLifecycleOwner.get().lifecycle.addObserver(this)

		bluenet.subscribe(BluenetEvent.SCAN_RESULT, { data -> onScannedDevice(data as ScannedDevice)})
		bluenet.subscribe(BluenetEvent.NEAREST_VALIDATED_NORMAL, ::onNearest)

		bluenet.subscribe(BluenetEvent.CORE_CONNECTED, { data: Any? -> Log.i(TAG, "connected to ${data as DeviceAddress}") })
		bluenet.subscribe(BluenetEvent.CORE_DISCONNECTED, { data: Any? -> Log.i(TAG, "disconnected from ${data as DeviceAddress}") })
//		handler.postDelayed(testBluenetRunnable, 1000)

		val arr = ByteArray(0)
		val str = String(arr, Charsets.US_ASCII)
		Log.i(TAG, "str=$str len=${str.length} cond=${str == ""}") // str= len=0 cond=true

		val testKovenant = TestKovenant()
		testKovenant.test()
//		testKovenant.testRecover()
//		testKovenant.testConversion()

		val testTemplate = TestTemplate()
		testTemplate.test()

		Log.i(TAG, "Timestamp=${Util.getLocalTimestamp()}")

//		val uuid = UUID.fromString("0000C002-0000-1000-8000-00805F9B34FB")
//		when (uuid) {
//			null -> Log.i(TAG, "null")
//			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_PLUG -> Log.i(TAG, "plug")
//			BluenetProtocol.SERVICE_DATA_UUID_CROWNSTONE_BUILTIN -> Log.i(TAG, "builtin")
//			BluenetProtocol.SERVICE_DATA_UUID_GUIDESTONE -> Log.i(TAG, "guide")
//			else -> Log.i(TAG, "unknown")
//		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	fun onAppForegrounded() {
		Log.i(TAG, "onAppForegrounded")
		if (bluenet.isScannerReady()) {
			bluenet.filterForCrownstones(true)
//			bluenet.filterForIbeacons(false)
//			bluenet.startScanning()
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
	fun onAppBackgrounded() {
		Log.i(TAG, "onAppBackgrounded")
		if (bluenet.isScannerReady()) {
//			bluenet.filterForIbeacons(true)
			bluenet.filterForCrownstones(false)
//			bluenet.stopScanning()
		}
	}

//	override fun onStart(owner: LifecycleOwner) {
//		Log.i(TAG, "onStart")
//	}
//
//	override fun onStop(owner: LifecycleOwner) {
//		Log.i(TAG, "onStop")
//	}

	override fun onTerminate() {
		super.onTerminate()
		stopKovenant() // Stop thread(s)
	}

	private fun onScannedDevice(device: ScannedDevice) {
//		if (!device.validated) {
//			return
//		}
		if (device.serviceData == null) {
			return
		}

		if (device.serviceData?.unique == false) {
			return
		}
		Log.v(TAG, "onScannedDevice $device")
		deviceMap.put(device.address, device)

		// Cache the latest service data of the selected device.
		if (device.address == selectedDevice?.address) {
			val serviceData = device.serviceData
			if (device.validated && serviceData != null && !serviceData.flagExternalData) {
				selectedDeviceServiceData = device.serviceData
			}
		}
	}


	private fun onNearest(data: Any?) {
		if (data == null) {
			return
		}
		val nearest = data as NearestDeviceListEntry
		Log.v(TAG, "nearest=${nearest.deviceAddress}")
		nearestDeviceAddress = nearest.deviceAddress
	}

	private val testBluenetRunnable = Runnable {
		val address = nearestDeviceAddress

		test(ArrayList(deviceMap.values), null)
	}

	fun getNotification(): Notification {
		val notificationChannelId = "Crownstone" // The id of the notification channel. Must be unique per package. The value may be truncated if it is too long.
		val notificationIntent = Intent(context, MainActivity::class.java)
		notificationIntent.action = Intent.ACTION_MAIN
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
		notificationIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		if (Build.VERSION.SDK_INT >= 26) {
			// Create the notification channel, must be done before posting any notification.
			// It's safe to call this repeatedly because creating an existing notification channel performs no operation.
			val name = "Dev stone" // The user visible name of the channel. The recommended maximum length is 40 characters; the value may be truncated if it is too long.
			val importance = android.app.NotificationManager.IMPORTANCE_MIN
			val channel = NotificationChannel(notificationChannelId, name, importance)

			// Register the channel with the system; you can't change the importance or other notification behaviors after this
			val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
		val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
		val notification = NotificationCompat.Builder(context, notificationChannelId)
				.setSmallIcon(R.drawable.icon_notification)
				.setContentTitle("Dev stone is running")
				.setContentText("test")
				.setContentIntent(pendingIntent)
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_LOW)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.build()
		return notification
	}

	fun setup(device: ScannedDevice, activity: Activity): Promise<Unit, Exception> {
//		bluenet.stopScanning()
		if (usingDevSphere) {
			val addressBytes = device.address.toByteArray()
			val stoneId = Conversion.hexStringToBytes(device.address.slice(0 until 2))[0].toUint8()
//			val stoneId = (1..255).random().toUint8()
			val major = (1..60000).random().toUint16()
			val minor = (1..60000).random().toUint16()
			val ibeaconData = IbeaconData(devIbeaconUuid, major, minor, -60)
			return bluenet.connect(device.address)
					.then {
						bluenet.setup(device.address).setup(stoneId, devSphereShortId, devKeySet, devMeshKeySet, devMeshAccessAddress, ibeaconData)
					}.unwrap()
					.success {
						Log.i(TAG, "Setup complete!")
						showResult("setup success", activity)
					}
					.fail {
						Log.e(TAG, "Setup failed: ${it.message}")
						it.printStackTrace()
						showResult("setup failed: ${it.message}", activity)
						bluenet.disconnect(device.address)
					}
					.always {
//						bluenet.startScanning()
					}
		}
		else {
			return Promise.ofFail(Exception("Don't setup when logged in"))

			var sphere: SphereData? = null
			var stoneData: StoneData? = null
			var userData: UserData? = null
			return selectSphereAlert(activity)
					.then {
						sphere = it
						user.getUserData()
					}.unwrap()
					.then {
						userData = it
						val name = device.name ?: ""
						Util.recoverablePromise(stone.createStone(userData!!, sphere!!, name, device.address), { error ->
							return@recoverablePromise stone.getStoneData(userData!!, sphere!!, device.address)
						})
					}.unwrap()
					.then {
						stoneData = it
						bluenet.connect(device.address)
					}.unwrap()
					.then {
						val keySet = KeySet(sphere?.keySet?.adminKey, sphere?.keySet?.memberKey, sphere?.keySet?.guestKey, sphere?.keySet?.serviceDataKey, sphere?.keySet?.localizationKey)
						val meshKeySet = MeshKeySet(stoneData?.meshDevKey, sphere?.keySet?.meshAppKey, sphere?.keySet?.meshNetKey)
						val meshAccessAddress = Conversion.byteArrayTo<Uint32>(Conversion.hexStringToBytes(sphere!!.meshAccessAddress))
						val ibeaconData = IbeaconData(UUID.fromString(stoneData!!.iBeaconUUID), stoneData!!.iBeaconMajor.toUint16(), stoneData!!.iBeaconMinor.toUint16(), 0)
						val stoneId = stoneData!!.stoneId
						bluenet.setup(device.address).setup(stoneId.toUint8(), sphere?.uid!!.toUint8(), keySet, meshKeySet, meshAccessAddress, ibeaconData)
					}.unwrap()
					.success {
						Log.i(TAG, "Setup complete!")
						showResult("setup success", activity)
					}
					.fail {
						Log.e(TAG, "Setup failed: ${it.message}")
						it.printStackTrace()
						showResult("setup failed: ${it.message}", activity)
						bluenet.disconnect(device.address)
					}
					.always {
//						bluenet.startScanning()
					}
		}
	}

	fun factoryReset(device: ScannedDevice, activity: Activity, autoConfirm: Boolean = false): Promise<Unit, Exception> {
		if ((device.sphereId != devSphereId) && (device.sphereId != null)) {
			Log.i(TAG, "Refuse factory reset. SphereId=${device.sphereId}")
			showResult("Don't factory reset stones that are not in the dev sphere. SphereId=${device.sphereId}", activity)
			return Promise.ofFail(Exception("Don't factory reset stones that are not in the dev sphere. SphereId=${device.sphereId}"))
		}
		return confirmAlert(activity, "factory reset", "Do you want to factory reset this device?", autoConfirm)
				.then { confirmed ->
					if (!confirmed) {
						return@then Promise.ofSuccess<Unit, Exception>(Unit)
					}
					bluenet.connect(device.address)
							.then {
								bluenet.control(device.address).factoryReset()
							}.unwrap()
							.success {
								Log.i(TAG, "factory reset success")
								showResult("factory reset success", activity)
								bluenet.disconnect(device.address, true)
							}
							.fail {
								Log.e(TAG, "factory reset failed: ${it.message}")
								showResult("factory reset failed: ${it.message}", activity)
								bluenet.disconnect(device.address, true)
							}
				}.unwrap()
	}

	fun removeStoneFromCloud(device: ScannedDevice): Promise<Unit, Exception> {
		if (usingDevSphere) {
			return Promise.ofSuccess(Unit)
		}
		else {
			val spheres = sphere.spheres
			for (sphere in spheres.values) {
				val uuid = UUID.fromString(sphere.iBeaconUUID)
				if (uuid == device.ibeaconData?.uuid) {
					return user.getUserData()
							.then {
								stone.removeStone(it, sphere, device.address)
							}.unwrap()
				}
			}
			return Promise.ofFail(Exception("Unknown sphere"))
		}
	}

	fun showResult(msg: String, activity: Activity?) {
		Log.i(TAG, msg)
		activity?.runOnUiThread {
			Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
		}
	}


	fun confirmAlert(activity: Activity, title: String, question: String, autoConfirm: Boolean = false): Promise<Boolean, Exception> {
		if (autoConfirm) {
			return Promise.ofSuccess(true)
		}
		return promiseOnUi {
			val deferred = deferred<Boolean, Exception>()
			val builder = AlertDialog.Builder(activity)
			builder.setTitle(title)
			builder.setMessage(question)
			builder.setPositiveButton(android.R.string.yes) { dialog, which ->
				Log.d(TAG, "yes")
				deferred.resolve(true)
				dialog.dismiss()
			}
			builder.setNegativeButton(android.R.string.no) { dialog, which ->
				Log.d(TAG, "no")
				deferred.resolve(false)
				dialog.dismiss()
			}
			builder.setOnCancelListener {
				Log.d(TAG, "canceled")
				// triggers when clicked next to dialog, or back button
				deferred.resolve(false)
			}
			builder.show()
			return@promiseOnUi deferred.promise
		}.unwrap()
	}

	fun selectOptionAlert(activity: Activity, title: String, options: List<String>): Promise<Int, Exception> {
		return promiseOnUi {
			val deferred = deferred<Int, Exception>()
			val adapter = ArrayAdapter<String>(activity, android.R.layout.select_dialog_item)
			for (opt in options) {
				adapter.add(opt)
			}
			val builder = AlertDialog.Builder(activity)
			builder.setTitle(title)
			builder.setAdapter(adapter) { dialog, which ->
				Log.i(TAG, "clicked $which == ${adapter.getItem(which)}")
				deferred.resolve(which)
			}
			builder.setNegativeButton(android.R.string.cancel) { dialog, which ->
				Log.d(TAG, "negative button")
				deferred.reject(Exception("Canceled"))
				dialog.dismiss()
			}
			builder.setOnCancelListener {
				Log.d(TAG, "canceled")
				// triggers when clicked next to dialog, or back button
				deferred.reject(Exception("Canceled"))
			}
			builder.setOnDismissListener {
				Log.d(TAG, "dismissed")
				// tiggers when dialog goes away (either via button, or cancel)
			}
			builder.show()
			return@promiseOnUi deferred.promise
		}.unwrap()
	}

	/**
	 * Show the list of spheres and let the user pick one.
	 *
	 * @param activity The activity to show the dialog.
	 * @param return Promise with the selected sphere when resolved.
	 */
	fun selectSphereAlert(activity: Activity): Promise<SphereData, Exception> {
		val sphereArray = ArrayList(sphere.spheres.values)
		val namesArray = ArrayList<String>()
		for (sphereData in sphereArray) {
			namesArray.add(sphereData.name)
		}
		return selectOptionAlert(activity, "Select a sphere", namesArray)
				.then {
					return@then sphereArray[it]
				}
	}

	fun testOldFirmware(device: ScannedDevice, activity: Activity): Promise<Unit, Exception> {
		return bluenet.connect(device.address)
				.then {
					bluenet.deviceInfo(device.address).getFirmwareVersion()
				}.unwrap()
				.then {
					showResult("Firmware version $it", activity)
					return@then when (device.operationMode) {
						OperationMode.SETUP -> setup(device, activity)
						else -> Promise.ofFail<Unit, Exception>(Errors.NotInMode(CrownstoneMode.SETUP))
					}
				}.unwrap()
				.then {
					// Wait some time for the crownstone to reboot.
					bluenet.waitPromise(1000)
				}.unwrap()
				.then {
					bluenet.connect(device.address)
				}.unwrap()
				.then {
					bluenet.state(device.address).getTime()
				}.unwrap()
				.then {
					showResult("Time: $it", activity)
					bluenet.control(device.address).setTime(1234U)
				}.unwrap()
				.then {
					bluenet.config(device.address).setTxPower(0)
				}.unwrap()
				.then {
					bluenet.config(device.address).getTxPower()
				}.unwrap()
				.then {
					if (it.toInt() != 0) {
						return@then Promise.ofFail<Unit, Exception>(Errors.ValueWrong("Expected TX power of 0"))
					}
					showResult("TX power: $it", activity)
					bluenet.control(device.address).factoryReset()
				}.unwrap()
				.then {
					setup(device, activity)
				}.unwrap()
				.then {
					// Wait some time for the crownstone to reboot.
					bluenet.waitPromise(1000)
				}.unwrap()
				.then {
					bluenet.connect(device.address)
				}.unwrap()
				.then {
					bluenet.control(device.address).goToDfu()
				}.unwrap()
				.success { showResult("Test success", activity) }
				.fail { showResult("Test failed: $it", activity) }
	}

	// Test connecting and reading multiple devices.
	fun test(devices: List<ScannedDevice>, activity: Activity?) {
//		handler.postDelayed(testBluenetRunnable, 100000)
		devices.sortedByDescending { it.rssi }
		val count = min(5, devices.size)

		if (activity == null) {
			return
		}

		// Connect to all in parallel.
		val startTime = SystemClock.elapsedRealtime()
		for (index in 0 until count) {
			val auto = true
			Log.i(TAG, "connect to ${devices[index].address} rssi=${devices[index].rssi}")
			bluenet.connect(devices[index].address, auto, 60*1000)
					.success {
						showResult("Connected to ${devices[index].address} after ${SystemClock.elapsedRealtime() - startTime} ms", activity)
						if (devices[index].operationMode == OperationMode.SETUP) {
							setup(devices[index], activity)
									.then {
										bluenet.connect(devices[index].address, !auto, 60 * 1000)
									}.unwrap()
									.then {
										readData(devices[index], activity)
									}.unwrap()
									.then {
										bluenet.disconnect(devices[index].address)
									}.unwrap()
									.then {
										bluenet.connect(devices[index].address, auto, 60 * 1000)
									}.unwrap()
									.then {
										factoryReset(devices[index], activity, true)
									}.unwrap()
									.then {
										bluenet.connect(devices[index].address, !auto, 60 * 1000)
									}.unwrap()
									.then {
										bluenet.control(devices[index].address).toggleSwitch(100U)
									}.unwrap()
									.always {
										bluenet.disconnect(devices[index].address)
									}
						}
					}
					.fail {
						showResult("Failed to connect to ${devices[index].address}: ${it.message} after ${SystemClock.elapsedRealtime() - startTime} ms", activity)
					}
		}

//		// Abort all connections after random amount of time
//		bluenet.waitPromise((100L..3000L).random())
//				.success {
//					for (index in 0 until count) {
//						bluenet.abort(devices[index].address)
//					}
//				}
	}

	private fun connectNext(devices: List<ScannedDevice>, activity: Activity, index: Int, maxIndex: Int) {
		if (index > maxIndex) {
			// Read after all connections were made.
			readNext(devices, activity, 0, maxIndex)
			return
		}
		bluenet.connect(devices[index].address)
				.success {
					showResult("Connected to ${devices[index].address}", activity)
				}
				.fail {
					showResult("Failed to connect to ${devices[index].address}: ${it.message}", activity)
				}
				.always {
					connectNext(devices, activity, index + 1, maxIndex)
				}
	}

	private fun readNext(devices: List<ScannedDevice>, activity: Activity, index: Int, maxIndex: Int) {
		if (index > maxIndex) {
			return
		}
		readData(devices[index], activity)
//				.always {
//					// Read 1 by 1.
//					readNext(devices, activity, index + 1, maxIndex)
//				}

		// Read all in parallel.
		readNext(devices, activity, index + 1, maxIndex)
	}

	private fun readData(device: ScannedDevice, activity: Activity?): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		bluenet.debugData(device.address).getPowerSamples(PowerSamplesType.NOW_UNFILTERED)
				.success {
					showResult("Read from ${device.address} $it", activity)
					deferred.resolve(Unit)
				}
				.fail {
					showResult("Failed to read ${device.address}: ${it.message}", activity)
					deferred.reject(it)
				}
		return deferred.promise
	}

	companion object {
		lateinit var instance: MainApp
	}
}
