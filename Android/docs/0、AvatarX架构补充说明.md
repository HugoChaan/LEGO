## 初始化配置

项目的配置信息都位于 `FuDevInitializeWrapper` 类中。需要在里面修改为对应的配置，添加对应的证书。

模块加载与日志开关都在该类。

## 更新项目

主要依赖的相芯 SDK 有两个：core、factory。相关配置位于 `version.gradle`。

两个没有严格的版本对应，在特殊情况下可以只更新其中一个。

core 的版本与 assets 目录下的 graphics、model 里的 Bundle 是一一对应的。

## Demo 项目架构

项目采用了 MVVM 架构，参考 Google 推荐的 [应用架构指南](https://developer.android.com/jetpack/guide?hl=zh-cn)。UI 采用单 Activity 多 Fragment。

每个页面在 ui 包中有独立的配置，互相独立。

Demo 中还提供了一些公共 ViewModel，位于 view_model 包下，正常可直接调用相关 ViewModel 即可。（不推荐改动里面代码，这部分变动会比较频繁）



依赖在 `FuDependencyInjection` 中由 Koin 进行管理。大部分具体的实现位于 compat 包中，可自行替换为需要的实现。

目前为了兼容性考虑，SDK 未依赖 Koin，故需要手动将依赖项指定给 `FuDevDataCenter`。



用例位于 `use_case` 包中。该包中的是一些“不稳定”用例，为了方便使用而有前置依赖。使用前请查阅注释，确保前置依赖就绪。

## Demo 其他部分补充

### data_center 包

资源的管理和加载。SDK 对所有的资源都由 配置器（FuResourcePath）、加载器（FuResourceLoader）构成。接入者可根据自己需求实现对应自定义。

### edit_cloud 包

对 AppAssets 的部分配置采用了云端接口下发，以实现云端配置编辑组件的自定义。

注意：**接入者很可能需要将这部分接口和对应资源换成自己的服务器下发，避免与 Demo 使用了同一套配置。**

## Factory SDK 接口格式说明

### 返回格式

SDK 返回有三种类型：正常格式、Result、Flow。

#### Result<T>

Result 的使用文档：<https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/>

它有成功和失败两种状态，成功返回对应结果，失败则返回对应异常。相比传统的「返回 null」更不易出错且信息更多，相比「返回错误码」更直观方便，相比「try catch」处理起来更容易。

SDK 中返回值为 Result 的接口已经内部 try catch 了，不需要调用者额外捕获异常。一些常见的用法：

```kotlin
//申请加载某 Bundle
viewModel.clickItemByDispatcher(fileId).onSuccess { fileId ->
    //成功则添加历史，或者刷新 UI
    fuEditViewModel.pushHistory()
}.onFailure { throwable ->
    //失败则错误提示。或者判断异常类型，如果是可修复异常（如网络超时）则可根据需求弹出重试弹窗等。
    ToastUtils.showFailureToast(requireContext(), "穿戴失败：$throwable")
}
```

#### Flow<T>

Flow 的使用文档：<https://developer.android.com/kotlin/flow?hl=zh-cn>

它用于一些异步任务。返回的类型通常为 `sealed class`，根据进度返回一些结果。这些接口不会 try catch，需要在收集 Flow 的时候进行捕获。一些常见用法：

```kotlin
//最简单的直接调用
lifecycleScope.launchWhenResumed {
    viewModel.saveCurrentAvatar().catch {
        ToastUtils.showFailureToast(requireContext(), "形象保存失败：$it")
    }.collect {
        //获取结果后进行某些操作
        fuEditViewModel.syncAvatarEditStatus()
        ToastUtils.showSuccessToast(requireContext(), "形象已保存")
    }
}
//带 Loading、处理多个结果、对解析代码也 try catch 的示例
initCloudPlatformUseCase().onStart {
    //Loading 开始
    notifyLoadingState(LoadingState.ShowWithTip("正在请求服务器"))
}.onCompletion {
    //Loading 结束
    notifyLoadingState(LoadingState.Hidden)
}.onEach { //在 onEach 中执行业务处理，如果发生异常会被后续的 catch 捕获。
    when(it) {
        is InitCloudPlatformUseCase.State.GotToken -> {
            //获取 Token 之后做的事
        }
        is InitCloudPlatformUseCase.State.GotProject -> {
            //获取 Project 之后做的事
        }
    }
}.catch {
    //异常弹窗
    notifyExceptionEvent(ExceptionEvent.RetryDialog(tipContent, {initCloud()}))
}.collect()
```

#### 正常格式

正常格式则是传统的接口返回，通常来说返回 T? 则 null 代表失败。

### 异步接口

目前提供的异步接口采用了 协程 的方式提供，相关使用文档：<https://developer.android.com/kotlin/coroutines?hl=zh-cn>

它的优点在于流程清晰，避免层层回调，更方便地处理一些耗时任务，性能更优。对于接口中的协程函数(suspend fun)调用方式如下：

```kotlin
//ViewModel 中
viewModelScope.launch {
    //do something
}
//Activity、Fragment 中
lifecycleScope.launchWhenResumed {
    //do something
}
```



