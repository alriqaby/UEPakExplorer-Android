package com.example.uepakexplorer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val openRequest = 100
    private val saveRequest = 101
    private var selectedPath: String? = null
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var searchInput: EditText
    private lateinit var results: LinearLayout
    private lateinit var searchRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,24) }
        root.addView(TextView(this).apply { text = "UE PAK Explorer"; textSize = 24f; setPadding(0,0,0,16) })
        root.addView(Button(this).apply { text = "Open PAK"; setOnClickListener { choosePak() } })
        status = TextView(this).apply { text = "No PAK opened"; setPadding(0,16,0,8) }; root.addView(status)
        info = TextView(this); root.addView(info)
        searchInput = EditText(this).apply { hint = "Search: Game.locres / Localization / Fonts"; setSingleLine(true); visibility = View.GONE }; root.addView(searchInput)
        searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; visibility = View.GONE }
        searchRow.addView(Button(this).apply { text = "Search"; setOnClickListener { doSearch() } }, LinearLayout.LayoutParams(0,-2,1f))
        searchRow.addView(Button(this).apply { text = "Extract"; setOnClickListener { if (selectedPath == null) toast("Select a file first") else chooseOutput() } }, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(searchRow)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(results) }, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }

    private fun choosePak() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/octet-stream"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE,false); addCategory(Intent.CATEGORY_OPENABLE)
        }, openRequest)
    }
    private fun chooseOutput() {
        val name = selectedPath?.substringAfterLast('/') ?: "extracted.bin"
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/octet-stream"; putExtra(Intent.EXTRA_TITLE,name); addCategory(Intent.CATEGORY_OPENABLE)
        }, saveRequest)
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data); if(resultCode!=RESULT_OK) return
        val uri=data?.data ?: return
        if(requestCode==openRequest) openUri(uri) else if(requestCode==saveRequest) extractToUri(uri)
    }
    private fun openUri(uri:Uri) {
        try {
            val pfd=contentResolver.openFileDescriptor(uri,"r") ?: error("Could not open file")
            val fd=pfd.detachFd()
            Thread {
                val result=NativePak.openPak(fd,null)
                runOnUiThread { handleOpenResult(result) }
            }.start()
        } catch(t:Throwable) { status.text="Open failed: ${t.message}" }
    }
    private fun handleOpenResult(result:String) {
        try {
            val json=JSONObject(result)
            if(!json.optBoolean("ok")) { status.text="Open failed: ${json.optString("error")}"; return }
            status.text="PAK opened successfully"
            info.text="PAK version: ${json.optString("version")}\nEncryption: ${if(json.optBoolean("encryptedIndex")) "Encrypted" else "None"}\nFiles: ${json.optInt("fileCount")}\nMount: ${json.optString("mountPoint")}\nCompression: ${json.optString("compression")}"
            searchInput.visibility=View.VISIBLE; searchRow.visibility=View.VISIBLE
            showFiles(json.optJSONArray("files") ?: JSONArray())
        } catch(t:Throwable) { status.text="Invalid PAK response: ${t.message}" }
    }
    private fun showFiles(array:JSONArray) {
        results.removeAllViews(); val limit=minOf(array.length(),5000)
        for(i in 0 until limit) addResult(array.optString(i))
        if(array.length()>limit) results.addView(TextView(this).apply { text="Showing first $limit files. Use Search for the full index."; setPadding(8,12,8,12) })
    }
    private fun addResult(path:String) {
        results.addView(TextView(this).apply {
            text=path; textSize=14f; setPadding(8,10,8,10)
            setOnClickListener { selectedPath=path; status.text="Selected: $path" }
        })
    }
    private fun doSearch() {
        Thread {
            val json=JSONArray(NativePak.search(searchInput.text.toString().trim(),null))
            runOnUiThread {
                results.removeAllViews(); for(i in 0 until json.length()) addResult(json.optString(i)); status.text="Search results: ${json.length()}"
            }
        }.start()
    }
    private fun extractToUri(uri:Uri) {
        val path=selectedPath ?: return
        Thread {
            try {
                val pfd=contentResolver.openFileDescriptor(uri,"w") ?: error("Could not create output")
                val fd=pfd.detachFd(); val result=NativePak.extract(path,fd)
                runOnUiThread { status.text=result }
            } catch(t:Throwable) { runOnUiThread { status.text="Extract failed: ${t.message}" } }
        }.start()
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy(){ NativePak.closePak(); super.onDestroy() }
}
