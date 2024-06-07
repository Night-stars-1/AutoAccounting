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
package net.ankio.auto.utils.server.model

import android.content.Context
import android.widget.ImageView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.ImageUtils
import net.ankio.auto.utils.SpUtils
import net.ankio.common.config.Config
import net.ankio.common.model.AccountingConfig

class BookName {
    /**
     * id
     */
    var id: String = "0"

    /**
     * 账户名
     */
    var name: String = ""

    /**
     * 图标是url或base64编码字符串
     */
    var icon: String = "" // 图标

    companion object {
        fun put(book: BookName) {
            AppUtils.getScope().launch {
                AppUtils.getService().sendMsg("book/put", book)
            }
        }

        private suspend fun getOne(): BookName? {
            val data = AppUtils.getService().sendMsg("book/get/one", null)
            return runCatching { Gson().fromJson(Gson().toJson(data), BookName::class.java) }.getOrNull()
        }

        suspend fun getByName(name: String): BookName {
            val data = AppUtils.getService().sendMsg("book/get/name", mapOf("name" to name))
            return if (data !is JsonNull) {
                Gson().fromJson(Gson().toJson(data), BookName::class.java)
            } else {
                BookName().apply { this.name = name }
            }
        }

        suspend fun get(): List<BookName> {
            val data = AppUtils.getService().sendMsg("book/get/all", null)
            return if (data !is JsonNull) {
                Gson().fromJson(data as JsonArray, Array<BookName>::class.java).toList()
            } else {
                emptyList()
            }
        }

        suspend fun remove(name: String) {
            AppUtils.getService().sendMsg("book/remove", mapOf("name" to name))
        }

        suspend fun getDefaultBook(bookName: String): BookName {
            val localBookName = if (Config.multiBooks) {
                SpUtils.getString("defaultBook", "默认账本")
            } else {
                bookName
            }
            if (localBookName == "默认账本") {
                var book = getOne()
                if (book == null) {
                    book = BookName()
                    book.name = localBookName
                }
                return book
            } else {
                return getByName(localBookName )
            }
        }

        suspend fun getDrawable(
            bookName: String,
            context: Context,
            imageView: ImageView,
        ) {
            imageView.setImageDrawable(
                ImageUtils.get(
                    context,
                    getDefaultBook(bookName).icon,
                    R.drawable.default_book,
                ),
            )
        }
    }
}
