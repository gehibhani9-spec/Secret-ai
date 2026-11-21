package com.ultron.ai

import android.content.Context
import android.content.Intent

object Apps {
    /** Opens an installed app by its visible name. Returns false if no match. */
    fun launch(ctx: Context, name: String): Boolean {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(main, 0)
        val q = name.lowercase().trim().removePrefix("the ").removeSuffix(" app").trim()
        if (q.isEmpty()) return false
        fun label(i: Int) = list[i].loadLabel(pm).toString().lowercase()
        val idx = list.indices.firstOrNull { label(it) == q }
            ?: list.indices.firstOrNull { label(it).contains(q) }
            ?: list.indices.firstOrNull { label(it).length > 2 && q.contains(label(it)) }
            ?: return false
        val intent = pm.getLaunchIntentForPackage(list[idx].activityInfo.packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        return true
    }
}
