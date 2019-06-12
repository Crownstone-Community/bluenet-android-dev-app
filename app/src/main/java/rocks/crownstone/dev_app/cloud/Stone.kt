package rocks.crownstone.dev_app.cloud

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import nl.komponents.kovenant.unwrap
import org.json.JSONObject
import rocks.crownstone.bluenet.structs.DeviceAddress
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

class Stone(context: Context, volleyQueue: RequestQueue) {
	private val TAG = this.javaClass.simpleName
	private val volleyQueue = volleyQueue

	fun createStone(user: UserData, sphere: SphereData, name: String, address: DeviceAddress): Promise<StoneData, Exception> {
		val deferred = deferred<StoneData, Exception>()
		val url = "https://my.crownstone.rocks/api/Spheres/${sphere.id}/ownedStones?access_token=${user.accessToken}"
		val data = JSONObject()
		data.put("name", name)
		data.put("address", address)
		Log.i(TAG, "createStone url=$url")
		Log.i(TAG, "createStone data=$data")
		val request = JsonObjectRequest(Request.Method.POST, url, data,
				Response.Listener { response ->
					Log.i(TAG, "Response: %s".format(response.toString()))
					val stoneData = jsonToStoneData(response, sphere.iBeaconUUID)
					deferred.resolve(stoneData)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: %s".format(error.toString()))
					deferred.reject(Exception("Failed to create stone: $error"))
				}
		)

		volleyQueue.add(request)
		return deferred.promise
	}

	fun getStoneData(user: UserData, sphere: SphereData, address: DeviceAddress): Promise<StoneData, Exception> {
		val deferred = deferred<StoneData, Exception>()
		val filter = URLEncoder.encode("{\"where\":{\"address\":\"$address\"}}", "UTF-8")
		val url = "https://my.crownstone.rocks/api/Spheres/${sphere.id}/ownedStones?filter=$filter&access_token=${user.accessToken}"
		Log.i(TAG, "getStoneData url=$url")
		val request = JsonArrayRequest(Request.Method.GET, url, null,
				Response.Listener { response ->
					Log.i(TAG, "Response: %s".format(response.toString()))
					if (response.length() != 1) {
						deferred.reject(Exception("Expected array of size 1, is ${response.length()}"))
						return@Listener
					}
					val stoneData = jsonToStoneData(response.getJSONObject(0), sphere.iBeaconUUID)
					deferred.resolve(stoneData)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: %s".format(error.toString()))
					deferred.reject(Exception("Failed to get stone: $error"))
				}
		)
		volleyQueue.add(request)
		return deferred.promise
	}

	fun getAllStones(user: UserData, sphere: SphereData): Promise<List<StoneData>, Exception> {
		val deferred = deferred<List<StoneData>, Exception>()
		var url = "https://my.crownstone.rocks/api/Spheres/${sphere.id}/ownedStones?access_token=${user.accessToken}"
		Log.i(TAG, "getStoneData url=$url")
		val request = JsonArrayRequest(Request.Method.GET, url, null,
				Response.Listener { response ->
//					Log.i(TAG, "Response: %s".format(response.toString()))
					val list = ArrayList<StoneData>()
					for (i in 0 until response.length()) {
						val stoneData = jsonToStoneData(response.getJSONObject(i), sphere.iBeaconUUID)
						list.add(stoneData)
					}
					deferred.resolve(list)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: %s".format(error.toString()))
					deferred.reject(Exception("Failed to get stone: $error"))
				}
		)
		volleyQueue.add(request)
		return deferred.promise
	}

	fun removeStone(user: UserData, sphere: SphereData, address: DeviceAddress): Promise<Unit, Exception> {
		return getStoneData(user, sphere, address)
				.then {
					removeStoneId(user, sphere, it.id)
				}.unwrap()
	}

	fun removeStoneId(user: UserData, sphere: SphereData, stoneId: String): Promise<Unit, Exception> {
		val deferred = deferred<Unit, Exception>()
		val url = "https://my.crownstone.rocks/api/Spheres/${sphere.id}/ownedStones/$stoneId?access_token=${user.accessToken}"
		val request = StringRequest(Request.Method.DELETE, url,
				Response.Listener { response ->
					Log.i(TAG, "Response: $response")
					deferred.resolve(Unit)
				},
				Response.ErrorListener { error ->
					Log.e(TAG, "Error: $error")
					deferred.reject(Exception("Failed to remove stone: $error"))
				}
		)
		volleyQueue.add(request)
		return deferred.promise
	}

	private fun jsonToStoneData(json: JSONObject, iBeaconUuid: String): StoneData {
		val id = json.getString("id")
		val stoneId = json.getInt("uid")
		val sphereId = json.getString("sphereId")
		val name = json.getString("name")
		val address = json.getString("address")
		val major = json.getInt("major")
		val minor = json.getInt("minor")
		Log.e(TAG, "Missing meshDeviceKey")
		Log.e(TAG, "Device key should be retrieved via https://my.crownstone.rocks/api/users/<id>/keysV2")
		// Create a semi random string for now..
		val meshDevKey = UUID.randomUUID().toString().replace("-", "")
		return StoneData(id, sphereId, stoneId, name, address, iBeaconUuid, major, minor, meshDevKey)
	}

}