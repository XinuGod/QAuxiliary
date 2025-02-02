/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.ketal.hook

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TabHost
import android.widget.TextView
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.plusAssign
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.isTim
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.util.findViewByType
import cc.ioctl.hook.sideswipe.SimplifyQQSettingMe
import io.github.qauxv.tlb.ConfigTable
import xyz.nextalone.util.*

@SuppressLint("StaticFieldLeak")
@FunctionHookEntry
@UiItemAgentEntry
object HideTab : CommonSwitchFunctionHook() {

    override val name = "隐藏底栏"
    override val description = "底栏项目移到侧滑"
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable: Boolean get() = !isTim()

    private lateinit var tab: TabHost

    override fun initOnce() = throwOrTrue {
        val clazz = "com.tencent.mobileqq.widget.QQTabHost".clazz ?: return@throwOrTrue
        for (m in clazz.declaredMethods) {
            if (m.name == "setOnTabSelectionListener") {
                m.hookBefore(this) {
                    tab = it.thisObject as TabHost
                    val blur = tab.findViewByType("com.tencent.mobileqq.widget.QQBlurView".clazz!!) as View
                    tab.tabWidget.isVisible = !isEnabled
                    blur.hide()
                }
            }
        }

        "com.tencent.mobileqq.activity.QQSettingMe".clazz?.hookAfterAllConstructors {
            if (!isEnabled) return@hookAfterAllConstructors
            val midContentName = ConfigTable.getConfig<String>(SimplifyQQSettingMe.MidContentName)
            val linearLayout = if (requireMinQQVersion(QQVersion.QQ_8_6_5)) {
                it.thisObject.get(midContentName, LinearLayout::class.java)
            } else {
                it.thisObject.get(midContentName, View::class.java) as LinearLayout
            } ?: return@hookAfterAllConstructors
            addSettingItem(linearLayout, "skin_tab_icon_conversation_normal", "消息") {
                tab.currentTab = 0
            }
            addSettingItem(linearLayout, "skin_tab_icon_contact_normal", "联系人") {
                tab.currentTab = 1
            }
            addSettingItem(linearLayout, "skin_tab_icon_plugin_normal", "动态") {
                tab.currentTab = tab.tabWidget.tabCount - 1
            }
        }
    }

    private fun addSettingItem(linearLayout: LinearLayout, resName: String, label: String, clickListener: View.OnClickListener) {
        val ctx = linearLayout.context
        val view = View.inflate(ctx, ctx.hostLayout("b2g")!!, null) as LinearLayout
        val imgView = view[0] as ImageView
        val textView = view[1] as TextView
        imgView.setImageResource(ctx.hostDrawable(resName)!!)
        textView.text = label
        view.setOnClickListener(clickListener)
        linearLayout += view
    }
}
