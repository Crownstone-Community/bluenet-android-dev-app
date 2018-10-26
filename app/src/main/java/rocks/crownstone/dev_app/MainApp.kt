package rocks.crownstone.dev_app

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.*
import android.content.Context
import android.os.Handler
import android.support.v7.app.AlertDialog
//import android.arch.lifecycle.ProcessLifecycleOwner
import android.util.Log
import android.widget.ArrayAdapter
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import nl.komponents.kovenant.deferred
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.dev_app.cloud.Sphere
import rocks.crownstone.dev_app.cloud.Stone
import rocks.crownstone.dev_app.cloud.User
import rocks.crownstone.dev_app.cloud.SphereData



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
//	val volleyQueue = Volley.newRequestQueue(this)
	lateinit var volleyQueue: RequestQueue
	lateinit var user: User
	lateinit var sphere: Sphere
	lateinit var stone: Stone
	val bluenet = Bluenet()

	var nearestDeviceAddress: DeviceAddress? = null
	var handler = Handler()

	override fun onCreate() {
		super<Application>.onCreate()
		Log.i(TAG, "onCreate")
		instance = this
		startKovenant() // Start thread(s)
		volleyQueue = Volley.newRequestQueue(this)
		user = User(this, volleyQueue)
		sphere = Sphere(this, volleyQueue)
		stone = Stone(this, volleyQueue)

		ProcessLifecycleOwner.get().lifecycle.addObserver(this)

		bluenet.subscribe(BluenetEvent.SCAN_RESULT, ::onScan)
		bluenet.subscribe(BluenetEvent.NEAREST_VALIDATED_NORMAL, ::onNearest)

//		handler.postDelayed(testBluenetRunnable, 1000)

		val testKovenant = TestKovenant()
		testKovenant.testRecover()

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
//		if (bluenet.isScannerReady()) {
//			bluenet.startScanning()
//		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
	fun onAppBackgrounded() {
		Log.i(TAG, "onAppBackgrounded")
//		if (bluenet.isScannerReady()) {
//			bluenet.stopScanning()
//		}
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

	private fun onNearest(data: Any) {
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

	/**
	 * Show the list of spheres and let the user pick one.
	 *
	 * @param activity The activity to show the dialog.
	 * @param return Promise with the selected sphere when resolved.
	 */
	fun selectSphereAlert(activity: Activity): Promise<SphereData, Exception> {
		val deferred = deferred<SphereData, Exception>()
		val adapter = ArrayAdapter<String>(activity, android.R.layout.select_dialog_item)
		val sphereArray = ArrayList(sphere.spheres.values)
		for (sphereData in sphereArray) {
			adapter.add(sphereData.name)
		}
		val builder = AlertDialog.Builder(activity)
		builder.setTitle("Select a sphere")
		builder.setAdapter(adapter) { dialog, which ->
			Log.i(TAG, "clicked $which ${sphereArray[which].name} == ${adapter.getItem(which)}")
			deferred.resolve(sphereArray[which])
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
		return deferred.promise
	}




	//	companion object {
//		private val _instance: MainApp = MainApp()
//
//		@Synchronized
//		fun getInstance(): MainApp {
//			return _instance
//		}
//	}
	companion object {
		lateinit var instance: MainApp
	}
}