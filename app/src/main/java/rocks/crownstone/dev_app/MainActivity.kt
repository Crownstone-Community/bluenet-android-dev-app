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
	val REQUEST_LOCATION = 1

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
//		setContentView(R.layout.activity_main)
//		navigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
//		setContentView(R.layout.activity_tabbed)


		if (Build.VERSION.SDK_INT >= 23) {
//			when (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
//				PackageManager.PERMISSION_GRANTED -> MainApp.instance.bleScanner.startScan()
//				else -> requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION)
//			}
		}
		else {
//			MainApp.instance.bleScanner.startScan()
		}

//		val intent = Intent(this, TabbedActivity::class.java)
		val intent = Intent(this, LoginActivity::class.java)
		this.startActivity(intent)

	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		when (requestCode) {
			REQUEST_LOCATION -> {
				if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
					Log.d(TAG, "onRequestPermissionsResult PERMISSION_GRANTED")
//					MainApp.instance.bleScanner.startScan()
				}
				else {
					Log.d(TAG, "onRequestPermissionsResult PERMISSION_DENIED")
				}
			}
			else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		}
	}
}
