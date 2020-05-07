package rocks.crownstone.dev_app.ui.control

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import nl.komponents.kovenant.then
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.toUint8
import rocks.crownstone.dev_app.MainApp
import rocks.crownstone.dev_app.R

/**
 * Fragment with the basic control commands.
 */
class ControlFragment : Fragment() {
	private val TAG = this.javaClass.simpleName

	private lateinit var pageViewModel: PageViewModel
	private var deviceAddress: String? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
			setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
		}
		deviceAddress = arguments?.getString(ARG_DEVICE_ADDRESS)
		Log.i(TAG, "deviceAddress=$deviceAddress")
	}

	override fun onCreateView(
			inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(R.layout.fragment_control, container, false)
		val textView: TextView = root.findViewById(R.id.section_label)
		pageViewModel.text.observe(this, Observer<String> {
			textView.text = it
		})

		root.findViewById<Button>(R.id.buttonOn).setOnClickListener {
			setSwitch(100U)
		}

		root.findViewById<Button>(R.id.buttonOff).setOnClickListener {
			setSwitch(0U)
		}

		root.findViewById<SeekBar>(R.id.dimmerSlider).apply {
			max = 100
			setOnSeekBarChangeListener(
					dimmerSliderListener
			)
		}

		root.findViewById<Button>(R.id.buttonBroadcastOn).setOnClickListener {
			broadcastSwitchOn()
		}

		root.findViewById<Button>(R.id.buttonBroadcastOff).setOnClickListener {
			broadcastSwitch(0U)
		}

		root.findViewById<SeekBar>(R.id.dimmerBroadcastSlider).apply {
			max = 100
			setOnSeekBarChangeListener(
					dimmerBroadcastSliderListener
			)
		}


		return root
	}

	companion object {
		private const val ARG_SECTION_NUMBER = "section_number"
		private const val ARG_DEVICE_ADDRESS = "device_address"

		/**
		 * Returns a new instance of this fragment.
		 */
		@JvmStatic
		fun newInstance(sectionNumber: Int, deviceAddress: String?): ControlFragment {
			return ControlFragment().apply {
				arguments = Bundle().apply {
					putInt(ARG_SECTION_NUMBER, sectionNumber)
					putString(ARG_DEVICE_ADDRESS, deviceAddress)
				}
			}
		}
	}

	private fun setSwitch(value: Uint8) {
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address).then {
			MainApp.instance.bluenet.control.setSwitch(value)
		}
	}

	private val dimmerSliderListener = object : SeekBar.OnSeekBarChangeListener {
		override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
			val switchVal = progress.toUint8()
			setSwitch(switchVal)
		}

		override fun onStartTrackingTouch(seekBar: SeekBar?) {
		}

		override fun onStopTrackingTouch(seekBar: SeekBar?) {
		}
	}

	private fun broadcastSwitchOn() {
		val device = MainApp.instance.selectedDevice ?: return
		val sphereId = device.sphereId ?: return
		val stoneId = device.serviceData?.crownstoneId ?: return
		MainApp.instance.bluenet.broadCast.switchOn(sphereId, stoneId)
	}

	private fun broadcastSwitch(value: Uint8) {
		val device = MainApp.instance.selectedDevice ?: return
		val sphereId = device.sphereId ?: return
		val stoneId = device.serviceData?.crownstoneId ?: return
		MainApp.instance.bluenet.broadCast.switch(sphereId, stoneId, value)
	}

	private val dimmerBroadcastSliderListener = object : SeekBar.OnSeekBarChangeListener {
		override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
			val switchVal = progress.toUint8()
			broadcastSwitch(switchVal)
		}

		override fun onStartTrackingTouch(seekBar: SeekBar?) {
		}

		override fun onStopTrackingTouch(seekBar: SeekBar?) {
		}
	}
}