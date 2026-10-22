package com.mangazz.integration

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/** Rules are user-supplied, opt-in and bound to the exact HTTPS host. */
class SourceRulesStore(context:Context){
    private val prefs=context.applicationContext.getSharedPreferences("mangazz_css_rules",Context.MODE_PRIVATE)
    fun get(url:String):CssRules?{
        val host=Uri.parse(url).host?:return null
        val data=prefs.getString(host,null)?:return null
        return runCatching {
            val json=JSONObject(data)
            CssRules(json.getString("chapters"),json.getString("pages"))
        }.getOrNull()
    }
    fun exportRules(): JSONObject = JSONObject().apply {
        prefs.all.forEach { (host, value) ->
            if (value is String && host.length <= 255) put(host, value)
        }
    }
    fun importRules(json:JSONObject) {
        val edit=prefs.edit()
        json.keys().forEach { host ->
            if(host.length<=255 && host.matches(Regex("[A-Za-z0-9.-]+"))) {
                val value=json.optString(host)
                runCatching {
                    val parsed=JSONObject(value)
                    val chapters=parsed.getString("chapters")
                    val pages=parsed.getString("pages")
                    if(chapters.isNotBlank() && pages.isNotBlank() && chapters.length<=250 && pages.length<=250)
                        edit.putString(host,value)
                }
            }
        }
        edit.apply()
    }
    fun save(url:String,chapters:String,pages:String){
        val uri=Uri.parse(url)
        require(uri.scheme=="https" && !uri.host.isNullOrBlank()) { "HTTPS فقط" }
        require(chapters.isNotBlank() && pages.isNotBlank() && chapters.length<=250 && pages.length<=250) {"أدخل محدد CSS صحيح"}
        prefs.edit().putString(uri.host!!,JSONObject().put("chapters",chapters).put("pages",pages).toString()).apply()
    }
}
