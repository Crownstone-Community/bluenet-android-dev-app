package rocks.crownstone.dev_app.ui.control

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
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
import rocks.crownstone.bluenet.packets.HubDataPacket
import rocks.crownstone.bluenet.packets.StringPacket
import rocks.crownstone.bluenet.packets.UuidPacket
import rocks.crownstone.bluenet.packets.behaviour.*
import rocks.crownstone.bluenet.packets.other.IbeaconConfigIdPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesType
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.scanparsing.ScannedDevice
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.dev_app.MainApp
import rocks.crownstone.dev_app.R
import java.util.*
import kotlin.collections.ArrayList

/**
 * Fragment with the service data.
 */
class ServiceDataFragment : Fragment() {
	private val TAG = this.javaClass.simpleName

	private lateinit var pageViewModel: PageViewModel
	private var deviceAddress: String? = null

	private lateinit var rootView: View
	private var viewGroup: ViewGroup? = null

	enum class ServiceDataFilterMode {
		SELF,
		ALL,
		APPLIED,
	}

	var mode = ServiceDataFilterMode.SELF
	lateinit var serviceDataView: TextView

	companion object {
		private const val ARG_SECTION_NUMBER = "section_number"
		private const val ARG_DEVICE_ADDRESS = "device_address"

		/**
		 * Returns a new instance of this fragment.
		 */
		@JvmStatic
		fun newInstance(sectionNumber: Int, deviceAddress: String?): ServiceDataFragment {
			return ServiceDataFragment().apply {
				arguments = Bundle().apply {
					putInt(ARG_SECTION_NUMBER, sectionNumber)
					putString(ARG_DEVICE_ADDRESS, deviceAddress)
				}
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		deviceAddress = arguments?.getString(ARG_DEVICE_ADDRESS)
		Log.i(TAG, "deviceAddress=$deviceAddress")
	}

	override fun onCreateView(
			inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(R.layout.fragment_service_data, container, false)
		rootView = root
		viewGroup = container

		val explanationView = root.findViewById<TextView>(R.id.textViewServiceDataExplanation)
		root.findViewById<Button>(R.id.buttonServiceDataSelf).setOnClickListener {
			setFilter(ServiceDataFilterMode.SELF, explanationView)
		}
		root.findViewById<Button>(R.id.buttonServiceDataAll).setOnClickListener {
			setFilter(ServiceDataFilterMode.ALL, explanationView)
		}
		root.findViewById<Button>(R.id.buttonServiceDataApplied).setOnClickListener {
			setFilter(ServiceDataFilterMode.APPLIED, explanationView)
		}

		serviceDataView = root.findViewById<TextView>(R.id.textViewServiceData)

		// Initial mode
		setFilter(ServiceDataFilterMode.SELF, explanationView)

		// Now that we have the text view, and set the mode, we can subscribe.
		MainApp.instance.bluenet.subscribe(BluenetEvent.SCAN_RESULT, { data -> onScannedDevice(data as ScannedDevice)})

		return root
	}

	private fun setFilter(mode: ServiceDataFilterMode, explanationView: TextView) {
		when (mode) {
			ServiceDataFilterMode.SELF -> {
				setText(explanationView, "Showing service data about this stone, broadcasted by this sthone.")
			}
			ServiceDataFilterMode.ALL -> {
				setText(explanationView, "Showing service data about all stones, broadcasted by this sthone.")
			}
			ServiceDataFilterMode.APPLIED -> {
				setText(explanationView, "Showing service data about this stone, broadcasted by any stone.")
			}
		}
		clearText(serviceDataView)
		this.mode = mode
	}

	private fun onScannedDevice(device: ScannedDevice) {
		if (!device.validated) {
			return
		}

		val selectedDevice = MainApp.instance.selectedDevice ?: return
		val serviceData = device.serviceData ?: return
		if (!serviceData.unique) {
			return
		}

		when (mode) {
			ServiceDataFilterMode.SELF -> {
				if (device.address != selectedDevice.address) {
					return
				}
				if (serviceData.flagExternalData) {
					return
				}
				setText(serviceDataView, serviceData.toString())
			}
			ServiceDataFilterMode.APPLIED -> {
				if (device.address != selectedDevice.address) {
					return
				}
				setText(serviceDataView, serviceData.toString())
			}
			ServiceDataFilterMode.ALL -> {
				val ownId = selectedDevice.serviceData?.crownstoneId
				if (ownId == null) {
					setText(serviceDataView, "Selected stone ID unknown")
					return
				}

				if (serviceData.crownstoneId == ownId) {
					setText(serviceDataView, serviceData.toString())
				}
			}
		}
	}

	private fun setText(view: TextView, text: String) {
		activity?.runOnUiThread {
			view.text = text
		}
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
