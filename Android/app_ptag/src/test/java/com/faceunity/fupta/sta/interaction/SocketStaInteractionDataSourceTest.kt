package com.faceunity.fupta.sta.interaction

import android.util.Base64
import com.faceunity.fupta.cloud_download.CloudSyncRepository
import com.faceunity.fupta.sta.interaction.entity.FuTtsData
import com.faceunity.fupta.sta.interaction.entity.FuTtsResult
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import util.KoinDITest
import java.util.concurrent.CountDownLatch


/**
 *
 */
@RunWith(MockitoJUnitRunner::class)
class SocketStaInteractionDataSourceTest: KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
//        printLogger()
        modules(KoinDITest.utilsModel + KoinDITest.cloudModel)
    }


    val staService: SocketStaInteractionDataSource by lazy {
        SocketStaInteractionDataSource(
            fuLog = get(),
            socketUrl = "ws://avatarx-websocket-test.faceunity.com:25001"
        ).apply {
            socket.setToken(getToken())
            init(object : StaInteractionDataSource.ConnectStateListener {
                override fun onConnect() {
                    println("onConnect()")
                }

                override fun onDisconnect() {
                    println("onDisconnect()")
                }

                override fun onUnauthorized() {
                    println("onUnauthorized()")
                }

                override fun onError(error: String) {
                    println("onError:$error")
                }
            })
        }
    }

    val syncAPI by lazy {
        get<CloudSyncRepository>()
    }

    fun getToken(): String {
        return "84c4334b-c6ff-4ccc-9ed4-739b5d18e747"
    }


    @Test
    fun asr() {
    }

    @Test
    fun nlp() {
    }

    @Before
    fun setup() {

    }

    @Test
    fun tts() {
        val mockStatic = Mockito.mockStatic(Base64::class.java)
        `when`(mockStatic)
//            mockStatic(Base64::class.java) {
//            `when`(Base64.decode(anyString(), anyInt())).thenReturn(
//                ByteArray(0)
//            )
////            `when`(Base64.decode(anyString(), anyInt())).thenAnswer { invocation ->
////                val input = invocation.arguments[0] as String
////                val ret = java.util.Base64.getMimeDecoder().decode(invocation.arguments[0] as String)
////                println("input:$input")
////                println("ret:$ret")
////                ret
////            }
//        }

        staService
        Thread.sleep(1000)
        val mutex = CountDownLatch(1)
        staService.tts(
            word = "测试内容",
            voice = "Kenny",
            format = "pcm",
            ttsApiVersion = "v2",
            listener = object : StaInteractionDataSource.Listener<FuTtsResult> {
                override fun onAck(ack: FuTtsResult) {
                    println(ack)
                }

                override fun onStream(audioData: FuTtsData) {
                    println(audioData)
                }
                override fun onResult(result: Result<FuTtsResult>) {
                    println(result)
                    mutex.countDown()
                }
            }
        )

        mutex.await()
    }

    @Test
    fun nlpTts() {
    }

    @Test
    fun asrNlpTts() {
    }
}