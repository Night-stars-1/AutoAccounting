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

package net.ankio.auto.ui.viewModes

import net.ankio.auto.utils.server.model.AppDataModel
import net.ankio.auto.utils.server.model.BaseModel

/**
 * AppDataFragment所有相关的数据绑定
 */
class AppDataViewModel: BaseViewModel<AppDataModel>() {
    override suspend fun fetchData(
        page: Int,
        pageSize: Int,
        params: HashMap<String, Any>
    ): MutableList<AppDataModel> {
        return BaseModel.get<AppDataModel>(page, pageSize, params) as MutableList<AppDataModel>
    }
}