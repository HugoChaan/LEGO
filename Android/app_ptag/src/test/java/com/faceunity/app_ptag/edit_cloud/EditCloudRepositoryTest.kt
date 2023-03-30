package com.faceunity.app_ptag.edit_cloud

import com.faceunity.editor_ptag.data_center.FuVersion
import com.faceunity.fupta.avatar_data.entity.resource.EditItemListResource
import com.faceunity.fupta.cloud_download.CloudSyncRepository
import com.faceunity.fupta.cloud_download.entity.CloudConfig
import com.faceunity.fupta.cloud_download.getCacheTokenOrRequest
import com.faceunity.fupta.cloud_download.parser.IFuAPIParser
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.koin.core.component.get
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import util.KoinDITest

/**
 *
 */
class EditCloudRepositoryTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(KoinDITest.utilsModel + KoinDITest.cloudModel)
    }

    val syncAPI by lazy {
        get<CloudSyncRepository>()
    }

    val editRepo by lazy {
        EditCloudRepository(
            request = get(),
            domain = get<CloudConfig>().domain,
            jsonParser = get(),
            coroutineContext = get()
        )
    }

    private suspend fun getItemListVersion(): String {
        val token = syncAPI.getCacheTokenOrRequest()
        val projectResult = syncAPI.getProject(token, FuVersion.FU_AAR_VERSION).getOrThrow()
        return projectResult.data.list.first().item_list_version.toString()
    }

    @Test
    fun centreConfig() = runBlockingTest {
        val token = syncAPI.getCacheTokenOrRequest()
        editRepo.token = token
        val itemListVersion = getItemListVersion()
        val centreConfig = editRepo.centreConfig(itemListVersion).getOrThrow()
        println(centreConfig)
        centreConfig.run {
            assert(code == 0)
            assert(data != null)
            assert(data!!.editItemListPath.isNotBlank())
        }
    }

    private suspend fun getEditItemListUrl(): String {
        val token = syncAPI.getCacheTokenOrRequest()
        editRepo.token = token
        val itemListVersion = getItemListVersion()
        val centreConfig = editRepo.centreConfig(itemListVersion).getOrThrow()
        return centreConfig.data!!.editItemListPath
    }

    @Test
    fun verifyEditItemList() = runBlockingTest {
        val url = getEditItemListUrl()
        val json = editRepo.loadContent(url).getOrThrow()
        val jsonParser = get<IFuAPIParser>()
        val editItemList = jsonParser.parse(json, EditItemListResource::class.java).getOrThrow()
        assert(editItemList.map.isNotEmpty())
        editItemList.map.forEach { (type, itemList) ->
            val maleList = itemList.filter { it.filter?.get("gender") == "male" }
            val femaleList = itemList.filter { it.filter?.get("gender") == "female" }
            maleList.forEach { item ->
                assert(!item.path!!.contains("AsiaFemale"))
                assert(item.icon_url != null)
            }
            femaleList.forEach { item ->
                assert(!item.path!!.contains("AsiaMale"))
                assert(item.icon_url != null)
            }
            maleList.filter { it.path!!.endsWith("_none.bundle") }.let { noneItemList ->
                assert(noneItemList.size in 0..1) //一个列表中置空按钮只能有 0 或 1 个。
                if (noneItemList.size == 1) {
                    assert(noneItemList[0] == maleList.first()) //如果有置空按钮，它应该在第一个。
                }
            }
            femaleList.filter { it.path!!.endsWith("_none.bundle") }.let { noneItemList ->
                assert(noneItemList.size in 0..1) //一个列表中置空按钮只能有 0 或 1 个。
                if (noneItemList.size == 1) {
                    assert(noneItemList[0] == femaleList.first()) //如果有置空按钮，它应该在第一个。
                }
            }
        }
    }
}