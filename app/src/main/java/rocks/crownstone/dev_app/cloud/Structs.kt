package rocks.crownstone.dev_app.cloud

import rocks.crownstone.bluenet.structs.DeviceAddress

data class UserData(val id: String, var accessToken: String, var ttl: Long, var creationDate: String)
//data class Key(val key: String?)
typealias Key = String
data class KeySet(var adminKey: Key?, var memberKey: Key?, var guestKey: Key?, var serviceDataKey: Key?, var meshAppKey: Key?, var meshNetKey: Key?)
data class SphereData(val id: String, val uid: Int, val name: String, var keySet: KeySet?, val meshAccessAddress: String, val iBeaconUUID: String)

data class StoneData(
		val id: String,
		val sphereId: String,
		val stoneId: Int,
		val name: String,
		val address: DeviceAddress,
		val iBeaconUUID: String,
		val iBeaconMajor: Int,
		val iBeaconMinor: Int,
		val meshDevKey: Key?)