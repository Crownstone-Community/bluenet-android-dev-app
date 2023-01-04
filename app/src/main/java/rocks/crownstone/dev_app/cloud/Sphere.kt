package rocks.crownstone.dev_app.cloud

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import nl.komponents.kovenant.*
import org.json.JSONArray
import org.json.JSONObject

class Sphere(context: Context, volleyQueue: RequestQueue) {
	private val TAG = this.javaClass.simpleName
	private val volleyQueue = volleyQueue

	var spheres = HashMap<String, SphereData>(); private set // Map with: sphere id as key.

	fun clear() {
		spheres.clear()
	}

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
					getSphereKeysV2(it)
				}.unwrap()
				.then {
					parseSphereKeysV2Response(it)
				}.unwrap()
	}

	override fun toString(): String {
		val sb = StringBuilder()
		sb.append("spheres:\n")
		for (sphere in spheres.values) {
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
		spheres.clear()
		val deferred = deferred<JSONArray, Exception>()
		val url = "https://my.crownstone.rocks/api/users/${userData.id}/spheres/?access_token=${userData.accessToken}"
		val jsonRequest = JsonArrayRequest(Request.Method.GET, url, null,
				Response.Listener { response ->
					Log.i(TAG, "Response: %s".format(response.toString()))
					deferred.resolve(response)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: %s".format(error.toString()))
					deferred.reject(Exception("Failed to get spheres: $error"))
				}
		)

		volleyQueue.add(jsonRequest)
		return deferred.promise
	}

	private fun parseSpheresResponse(response: JSONArray): Promise<Unit, Exception> {
		return task {
			for (i in 0 until response.length()) {
				val sphereJson = response.getJSONObject(i)
				val id = sphereJson.getString("id")
				val sphereId = sphereJson.getInt("uid")
				val name = sphereJson.getString("name")
				val iBeaconUUID = sphereJson.getString("uuid")
				val meshAccessAddress = sphereJson.optString("meshAccessAddress", "f001ba11")
				val sphere = SphereData(id, sphereId, name, null, meshAccessAddress, iBeaconUUID)
				spheres[id] = sphere
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
					deferred.reject(Exception("Failed to get keys: $error"))
				}
		)
		volleyQueue.add(jsonRequest)
		return deferred.promise
	}

	private fun parseSphereKeysResponse(response: JSONArray): Promise<Unit, Exception> {
		return task {
			for (i in 0 until response.length()) {
				val sphereJson = response.getJSONObject(i)
				val id = sphereJson.getString("sphereId")
				val keysJson = sphereJson.getJSONObject("keys")
				spheres[id]?.keySet = parseSphereKeysJson(keysJson)
			}
		}
	}

	private fun parseSphereKeysJson(json: JSONObject): KeySet {
		val keySet = KeySet(null, null, null, null, null, null, null)
		keySet.adminKey = json.optString("admin", null)
		keySet.memberKey = json.optString("member", null)
		keySet.guestKey = json.optString("guest", null)
		return keySet
	}

	private fun parseSphereKeyString(key: String?): Key? {
		return key // Key is just a string
	}




	private fun getSphereKeysV2(userData: UserData): Promise<JSONArray, Exception> {
		val deferred = deferred<JSONArray, Exception>()
		val url = "https://my.crownstone.rocks/api/users/${userData.id}/keysV2/?access_token=${userData.accessToken}"
		val jsonRequest = JsonArrayRequest(Request.Method.GET, url, null,
				Response.Listener { response ->
					Log.i(TAG, "Response: %s".format(response.toString()))
					deferred.resolve(response)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: %s".format(error.toString()))
					deferred.reject(Exception("Failed to get keys: $error"))
				}
		)
		volleyQueue.add(jsonRequest)
		return deferred.promise
	}

	private fun parseSphereKeysV2Response(response: JSONArray): Promise<Unit, Exception> {
		return task {
			for (i in 0 until response.length()) {
				val sphereJson = response.getJSONObject(i)
				val id = sphereJson.getString("sphereId")
				val sphereKeysJson = sphereJson.getJSONArray("sphereKeys")
				val stoneKeysJson = sphereJson.getJSONObject("stoneKeys")
				spheres[id]?.keySet = parseSphereKeysV2Json(sphereKeysJson)
			}
		}
	}

	private fun parseSphereKeysV2Json(json: JSONArray): KeySet {
		val keySet = KeySet(null, null, null, null, null, null, null)
		for (i in 0 until json.length()) {
			val keyJson = json.getJSONObject(i)
			val type = keyJson.getString("keyType")
			val ttl = keyJson.getInt("ttl")
			val key = keyJson.getString("key")
			when (type) {
				"ADMIN_KEY" -> keySet.adminKey = key
				"MEMBER_KEY" -> keySet.memberKey = key
				"BASIC_KEY" -> keySet.guestKey = key
				"SERVICE_DATA_KEY" -> keySet.serviceDataKey = key
				"LOCALIZATION_KEY" -> keySet.localizationKey = key
				"MESH_APPLICATION_KEY" -> keySet.meshAppKey = key
				"MESH_NETWORK_KEY" -> keySet.meshNetKey = key
			}
		}
		return keySet
	}

}