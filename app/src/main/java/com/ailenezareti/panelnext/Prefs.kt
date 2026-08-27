package com.ailenezareti.panelnext

import android.content.Context

object Prefs {
    private const val NAME = "panelnext_prefs"
    private const val TOKEN = "token"
    private const val CHILD = "child_id"
    private const val CHILD_NAME = "child_name"
    fun token(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(TOKEN, "") ?: ""
    fun saveToken(c: Context, token: String) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(TOKEN, token).apply()
    fun childId(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(CHILD, 0)
    fun childName(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(CHILD_NAME, "Uşaq") ?: "Uşaq"
    fun saveChild(c: Context, id: Int, name: String) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putInt(CHILD, id).putString(CHILD_NAME, name).apply()
    fun clear(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().apply()
}
