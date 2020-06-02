package rocks.crownstone.dev_app.ui.control

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import nl.komponents.kovenant.unwrap
import rocks.crownstone.bluenet.packets.behaviour.*
import rocks.crownstone.bluenet.structs.Int32
import rocks.crownstone.bluenet.structs.Uint8
import rocks.crownstone.dev_app.MainApp
import rocks.crownstone.dev_app.R

/**
 * Fragment with the debug commands.
 */
class BehaviourFragment : Fragment() {
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
		val root = inflater.inflate(R.layout.fragment_behaviour, container, false)
		rootView = root
		viewGroup = container
		val textView: TextView = root.findViewById(R.id.section_label)
		pageViewModel.text.observe(this, Observer<String> {
			textView.text = it
		})

		val behaviourTypes = BehaviourType.values()
		val behaviourTypeStrings = ArrayList<String>()
		for (type in behaviourTypes) {
			behaviourTypeStrings.add(type.name)
		}
		val behaviourTypeAdapter = ArrayAdapter(this.context, android.R.layout.simple_spinner_item, behaviourTypeStrings)
		behaviourTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		root.findViewById<Spinner>(R.id.spinnerBehaviourType).adapter = behaviourTypeAdapter

		val presenceTypes = PresenceType.values()
		val presenceTypeStrings = ArrayList<String>()
		for (type in presenceTypes) {
			presenceTypeStrings.add(type.name)
		}
		val presenceTypeAdapter = ArrayAdapter(this.context, android.R.layout.simple_spinner_item, presenceTypeStrings)
		presenceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		root.findViewById<Spinner>(R.id.spinnerPresenceType).adapter = presenceTypeAdapter

		root.findViewById<Button>(R.id.buttonAdd).setOnClickListener {
			addBehaviour(
					root.findViewById<Spinner>(R.id.spinnerBehaviourType).selectedItem.toString(),
					root.findViewById<EditText>(R.id.editSwitchVal).text.toString(),
					root.findViewById<EditText>(R.id.editProfileId).text.toString(),
					root.findViewById<EditText>(R.id.editFromTime).text.toString(),
					root.findViewById<EditText>(R.id.editUntilTime).text.toString(),
					root.findViewById<Spinner>(R.id.spinnerPresenceType).selectedItem.toString(),
					root.findViewById<EditText>(R.id.editRoom).text.toString(),
					root.findViewById<CheckBox>(R.id.checkMonday).isChecked,
					root.findViewById<CheckBox>(R.id.checkTuesday).isChecked,
					root.findViewById<CheckBox>(R.id.checkWednesday).isChecked,
					root.findViewById<CheckBox>(R.id.checkThursday).isChecked,
					root.findViewById<CheckBox>(R.id.checkFriday).isChecked,
					root.findViewById<CheckBox>(R.id.checkSaturday).isChecked,
					root.findViewById<CheckBox>(R.id.checkSunday).isChecked
			)
		}

		root.findViewById<Button>(R.id.buttonRem).setOnClickListener {
			remBehaviour(root.findViewById<TextView>(R.id.editRem).text.toString())
		}

		root.findViewById<Button>(R.id.buttonGet).setOnClickListener {
			getBehaviours(root.findViewById<TextView>(R.id.textBehaviours))
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
		fun newInstance(sectionNumber: Int, deviceAddress: String?): BehaviourFragment {
			return BehaviourFragment().apply {
				arguments = Bundle().apply {
					putInt(ARG_SECTION_NUMBER, sectionNumber)
					putString(ARG_DEVICE_ADDRESS, deviceAddress)
				}
			}
		}
	}

	private fun addBehaviour(
			typeStr: String,
			switchValueStr: String,
			profileIdStr: String,
			fromStr: String,
			untilStr: String,
			presenceTypeStr: String,
			presenceRoomStr: String,
			mon: Boolean,
			tue: Boolean,
			wed: Boolean,
			thu: Boolean,
			fri: Boolean,
			sat: Boolean,
			sun: Boolean) {
		val behaviourType = BehaviourType.valueOf(typeStr)
		val switchValue: Uint8 = switchValueStr.toUByte()
		val profileId: Uint8 = profileIdStr.toUByte()

		val fromStrings = fromStr.split(':')
		val fromOffset: Int32 = fromStrings[0].toInt() * 3600 + fromStrings[1].toInt() * 60 + fromStrings[2].toInt()
		val from = TimeOfDayPacket(BaseTimeType.MIDNIGHT, fromOffset)

		val untilStrings = untilStr.split(':')
		val untilOffset: Int32 = untilStrings[0].toInt() * 3600 + untilStrings[1].toInt() * 60 + untilStrings[2].toInt()
		val until = TimeOfDayPacket(BaseTimeType.MIDNIGHT, untilOffset)

		val daysOfWeek = DaysOfWeekPacket(sun, mon, tue, wed, thu, fri, sat)

		val presenceRooms = arrayListOf(presenceRoomStr.toUByte())
		val presenceType = PresenceType.valueOf(presenceTypeStr)
		val presence = PresencePacket(presenceType, presenceRooms, 0U)

		val behaviour: BehaviourPacket = when (behaviourType) {
			BehaviourType.TWILIGHT -> {
				TwilightBehaviourPacket(switchValue, profileId, daysOfWeek, from, until)
			}
			BehaviourType.SWITCH -> {
				SwitchBehaviourPacket(switchValue, profileId, daysOfWeek, from, until, presence)
			}
			BehaviourType.SMART_TIMER -> {
				SmartTimerBehaviourPacket(switchValue, profileId, daysOfWeek, from, until, presence, presence)
			}
			BehaviourType.UNKNOWN -> {
				BehaviourPacket(behaviourType, switchValue, profileId, daysOfWeek, from, until)
			}
		}

		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.control.addBehaviour(behaviour)
				}.unwrap()
				.successUi {
					showResult("Add behaviour success: $it")
				}
				.fail { showResult("Add behaviour failed: ${it.message}") }
	}

	private fun remBehaviour(indexStr: String) {
		val index: BehaviourIndex = indexStr.toUByte()
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.control.removeBehaviour(index)
				}.unwrap()
				.successUi {
					showResult("Rem behaviour success: $it")
				}
				.fail { showResult("Rem behaviour failed: ${it.message}") }
	}

	private fun getBehaviours(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.behaviourSyncer.setBehaviours(MainApp.instance.behaviours)
					MainApp.instance.behaviourSyncer.sync()
				}.unwrap()
				.successUi {
					view.text = it.toString()
					showResult("Get behaviours success: $it")
				}
				.fail { showResult("Get behaviours failed: ${it.message}") }
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