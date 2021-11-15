package rocks.crownstone.dev_app.ui.control

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.structs.Int8
import rocks.crownstone.bluenet.structs.UartMode
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.bluenet.util.toUint8
import rocks.crownstone.dev_app.MainApp
import rocks.crownstone.dev_app.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment with the config commands.
 */
class ConfigFragment : Fragment() {
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
		val root = inflater.inflate(R.layout.fragment_config, container, false)
		val textView: TextView = root.findViewById(R.id.section_label)
		pageViewModel.text.observe(this, Observer<String> {
			textView.text = it
		})

		root.findViewById<Button>(R.id.buttonEnableDimming).setOnClickListener { enableDimming(true) }
		root.findViewById<Button>(R.id.buttonDisableDimming).setOnClickListener { enableDimming(false) }
		root.findViewById<Button>(R.id.buttonEnableSwitchcraft).setOnClickListener { enableSwitchcraft(true) }
		root.findViewById<Button>(R.id.buttonDisableSwitchcraft).setOnClickListener { enableSwitchcraft(false) }
		root.findViewById<Button>(R.id.buttonGetTime).setOnClickListener { getTime(root.findViewById<EditText>(R.id.editTime)) }
		root.findViewById<Button>(R.id.buttonSetTime).setOnClickListener { setTime(root.findViewById<EditText>(R.id.editTime)) }
		root.findViewById<Button>(R.id.buttonSetCurrentTime).setOnClickListener { setTime(null) }
		root.findViewById<Button>(R.id.buttonGetSoftOnSpeed).setOnClickListener { getSoftOnSpeed(root.findViewById<EditText>(R.id.editSoftOnSpeed)) }
		root.findViewById<Button>(R.id.buttonSetSoftOnSpeed).setOnClickListener { setSoftOnSpeed(root.findViewById<EditText>(R.id.editSoftOnSpeed)) }
		root.findViewById<Button>(R.id.buttonEnableUart).setOnClickListener { enableUart(true) }
		root.findViewById<Button>(R.id.buttonDisableUart).setOnClickListener { enableUart(false) }
		root.findViewById<Button>(R.id.buttonGetTxPower).setOnClickListener { getTxPower(root.findViewById<EditText>(R.id.editTxPower)) }
		root.findViewById<Button>(R.id.buttonSetTxPower).setOnClickListener { setTxPower(root.findViewById<EditText>(R.id.editTxPower)) }

		return root
	}

	companion object {
		private const val ARG_SECTION_NUMBER = "section_number"
		private const val ARG_DEVICE_ADDRESS = "device_address"

		/**
		 * Returns a new instance of this fragment.
		 */
		@JvmStatic
		fun newInstance(sectionNumber: Int, deviceAddress: String?): ConfigFragment {
			return ConfigFragment().apply {
				arguments = Bundle().apply {
					putInt(ARG_SECTION_NUMBER, sectionNumber)
					putString(ARG_DEVICE_ADDRESS, deviceAddress)
				}
			}
		}
	}

	private fun enableDimming(enable: Boolean) {
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.control.allowDimming(enable)
				}.unwrap()
				.success { showResult("Enable dimming success") }
				.fail { showResult("Enable dimming failed: ${it.message}") }
	}

	private fun enableSwitchcraft(enable: Boolean) {
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.config.setSwitchCraftEnabled(enable)
				}.unwrap()
				.success { showResult("Enable switchcraft success") }
				.fail { showResult("Enable switchcraft failed: ${it.message}") }
	}

	private fun getTime(editText: EditText) {
		editText.setText("")
		val device = MainApp.instance.selectedDevice ?: return

		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.state.getTime()
				}.unwrap()
				.success {
					val timestampStr = Util.getTimestampString(it)
					editText.setText("$it $timestampStr")
					showResult("Time: $it $timestampStr")
				}
				.fail { showResult("Get time failed: ${it.message}") }
	}

	private fun setTime(editText: EditText?) {
		val device = MainApp.instance.selectedDevice ?: return
		val timestamp = if (editText == null || editText.text.isBlank()) {
			Util.getLocalTimestamp()
		}
		else {
			editText.text.toString().toUIntOrNull() ?: Util.getLocalTimestamp()
		}
		val timestampStr = Util.getTimestampString(timestamp)
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.control.setTime(timestamp)
				}.unwrap()
				.success { showResult("Set time to $timestamp $timestampStr") }
				.fail { showResult("Set time failed: ${it.message}") }
	}

	private fun getSoftOnSpeed(editText: EditText) {
		editText.setText("")
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.config.getSoftOnSpeed()
				}.unwrap()
				.success {
					editText.setText("$it")
					showResult("Soft on speed: $it")
				}
				.fail { showResult("Get soft on speed failed: ${it.message}") }
	}

	private fun setSoftOnSpeed(editText: EditText) {
		val device = MainApp.instance.selectedDevice ?: return
		val speed: Uint8 = editText.text.toString().toUIntOrNull()?.toUint8() ?: 1U
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.config.setSoftOnSpeed(speed)
				}.unwrap()
				.success { showResult("Set soft on speed to $speed") }
				.fail { showResult("Set soft on speed failed: ${it.message}") }
	}

	private fun enableUart(enable: Boolean) {
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.config.setUartEnabled(if (enable) UartMode.RX_AND_TX else UartMode.NONE)
				}.unwrap()
				.success { showResult("Enable UART success") }
				.fail { showResult("Enable UART failed: ${it.message}") }
	}

	private fun getTxPower(editText: EditText) {
		editText.setText("")
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.config.getTxPower()
				}.unwrap()
				.success {
					editText.setText("$it")
					showResult("TX power: $it")
				}
				.fail { showResult("Get TX power failed: ${it.message}") }
	}

	private fun setTxPower(editText: EditText) {
		val device = MainApp.instance.selectedDevice ?: return
		val txPower: Int8 = editText.text.toString().toIntOrNull()?.toByte() ?: 4
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.config.setTxPower(txPower)
				}.unwrap()
				.success { showResult("Set TX power to $txPower") }
				.fail { showResult("Set TX power failed: ${it.message}") }
	}


	private fun showResult(text: String) {
		val activ = activity ?: return
		MainApp.instance.showResult(text, activ)
	}
}