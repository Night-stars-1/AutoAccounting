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

package net.ankio.auto.utils

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeUtils {

    /**
     * 将时间字符串转换为时间戳（秒）
     * @param dateTimeString 要转换的时间字符串，格式为 "yyyy-MM-dd HH:mm:ss"
     * @return 转换后的时间戳（秒），如果解析失败，返回 null
     */
    fun toTimestamp(dateTimeString: String): Long {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
            localDateTime.toEpochSecond(ZoneOffset.ofHours(8))
        } catch (e: Exception) {
            0
        }
    }
}