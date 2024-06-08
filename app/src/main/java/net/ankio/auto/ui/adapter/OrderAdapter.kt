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

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.elevation.SurfaceColors
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ankio.auto.databinding.AdapterOrderBinding
import net.ankio.auto.ui.dialog.BillMoreDialog
import net.ankio.auto.ui.dialog.FloatEditorDialog
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.server.model.BillInfo
import net.ankio.common.model.AccountingConfig

class OrderAdapter(
    override val dataItems: ArrayList<Pair<String, Array<BillInfo>>>,
) : BaseAdapter(dataItems, AdapterOrderBinding::class.java) {

    private fun onInitView(holder: BaseViewHolder) {
        val binding = holder.binding as AdapterOrderBinding
        val context = holder.itemView.context
        binding.groupCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int
    ) {
        val binding = holder.binding as AdapterOrderBinding
        val context = holder.itemView.context

        onInitView(holder)

        val dataInnerItems = mutableListOf<BillInfo>()
        val layoutManager: LinearLayoutManager =
            object : LinearLayoutManager(context) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        binding.recyclerView.layoutManager = layoutManager

        val adapter =
            OrderItemAdapter(
                dataInnerItems,
                onItemChildClick = { itemBill ->
                    Logger.i("onItemChildClick1")
                    holder.scope.launch {
                        Logger.i("onItemChildClick2")
                        FloatEditorDialog(context, itemBill, config, onlyShow = true){ billInfo ->
                            val position1 = dataInnerItems.indexOfFirst { it.id == billInfo.id }
                            if (position1 != -1) {
                                dataInnerItems[position1].remark = billInfo.remark
                                notifyItemChanged(position1)
                            }
                        }.show(float=false, cancel=true)
                    }
                },
                onItemChildMoreClick = { itemBill ->
                    BillMoreDialog(context, itemBill).show(float=false, cancel=true)
                },
            )

        val items = dataItems[position]

        binding.recyclerView.adapter = adapter
        dataInnerItems.clear()
        dataInnerItems.addAll(items.second)
        adapter.notifyConfig(config)
        adapter.notifyDataSetChanged()
        binding.title.text = items.first
    }

    private lateinit var config: AccountingConfig

    fun notifyConfig(autoAccountingConfig: AccountingConfig) {
        config = autoAccountingConfig
    }
}
