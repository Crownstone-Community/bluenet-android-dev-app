package rocks.crownstone.dev_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.structs.KeyData
import rocks.crownstone.bluenet.structs.Keys
import rocks.crownstone.bluenet.encryption.KeySet
import java.util.*

class MainActivity : AppCompatActivity() {
	private val TAG = this.javaClass.simpleName

	private val REQUEST_CODE_LOGIN = 1

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Log.i(TAG, "onCreate")
		setContentView(R.layout.activity_main)

//		val intent = Intent(this, TabbedActivity::class.java)
		val intent = Intent(this, LoginActivity::class.java)
//		this.startActivity(intent)
		this.startActivityForResult(intent, REQUEST_CODE_LOGIN)


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
//					MainApp.instance.bluenet.filterForIbeacons(true)
					MainApp.instance.bluenet.filterForCrownstones(true)
					Log.i(TAG, "start scanning")
//					MainApp.instance.bluenet.startScanning()
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

	fun onLogin(resultCode: Int) {
		Log.i(TAG, "onLogin result=$resultCode")

		val spheres = MainApp.instance.sphere.spheres
		val keys = Keys()
		for (sphere in spheres.values) {
			val keySet = KeySet(sphere.keySet?.adminKey, sphere.keySet?.memberKey, sphere.keySet?.guestKey, sphere.keySet?.serviceDataKey, sphere.keySet?.meshAppKey, sphere.keySet?.meshNetKey)
			val uuid = UUID.fromString(sphere.iBeaconUUID)
//			MainApp.instance.bluenet.addIbeaconFilter(uuid)
			val keyData = KeyData(keySet, uuid)
			keys.put(sphere.id, keyData)
			MainApp.instance.bluenet.iBeaconRanger.track(uuid, sphere.id)
		}
		MainApp.instance.bluenet.loadKeys(keys)
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
}
