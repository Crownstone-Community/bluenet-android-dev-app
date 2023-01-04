package rocks.crownstone.dev_app.ui.control

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import nl.komponents.kovenant.*
import nl.komponents.kovenant.ui.successUi
import rocks.crownstone.bluenet.core.CoreInit
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
 * Fragment with the microapp commands.
 */
class MicroappFragment : Fragment() {
	private val TAG = this.javaClass.simpleName

	private lateinit var pageViewModel: PageViewModel
	private var deviceAddress: String? = null

	private lateinit var rootView: View
	private var viewGroup: ViewGroup? = null

	private var openFilePromise: Deferred<Uri, Exception>? = null

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
		val root = inflater.inflate(R.layout.fragment_microapp, container, false)
		rootView = root
		viewGroup = container

		root.findViewById<Button>(R.id.buttonMicroappInfo).setOnClickListener {
			getInfo(root.findViewById(R.id.textViewMicroappInfo))
		}
		root.findViewById<Button>(R.id.buttonMicroappUpload).setOnClickListener {
			upload()
		}

		root.findViewById<Button>(R.id.buttonMicroappEnable).setOnClickListener {
			enable(true)
		}
		root.findViewById<Button>(R.id.buttonMicroappDisable).setOnClickListener {
			enable(false)
		}
		return root
	}

	companion object {
		const val PICK_MICROAPP_FILE = 84
		private const val ARG_SECTION_NUMBER = "section_number"
		private const val ARG_DEVICE_ADDRESS = "device_address"

		/**
		 * Returns a new instance of this fragment.
		 */
		@JvmStatic
		fun newInstance(sectionNumber: Int, deviceAddress: String?): MicroappFragment {
			return MicroappFragment().apply {
				arguments = Bundle().apply {
					putInt(ARG_SECTION_NUMBER, sectionNumber)
					putString(ARG_DEVICE_ADDRESS, deviceAddress)
				}
			}
		}
	}

	private fun getInfo(view: TextView) {
		clearText(view)
		val device = MainApp.instance.selectedDevice ?: return
		MainApp.instance.bluenet.connect(device.address)
				.then {
					MainApp.instance.bluenet.microapp(device.address).getMicroappInfo()
				}.unwrap()
				.successUi {
					view.text = it.toString()
				}
				.fail {
					showResult("Failed to get microapp info: $it")
				}
	}

	private fun upload() {
		openFile()
				.then {
					showResult("Upload complete")
				}
				.fail {
					showResult("Failed to upload: $it")
				}
	}

	private fun enable(enable: Boolean) {
		val device = MainApp.instance.selectedDevice ?: return
		val enableString = when (enable) {
			true -> "enable"
			false -> "disable"
		}
		MainApp.instance.bluenet.connect(device.address)
				.then {
					if (enable) {
						MainApp.instance.bluenet.microapp(device.address).enableMicroapp(0)
					}
					else {
						MainApp.instance.bluenet.microapp(device.address).disableMicroapp(0)
					}
				}.unwrap()
				.success {
					showResult("Successfully ${enableString}d microapp")
				}
				.fail {
					showResult("Failed to ${enableString} microapp: $it")
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

	private fun openFile(): Promise<Uri, Exception> {
		val context = this.context
		if (context == null) {
			return Promise.ofFail(Exception("No context"))
		}
		val permission = Manifest.permission.READ_EXTERNAL_STORAGE
		if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, permission)) {
				return Promise.ofFail(Exception("No permission to open files"))
			}
			ActivityCompat.requestPermissions(activity as Activity, arrayOf(permission), 234)
			return Promise.ofFail(Exception("Try again after giving permission"))
		}

		// Reject the promise if it's pending
		openFilePromise?.reject(Exception("Canceled"))

		// Start a new promise
		val deferred = deferred<Uri, Exception>()
		openFilePromise = deferred

//		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
		val intent = Intent(Intent.ACTION_GET_CONTENT)
//		intent.addCategory(Intent.CATEGORY_OPENABLE)
//		intent.type = "application/bin"
//		intent.type = "text/plain"
		intent.type = "application/octet-stream"
//		intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, )

//		startActivityForResult(intent, PICK_MICROAPP_FILE)

		val chooser = Intent.createChooser(intent, "Pick a file")
		startActivityForResult(chooser, PICK_MICROAPP_FILE)

		return deferred.promise
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode != PICK_MICROAPP_FILE) {
			super.onActivityResult(requestCode, resultCode, data)
			return
		}
		Log.i(TAG, "onActivityResult: data=$data")
		val deferred = openFilePromise ?: return
		openFilePromise = null

		if (data == null) {
			deferred.reject(Exception("No file selected: result=$resultCode data=$data"))
			return
		}

		val uri = data.data
		if (uri != null && resultCode == Activity.RESULT_OK) {
			deferred.resolve(uri)
		}
		else {
			deferred.reject(Exception("No file selected: result=$resultCode uri=$uri"))
		}
	}
}
