package rocks.crownstone.dev_app

import android.util.Log
import rocks.crownstone.bluenet.*
import rocks.crownstone.bluenet.util.Conversion

class TestTemplate {
	val TAG = this.javaClass.simpleName
	fun test() {
		var arr = byteArrayOf(-100)
		someFun<Uint8>(arr)
		Log.i(TAG, "${arr[0]} = ${Conversion.byteArrayToSigned<Uint8>(arr)}")
		arr = byteArrayOf(0)
		Log.i(TAG, "0 = ${Conversion.byteArrayToSigned<Uint8>(arr)}")
		arr = byteArrayOf(100)
		Log.i(TAG, "100 = ${Conversion.byteArrayToSigned<Uint8>(arr)}")
	}

	inline fun <reified T> someFun(array: ByteArray): T {
		Log.i(TAG, "class=${T::class}")
		when (T::class) {
			Byte::class, Int8::class ->
				Log.i(TAG, "int8")
			Uint8::class ->
				Log.i(TAG, "uint8")
			Short::class, Int16::class ->
				Log.i(TAG, "int16")
			Uint16::class ->
				Log.i(TAG, "uint16")
			Int::class, Int32::class ->
				Log.i(TAG, "int32")
			Uint32::class ->
				Log.i(TAG, "uint32")
			Float::class ->
				Log.i(TAG, "float")
		}
		return Conversion.byteArrayToSigned(array)
	}
}