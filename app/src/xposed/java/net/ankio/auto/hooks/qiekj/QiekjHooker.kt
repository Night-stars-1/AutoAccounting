/*
 * Copyright (C) 2024 Night-stars-1
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

package net.ankio.auto.hooks.qiekj

import android.content.Context
import net.ankio.auto.api.Hooker
import net.ankio.auto.api.PartHooker
import net.ankio.auto.hooks.qiekj.hooks.ResponseHooker


class QiekjHooker: Hooker(){
    override val packPageName: String = "com.qiekj.user"
    override val appName: String = "胖乖生活"
    override var partHookers: MutableList<PartHooker> = arrayListOf(
        ResponseHooker(this),
    )

    override fun hookLoadPackage(classLoader: ClassLoader, context: Context) {

    }


}


