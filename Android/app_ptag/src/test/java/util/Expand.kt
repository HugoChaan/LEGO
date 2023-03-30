package util

import org.koin.test.KoinTest
import java.io.File

/**
 *
 */

fun KoinTest.toTestFile(path: String): File {
    return this.javaClass.classLoader!!.getResource(path).let {
        File(it.path)
    }
}