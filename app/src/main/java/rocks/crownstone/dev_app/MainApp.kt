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
import rocks.crownstone.bluenet.packets.behaviour.IndexedBehaviourPacket
import rocks.crownstone.bluenet.scanhandling.NearestDeviceListEntry
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.*
import rocks.crownstone.dev_app.cloud.*
import java.util.*


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

	var nearestDeviceAddress: DeviceAddress? = null
	var handler = Handler()

	var switchCmd = 100

	// Device selected from list.
	var selectedDevice: ScannedDevice? = null

	// Dev sphere.
	val devSphereId = "devSphere"
	val devKeySet = KeySet("adminKeyForCrown", "memberKeyForHome", "guestKeyForOther", "basicKeyForOther", "LocalizationKeyX")
	val devMeshKeySet = MeshKeySet("aStoneKeyForMesh", "meshAppForStones", "meshKeyForStones")
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

		bluenet.subscribe(BluenetEvent.SCAN_RESULT, ::onScan)
		bluenet.subscribe(BluenetEvent.NEAREST_VALIDATED_NORMAL, ::onNearest)

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

	private fun onScan(data: Any?) {
		if (data == null) {
			return
		}
		val device = data as ScannedDevice
		Log.v(TAG, "onScan: $device")
	}

	private fun onNearest(data: Any?) {
		if (data == null) {
			return
		}
		val nearest = data as NearestDeviceListEntry
		Log.d(TAG, "nearest=${nearest.deviceAddress}")
		nearestDeviceAddress = nearest.deviceAddress
	}

	private val testBluenetRunnable = Runnable {
		val address = nearestDeviceAddress
		testBluenet(address)
	}

	private fun testBluenet(address: DeviceAddress?) {
//		if (address == null) {
//			handler.postDelayed(testBluenetRunnable, 1000)
//			return
//		}
//		Log.i(TAG, "---- connect ----")
//		bluenet.connect(address)
//				.then {
//					Log.i(TAG, "---- discover services ----")
//					bluenet.discoverServices()
//				}.unwrap()
//				.then {
//					Log.i(TAG, "---- read ----")
//					bluenet.read(BluenetProtocol.DEVICE_INFO_SERVICE_UUID, BluenetProtocol.CHAR_FIRMWARE_REVISION_UUID)
//				}.unwrap()
//				.then {
//					Log.i(TAG, "---- write ----")
//					val writeData = ByteArray(1)
//					writeData[0] = 5
//					bluenet.write(BluenetProtocol.CROWNSTONE_SERVICE_UUID, BluenetProtocol.CHAR_CONTROL_UUID, writeData)
//				}.unwrap()
//				.then {
//					Log.i(TAG, "---- disconnect ----")
//					bluenet.disconnect(true)
//				}.unwrap()
//				.success {
//					Log.i(TAG, "---- success ----")
//					handler.post(connectRunnable)
//				}
//				.fail {
//					Log.e(TAG, "---- error: ${it.message} ----")
//					handler.postDelayed(connectRunnable, 100)
//				}
//				.always {
//
//				}
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
		bluenet.stopScanning()
		if (usingDevSphere) {
			val stoneId = (1..255).random().toUint8()
			val major = (1..60000).random().toUint16()
			val minor = (1..60000).random().toUint16()
			val ibeaconData = IbeaconData(devIbeaconUuid, major, minor, -60)
			return bluenet.connect(device.address)
					.then {
						bluenet.setup.setup(stoneId, devSphereShortId, devKeySet, devMeshKeySet, devMeshAccessAddress, ibeaconData)
					}.unwrap()
					.success {
						Log.i(TAG, "Setup complete!")
						showResult("setup success", activity)
					}
					.fail {
						Log.e(TAG, "Setup failed: ${it.message}")
						it.printStackTrace()
						showResult("setup failed: ${it.message}", activity)
						bluenet.disconnect()
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
						bluenet.setup.setup(stoneId.toUint8(), sphere?.uid!!.toUint8(), keySet, meshKeySet, meshAccessAddress, ibeaconData)
					}.unwrap()
					.success {
						Log.i(TAG, "Setup complete!")
						showResult("setup success", activity)
					}
					.fail {
						Log.e(TAG, "Setup failed: ${it.message}")
						it.printStackTrace()
						showResult("setup failed: ${it.message}", activity)
						bluenet.disconnect()
					}
					.always {
//						bluenet.startScanning()
					}
		}
	}

	fun factoryReset(device: ScannedDevice, activity: Activity): Promise<Unit, Exception> {
		if (device.sphereId != devSphereId) {
			Log.i(TAG, "Refuse factory reset. SphereId=${device.sphereId}")
			showResult("Don't factory reset stones that are not in the dev sphere. SphereId=${device.sphereId}", activity)
			return Promise.ofFail(Exception("Don't factory reset stones that are not in the dev sphere. SphereId=${device.sphereId}"))
		}
		return confirmAlert(activity, "factory reset", "Do you want to factory reset this device?")
				.then { confirmed ->
					if (!confirmed) {
						return@then Promise.ofSuccess<Unit, Exception>(Unit)
					}
					bluenet.connect(device.address)
							.then {
								bluenet.control.factoryReset()
							}.unwrap()
							.success {
								Log.i(TAG, "factory reset success")
								showResult("factory reset success", activity)
								bluenet.disconnect(true)
										.then {
											removeStoneFromCloud(device)
										}.unwrap()
										.success {
											Log.i(TAG, "removed from cloud")
											showResult("removed from cloud", activity)
										}
										.fail {
											Log.e(TAG, "failed to remove from cloud: ${it.message}")
											showResult("failed to remove from cloud: ${it.message}", activity)
										}
							}
							.fail {
								Log.e(TAG, "factory reset failed: ${it.message}")
								showResult("factory reset failed: ${it.message}", activity)
								bluenet.disconnect(true)
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

	fun showResult(msg: String, activity: Activity) {
		Log.i(TAG, msg)
		activity.runOnUiThread {
			Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
		}
	}


	fun confirmAlert(activity: Activity, title: String, question: String): Promise<Boolean, Exception> {
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

	companion object {
		lateinit var instance: MainApp
	}
}
