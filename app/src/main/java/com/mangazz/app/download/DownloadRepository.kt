package com.mangazz.app.download

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.mangazz.app.data.safeWebUrl

data class DownloadEntry(val id:Long,val name:String,val state:String,val bytes:Long,val total:Long,val reason:Int){
    val percent:Int get()=if(total>0L) ((bytes*100)/total).toInt().coerceIn(0,100) else 0
}

/** Only user-supplied, direct HTTPS files. No site scraping or DRM bypass. */
class DownloadRepository(context:Context){
    private val app=context.applicationContext
    private val manager=app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs=app.getSharedPreferences("mangazz_downloads",Context.MODE_PRIVATE)
    fun enqueue(url:String):Long {
        val valid=safeWebUrl(url)
        val uri=Uri.parse(valid)
        val filename=(uri.lastPathSegment?.substringBefore('?')?.take(70)?.replace(Regex("[^a-zA-Z0-9._-]"),"_") ?: "file")
            .ifBlank{"download"}.let{if(it.startsWith("."))"download$it" else it}
        val request=DownloadManager.Request(uri)
            .setTitle(filename)
            .setDescription("Manga ZZ — تنزيل مباشر اختاره المستخدم")
            .setAllowedOverMetered(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(app,Environment.DIRECTORY_DOWNLOADS,filename)
        val id=manager.enqueue(request)
        prefs.edit().putString(id.toString(),filename).apply()
        return id
    }
    fun list():List<DownloadEntry>{
        val ids=prefs.all.keys.mapNotNull{it.toLongOrNull()}.sortedDescending()
        if(ids.isEmpty())return emptyList()
        val query=DownloadManager.Query().setFilterById(*ids.toLongArray())
        val entries=mutableListOf<DownloadEntry>()
        manager.query(query)?.use { cursor ->
            val idCol=cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val statusCol=cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val doneCol=cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalCol=cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val reasonCol=cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
            while(cursor.moveToNext()){
                val id=cursor.getLong(idCol)
                val state=when(cursor.getInt(statusCol)){
                    DownloadManager.STATUS_PENDING->"في الانتظار"
                    DownloadManager.STATUS_RUNNING->"جارٍ التنزيل"
                    DownloadManager.STATUS_PAUSED->"متوقف مؤقتًا"
                    DownloadManager.STATUS_SUCCESSFUL->"مكتمل"
                    DownloadManager.STATUS_FAILED->"فشل"
                    else->"غير معروف"
                }
                entries.add(DownloadEntry(id,prefs.getString(id.toString(),"ملف")?:"ملف",state,cursor.getLong(doneCol),cursor.getLong(totalCol),cursor.getInt(reasonCol)))
            }
        }
        return entries
    }
    fun remove(id:Long){manager.remove(id);prefs.edit().remove(id.toString()).apply()}
}
