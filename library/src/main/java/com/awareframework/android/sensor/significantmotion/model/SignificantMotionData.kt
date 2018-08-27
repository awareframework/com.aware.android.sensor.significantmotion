package com.awareframework.android.sensor.significantmotion.model

import com.awareframework.android.core.model.AwareObject
import com.google.gson.Gson

/**
 * Contains the significant motion data.
 *
 * @author  sercant
 * @date 27/08/2018
 */
data class SignificantMotionData(
        var isMoving: Boolean = false
) : AwareObject(jsonVersion = 1) {

    companion object {
        const val TABLE_NAME = "significantMotionData"
    }

    override fun toString(): String = Gson().toJson(this)
}