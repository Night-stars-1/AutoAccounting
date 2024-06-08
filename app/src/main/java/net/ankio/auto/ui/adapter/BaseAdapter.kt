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

package net.ankio.auto.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class BaseAdapter(open val dataItems: List<Any>, private val viewBindingClazz: Class<*>) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder>() {
    open inner class BaseViewHolder(open val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)

    private val job = Job()
    protected val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder {
        // 反射判断viewBindingClazz是否为viewbing的子类，并存在inflate方法，存在就调用
        val inflateMethod =
            viewBindingClazz.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java,
            )
        val viewBinding = inflateMethod.invoke(null, LayoutInflater.from(parent.context), parent, false) as ViewBinding
        return BaseViewHolder(viewBinding)
    }

    override fun getItemCount(): Int {
        return dataItems.size
    }

//    fun getHolderIndex(holder: BaseViewHolder): Int {
//        return dataItems.lastIndexOf(holder.item)
//    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (!job.isCancelled) job.cancel()
    }
}
