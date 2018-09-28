package rocks.crownstone.dev_app

import android.app.Application
import android.arch.lifecycle.*
//import android.arch.lifecycle.ProcessLifecycleOwner
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.dev_app.cloud.Spheres
import rocks.crownstone.dev_app.cloud.User
import java.util.*

// Singleton class that is accessible in all activities
//object MainApp : Application() {
//	private val TAG = MainApp::class.java.canonicalName
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
	private val TAG = MainApp::class.java.canonicalName
//	val volleyQueue = Volley.newRequestQueue(this)
	lateinit var volleyQueue: RequestQueue
	lateinit var user: User
	lateinit var spheres: Spheres
	val bluenet = Bluenet()


	override fun onCreate() {
		super<Application>.onCreate()
		Log.i(TAG, "onCreate")
		instance = this
		startKovenant() // Start thread(s)
		volleyQueue = Volley.newRequestQueue(this)
		user = User(this, volleyQueue)
		spheres = Spheres(this, volleyQueue)

		ProcessLifecycleOwner.get().lifecycle.addObserver(this)

		bluenet.subscribe(BluenetEvent.SCAN_RESULT, ::onScan)

//		bluenet.init(instance)
//				.success {
//					Log.i(TAG, "bluenet initialized")
//				}
//				.fail {
//					Log.e(TAG, "bluenet init failed: $it")
//				}

//		val test = TestKovenant()
//		test.test()


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
			bluenet.startScanning()
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
	fun onAppBackgrounded() {
		Log.i(TAG, "onAppBackgrounded")
		if (bluenet.isScannerReady()) {
			bluenet.stopScanning()
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
		if (device.validated) {
			Log.i(TAG, "validated: $device")
		}
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