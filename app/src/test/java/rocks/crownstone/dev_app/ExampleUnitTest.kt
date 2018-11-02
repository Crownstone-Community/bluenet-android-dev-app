package rocks.crownstone.dev_app

import android.util.Log
import org.junit.Test

import org.junit.Assert.*
import java.nio.ByteBuffer

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
	val TAG = "ExampleUnitTest"
    @Test
    fun addition_isCorrect() {
		val arr = byteArrayOf(0,1,2,3,4,5,6,7,8,9)
		val bb = ByteBuffer.wrap(arr)
		System.out.println("pos=${bb.position()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "pos=${bb.position()}")
		bb.position(bb.position() - 4)
		System.out.println( "pos=${bb.position()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "b=${bb.get()}")
		System.out.println( "pos=${bb.position()}")

        assertEquals(4, 2 + 2)
    }
}
