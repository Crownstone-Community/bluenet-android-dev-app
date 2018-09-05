package rocks.crownstone.dev_app.cloud

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import nl.komponents.kovenant.*
import org.json.JSONArray
import org.json.JSONObject
import rocks.crownstone.dev_app.util.CsError

class Spheres(context: Context, volleyQueue: RequestQueue) {
	private val TAG = User::class.java.canonicalName
	private val _volleyQueue = volleyQueue
	private val _context = context

	private var _spheres = HashMap<String, SphereData>()


	fun getSpheres(user: User): Promise<Unit, Exception> {
		return user.getUserData()
				.then {
					getAllSpheres(it)
				}.unwrap()
				.then {
					parseSpheresResponse(it)
				}.unwrap()
				.then {
					user.getUserData() // TODO: no need to getUserData() twice
				}. unwrap()
				.then {
					getSphereKeys(it)
				}.unwrap()
				.then {
					parseSphereKeysResponse(it)
				}.unwrap()
	}

	override fun toString(): String {
		val sb = StringBuilder()
		sb.append("spheres:\n")
		for (sphere in _spheres.values) {
			sb.append("id=")
			sb.append(sphere.id)
			sb.append(" UUID=")
			sb.append(sphere.iBeaconUUID)
			sb.append(" name=")
			sb.append(sphere.name)
			sb.append("\n")
		}
		return sb.toString()
	}



	private fun getAllSpheres(userData: UserData): Promise<JSONArray, Exception> {
		_spheres.clear()
		val deferred = deferred<JSONArray, Exception>()
		val url = "https://my.crownstone.rocks/api/users/${userData.id}/spheres/?access_token=${userData.accessToken}"
		val jsonRequest = JsonArrayRequest(Request.Method.GET, url, null,
				Response.Listener { response ->
					Log.i(TAG, "Response: %s".format(response.toString()))
					deferred.resolve(response)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: %s".format(error.toString()))
					deferred.reject(CsError.LoginException("Login failed"))
				}
		)

		_volleyQueue.add(jsonRequest)
		return deferred.promise
	}

	private fun parseSpheresResponse(response: JSONArray): Promise<Unit, Exception> {
		return task {
			for (i in 0 until response.length()) {
				val sphereJson = response.getJSONObject(i)
				val id = sphereJson.getString("id")
				val name = sphereJson.getString("name")
				val iBeaconUUID = sphereJson.getString("uuid")
				val meshAccessAddress = sphereJson.getString("meshAccessAddress")

				val sphere = SphereData(id, name, null, meshAccessAddress, iBeaconUUID)
				_spheres[id] = sphere
			}
		}
	}


	private fun getSphereKeys(userData: UserData): Promise<JSONArray, Exception> {
		val deferred = deferred<JSONArray, Exception>()
		val url = "https://my.crownstone.rocks/api/users/${userData.id}/keys/?access_token=${userData.accessToken}"
		val jsonRequest = JsonArrayRequest(Request.Method.GET, url, null,
				Response.Listener { response ->
					Log.i(TAG, "Response: %s".format(response.toString()))
					deferred.resolve(response)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: %s".format(error.toString()))
					deferred.reject(CsError.LoginException("Login failed"))
				}
		)

		_volleyQueue.add(jsonRequest)
		return deferred.promise
	}

	private fun parseSphereKeysResponse(response: JSONArray): Promise<Unit, Exception> {
		return task {
			for (i in 0 until response.length()) {
				val sphereJson = response.getJSONObject(i)
				val id = sphereJson.getString("sphereId")
				val keysJson = sphereJson.getJSONObject("keys")
				_spheres[id]?.keySet = parseSphereKeysJson(keysJson)
			}
		}
	}

	private fun parseSphereKeysJson(json: JSONObject): KeySet {
		val keySet = KeySet(null, null, null)
		keySet.adminKey = Key(json.optString("admin", null))
		keySet.memberKey = Key(json.optString("member", null))
		keySet.memberKey = Key(json.optString("member", null))
		return keySet
	}

	private fun parseSphereKeyString(key: String?): Key {
		return Key(key) // Key is just a string
	}
}