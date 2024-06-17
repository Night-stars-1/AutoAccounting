/*
 * Copyright (C) 2024 Night-stars-1
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

package net.ankio.auto.hooks.qiekj.hooks

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.constant.DataType
import net.ankio.auto.hooks.qiekj.model.OrderDetail
import okhttp3.Response

class ResponseHooker(hooker: Hooker) : PartHooker(hooker){
    override val hookName: String
        get() = "网络请求hook"

    override fun onInit(classLoader: ClassLoader, context: Context) {
        val responseClass = classLoader.loadClass(Response::class.java.name)
        XposedHelpers.findAndHookConstructor(
            responseClass,
            "okhttp3.Response\$Builder",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val request = XposedHelpers.getObjectField(param.args[0], "request")
                    val url = XposedHelpers.callMethod(request, "url")
                    val responseBody = XposedHelpers.getObjectField(param.args[0], "body")
                    if (responseBody != null && responseBody::class.java.canonicalName?.trim().equals("okhttp3.internal.http.RealResponseBody".trim())) {
                        try {
                            val contentType =
                                XposedHelpers.callMethod(responseBody, "contentType")
                            if (contentType.toString().contains("application/json")) {
                                val source = XposedHelpers.getObjectField(responseBody, "source")
                                val buffer = XposedHelpers.callMethod(source, "buffer")
                                val copy = XposedHelpers.callMethod(buffer, "clone")
                                val body = XposedHelpers.callMethod(copy, "readUtf8").toString()
                                if (body.isNotEmpty()) {
                                    val result: OrderDetail? = when (url.toString()) {
                                        "https://userapi.qiekj.com/order/detail" -> OrderDetail.parse(
                                            body
                                        )
                                        else -> null
                                    }
                                    if (result != null) {
                                         analyzeData(DataType.App.ordinal, result.toJson())
                                    }
                                }
                            }
                        } catch (_: NoSuchFieldError) {

                        }
                    }
                }
            }
        )
    }
}