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

package net.ankio.auto.ui.adapter

import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterLogBinding
import net.ankio.auto.utils.server.model.LogModel

class LogAdapter(
    override val dataItems: ArrayList<LogModel>,
) : BaseAdapter(dataItems, AdapterLogBinding::class.java) {

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int
    ) {
        val binding = holder.binding as AdapterLogBinding
        val context = holder.itemView.context
        val item = dataItems[position]
        val level = item.level
        binding.logDate.text = item.date
        binding.app.text = item.app
        binding.logThread.text = item.thread
        binding.logFile.text = item.line
        // 数据需要进行截断处理，防止过长导致绘制ANR
        binding.log.text = item.log
        when (level) {
            LogModel.LOG_LEVEL_DEBUG -> binding.log.setTextColor(context.getColor(R.color.success))
            LogModel.LOG_LEVEL_INFO -> binding.log.setTextColor(context.getColor(R.color.info))
            LogModel.LOG_LEVEL_WARN -> binding.log.setTextColor(context.getColor(R.color.warning))
            LogModel.LOG_LEVEL_ERROR -> binding.log.setTextColor(context.getColor(R.color.danger))
            else -> binding.log.setTextColor(context.getColor(R.color.info))
        }
    }
}
