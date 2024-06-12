/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.hooks.qiekj.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import de.robv.android.xposed.XposedBridge
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.TimeUtils.toTimestamp

class OrderDetail {
    /**
     * 机器名称
     */
    var machineName: String = ""

    /**
     * 功能名称
     */
    var machineFunctionName: String = ""

    /**
     * 订单状态 0->已支付 1->已失效 3->以完成
     */
    var orderStatus: Int = 0

    var payPrice: Double = 0.0

    var payTime: Long = 0

    var payTypeName: String = ""

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun parse(body: String): OrderDetail {
            val jsonObject = JsonParser.parseString(body).asJsonObject
            val dataObject = jsonObject.getAsJsonObject("data")
            val orderDetail = OrderDetail()
            orderDetail.machineName = dataObject.get("machineName").asString
            orderDetail.machineFunctionName = dataObject.get("machineFunctionName").asString
            orderDetail.orderStatus = dataObject.get("orderStatus").asInt
            orderDetail.payPrice = dataObject.get("payPrice").asDouble
            if (dataObject.get("payTime") != null) {
                orderDetail.payTime = toTimestamp(dataObject.get("payTime").asString)
                orderDetail.payTypeName = dataObject.get("payTypeName").asString
            }
            return orderDetail
        }
    }
}
