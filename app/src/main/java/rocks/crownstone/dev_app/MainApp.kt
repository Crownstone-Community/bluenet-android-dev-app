package rocks.crownstone.dev_app

import android.app.Application
import android.os.Handler
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import rocks.crownstone.bluenet.*
import rocks.crownstone.dev_app.cloud.Spheres
import rocks.crownstone.dev_app.cloud.User

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


class MainApp : Application() {
	private val TAG = MainApp::class.java.canonicalName
//	val volleyQueue = Volley.newRequestQueue(this)
	lateinit var volleyQueue: RequestQueue
	lateinit var user: User
	lateinit var spheres: Spheres
	val bluenet = Bluenet()

	val handler = Handler()
	var testHandlerNextNum = 0

	override fun onCreate() {
		super.onCreate()
		Log.i(TAG, "onCreate")
		instance = this
		startKovenant() // Start thread(s)
		volleyQueue = Volley.newRequestQueue(this)
		user = User(this, volleyQueue)
		spheres = Spheres(this, volleyQueue)

		bluenet.init(instance)

//		service = BleServiceManager(this, eventBus)
//		service.runInBackground()
//
//		bleCore = BleCore(this, eventBus)
//		bleCore.init()
//		bleScanner = BleScanner(eventBus, bleCore)


		val test = TestKovenant()
		test.test()

		testHandlerNextNum = 0
		for (i in 0 until 1000000) {
//		for (i in 0..1000 step 2) {
			handler.post {
				if (testHandlerNextNum != i) {
					Log.e(TAG, "$i not ordered!")
				}
				testHandlerNextNum = i+1
			}
		}
	}

	override fun onTerminate() {
		super.onTerminate()
		stopKovenant() // Stop thread(s)
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