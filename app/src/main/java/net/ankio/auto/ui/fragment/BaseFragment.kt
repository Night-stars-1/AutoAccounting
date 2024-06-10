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

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.view.Menu
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityMainBinding
import net.ankio.auto.databinding.DialogProgressBinding
import net.ankio.auto.ui.activity.MainActivity
import net.ankio.auto.ui.utils.MenuItem
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

abstract class BaseFragment : Fragment() {
    open val menuList: ArrayList<MenuItem> = arrayListOf()

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    protected lateinit var activityBinding: ActivityMainBinding

    private var init = false
    lateinit var scrollView: View

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val mainActivity = activity as MainActivity
//        activityBinding = mainActivity.getBinding()
//        val toolbar = activityBinding.toolbar
//
//        toolbar.addMenuProvider(object : MenuProvider {
//            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                // 使用菜单填充器将菜单项添加到传入的菜单对象中
//                menuInflater.inflate(R.menu.home_top_menu, menu)
//            }
//
//            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
//                return when (menuItem.itemId) {
//                    else -> false
//                }
//            }
//        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
//    }

    override fun onResume() {
        super.onResume()
        val mainActivity = activity as MainActivity
        if (!this::activityBinding.isInitialized) {
            activityBinding = mainActivity.getBinding()
        }
        if (init)return
        activityBinding.toolbar.visibility = View.VISIBLE
        // 重置顶部导航栏图标
        activityBinding.toolbar.menu.clear()
        // 添加菜单
        menuList.forEach {
            addMenuItem(it)
        }
        if (mainActivity.toolbarLayout != null && ::scrollView.isInitialized) {
            var animatorStart = false
            // 滚动页面调整toolbar颜色
            scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                var scrollYs = scrollY // 获取宽度
                if (scrollView is RecyclerView) {
                    // RecyclerView获取真实高度
                    scrollYs = (scrollView as RecyclerView).computeVerticalScrollOffset()
                }

                if (animatorStart) return@setOnScrollChangeListener

                if (scrollYs.toFloat() > 0) {
                    if (mainActivity.last != mainActivity.mStatusBarColor2) {
                        animatorStart = true
                        viewBackgroundGradientAnimation(
                            mainActivity.toolbarLayout!!,
                            mainActivity.mStatusBarColor!!,
                            mainActivity.mStatusBarColor2!!,
                        )
                        mainActivity.last = mainActivity.mStatusBarColor2
                    }
                } else {
                    if (mainActivity.last != mainActivity.mStatusBarColor) {
                        animatorStart = true
                        viewBackgroundGradientAnimation(
                            mainActivity.toolbarLayout!!,
                            mainActivity.mStatusBarColor2!!,
                            mainActivity.mStatusBarColor!!,
                        )
                        mainActivity.last = mainActivity.mStatusBarColor
                    }
                }
                animatorStart = false
            }

            scrollView.addNavigationBarBottomPadding()
        }
    }

    private fun addMenuItem(menuItemObject: MenuItem) {
        val menu = activityBinding.toolbar.menu
        val menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, getString(menuItemObject.title))
        menuItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        val icon = AppCompatResources.getDrawable(requireActivity(), menuItemObject.drawable)
        if (icon != null) {
            menuItem.setIcon(icon)
            DrawableCompat.setTint(
                icon,
                AppUtils.getThemeAttrColor(com.google.android.material.R.attr.colorOnBackground),
            )
        }
        menuItem.setOnMenuItemClickListener {
            menuItemObject.callback.invoke((activity as MainActivity).getNavController())
            true
        }
    }

    /**
     * toolbar颜色渐变动画
     */
    private fun viewBackgroundGradientAnimation(
        view: View,
        fromColor: Int,
        toColor: Int,
        duration: Long = 600,
    ) {
        val colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int // 之后就可以得到动画的颜色了
            view.setBackgroundColor(color) // 设置一下, 就可以看到效果.
        }
        colorAnimator.duration = duration
        colorAnimator.start()
    }

    fun serverByRoot(shell: String) {
        val dialogBinding = DialogProgressBinding.inflate(layoutInflater)
        val textView = dialogBinding.progressText
        val scrollView = dialogBinding.scrollView
        val progressDialog =
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.title_command)
                .setView(dialogBinding.root)
                .setCancelable(false) // 设置对话框不可关闭
                .show()

        // 在协程中检查 root 权限并执行命令
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("su -t 1")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val bufferedWriter = OutputStreamWriter(process.outputStream)

                Logger.i("Executing shell command: $shell")

                // 写入命令
                bufferedWriter.write(shell + "\n")
                bufferedWriter.flush()
                bufferedWriter.close()

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        // 更新 TextView 来显示命令输出
                        textView.append(line + "\n")
                        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                    }
                }
                // 读取错误输出
                while (errorReader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        // 更新 TextView 来显示命令输出
                        textView.append(" [ERROR] $line\n")
                        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                    }
                }
                // 等待进程结束
                val exitCode = process.waitFor()
                withContext(Dispatchers.Main) {
                    if (exitCode == 0) {
                        textView.append(" [SUCCESS] 命令执行成功\n")
                    } else {
                        textView.append(" [FAILURE] 命令执行失败，退出代码: $exitCode\n")
                    }
                    scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                }
                process.waitFor()
                bufferedReader.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.e("Error executing shell command", e)
                withContext(Dispatchers.Main) {
                    textView.append(getText(R.string.no_root_permission))
                }
            } finally {
                progressDialog.setCancelable(true) // 设置对话框可关闭
                // 等待5秒钟关闭对话框
                delay(5000L)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            }
        }
    }
}
