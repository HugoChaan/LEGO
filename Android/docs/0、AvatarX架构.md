相芯 AvatarX Android 端对外输出的可以分为三部分：

**FURenderKit**：相芯核心渲染 SDK。对应 `com.faceunity.gpb:core-*`

**FUAvatarFactory**：开发辅助 SDK。对应 `com.faceunity.gpb:avatar-factory`

**Demo**：集成上述 SDK 的一个展示型 Demo。



## FURenderKit





## FUAvatarFactory

遵循 Google 的 [应用架构指南](https://developer.android.com/jetpack/guide?hl=zh-cn)，将 SDK 划分为了 **数据层（Repository）**、**网域层（UseCase）**、**业务定制**、**工具类**。

### FUAvatarFactory - 数据层

每一个 Repository 分为两部分：一个是 抽象类的 `*Repository`，接入者直接继承即可使用相关能力；一个是继承上述类的单例 `Dev*Repository`，它提供了更便携的调用，和数据管理。

#### AvatarManagerRepository 形象管理

提供 Avatar 数据（json、图标）的增删改查。

#### SceneManagerRepository 场景管理

提供 Scene 数据（灯光、相机、背景等）的增删改查。

#### DrawRepository 渲染

封装 FURenderKit 的渲染接口，提供渲染 Scene、Avatar 到屏幕上的能力。

#### AIRepository 智能驱动

封装 FUAIKit 的驱动接口，提供 AR、面部驱动、身体驱动、手势驱动的相关能力。

#### EditRepository 编辑形象

提供了一套编辑菜单模型和其相关构造、管理功能；提供了一套捏脸模型和其相关构造、管理功能。

### FUAvatarFactory  - 网域层

网域层以一个个 UseCase 的方式提供，接入者可以直接实例化它以快速实现对应功能。

根据不同的职责划分为不同的包，相关 UseCase 如下：

| 用例名                        | 说明                                                         | 注意事项                                                     |
| ----------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **render_kit 包**             | 该包底下是渲染相关的 UseCase                                 |                                                              |
| CreateSceneUseCase            | 创建一个 Scene                                               | 该 UseCase 返回的是空 Scene，灯光背景等需要使用者添加。      |
| DecodeAvatarJsonUseCase       | 解析 avatar.json，返回结果会用在 CreateAvatarUseCase         | 解析到数据后，需要下载对应 Bundle （获取 Meta 信息）才能执行后续操作。 |
| CreateAvatarUseCase           | 根据 DecodeAvatarJsonUseCase 的解析结果，调用 FuDefaultAvatarConfigurator 进行补全，得到一个与 avatar.json 对应的 Avatar。并视选项绑定动画配置文件。 | 构造出的 Avatar 只是一个模型，它里面的 Bundle 可能还需要下载。它仅与 avatar.json  对应，动画需要额外添加。 |
| VerifySceneFileExistUseCase   | 验证该 Scene 的所有所需 Bundle 是否都已存在。                |                                                              |
| VerifyAvatarFileExistUseCase  | 验证该 Avatar 的所有所需 Bundle 是否都已存在。               |                                                              |
| **edit 包**                   | 该包底下是形象编辑相关的 UseCase                             |                                                              |
| ComponentModifyCheckUseCase   | 验证一个 Bundle 如果穿在该 Avatar 身上，需要加绑解绑哪些 Bundle（解决套装、兜底等逻辑） | 默认需要该 Avatar 处于渲染中。选项 **isBackgroundRenderAvatar** 可以在该 Avatar 没有渲染时生效。 |
| WearBundleUseCase             | 将 ComponentModifyCheckUseCase 的结果进行实际穿戴            | 如果在 ComponentModifyCheckUseCase 和 WearBundleUseCase 之间对 Avatar 进行过修改则会抛出 AvatarChangedThrowable，需要重新执行 ComponentModifyCheckUseCase。 |
| SaveAvatarJsonUseCase         | 保存当前 Avatar 为一个 json                                  | 需要在当前 Avatar 正在渲染时才能正确保存                     |
| **cloud_platform 包**         | 该包底下是云端相关的 UseCase                                 |                                                              |
| InitCloudPlatformUseCase      | 初始化云端接口，得到对应必要信息。                           |                                                              |
| RequestItemListUseCase        | 得到 ItemList 的数据。                                       |                                                              |
| DownloadAvatarConfigUseCase   | 根据传入的 Avatar ID，下载对应 avatar.json、avatar.png 到指定目录。 |                                                              |
| DownloadBundleUseCase         | 将传入的 fileId 列表进行下载                                 |                                                              |
| DownloadAvatarBundleUseCase   | 检查传入的 Avatar，计算其需要下载的 Bundle 列表，调用 DownloadBundleUseCase 下载。 |                                                              |
| UploadAvatarConfigUseCase     | 上传传入的 avatar.json，得到一个对应的 Avatar ID。           |                                                              |
| DownloadAvatarAndSceneUseCase | 检查传入的 AvatarInfo、SceneInfo，调用 DownloadBundleUseCase 下载需要的 Bundle。 |                                                              |

### FUAvatarFactory  - 工具类

有一些更独立的功能，它的复杂度没到需要转为 Repository，因此以工具类的形式提供。

#### FuDevDataCenter

各种依赖和实例的管理统一入口。

因为未在库中引入 Koin，故 SDK 提供的大部分实例都在该单例中索引。

#### FuCacheResource

部分全局共用资源的缓存。

主要是 itemlist.json 和 control_config.bundle 的缓存。

#### FuResourceManager

资源管理类，由 FuResourcePath 和 FuResourceLoader 构成。

获取方式：`FuDevDataCenter.resourceManager`

#### FuComponentModifyCheck

套装的校验工具类。用于编辑组件设置某个道具前的校验、Avatar 显示前的校验。

#### CloudResourceManager

资源的状态管理器。记录每个资源当前的状态（未下载、待更新、正常等）。

它的原理是：在新的 ItemList 获取后，与旧 ItemList 比较并更新相关状态。然后在每次下载完成后，调用该类 checkResourceStatus 接口校验资源（文件是否下载成功，Hash 校验正确等），并更新相关状态。或通过 getCloudResourceStatus 接口得到该资源的状态。

获取方式：`FuDevDataCenter.getCloudResourceManager()`

#### FuResourceCheck

基于上面 CloudResourceManager 封装的 一组资源获取状态 的工具类。

#### FuDefaultAvatarConfigurator

形象的默认衣服配置器。在显示与编辑 Avatar 时使用，避免 Avatar 出现裸体。

获取方式：`FuDevDataCenter.defaultAvatarConfigurator`

#### FuBundleMetaManager

资源的 Meta 信息管理器。可以获取或者刷新某个 Bundle 的 Meta 信息。

### FUAvatarFactory  - 业务定制

该部分为相芯核心渲染能力外的一些辅助能力。

#### cloud 相芯云服务

核心接口：ICloudPlatformSyncAPI

提供对接相芯云平台的相关能力。包括信息获取、资源下载、形象上传等。

#### pta 形象生成

核心接口：IPhotoToAvatarSyncControl

提供基于云服务的照片生成形象。

#### sta 语音驱动

核心接口：ISTARenderControl、ISTAServiceControl

提供语音驱动 Avatar 的能力，与基于相芯云服务的 ASR、NLP、TTS 能力。

#### facepup 捏脸定制业务

核心接口：IFacepupControl

该业务为可选能力，效果是：已知每个支持捏脸的部位都有一组道具。要能对每个道具进行独立的捏脸配置，并对其可以添加、编辑、重置。



## Demo

Demo 采用了 MVVM 架构。使用了 Koin 管理依赖，使用了 协程 管理任务调度，提供了公共 ViewModel 供接入者快速接入，提供了更业务性质的 UseCase 供接入者参考，对部分接口添加了 unitTest。

### Demo - 依赖

整个 SDK 与 Demo 都采用了依赖倒转的方式 管理第三方依赖。相关依赖管理都位于 `FuDependencyInjection` 单例中，接入者可定制相关依赖，更换实现方式。

### Demo - UseCase

Demo 提供了一些更具业务性质的 UseCase，接入者可视情况集成、修改。

| 用例名                       | 说明                                                         | 注意事项                                                     |
| ---------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **renderer 包**              | 该包底下是 Renderer 相关的 UseCase                           |                                                              |
| CreateRendererUseCase        | 创建一个指定场景的 Renderer                                  | 接入者可实现自己的 Renderer                                  |
| BindRendererListenerUseCase  | 绑定 Renderer 的一些事件监听                                 | 该部分目的为消除模板代码，接入者最好直接实现对应监听以应对更多需求。 |
| RendererBindLifecycleUseCase | 绑定 Renderer 的生命周期相关方法                             |                                                              |
| PhotoRecordUseCase           | 将输出数据转为 Bitmap                                        |                                                              |
| **renderer_kit 包**          | 一些更业务性质的渲染相关 UseCase                             |                                                              |
| RebuildAvatarUseCase         | 重建指定 AvatarInfo 里的 Avatar                              | 推荐在每个页面进入时，重建一次 Avatar 模型，避免特定情况下的数据错乱。（比如在播报后直接退出，嘴型未正确关闭，则首页的嘴型效果不对，重建 Avatar 即可解决类似问题） |
| SmartCreateSceneUseCase      | 创建一个配置了 scene_list.json 里对应数据的 Scene            | SDK 中的 CreateAvatarUseCase 创建的是一个空 Scene，该接口创建的是一个对应 scene_list.json 里包含灯光、相机、背景等的 Scene。它仅创建对应模型，持有的 Bundle 可能未下载。 |
| SmartCheckAvatarStateUseCase | 检查该 Avatar 对应的完整状态                                 |                                                              |
| SwitchCameraUseCase          | 根据 scene_list.json 里配置的相机名称，切换为对应的相机。    |                                                              |
| ShotSceneUseCase             | 将传入的 Scene 用后台渲染的方式（不会影响到渲染画面），生成一个 Bitmap。 |                                                              |
| SaveAvatarIconUseCase        | 根据 Avatar 的当前配置，通过后台渲染的方式生成一个大头照，并更新相关预览图。 |                                                              |
| RandomAvatarUseCase          | 随机生成一个 Avatar                                          | 该功能仅用于调试使用。可以借此熟悉构造 Avatar 的流程。       |
| **download 包**              | 该包底下是包含网络下载的 UseCase                             |                                                              |
| SmartPrepareAvatarUseCase    | 分析 Avatar 的状态，在 avatar.json 就绪的情况下，对需要下载的 Bundle 进行下载，构造出一个可以显示的 Avatar，并将其更新至 AvatarInfo。 | 需要该 AvatarInfo 的 avatar.json 已经就绪                    |

### Demo - ViewModel

Demo 提供了一些公共的 ViewModel，可供接入者快速使用实现某些功能。例如：只需要写好自己的 Activity，调用对应 ViewModel 即可实现相关功能。

#### FuPreviewViewModel 形象预览

对 Avatar 进行旋转、位移、缩放。

#### FuAvatarManagerViewModel 形象管理

对 Avatar 列表进行加载，切换等。

#### FuEditViewModel 形象编辑

对 Avatar 进行形象编辑、捏脸等。

#### FuBuildAvatarViewModel 形象生成

根据照片生成 Avatar。

#### FuDriveViewModel 形象驱动

开启 Avatar 的 AR 模式、面部驱动、身体驱动、文字驱动等。

#### FuStaViewModel 语音互动

对 Avatar 进行语音驱动，回答相关问题等。



Demo 中剩余部分为常规实现方式，不再赘述。