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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.lifecycle.lifecycleScope
import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentServiceBinding
import net.ankio.auto.exceptions.UnsupportedDeviceException
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.server.AutoServer
import java.io.File
import java.net.Socket

class ServiceFragment : BaseFragment() {
    private lateinit var binding: FragmentServiceBinding
    private lateinit var startShell: String
    private lateinit var stopShell: String
    private var cacheDir: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentServiceBinding.inflate(layoutInflater)

        //   EventBus.register(AutoServerConnectedEvent::class.java, onConnectedListener)

        initView()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activityBinding.toolbar.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        //   EventBus.unregister(AutoServerConnectedEvent::class.java, onConnectedListener)
    }

    private fun initView() {
        lifecycleScope.launch {
            cacheDir = AppUtils.getApplication().externalCacheDir
            if (cacheDir === null) {
                throw UnsupportedDeviceException(getString(R.string.unsupport_device))
            }
            AppUtils.getService().copyAssets()
        }
        startShell = "sh ${cacheDir!!.path}/shell/starter.sh"
        stopShell = "sh ${cacheDir!!.path}/shell/stop.sh"

        binding.start.setOnClickListener {
            // 启动服务
            serverByRoot(startShell)
            lifecycleScope.launch {
                // 检测52045端口是否开放
                val host = AutoServer.HOST.replace("ws://", "")
                val port = AutoServer.PORT

                withContext(Dispatchers.IO) {
                    while (!isPortOpen(host, port)) {
                        Logger.i("Waiting for port $port to open")
                        delay(1000)
                    }
                    withContext(Dispatchers.Main) {
                        AppUtils.restart()
                    }
                }
            }
        }
        binding.copyCommand.setOnClickListener {
            // 复制命令
            AppUtils.copyToClipboard("adb shell $startShell")
            Toaster.show(getString(R.string.copy_command_success))
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAffinity(requireActivity()) // 关闭所有活动并退出应用
                }
            },
        )
    }

    private fun isPortOpen(
        ip: String,
        port: Int,
    ): Boolean {
        return try {
            Socket(ip, port).use {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
