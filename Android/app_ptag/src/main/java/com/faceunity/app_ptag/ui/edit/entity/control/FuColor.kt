package com.faceunity.app_ptag.ui.edit.entity.control

/**
 *
 */
class FuColor {

    val red: Int
    val green: Int
    val blue: Int
    val intensity: Float?

    constructor(r: Int, g: Int, b: Int, intensity: Float? = null) {
        this.red = r
        this.green = g
        this.blue = b
        this.intensity = intensity ?: 255f
    }

    fun color(): Int {
        return -0x1000000 or (red shl 16) or (green shl 8) or blue
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuColor) return false

        if (red != other.red) return false
        if (green != other.green) return false
        if (blue != other.blue) return false
        if (intensity != other.intensity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = red
        result = 31 * result + green
        result = 31 * result + blue
        result = 31 * result + (intensity?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "FuColor(red=$red, green=$green, blue=$blue, intensity=$intensity)"
    }


}