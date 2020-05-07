package rocks.crownstone.dev_app

import android.os.Bundle
import android.util.Log
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import rocks.crownstone.dev_app.ui.control.SectionsPagerAdapter

class ControlActivity : AppCompatActivity() {
	private val TAG = this.javaClass.simpleName

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_control)

		val deviceAddress = intent.getStringExtra("deviceAddress")
		Log.i(TAG, "deviceAddress=$deviceAddress")

		val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager, intent.extras)
		val viewPager: ViewPager = findViewById(R.id.view_pager)
		viewPager.adapter = sectionsPagerAdapter

		val tabs: TabLayout = findViewById(R.id.tabs)
		tabs.setupWithViewPager(viewPager)
	}
}
