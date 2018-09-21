package rocks.crownstone.dev_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap

class MainActivity : AppCompatActivity() {
	private val TAG = this.javaClass.canonicalName

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Log.i(TAG, "onCreate")
		setContentView(R.layout.activity_main)

//		val intent = Intent(this, TabbedActivity::class.java)
//		val intent = Intent(this, LoginActivity::class.java)
//		this.startActivity(intent)

//		MainApp.instance.bluenet.subscribe(BluenetEvent.INITIALIZED, ::onBluenetInitialized)
//		MainApp.instance.bluenet.subscribe(BluenetEvent.SCANNER_READY, ::onScannerReady)

		Log.i(TAG, "init bluenet")
//		MainApp.instance.bluenet.init(this)
		MainApp.instance.bluenet.init(applicationContext)
				.then {
					Log.i(TAG, "make scanner ready")
					MainApp.instance.bluenet.makeScannerReady(this)
				}.unwrap()
				.then {
					Log.i(TAG, "start scanning")
					MainApp.instance.bluenet.startScanning()
				}
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

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		Log.i(TAG, "onActivityResult $requestCode $resultCode")
		if (MainApp.instance.bluenet.handleActivityResult(requestCode, resultCode, data)) {
			return
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		Log.i(TAG, "onRequestPermissionsResult $requestCode")
		if (MainApp.instance.bluenet.handlePermissionResult(requestCode, permissions, grantResults)) {
			return
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}
}
