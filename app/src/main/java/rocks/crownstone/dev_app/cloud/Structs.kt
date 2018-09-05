package rocks.crownstone.dev_app.cloud

import java.util.*


data class UserData(val id: String, var accessToken: String, var ttl: Long, var creationDate: String)
data class Key(val key: String?)
data class KeySet(var adminKey: Key?, var memberKey: Key?, var guestKey: Key?)
data class SphereData(val id: String, val name: String, var keySet: KeySet?, val meshAccessAddress: String, val iBeaconUUID: String)