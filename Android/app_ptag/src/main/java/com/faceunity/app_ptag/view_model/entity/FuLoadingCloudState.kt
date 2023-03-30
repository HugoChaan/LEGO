package com.faceunity.app_ptag.view_model.entity

/**
 *
 */
sealed class FuLoadingCloudState {
    class Start : FuLoadingCloudState()
    class Finish : FuLoadingCloudState()
}
