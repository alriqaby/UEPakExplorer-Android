package com.example.uepakexplorer
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val openRequest = 100
    private val saveRequest = 101

    private var selectedPath: String? = null

    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var searchInput: EditText
    private lateinit var results: LinearLayout
    private lateinit var searchRow: LinearLayout
    private lateinit var themeButton: TextView

    private var darkMode = false

    private val bgLight = Color.parseColor("#F6F7F9")
    private val surfaceLight = Color.WHITE
    private val textLight = Color.parseColor("#202124")
    private val secondaryLight = Color.parseColor("#5F6368")
    private val accentLight = Color.parseColor("#2563EB")

    private val bgDark = Color.parseColor("#121212")
    private val surfaceDark = Color.parseColor("#1E1E1E")
    private val textDark = Color.parseColor("#F1F3F4")
    private val secondaryDark = Color.parseColor("#B8BCC2")
    private val accentDark = Color.parseColor("#8AB4F8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkMode = isSystemDarkMode()

        buildInterface()
        applyTheme()
    }

    private fun isSystemDarkMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleBox.addView(TextView(this).apply {
            text = "UEPak Explorer"
            textSize = 25f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        titleBox.addView(TextView(this).apply {
            text = "Unreal Engine PAK Explorer"
            textSize = 13f
        })

        topBar.addView(
            titleBox,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        themeButton = TextView(this).apply {
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener {
                darkMode = !darkMode
                applyTheme()
            }
        }

        topBar.addView(
            themeButton,
            LinearLayout.LayoutParams(dp(52), dp(52))
        )

        root.addView(
            topBar,
            LinearLayout.LayoutParams(-1, -2)
        )

        // Open PAK
        val openButton = makeButton("Open PAK") {
            choosePak()
        }

        val openParams = LinearLayout.LayoutParams(
            -1,
            dp(52)
        )
        openParams.topMargin = dp(18)
        root.addView(openButton, openParams)

        status = TextView(this).apply {
            text = "No PAK opened"
            textSize = 14f
            setPadding(dp(4), dp(14), dp(4), dp(8))
        }
        root.addView(status)

        info = TextView(this).apply {
            textSize = 13f
            setPadding(dp(4), 0, dp(4), dp(10))
        }
        root.addView(info)

        // Search input
        searchInput = EditText(this).apply {
            hint = "Search files, Localization, Fonts..."
            setSingleLine(true)
            visibility = View.GONE
            setPadding(dp(14), 0, dp(14), 0)
        }

        val searchParams = LinearLayout.LayoutParams(
            -1,
            dp(52)
        )
        searchParams.bottomMargin = dp(10)
        root.addView(searchInput, searchParams)

        // Search buttons
        searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }

        searchRow.addView(
            makeButton("Search") {
                doSearch()
            },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(6)
            }
        )

        searchRow.addView(
            makeButton("Extract") {
                if (selectedPath == null) {
                    toast("Select a file first")
                } else {
                    chooseOutput()
                }
            },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(6)
            }
        )

        root.addView(
            searchRow,
            LinearLayout.LayoutParams(-1, -2)
        )

        // Results
        results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(results)
        }

        val scrollParams = LinearLayout.LayoutParams(
            -1,
            0,
            1f
        )
        scrollParams.topMargin = dp(12)

        root.addView(scroll, scrollParams)

        setContentView(root)
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setOnClickListener { action() }
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(8), 0, dp(8), 0)
        }
    }

    private fun applyTheme() {
        val bg = if (darkMode) bgDark else bgLight
        val surface = if (darkMode) surfaceDark else surfaceLight
        val primaryText = if (darkMode) textDark else textLight
        val secondaryText = if (darkMode) secondaryDark else secondaryLight
        val accent = if (darkMode) accentDark else accentLight

        root.setBackgroundColor(bg)

        status.setTextColor(primaryText)
        info.setTextColor(secondaryText)

        searchInput.setTextColor(primaryText)
        searchInput.setHintTextColor(secondaryText)
        searchInput.setBackgroundColor(surface)

        themeButton.text = if (darkMode) "☀️" else "🌙"
        themeButton.setTextColor(primaryText)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        val flags = if (!darkMode) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            0
        }

        window.decorView.systemUiVisibility = flags

        updateTextColors(root, primaryText, secondaryText)

        updateButtons(root, accent, surface)
    }

    private fun updateTextColors(
        view: View,
        primary: Int,
        secondary: Int
    ) {
        when (view) {
            is TextView -> {
                if (view !== themeButton &&
                    view !== searchInput &&
                    view !is Button
                ) {
                    view.setTextColor(primary)
                }
            }
        }

        if (view is LinearLayout) {
            for (i in 0 until view.childCount) {
                updateTextColors(view.getChildAt(i), primary, secondary)
            }
        }
    }

    private fun updateButtons(
        view: View,
        accent: Int,
        surface: Int
    ) {
        if (view is Button) {
            view.setTextColor(Color.WHITE)

            val drawable = GradientDrawable().apply {
                setColor(accent)
                cornerRadius = dp(12).toFloat()
            }

            view.background = drawable
        }

        if (view is LinearLayout) {
            for (i in 0 until view.childCount) {
                updateButtons(view.getChildAt(i), accent, surface)
            }
        }
    }

    private fun choosePak() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            openRequest
        )
    }

    private fun chooseOutput() {
        val name = selectedPath?.substringAfterLast('/')
            ?: "extracted.bin"

        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, name)
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            saveRequest
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        val uri = data?.data ?: return

        if (requestCode == openRequest) {
            openUri(uri)
        } else if (requestCode == saveRequest) {
            extractToUri(uri)
        }
    }

    private fun openUri(uri: Uri) {
        try {
            val pfd = contentResolver.openFileDescriptor(uri, "r")
                ?: error("Could not open file")

            val fd = pfd.detachFd()

            Thread {
                val result = NativePak.openPak(fd, null)

                runOnUiThread {
                    handleOpenResult(result)
                }
            }.start()

        } catch (t: Throwable) {
            status.text = "Open failed: ${t.message}"
        }
    }

    private fun handleOpenResult(result: String) {
        try {
            val json = JSONObject(result)

            if (!json.optBoolean("ok")) {
                status.text = "Open failed: ${json.optString("error")}"
                return
            }

            status.text = "PAK opened successfully"

            info.text =
                "PAK version: ${json.optString("version")}\n" +
                "Encryption: ${
                    if (json.optBoolean("encryptedIndex"))
                        "Encrypted"
                    else
                        "None"
                }\n" +
                "Files: ${json.optInt("fileCount")}\n" +
                "Mount: ${json.optString("mountPoint")}\n" +
                "Compression: ${json.optString("compression")}"

            searchInput.visibility = View.VISIBLE
            searchRow.visibility = View.VISIBLE

            showFiles(
                json.optJSONArray("files") ?: JSONArray()
            )

        } catch (t: Throwable) {
            status.text =
                "Invalid PAK response: ${t.message}"
        }
    }

    private fun showFiles(array: JSONArray) {
        results.removeAllViews()

        val limit = minOf(array.length(), 5000)

        for (i in 0 until limit) {
            addResult(array.optString(i))
        }

        if (array.length() > limit) {
            results.addView(
                TextView(this).apply {
                    text =
                        "Showing first $limit files. Use Search for the full index."
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                }
            )
        }
    }

    private fun addResult(path: String) {
        val item = TextView(this).apply {
            text = path
            textSize = 14f
            setPadding(dp(12), dp(14), dp(12), dp(14))

            setOnClickListener {
                selectedPath = path
                status.text = "Selected: $path"
            }
        }

        results.addView(item)

        val divider = View(this).apply {
            setBackgroundColor(
                if (darkMode)
                    Color.parseColor("#303030")
                else
                    Color.parseColor("#E0E0E0")
            )
        }

        results.addView(
            divider,
            LinearLayout.LayoutParams(-1, 1)
        )
    }

    private fun doSearch() {
        Thread {
            try {
                val query =
                    searchInput.text.toString().trim()

                val json =
                    JSONArray(
                        NativePak.search(query, null)
                    )

                runOnUiThread {
                    results.removeAllViews()

                    for (i in 0 until json.length()) {
                        addResult(json.optString(i))
                    }

                    status.text =
                        "Search results: ${json.length()}"
                }

            } catch (t: Throwable) {
                runOnUiThread {
                    status.text =
                        "Search failed: ${t.message}"
                }
            }
        }.start()
    }

    private fun extractToUri(uri: Uri) {
        val path = selectedPath ?: return

        Thread {
            try {
                val pfd =
                    contentResolver.openFileDescriptor(uri, "w")
                        ?: error("Could not create output")

                val fd = pfd.detachFd()

                val result =
                    NativePak.extract(path, fd)

                runOnUiThread {
                    status.text = result
                }

            } catch (t: Throwable) {
                runOnUiThread {
                    status.text =
                        "Extract failed: ${t.message}"
                }
            }
        }.start()
    }

    private fun toast(s: String) {
        Toast.makeText(
            this,
            s,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        NativePak.closePak()
        super.onDestroy()
    }
}
