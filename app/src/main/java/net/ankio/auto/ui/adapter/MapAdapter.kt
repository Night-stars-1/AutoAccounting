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

import kotlinx.coroutines.launch
import net.ankio.auto.databinding.AdapterMapBinding
import net.ankio.auto.utils.server.model.Assets
import net.ankio.auto.utils.server.model.AssetsMap

class MapAdapter(
    override val dataItems: List<AssetsMap>,
    private val onClick: (adapter: MapAdapter, item: AssetsMap, pos: Int) -> Unit,
    private val onLongClick: (adapter: MapAdapter, item: AssetsMap, pos: Int) -> Unit,
) : BaseAdapter(dataItems, AdapterMapBinding::class.java) {
    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int
    ) {
        val binding = holder.binding as AdapterMapBinding
        val item = dataItems[position]
        val context = holder.itemView.context

        onInitView(holder, item)

        // 图片加载丢到IO线程
        scope.launch {
            Assets.getDrawable(item.mapName, context).let { drawable ->
                binding.target.setIcon(drawable)
            }
        }

        binding.raw.text = item.name
        binding.target.setText(item.mapName)
    }

    private fun onInitView(holder: BaseViewHolder, item: AssetsMap) {
        val binding = holder.binding as AdapterMapBinding

        // 单击编辑
        binding.item.setOnClickListener {
            onClick(this@MapAdapter, item, dataItems.indexOf(item))
        }
        // 长按删除
        binding.item.setOnLongClickListener {
            onLongClick(this@MapAdapter, item, dataItems.indexOf(item))
            true
        }
    }
}
