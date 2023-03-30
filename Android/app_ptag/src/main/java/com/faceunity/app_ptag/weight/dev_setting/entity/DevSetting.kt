package com.faceunity.app_ptag.weight.dev_setting.entity

/**
 *
 */
data class DevSetting(val title: String, val action: DevAction) {

    companion object {
        fun buildClick(title: String = "", action: () -> Unit): DevSetting {
            return DevSetting(title, DevAction.Click(action))
        }
    }
}

sealed class DevAction {
    class Click(val action: () -> Unit) : DevAction()
    class Switch(var nowState: Boolean, val action: (Boolean) -> Unit) : DevAction()
    class List(var nowText: String, val list: MutableList<Item>) : DevAction() {
        data class Item(val text: String, val action: () -> Unit)
    }
}

