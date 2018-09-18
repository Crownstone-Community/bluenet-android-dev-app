package rocks.crownstone.dev_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
	private val TAG = this.javaClass.canonicalName

//	private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
//		when (item.itemId) {
//
//			R.id.navigation_home -> {
//				messageView.setText(R.string.title_home)
//				return@OnNavigationItemSelectedListener true
//			}
//			R.id.navigation_dashboard -> {
//				messageView.setText(R.string.title_dashboard)
//				return@OnNavigationItemSelectedListener true
//			}
//			R.id.navigation_notifications -> {
//				messageView.setText(R.string.title_notifications)
//				return@OnNavigationItemSelectedListener true
//			}
//		}
//		false
//	}

	private val mOnNavigationItemSelectedListener = object : BottomNavigationView.OnNavigationItemSelectedListener {
		override fun onNavigationItemSelected(item: MenuItem): Boolean {
			when (item.itemId) {
				R.id.navigation_home -> {
					messageView.setText(R.string.title_home)
					return true
				}
				R.id.navigation_dashboard -> {
					messageView.setText(R.string.title_dashboard)
					return true
				}
				R.id.navigation_notifications -> {
					messageView.setText(R.string.title_notifications)
					return true
				}
			}
			return false
		}
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		navigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
		setContentView(R.layout.activity_tabbed)


		MainApp.instance.bluenet.makeScannerReady(this)
				.success {
					Log.i(TAG, "start scanning")
					MainApp.instance.bluenet.startScanning()
				}
				.fail {
					Log.w(TAG, "unable to start scanning: $it")
					it.printStackTrace()
				}

//		val intent = Intent(this, TabbedActivity::class.java)
//		val intent = Intent(this, LoginActivity::class.java)
//		this.startActivity(intent)

	}




	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (MainApp.instance.bluenet.handleActivityResult(requestCode, resultCode, data)) {
			return
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		if (MainApp.instance.bluenet.handlePermissionResult(requestCode, permissions, grantResults)) {
			return
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}
}
