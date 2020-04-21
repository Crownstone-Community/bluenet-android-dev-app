package rocks.crownstone.dev_app

import com.google.android.material.tabs.TabLayout
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

//import kotlinx.android.synthetic.main.activity_tabbed.toolbarWidget
//import kotlinx.android.synthetic.main.activity_tabbed.appbarLayout
//import kotlinx.android.synthetic.main.activity_tabbed.containerViewPager
//import kotlinx.android.synthetic.main.activity_tabbed.fabButton
//import kotlinx.android.synthetic.main.activity_tabbed.tabsLayout
import kotlinx.android.synthetic.main.activity_tabbed.*
import kotlinx.android.synthetic.main.fragment_tabbed.view.*

class TabbedActivity : AppCompatActivity() {

	/**
	 * The [android.support.v4.view.PagerAdapter] that will provide
	 * fragments for each of the sections. We use a
	 * {@link FragmentPagerAdapter} derivative, which will keep every
	 * loaded fragment in memory. If this becomes too memory intensive, it
	 * may be best to switch to a
	 * [android.support.v4.app.FragmentStatePagerAdapter].
	 */
	private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_tabbed)

		setSupportActionBar(toolbarWidget)
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

		// Set up the ViewPager with the sections adapter.
		containerViewPager.adapter = mSectionsPagerAdapter

//		containerViewPager.addOnAdapterChangeListener(object: ViewPager.OnPageChangeListener {
//			override fun onPageScrollStateChanged(p0: Int) {
//
//			}
//
//			override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
//
//			}
//
//			override fun onPageSelected(p0: Int) {
//
//			}
//		})
		containerViewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabsLayout))
		tabsLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(containerViewPager))

		fabButton.setOnClickListener(object: View.OnClickListener {
			override fun onClick(view: View) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show()
			}
		})

	}


	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.menu_tabbed, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		val id = item.itemId

		if (id == R.id.action_settings) {
			return true
		}

		return super.onOptionsItemSelected(item)
	}


	/**
	 * A [FragmentPagerAdapter] that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	inner class SectionsPagerAdapter(fm: androidx.fragment.app.FragmentManager) : androidx.fragment.app.FragmentPagerAdapter(fm) {

		override fun getItem(position: Int): androidx.fragment.app.Fragment {
			when (position) {
				1 -> return ControlFragment()
			}
			// getItem is called to instantiate the fragment for the given page.
			// Return a PlaceholderFragment (defined as a static inner class below).
			return PlaceholderFragment.newInstance(position + 1)
		}

		override fun getCount(): Int {
			// Show 3 total pages.
			return 3
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	class PlaceholderFragment : androidx.fragment.app.Fragment() {

		override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
								  savedInstanceState: Bundle?): View? {
			val rootView = inflater.inflate(R.layout.fragment_tabbed, container, false)
			rootView.section_label.text = getString(R.string.section_format, arguments?.getInt(ARG_SECTION_NUMBER))
			return rootView
		}

		companion object {
			/**
			 * The fragment argument representing the section number for this
			 * fragment.
			 */
			private val ARG_SECTION_NUMBER = "section_number"

			/**
			 * Returns a new instance of this fragment for the given section
			 * number.
			 */
			fun newInstance(sectionNumber: Int): PlaceholderFragment {
				val fragment = PlaceholderFragment()
				val args = Bundle()
				args.putInt(ARG_SECTION_NUMBER, sectionNumber)
				fragment.arguments = args
				return fragment
			}
		}
	}
}
