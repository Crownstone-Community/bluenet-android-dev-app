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
import rocks.crownstone.bluenet.packets.HubDataPacket
import rocks.crownstone.bluenet.packets.StringPacket
import rocks.crownstone.bluenet.packets.UuidPacket
import rocks.crownstone.bluenet.packets.behaviour.*
import rocks.crownstone.bluenet.packets.other.IbeaconConfigIdPacket
import rocks.crownstone.bluenet.packets.powerSamples.PowerSamplesType
import rocks.crownstone.bluenet.packets.wrappers.v5.StatePacketV5
import rocks.crownstone.bluenet.structs.*
import rocks.crownstone.bluenet.util.Util
import rocks.crownstone.dev_app.MainApp
import rocks.crownstone.dev_app.R
import java.util.*
import kotlin.collections.ArrayList

/**
 * Fragment with the test commands.
 */
class TestFragment : Fragment() {
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
		val root = inflater.inflate(R.layout.fragment_test, container, false)
		rootView = root
		viewGroup = container
		val textView: TextView = root.findViewById(R.id.section_label)
		pageViewModel.text.observe(this, Observer<String> {
			textView.text = it
		})

		root.findViewById<Button>(R.id.buttonBroadcastNoop).setOnClickListener {
			broadcastNoop()
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
		fun newInstance(sectionNumber: Int, deviceAddress: String?): TestFragment {
			return TestFragment().apply {
				arguments = Bundle().apply {
					putInt(ARG_SECTION_NUMBER, sectionNumber)
					putString(ARG_DEVICE_ADDRESS, deviceAddress)
				}
			}
		}
	}

	private fun broadcastNoop() {
		val device = MainApp.instance.selectedDevice ?: return
		val sphereId = device.sphereId ?: return
		val stoneId = device.serviceData?.crownstoneId ?: return
		MainApp.instance.bluenet.broadCast.noop(sphereId)
				.success { showResult("Done: broadcasted noop") }
				.fail { showResult("Failed: ${it.message}") }
	}


	private fun sendHubData() {
		val device = MainApp.instance.selectedDevice ?: return
		val sphereId = device.sphereId ?: return
		val stoneId = device.serviceData?.crownstoneId ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.control.hubData(HubDataPacket(HubDataPacket.EncryptType.NOT_ENCRYPTED, StringPacket("test")) )
				}.unwrap()
				.success { showResult("Hub data reply: $it") }
				.fail { showResult("Failed to send hub data: ${it.message}") }
	}

	private fun setBehaviour() {
		val device = MainApp.instance.selectedDevice ?: return
		val sphereId = device.sphereId ?: return
		val stoneId = device.serviceData?.crownstoneId ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					val daysOfWeek = DaysOfWeekPacket(true, true, true, true, true, true, true)
					val startTime = TimeOfDayPacket(BaseTimeType.MIDNIGHT, 6*3600)
					val endTime = TimeOfDayPacket(BaseTimeType.MIDNIGHT, 23*3600)
					val presence = PresencePacket(PresenceType.ALWAYS_TRUE, ArrayList(), 5U * 60U)
					val behaviourPacket = SwitchBehaviourPacket(0U, 0U, daysOfWeek, startTime, endTime, presence)
					MainApp.instance.bluenet.control.addBehaviour(behaviourPacket)
				}.unwrap()
				.success { showResult("Added behaviour at index=${it.index} hash=${it.hash}") }
				.fail { showResult("Failed to set behaviour: ${it.message}") }
	}

	private fun setIbeaconUuid() {
		val device = MainApp.instance.selectedDevice ?: return
		val sphereId = device.sphereId ?: return
		val stoneId = device.serviceData?.crownstoneId ?: return

		val uuid = UUID.randomUUID()
		val major: Uint16 = 1U
		val minor: Uint16 = 2U

		val statePacket = StatePacketV5(StateTypeV4.IBEACON_PROXIMITY_UUID, 1U, PersistenceModeSet.STORED.num, UuidPacket(uuid))
		val ids = listOf<Uint8>(79U, 77U)

		showResult("Set ibeaconUuid: $uuid")

		MainApp.instance.bluenet.connect(device.address)
//				.then {
//					MainApp.instance.bluenet.config.setIbeaconUuid(uuid, 0U, PersistenceModeSet.TEMPORARY)
//				}.unwrap()
//				.then {
//					MainApp.instance.bluenet.config.getIbeaconUuid(0U, PersistenceModeGet.CURRENT)
//				}.unwrap()
//				.then {
//					showResult("Current ibeaconUuid: $it")
//					MainApp.instance.bluenet.config.getIbeaconUuid(0U, PersistenceModeGet.STORED)
//				}.unwrap()
//				.then {
//					showResult("Stored ibeaconUuid: $it")
//					MainApp.instance.bluenet.config.getIbeaconUuid(0U, PersistenceModeGet.FIRMWARE_DEFAULT)
//				}.unwrap()
//				.then {
//					showResult("Default ibeaconUuid: $it")
//				}

//				.then {
//					MainApp.instance.bluenet.config.setIbeaconUuid(uuid, 1U, PersistenceModeSet.STORED)
//				}.unwrap()
//				.then {
//					MainApp.instance.bluenet.config.setIbeaconMajor(major, 1U, PersistenceModeSet.STORED)
//				}.unwrap()
//				.then {
//					MainApp.instance.bluenet.config.setIbeaconMinor(minor, 1U, PersistenceModeSet.STORED)
//				}.unwrap()
//				.then {
//					val timestamp: Uint32 = 0U
//					val interval: Uint16 = 4U
//					MainApp.instance.bluenet.control.setIbeaconConfigId(IbeaconConfigIdPacket(0U, timestamp, interval))
//				}.unwrap()
//				.then {
//					val timestamp: Uint32 = 2U
//					val interval: Uint16 = 4U
//					MainApp.instance.bluenet.control.setIbeaconConfigId(IbeaconConfigIdPacket(1U, timestamp, interval))
//				}

				// Via mesh
				.then {
					MainApp.instance.bluenet.mesh.setState(statePacket, ids[0])
				}.unwrap()
				.then {
					MainApp.instance.bluenet.mesh.setState(statePacket, ids[1])
				}.unwrap()
				.then {
					val timestamp: Uint32 = 0U
					val interval: Uint16 = 4U
					MainApp.instance.bluenet.mesh.setIbeaconConfigId(IbeaconConfigIdPacket(0U, timestamp, interval), ids)
				}.unwrap()
				.then {
					val timestamp: Uint32 = 2U
					val interval: Uint16 = 4U
					MainApp.instance.bluenet.mesh.setIbeaconConfigId(IbeaconConfigIdPacket(1U, timestamp, interval), ids)
				}.unwrap()
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