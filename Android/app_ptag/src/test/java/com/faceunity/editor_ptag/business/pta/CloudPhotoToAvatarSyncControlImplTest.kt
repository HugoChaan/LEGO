package com.faceunity.editor_ptag.business.pta

import com.faceunity.editor_ptag.business.pta.config.RequestPhotoToAvatarParams
import com.faceunity.fupta.cloud_download.CloudSyncRepository
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.component.get
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import util.KoinDITest
import util.toTestFile


/**
 * 形象生成的自动测试
 */
internal class CloudPhotoToAvatarSyncControlImplTest: KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(KoinDITest.utilsModel + KoinDITest.cloudModel)
    }

    val syncAPI by lazy {
        get<CloudSyncRepository>()
    }

    val ptaControl by lazy {
        CloudPhotoToAvatarSyncControlImpl(syncAPI)
    }

    @Test
    fun initPTAResource() = runBlockingTest {
        val result = ptaControl.initPTAResource()
        assert(result.isSuccess)
    }

    @Test
    fun requestPhotoToAvatar() = runBlockingTest {
        val testFile = toTestFile("photo/test1.jpg")
        val gender = RequestPhotoToAvatarParams.Gender.Male
        ptaControl.initPTAResource().getOrThrow()
        val avatarJson = ptaControl.requestPhotoToAvatar(RequestPhotoToAvatarParams(
            gender,
            testFile
        )).getOrThrow()
        println(avatarJson)
        assert(avatarJson.isNotBlank())
        assert(avatarJson != "{}")
        ptaControl.releasePTAResource().getOrThrow()
    }

    /**
     * 测试 photo/success 文件夹中的照片是否可以成功。
     */
    @Test
    fun requestMoreSuccessPhotoToAvatar() = runBlockingTest {
        val testFileList = (1..11).map { "photo/success/$it.jpg" }.map { toTestFile(it) }
        ptaControl.initPTAResource().getOrThrow()
        val resultList = mutableSetOf<String>()
        testFileList.forEach { photoFile ->
            val gender = listOf(
                RequestPhotoToAvatarParams.Gender.Male,
                RequestPhotoToAvatarParams.Gender.Female
            ).random()
            val avatarJson = ptaControl.requestPhotoToAvatar(RequestPhotoToAvatarParams(
                gender,
                photoFile
            )).getOrThrow()
            println(avatarJson)
            assert(avatarJson.isNotBlank())
            assert(avatarJson != "{}")
            resultList.add(avatarJson)
        }
        //断言没有重复的 avatar.json
        assert(testFileList.size == resultList.size)

        ptaControl.releasePTAResource().getOrThrow()
    }

    /**
     * 测试 photo/failed 文件夹中的照片是否可以失败。
     */
    @Test
    fun requestMoreFailedPhotoToAvatar() = runBlockingTest {
        val testFileList = (1..4).map { "photo/failed/$it.jpg" }.map { toTestFile(it) }
        ptaControl.initPTAResource().getOrThrow()
        testFileList.forEach { photoFile ->
            val gender = listOf(
                RequestPhotoToAvatarParams.Gender.Male,
                RequestPhotoToAvatarParams.Gender.Female
            ).random()
            val result = ptaControl.requestPhotoToAvatar(RequestPhotoToAvatarParams(
                gender,
                photoFile
            ))
            assert(result.isFailure)
            println(result.exceptionOrNull())
        }

        ptaControl.releasePTAResource().getOrThrow()
    }
}