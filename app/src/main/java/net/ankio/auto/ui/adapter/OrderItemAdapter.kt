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

import android.view.View
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ankio.auto.app.BillUtils
import net.ankio.auto.databinding.AdapterOrderItemBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.DateUtils
import net.ankio.auto.utils.Logger
import net.ankio.auto.utils.server.model.Assets
import net.ankio.auto.utils.server.model.BillInfo
import net.ankio.auto.utils.server.model.BookName
import net.ankio.auto.utils.server.model.Category
import net.ankio.common.model.AccountingConfig
import net.ankio.common.constant.BillType

class OrderItemAdapter(
    override val dataItems: MutableList<BillInfo>,
    private val onItemChildClick: ((item: BillInfo) -> Unit)?,
    private val onItemChildMoreClick: ((item: BillInfo) -> Unit)?,
) : BaseAdapter(dataItems, AdapterOrderItemBinding::class.java) {

    private fun onInitView(holder: BaseViewHolder, item: BillInfo) {
        val binding = holder.binding as AdapterOrderItemBinding
        binding.root.setOnClickListener {
            onItemChildClick?.invoke(item)
        }
        binding.moreBills.setOnClickListener {
            onItemChildMoreClick?.invoke(item)
        }

        binding.payTools.visibility = if (config.assetManagement) View.VISIBLE else View.GONE
    }

    private lateinit var config: AccountingConfig

    fun notifyConfig(autoAccountingConfig: AccountingConfig) {
        config = autoAccountingConfig
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int
    ) {
        val binding = holder.binding as AdapterOrderItemBinding
        val item = dataItems[position]
        val context = holder.itemView.context

        onInitView(holder, item)

        binding.category.setText(item.cateName)
        holder.scope.launch {
            val book = BookName.getDefaultBook(item.bookName)
            Category.getDrawable(item.cateName, book.id, context).let {
                withContext(Dispatchers.Main) {
                    binding.category.setIcon(it, true)
                }
            }
        }

        binding.date.text = DateUtils.getTime("HH:mm:ss", item.timeStamp)

        val type =
            when (BillType.fromInt(item.type)) {
                BillType.Expend -> BillType.Expend
                BillType.ExpendReimbursement -> BillType.Expend
                BillType.ExpendLending -> BillType.Expend
                BillType.ExpendRepayment -> BillType.Expend
                BillType.Income -> BillType.Income
                BillType.IncomeLending -> BillType.Income
                BillType.IncomeRepayment -> BillType.Income
                BillType.IncomeReimbursement -> BillType.Income
                BillType.Transfer -> BillType.Transfer
            }

        val symbols =
            when (type.toInt()) {
                0 -> "- "
                1 -> "+ "
                2 -> "→ "
                else -> "- "
            }

        val tintRes = BillUtils.getColor(type.toInt())

        val color = ContextCompat.getColor(context, tintRes)
        binding.money.setColor(color)

        binding.money.setText(symbols + item.money.toString())

        binding.remark.text = item.remark

        binding.payTools.setText(item.accountNameFrom)

        holder.scope.launch {
            Assets.getDrawable(item.accountNameFrom, context).let {
                binding.payTools.setIcon(it, false)
            }
            AppUtils.getAppInfoFromPackageName(item.fromApp, context)?.let {
                binding.fromApp.setImageDrawable(it.icon)
            }
        }

        val rule = item.channel
        val regex = "\\[(.*?)]".toRegex()
        val matchResult = regex.find(rule)
        if (matchResult != null) {
            val (value) = matchResult.destructured
            binding.channel.text = value
        } else {
            binding.channel.text = item.channel
        }

        //   binding.fromApp.setIcon()

        if (BillUtils.noNeedFilter(item)) {
            binding.moreBills.visibility = View.GONE
        }

        if (onItemChildMoreClick == null) {
            binding.moreBills.visibility = View.GONE
        }
    }
}
