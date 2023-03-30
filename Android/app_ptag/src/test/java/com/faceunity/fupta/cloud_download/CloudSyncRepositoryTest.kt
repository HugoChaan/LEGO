package com.faceunity.fupta.cloud_download

import com.faceunity.editor_ptag.data_center.FuVersion
import com.faceunity.fupta.avatar_data.entity.resource.ItemListResource
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import com.faceunity.fupta.cloud_download.util.NetRequest
import com.faceunity.fupta.cloud_download.util.RequestWrapper
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.component.get
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import util.KoinDITest
import util.toTestFile

/**
 * 云端接口的自动测试
 */
internal class CloudSyncRepositoryTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
//        printLogger()
        modules(KoinDITest.utilsModel + KoinDITest.cloudModel)
    }

    val syncAPI by lazy {
        get<CloudSyncRepository>()
    }

    val request: NetRequest by lazy {
        get()
    }

    val jsonParser: IFuAPIParser by lazy {
        get()
    }

    @Test
    fun requestToken() = runBlockingTest {
        val result = syncAPI.requestToken()
        //验证获取 Token 是否正常
        assert(result.isSuccess)
        println("token:${result.getOrThrow().data}")
    }

    @Test
    fun getProject() = runBlockingTest {
        val token = syncAPI.getCacheTokenOrRequest()
        val project = syncAPI.getProject(token, FuVersion.FU_AAR_VERSION).getOrThrow()
        println(project)
        //必须包含有 PTAG 的项目
        project.data.list.filter { it.type == "PTAG" }.run {
            assert(isNotEmpty())
        }
    }

    @Test
    fun getItemList() = runBlockingTest {
        val token = syncAPI.getCacheTokenOrRequest()
        val project = syncAPI.getProject(token, FuVersion.FU_AAR_VERSION).getOrThrow()
        val itemListVersion = project.data.list.first().item_list_version.toString()
        val itemListResult = syncAPI.getItemList(token, itemListVersion).getOrThrow()
        //不能没有 ItemList 内容
        itemListResult.data.values.filterNotNull().run {
            assert(isNotEmpty())
        }
        println(itemListResult.data)
    }

    suspend fun getItemListData(): ItemListResource {
        val token = syncAPI.getCacheTokenOrRequest()
        val project = syncAPI.getProject(token, FuVersion.FU_AAR_VERSION).getOrThrow()
        val itemListVersion = project.data.list.first().item_list_version.toString()
        val itemListResult = syncAPI.getItemList(token, itemListVersion).getOrThrow()
        val itemListUrl = itemListResult.data["public"]!!
        val itemListResource = request.request(RequestWrapper(itemListUrl)).getOrThrow().let {
            jsonParser.parse(it, ItemListResource::class.java).getOrThrow()
        }
        return itemListResource
    }

    @Test
    fun getResourceSingleItem() = runBlockingTest {
        val fileId = "GAssets/camera/scene/cam_ptag_amale_texie.bundle"

        val token = syncAPI.getCacheTokenOrRequest()
        val itemListResource = getItemListData()
        val resourceId = itemListResource.list.first { it.path == fileId }.resource_id

        val fuResourceResult = syncAPI.getResourceSingleItem(token, resourceId).getOrThrow()
        println("下载链接：${fuResourceResult.data.url}")
    }

    @Test
    fun getAvatar() = runBlockingTest {
        val avatarIdList = mutableListOf(
            "hGkXsPkK",
            "1sMWmMjSQ"
        )
        val token = syncAPI.getCacheTokenOrRequest()
        avatarIdList.forEach { avatarId ->
            val avatarResult = syncAPI.getAvatar(token, avatarId).getOrThrow()
            val avatarJson = avatarResult.data.data
            println(avatarJson)
            assert(avatarJson.isNotBlank())
            assert(avatarJson != "{}")
            //TODO 验证 avatar.json 的有效性
        }
    }

    @Test
    fun uploadAvatar() = runBlockingTest {
        val jsonList = listOf(
            "avatarJson/1.json",
            "avatarJson/2.json",
        ).map { toTestFile(it) }
        val iconList = listOf(
            "avatarIcon/1.png",
            "avatarIcon/2.png",
        ).map { toTestFile(it) }
        val token = syncAPI.getCacheTokenOrRequest()
        jsonList.forEach { jsonFile ->
            val result =
                syncAPI.uploadAvatar(token, jsonFile.readText(), iconList.random()).getOrThrow()
            val avatarId = result.data.avatar_id
            println(avatarId)
        }
    }
}