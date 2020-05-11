package rocks.crownstone.dev_app.ui.control

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.structs.Errors
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.bluenet.util.toUint8
import rocks.crownstone.dev_app.MainApp
import rocks.crownstone.dev_app.R

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
		val editTestResets = root.findViewById<EditText>(R.id.editResets)
		root.findViewById<Button>(R.id.buttonGetResets).setOnClickListener { resetCount(false, editTestResets) }
		root.findViewById<Button>(R.id.buttonSetResets).setOnClickListener { resetCount(true, editTestResets) }

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
		val activ = activity ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.control.allowDimming(enable)
				}.unwrap()
				.success { showResult("Enable dimming success") }
				.fail { showResult("Enable dimming failed: ${it.message}") }
	}

	private fun enableSwitchcraft(enable: Boolean) {
		val device = MainApp.instance.selectedDevice ?: return
		val activ = activity ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.config.setSwitchCraftEnabled(enable)
				}.unwrap()
				.success { showResult("Enable switchcraft success") }
				.fail { showResult("Enable switchcraft failed: ${it.message}") }
	}

	private fun resetCount(set: Boolean, editText: EditText) {
		val device = MainApp.instance.selectedDevice ?: return
		val activ = activity ?: return
		if (!set) {
			editText.setText("")
		}
		MainApp.instance.bluenet.connect(device.address)
				.then {
					when (set) {
						true -> Promise.ofFail(Errors.NotImplemented())
						false -> MainApp.instance.bluenet.state.getResetCount()
					}
				}.unwrap()
				.success {
//					showResult("Reset count: $it")
					editText.setText(it.toString())
				}
				.fail { showResult("Reset count failed: ${it.message}") }
	}





	private fun showResult(text: String) {
		val activ = activity ?: return
		MainApp.instance.showResult(text, activ)
	}
}