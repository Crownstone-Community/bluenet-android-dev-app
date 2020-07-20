package rocks.crownstone.dev_app.ui.control

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesType
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.dev_app.MainApp
import rocks.crownstone.dev_app.R

/**
 * Fragment with the debug commands.
 */
class DebugFragment : Fragment() {
	private val TAG = this.javaClass.simpleName

	private lateinit var pageViewModel: PageViewModel
	private var deviceAddress: String? = null

	private lateinit var rootView: View
	private var viewGroup: ViewGroup? = null

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
		val root = inflater.inflate(R.layout.fragment_debug, container, false)
		rootView = root
		viewGroup = container
		val textView: TextView = root.findViewById(R.id.section_label)
		pageViewModel.text.observe(this, Observer<String> {
			textView.text = it
		})

		root.findViewById<Button>(R.id.buttonFirmwareVersion).setOnClickListener {
			getFirmwareVersion(root.findViewById<TextView>(R.id.textViewFirmwareVersion))
		}
		root.findViewById<Button>(R.id.buttonBootloaderVersion).setOnClickListener {
			getBootloaderVersion(root.findViewById<TextView>(R.id.textViewBootloaderVersion))
		}
		root.findViewById<Button>(R.id.buttonHardwareVersion).setOnClickListener {
			getHardwareVersion(root.findViewById<TextView>(R.id.textViewHardwareVersion))
		}
		root.findViewById<Button>(R.id.buttonUicrData).setOnClickListener {
			getUicrData(root.findViewById<TextView>(R.id.textViewUicrData))
		}
		root.findViewById<Button>(R.id.buttonUptime).setOnClickListener {
			getUptime(root.findViewById<TextView>(R.id.textViewUptime))
		}
		root.findViewById<Button>(R.id.buttonTime).setOnClickListener {
			getTime(root.findViewById<TextView>(R.id.textViewTime))
		}
		root.findViewById<Button>(R.id.buttonAdcRestarts).setOnClickListener {
			getAdcRestarts(root.findViewById<TextView>(R.id.textViewAdcRestarts))
		}
		root.findViewById<Button>(R.id.buttonSwitchHistory).setOnClickListener {
			getSwitchHistory(root.findViewById<TextView>(R.id.textViewSwitchHistory))
		}
		root.findViewById<Button>(R.id.buttonSwitchcraftSamples).setOnClickListener {
			getPowerSamples(root.findViewById<TextView>(R.id.textViewSwitchcraftSamples), PowerSamplesType.SWITCHCRAFT)
		}
		root.findViewById<Button>(R.id.buttonFilteredPowerSamples).setOnClickListener {
			getPowerSamples(root.findViewById<TextView>(R.id.textViewFilteredPowerSamples), PowerSamplesType.NOW_FILTERED)
		}
		root.findViewById<Button>(R.id.buttonUnfilteredPowerSamples).setOnClickListener {
			getPowerSamples(root.findViewById<TextView>(R.id.textViewUnfilteredPowerSamples), PowerSamplesType.NOW_UNFILTERED)
		}
		root.findViewById<Button>(R.id.buttonSoftFusePowerSamples).setOnClickListener {
			getPowerSamples(root.findViewById<TextView>(R.id.textViewSoftFusePowerSamples), PowerSamplesType.SOFT_FUSE)
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
		fun newInstance(sectionNumber: Int, deviceAddress: String?): DebugFragment {
			return DebugFragment().apply {
				arguments = Bundle().apply {
					putInt(ARG_SECTION_NUMBER, sectionNumber)
					putString(ARG_DEVICE_ADDRESS, deviceAddress)
				}
			}
		}
	}

	private fun getFirmwareVersion(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.deviceInfo.getFirmwareVersion()
				}.unwrap()
				.successUi {
					view.text = it
					showResult("Firmware version: $it")
				}
				.fail { showResult("Get firmware version failed: ${it.message}") }
	}

	private fun getBootloaderVersion(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.deviceInfo.getBootloaderVersion()
				}.unwrap()
				.successUi {
					view.text = it
					showResult("Bootloader version: $it")
				}
				.fail { showResult("Get bootloader version failed: ${it.message}") }
	}

	private fun getHardwareVersion(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.deviceInfo.getHardwareVersion()
				}.unwrap()
				.successUi {
					view.text = it
					showResult("Hardware version: $it")
				}
				.fail { showResult("Get hardware version failed: ${it.message}") }
	}

	private fun getUicrData(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.deviceInfo.getUicrData()
				}.unwrap()
				.successUi {
					view.text = it.toString()
					showResult("UICR data: $it")
				}
				.fail { showResult("Get UICR data failed: ${it.message}") }
	}

	private fun getUptime(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.debugData.getUptime()
				}.unwrap()
				.successUi {
					view.text = it.toString()
					showResult("Uptime: $it")
				}
				.fail { showResult("Get uptime failed: ${it.message}") }
	}

	private fun getTime(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.state.getTime()
				}.unwrap()
				.successUi {
					val timestampStr = Util.getTimestampString(it)
					view.text = "$timestampStr $it"
					showResult("Time: $timestampStr $it")
				}
				.fail { showResult("Get time failed: ${it.message}") }
	}

	private fun getAdcRestarts(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.debugData.getAdcRestarts()
				}.unwrap()
				.successUi {
					view.text = it.toString()
					showResult("ADC restarts: $it")
				}
				.fail { showResult("Get ADC restarts failed: ${it.message}") }
	}

	private fun getSwitchHistory(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.debugData.getSwitchHistory()
				}.unwrap()
				.successUi {
					view.text = it.toString()
					showResult("Switch history: $it")
				}
				.fail { showResult("Get switch history failed: ${it.message}") }
	}

	private fun getPowerSamples(view: TextView, type: PowerSamplesType) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.debugData.getPowerSamples(type)
				}.unwrap()
				.successUi {
					val timestampStr = Util.getTimestampString(it[0].timestamp)
					Log.i(TAG, "Power samples: $it")
					view.text = "$timestampStr $it"
					showResult("Power samples $timestampStr $it")
				}
				.fail { showResult("Get power samples failed: ${it.message}") }
	}



	private fun clearText(view: TextView) {
		activity?.runOnUiThread {
			view.text = ""
		}
	}

	private fun showResult(text: String) {
		val activ = activity ?: return
		MainApp.instance.showResult(text, activ)
	}
}