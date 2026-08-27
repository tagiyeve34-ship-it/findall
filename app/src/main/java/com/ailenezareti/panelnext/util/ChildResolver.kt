package com.ailenezareti.panelnext.util

import android.content.Context
import com.ailenezareti.panelnext.Prefs
import com.ailenezareti.panelnext.api.ApiClient

object ChildResolver {
    suspend fun id(context: Context): Int {
        val saved = Prefs.childId(context)
        if (saved > 0) return saved
        val r = ApiClient.get(context).children()
        val c = r.body()?.children?.firstOrNull() ?: return 0
        Prefs.saveChild(context, c.id, c.name)
        return c.id
    }
}
