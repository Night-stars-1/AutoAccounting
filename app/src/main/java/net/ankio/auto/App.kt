/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package net.ankio.auto

import android.app.Application
import android.content.Context
import com.hjq.toast.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ankio.auto.broadcast.LocalBroadcastHelper
import net.ankio.auto.utils.ExceptionHandler
import net.ankio.auto.storage.SpUtils

class App : Application() {
    override fun onTerminate() {
        super.onTerminate()
        /**
         * 取消全局协程
         */
        job.cancel()
    }

    companion object{
        /* 本地广播 */
        lateinit var localBroadcastHelper: LocalBroadcastHelper
        /* App实例 */
        lateinit var app: Application
        /**
         * 是否是调试模式
         */
        var debug:Boolean = false
        /* 全局协程 */
        private val job = Job()
        private val scope = CoroutineScope(Dispatchers.IO + job)

        /**
         * 获取全局协程
         */
         fun launch(block: suspend CoroutineScope.() -> Unit) {
            scope.launch(block = block)
        }
    }

    /**
     * 初始化
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        app = this
        // 初始化调试模式
        debug = BuildConfig.DEBUG || SpUtils.getBoolean("debug", false)
        // 初始化本地广播
        localBroadcastHelper = LocalBroadcastHelper()
        // 设置全局异常
        ExceptionHandler.init(this)
        // 初始化 Toast 框架
        Toaster.init(this)
    }


}
