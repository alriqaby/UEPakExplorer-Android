package com.example.uepakexplorer
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Bundle
import android.view.View
import android.widget.*
import java.io.File
import java.io.FileInputStream
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val openRequest = 100
    private val saveRequest = 101
    private val saveFolderRequest = 102

    private var selectedPath: String? = null
    private val selectedPaths = linkedSetOf<String>()
    private var selectionMode = false

    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var searchInput: EditText
    private lateinit var results: LinearLayout
    private lateinit var searchRow: LinearLayout
    private lateinit var selectionBar: LinearLayout
    private lateinit var selectionCount: TextView
    private lateinit var selectAllButton: Button
    private lateinit var clearSelectionButton: Button
    private lateinit var themeButton: ImageButton

    private var extractionDialog: Dialog? = null
    private var extractionProgress: ProgressBar? = null
    private var extractionTitle: TextView? = null
    private var extractionDetails: TextView? = null

    private var selectedResultView: LinearLayout? = null
    private val resultItems = mutableListOf<LinearLayout>()
    private val resultPaths = mutableMapOf<LinearLayout, String>()

    private var darkMode = false

    // =====================================================
    // UEPak Explorer — Glass UI palette
    // =====================================================

    // Light glass
    private val bgLight = Color.parseColor("#EEF6FF")
    private val surfaceLight = Color.parseColor("#F8FCFF")
    private val glassLight = Color.parseColor("#CCFFFFFF")
    private val glassLightStrong = Color.parseColor("#E6FFFFFF")
    private val borderLight = Color.parseColor("#80FFFFFF")

    private val textLight = Color.parseColor("#10233F")
    private val secondaryLight = Color.parseColor("#58708F")
    private val accentLight = Color.parseColor("#2477FF")
    private val accentLightSoft = Color.parseColor("#DCEBFF")

    // Dark glass
    private val bgDark = Color.parseColor("#071321")
    private val surfaceDark = Color.parseColor("#102033")
    private val glassDark = Color.parseColor("#CC102A43")
    private val glassDarkStrong = Color.parseColor("#E619304A")
    private val borderDark = Color.parseColor("#4D9CCBFF")

    private val textDark = Color.parseColor("#F3F8FF")
    private val secondaryDark = Color.parseColor("#AFC3D9")
    private val accentDark = Color.parseColor("#73B7FF")
    private val accentDarkSoft = Color.parseColor("#193D64")

    // Glass highlights / shadows
    private val glassHighlightLight = Color.parseColor("#66FFFFFF")
    private val glassHighlightDark = Color.parseColor("#338BD0FF")
    private val glassShadowLight = Color.parseColor("#180B4EA2")
    private val glassShadowDark = Color.parseColor("#66000000")

    // =====================================================
    // Glass UI helpers
    // =====================================================

    private fun glassBackground(
        color: Int,
        strokeColor: Int,
        radiusDp: Float = 18f,
        strokeDp: Int = 1
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setStroke(dp(strokeDp), strokeColor)
        }
    }

    private fun glassCardBackground(): GradientDrawable {
        return if (darkMode) {
            glassBackground(
                glassDark,
                borderDark,
                20f,
                1
            )
        } else {
            glassBackground(
                glassLight,
                borderLight,
                20f,
                1
            )
        }
    }

    private fun glassButtonBackground(): GradientDrawable {
        return if (darkMode) {
            glassBackground(
                accentDarkSoft,
                borderDark,
                16f,
                1
            )
        } else {
            glassBackground(
                accentLightSoft,
                borderLight,
                16f,
                1
            )
        }
    }

    private fun applyGlassStyle(view: View) {
        view.background = glassCardBackground()
        view.elevation = dp(3).toFloat()
    }

    private fun applyGlassButtonStyle(button: Button) {
        button.background = glassButtonBackground()
        button.elevation = dp(4).toFloat()
        button.setPadding(
            dp(18),
            dp(10),
            dp(18),
            dp(10)
        )
    }

    // =====================================================
    // Glass UI — Stage 2
    // Applies the visual layer without touching PAK logic.
    // =====================================================

    private fun applyGlassInterface() {

        if (!::root.isInitialized) {
            return
        }

        val glassCard = glassCardBackground()
        val glassButton = glassButtonBackground()

        // Root canvas
        root.setBackgroundColor(
            if (darkMode) bgDark else bgLight
        )

        // Direct children of the main layout.
        // We intentionally avoid changing extraction/search logic.
        for (i in 0 until root.childCount) {

            val child = root.getChildAt(i)

            when (child) {

                is Button -> {
                    child.background = glassButtonBackground()
                    child.elevation = dp(4).toFloat()
                    child.setTextColor(
                        if (darkMode) textDark else textLight
                    )
                }

                is EditText -> {
                    child.background = glassCardBackground()
                    child.elevation = dp(2).toFloat()
                    child.setTextColor(
                        if (darkMode) textDark else textLight
                    )
                    child.setHintTextColor(
                        if (darkMode) secondaryDark else secondaryLight
                    )
                }

                is LinearLayout -> {
                    // Top bar / search row / selection bar
                    child.background = glassCardBackground()
                    child.elevation = dp(2).toFloat()
                }

                is TextView -> {
                    child.setTextColor(
                        if (darkMode) textDark else textLight
                    )
                }
            }
        }

        // Main information text
        if (::status.isInitialized) {
            status.setTextColor(
                if (darkMode) textDark else textLight
            )
        }

        if (::info.isInitialized) {
            info.setTextColor(
                if (darkMode) secondaryDark else secondaryLight
            )
        }

        // Search row
        if (::searchRow.isInitialized) {
            searchRow.background = glassCardBackground()
            searchRow.elevation = dp(2).toFloat()

            for (i in 0 until searchRow.childCount) {
                val child = searchRow.getChildAt(i)

                if (child is Button) {
                    child.background = glassButtonBackground()
                    child.setTextColor(
                        if (darkMode) textDark else textLight
                    )
                    child.elevation = dp(3).toFloat()
                }
            }
        }

        // Selection bar
        if (::selectionBar.isInitialized) {
            selectionBar.background = glassCardBackground()
            selectionBar.elevation = dp(3).toFloat()

            if (::selectionCount.isInitialized) {
                selectionCount.setTextColor(
                    if (darkMode) textDark else textLight
                )
            }

            if (::selectAllButton.isInitialized) {
                applyGlassButtonStyle(selectAllButton)
                selectAllButton.setTextColor(
                    if (darkMode) textDark else textLight
                )
            }

            if (::clearSelectionButton.isInitialized) {
                applyGlassButtonStyle(clearSelectionButton)
                clearSelectionButton.setTextColor(
                    if (darkMode) textDark else textLight
                )
            }
        }

        // Result cards that already exist
        for (item in resultItems) {
            styleResultCard(item)
        }

        // Result cards created later
        if (::results.isInitialized) {

            results.setPadding(
                dp(2),
                dp(4),
                dp(2),
                dp(8)
            )

            results.setOnHierarchyChangeListener(
                object : ViewGroup.OnHierarchyChangeListener {

                    override fun onChildViewAdded(
                        parent: View?,
                        child: View?
                    ) {
                        if (child is LinearLayout) {
                            styleResultCard(child)
                        }
                    }

                    override fun onChildViewRemoved(
                        parent: View?,
                        child: View?
                    ) {
                    }
                }
            )
        }
    }

    private fun styleResultCard(card: LinearLayout) {

        card.background = glassCardBackground()
        card.elevation = dp(3).toFloat()

        card.setPadding(
            dp(14),
            dp(12),
            dp(14),
            dp(12)
        )

        for (i in 0 until card.childCount) {

            val child = card.getChildAt(i)

            when (child) {

                is TextView -> {
                    child.setTextColor(
                        if (darkMode) textDark else textLight
                    )
                }

                is Button -> {
                    applyGlassButtonStyle(child)
                    child.setTextColor(
                        if (darkMode) textDark else textLight
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkMode = isSystemDarkMode()

        buildInterface()
        applyTheme()
        applyGlassInterface()
    }

    private fun isSystemDarkMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
            clipToPadding = false
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleBox.addView(TextView(this).apply {
            text = "UEPak Explorer"
            textSize = 26f
            setTypeface(null, android.graphics.Typeface.BOLD)
            letterSpacing = 0.01f
        })

        titleBox.addView(TextView(this).apply {
            text = "Unreal Engine PAK Explorer"
            textSize = 13f
            letterSpacing = 0.02f
        })

        topBar.addView(
            titleBox,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        themeButton = ImageButton(this).apply {
            scaleType = ImageView.ScaleType.CENTER
            setPadding(dp(9), dp(9), dp(9), dp(9))
            contentDescription = "Toggle light and dark mode"

            setOnClickListener {
                darkMode = !darkMode
                applyTheme()
                applyGlassInterface()
            }
        }

        topBar.addView(
            themeButton,
            LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                marginStart = dp(8)
            }
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
            LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginEnd = dp(4)
            }
        )

        searchRow.addView(
            makeButton("Extract") {
                if (selectionMode) {
                    if (selectedPaths.isEmpty()) {
                        toast("Select at least one file")
                    } else {
                        chooseOutputFolder()
                    }
                } else {
                    if (selectedPath == null) {
                        toast("Select a file first")
                    } else {
                        chooseOutputFolder()
                    }
                }
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginStart = dp(4)
            }
        )

        root.addView(
            searchRow,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(4)
            }
        )

        // Multi-selection bar
        selectionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(0, dp(4), 0, dp(4))
        }

        selectionCount = TextView(this).apply {
            text = "0 selected"
            textSize = 13f
            setPadding(dp(4), dp(6), dp(8), dp(6))
        }

        selectionBar.addView(
            selectionCount,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        selectAllButton = makeButton("Select all") {
            selectAllResults()
        }

        selectionBar.addView(
            selectAllButton,
            LinearLayout.LayoutParams(
                dp(104),
                dp(40)
            ).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        clearSelectionButton = makeButton("Cancel") {
            exitSelectionMode()
        }

        selectionBar.addView(
            clearSelectionButton,
            LinearLayout.LayoutParams(
                dp(82),
                dp(40)
            ).apply {
                marginStart = dp(4)
            }
        )

        root.addView(
            selectionBar,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(4)
            }
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
        scrollParams.topMargin = dp(14)

        root.addView(scroll, scrollParams)

        setContentView(root)
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setOnClickListener { action() }
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(6), 0, dp(6), 0)
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

        themeButton.setImageResource(
            if (darkMode)
                com.example.uepakexplorer.R.drawable.ic_light_mode
            else
                com.example.uepakexplorer.R.drawable.ic_dark_mode
        )

        themeButton.setColorFilter(
            primaryText,
            android.graphics.PorterDuff.Mode.SRC_IN
        )

        themeButton.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(surface)
            cornerRadius = dp(12).toFloat()
            setStroke(
                dp(1),
                if (darkMode)
                    Color.parseColor("#383838")
                else
                    Color.parseColor("#D9DDE3")
            )
        }

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

        selectionCount.setTextColor(secondaryText)

        applyAllResultStyles()
        updateSelectionUi()
    }

    private fun updateTextColors(
        view: View,
        primary: Int,
        secondary: Int
    ) {
        if (view is TextView) {
            when {
                view === searchInput -> {
                    view.setTextColor(primary)
                    view.setHintTextColor(secondary)
                }

                view is Button -> {
                    // Button colors are handled by updateButtons()
                }

                else -> {
                    view.setTextColor(primary)
                }
            }
        }

        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextColors(
                    view.getChildAt(i),
                    primary,
                    secondary
                )
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

    private fun chooseOutputFolder() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            },
            saveFolderRequest
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
        } else if (requestCode == saveFolderRequest) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Throwable) {
                // Some providers do not support persistable permissions.
            }

            extractSelectedToFolder(uri)
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
        resultItems.clear()
        resultPaths.clear()
        selectedResultView = null
        selectedPath = null
        selectedPaths.clear()
        selectionMode = false
        selectionBar.visibility = View.GONE

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
        val fileName = path.substringAfterLast('/').ifEmpty {
            path
        }

        val parentPath = path
            .substringBeforeLast('/', "")
            .let {
                if (it.isEmpty()) "Root" else "$it/"
            }

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL

            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )
        }

        val icon = TextView(this).apply {
            text = "📄"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
        }

        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val nameView = TextView(this).apply {
            text = fileName
            textSize = 15f
            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val pathView = TextView(this).apply {
            text = parentPath
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            setPadding(0, dp(3), 0, 0)
        }

        textBox.addView(nameView)
        textBox.addView(pathView)

        item.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(34),
                dp(44)
            )
        )

        item.addView(
            textBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(10)
            }
        )

        val params = LinearLayout.LayoutParams(
            -1,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(4)
            bottomMargin = dp(4)
        }

        results.addView(item, params)
        resultItems.add(item)
        resultPaths[item] = path

        styleResultItem(
            item,
            nameView,
            pathView,
            icon,
            selected = false
        )

        item.setOnClickListener {
            if (selectionMode) {
                if (selectedPaths.contains(path)) {
                    selectedPaths.remove(path)
                } else {
                    selectedPaths.add(path)
                }

                updateSelectionUi()
                applyAllResultStyles()

                status.text = "${selectedPaths.size} selected"
                return@setOnClickListener
            }

            selectedPaths.clear()
            selectedPaths.add(path)
            selectedPath = path
            selectedResultView = item

            applyAllResultStyles()

            status.text = "Selected: $fileName"
        }

        item.setOnLongClickListener {
            if (!selectionMode) {
                selectionMode = true
                selectedPaths.clear()
            }

            if (selectedPaths.contains(path)) {
                selectedPaths.remove(path)
            } else {
                selectedPaths.add(path)
            }

            updateSelectionUi()
            applyAllResultStyles()

            status.text = "${selectedPaths.size} selected"

            true
        }
    }

    private fun updateSelectionUi() {
        if (!selectionMode) {
            selectionBar.visibility = View.GONE
            return
        }

        selectionBar.visibility = View.VISIBLE

        val count = selectedPaths.size
        selectionCount.text = "$count selected"

        val allSelected =
            resultItems.isNotEmpty() &&
            selectedPaths.size == resultItems.size

        selectAllButton.text =
            if (allSelected) "Deselect all" else "Select all"

        val extractButton = searchRow.getChildAt(1) as Button
        extractButton.text =
            if (selectionMode) "Extract selected" else "Extract"
    }

    private fun selectAllResults() {
        if (resultItems.isEmpty()) return

        val allSelected =
            selectedPaths.size == resultItems.size

        if (allSelected) {
            selectedPaths.clear()
        } else {
            selectedPaths.clear()

            resultItems.forEach { item ->
                resultPaths[item]?.let { path ->
                    selectedPaths.add(path)
                }
            }
        }

        updateSelectionUi()
        applyAllResultStyles()

        status.text = "${selectedPaths.size} selected"
    }

    private fun exitSelectionMode() {
        selectionMode = false
        selectedPaths.clear()
        selectedPath = null
        selectedResultView = null

        selectionBar.visibility = View.GONE

        val extractButton = searchRow.getChildAt(1) as Button
        extractButton.text = "Extract"

        applyAllResultStyles()

        status.text = "Selection cancelled"
    }

    private fun applyAllResultStyles() {
        resultItems.forEach { item ->
            val box = item.getChildAt(1) as LinearLayout

            val nameView = box.getChildAt(0) as TextView
            val pathView = box.getChildAt(1) as TextView
            val icon = item.getChildAt(0) as TextView

            val path = resultPaths[item]

            val selected =
                if (selectionMode) {
                    path != null && selectedPaths.contains(path)
                } else {
                    item === selectedResultView
                }

            styleResultItem(
                item,
                nameView,
                pathView,
                icon,
                selected
            )
        }
    }

    private fun styleResultItem(
        item: LinearLayout,
        nameView: TextView,
        pathView: TextView,
        icon: TextView,
        selected: Boolean
    ) {
        val cardColor = when {
            selected && darkMode ->
                Color.parseColor("#263B5C")

            selected ->
                Color.parseColor("#E8F0FE")

            darkMode ->
                Color.parseColor("#1E1E1E")

            else ->
                Color.WHITE
        }

        val primary = if (darkMode) {
            Color.WHITE
        } else {
            textLight
        }

        val secondary = if (darkMode) {
            Color.parseColor("#B8BCC2")
        } else {
            secondaryLight
        }

        nameView.setTextColor(primary)
        pathView.setTextColor(secondary)
        icon.setTextColor(primary)

        item.background = GradientDrawable().apply {
            setColor(cardColor)
            cornerRadius = dp(12).toFloat()

            setStroke(
                dp(1),
                when {
                    selected && darkMode ->
                        Color.parseColor("#4D78B8")

                    selected ->
                        Color.parseColor("#9AB8E8")

                    darkMode ->
                        Color.parseColor("#303030")

                    else ->
                        Color.parseColor("#E2E5E9")
                }
            )
        }
    }

    private fun extractSelectedToFolder(treeUri: Uri) {
        val paths = selectedPaths.toList()

        if (paths.isEmpty()) {
            toast("Select at least one file")
            return
        }

        val tempRoot = File(
            externalCacheDir ?: cacheDir,
            "uepak-extract-${System.currentTimeMillis()}"
        )

        if (!tempRoot.mkdirs() && !tempRoot.isDirectory) {
            toast("Could not create temporary extraction folder")
            return
        }

        showExtractionDialog(paths.size)

        Thread {
            try {
                val pathsJson = JSONArray().apply {
                    paths.forEach { put(it) }
                }.toString()

                val nativeJson = JSONObject(
                    NativePak.extractBatch(
                        pathsJson,
                        tempRoot.absolutePath
                    )
                )

                if (!nativeJson.optBoolean("ok")) {
                    throw IllegalStateException(
                        nativeJson.optString(
                            "error",
                            "Native extraction failed"
                        )
                    )
                }

                val nativeSuccess =
                    nativeJson.optInt("success", 0)

                val nativeFailed =
                    nativeJson.optInt("failed", 0)

                val copyResult =
                    copyExtractedTreeToSaf(
                        tempRoot,
                        treeUri,
                        nativeSuccess
                    )

                val totalFailed =
                    nativeFailed + copyResult.failed

                runOnUiThread {
                    extractionProgress?.max = paths.size
                    extractionProgress?.progress =
                        minOf(
                            copyResult.success + totalFailed,
                            paths.size
                        )

                    extractionTitle?.text =
                        if (totalFailed == 0) {
                            "Extraction complete"
                        } else {
                            "Extraction finished with errors"
                        }

                    extractionDetails?.text =
                        "${copyResult.success} copied, " +
                        "$totalFailed failed"

                    status.text =
                        if (totalFailed == 0) {
                            "Extracted ${copyResult.success} files successfully"
                        } else {
                            "Extraction finished: " +
                            "${copyResult.success} succeeded, " +
                            "$totalFailed failed"
                        }
                }

                window.decorView.postDelayed({
                    extractionDialog?.dismiss()
                    extractionDialog = null
                    extractionProgress = null
                    extractionTitle = null
                    extractionDetails = null

                    selectionMode = false
                    selectedPaths.clear()
                    selectedPath = null
                    selectedResultView = null
                    selectionBar.visibility = View.GONE

                    val extractButton =
                        searchRow.getChildAt(1) as Button
                    extractButton.text = "Extract"

                    applyAllResultStyles()
                }, 1200)

            } catch (t: Throwable) {
                runOnUiThread {
                    extractionTitle?.text = "Extraction failed"
                    extractionDetails?.text =
                        t.message ?: "Unknown extraction error"
                    status.text =
                        "Extraction failed: " +
                        (t.message ?: "Unknown error")
                }
            } finally {
                tempRoot.deleteRecursively()
            }
        }.start()
    }

    private data class CopyResult(
        val success: Int,
        val failed: Int
    )

    private fun copyExtractedTreeToSaf(
        tempRoot: File,
        treeUri: Uri,
        total: Int
    ): CopyResult {
        var success = 0
        var failed = 0

        val files = tempRoot
            .walkTopDown()
            .filter { it.isFile }
            .toList()

        for (file in files) {
            try {
                val relative = tempRoot
                    .toPath()
                    .relativize(file.toPath())
                    .toString()
                    .replace(File.separatorChar, '/')

                val parts = relative
                    .split('/')
                    .filter { it.isNotEmpty() }

                require(parts.isNotEmpty()) {
                    "Invalid extracted path"
                }

                var parentUri = treeUri

                for (part in parts.dropLast(1)) {
                    parentUri = findOrCreateDirectory(
                        treeUri,
                        parentUri,
                        part
                    )
                }

                val fileName = parts.last()

                val fileUri = findOrCreateFile(
                    treeUri,
                    parentUri,
                    fileName
                )

                contentResolver
                    .openOutputStream(fileUri, "wt")
                    ?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(
                                output,
                                64 * 1024
                            )
                        }
                    }
                    ?: error(
                        "Could not open SAF output stream"
                    )

                success++

            } catch (_: Throwable) {
                failed++
            }

            val completed = success + failed

            runOnUiThread {
                extractionProgress?.max = total
                extractionProgress?.progress =
                    minOf(completed, total)

                extractionDetails?.text =
                    "$completed / $total\n" +
                    "Success: $success    Failed: $failed"
            }
        }

        return CopyResult(success, failed)
    }


    private fun showExtractionDialog(total: Int) {
        val dialog = Dialog(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        val title = TextView(this).apply {
            text = "Extracting files..."
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val details = TextView(this).apply {
            text = "0 / $total\nSuccess: 0    Failed: 0"
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        }

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = total
            progress = 0
        }

        container.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        container.addView(
            details,
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        container.addView(
            progress,
            LinearLayout.LayoutParams(
                -1,
                dp(8)
            )
        )

        dialog.setContentView(container)
        dialog.setCancelable(false)

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                if (darkMode) surfaceDark else Color.WHITE
            )
        )

        extractionDialog = dialog
        extractionProgress = progress
        extractionTitle = title
        extractionDetails = details

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.86f).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createFileForPakPath(
        treeUri: Uri,
        pakPath: String
    ): Uri {
        val parts = pakPath
            .split('/')
            .filter {
                it.isNotEmpty() &&
                it != "." &&
                it != ".."
            }

        require(parts.isNotEmpty()) {
            "Invalid PAK path"
        }

        var currentUri = treeUri

        for (i in 0 until parts.size - 1) {
            currentUri = findOrCreateDirectory(
                treeUri,
                currentUri,
                parts[i]
            )
        }

        val fileName = parts.last()

        return findOrCreateFile(
            treeUri,
            currentUri,
            fileName
        )
    }

    private fun findOrCreateDirectory(
        treeUri: Uri,
        parentUri: Uri,
        name: String
    ): Uri {
        findChild(
            treeUri,
            parentUri,
            name,
            DocumentsContract.Document.MIME_TYPE_DIR
        )?.let {
            return it
        }

        val parentDocumentUri =
            if (parentUri == treeUri) {
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
            } else {
                parentUri
            }

        return DocumentsContract.createDocument(
            contentResolver,
            parentDocumentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        ) ?: error("Could not create directory: $name")
    }

    private fun findOrCreateFile(
        treeUri: Uri,
        parentUri: Uri,
        name: String
    ): Uri {
        findChild(
            treeUri,
            parentUri,
            name,
            null
        )?.let {
            return it
        }

        val parentDocumentUri =
            if (parentUri == treeUri) {
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
            } else {
                parentUri
            }

        return DocumentsContract.createDocument(
            contentResolver,
            parentDocumentUri,
            "application/octet-stream",
            name
        ) ?: error("Could not create file: $name")
    }

    private fun findChild(
        treeUri: Uri,
        parentUri: Uri,
        name: String,
        requiredMimeType: String?
    ): Uri? {
        val parentId =
            if (parentUri == treeUri) {
                DocumentsContract.getTreeDocumentId(treeUri)
            } else {
                DocumentsContract.getDocumentId(parentUri)
            }

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentId
            )

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            val idIndex = cursor.getColumnIndex(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            )

            val nameIndex = cursor.getColumnIndex(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )

            val mimeIndex = cursor.getColumnIndex(
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            while (cursor.moveToNext()) {
                val childName = cursor.getString(nameIndex)

                if (childName != name) continue

                val mime =
                    cursor.getString(mimeIndex)

                if (
                    requiredMimeType == null ||
                    mime == requiredMimeType
                ) {
                    val id = cursor.getString(idIndex)

                    return DocumentsContract
                        .buildDocumentUriUsingTree(
                            treeUri,
                            id
                        )
                }
            }
        }

        return null
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
                    resultItems.clear()
                    selectedResultView = null
                    selectedPath = null

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
            var tempFile: File? = null

            try {
                tempFile = File.createTempFile(
                    "uepak_extract_",
                    ".tmp",
                    cacheDir
                )

                val result = NativePak.extractToPath(
                    path,
                    tempFile.absolutePath
                )

                if (!result.startsWith("Extracted:")) {
                    error(result)
                }

                contentResolver
                    .openOutputStream(uri, "wt")
                    ?.use { output ->
                        FileInputStream(tempFile).use { input ->
                            input.copyTo(
                                output,
                                64 * 1024
                            )
                        }

                        output.flush()
                    }
                    ?: error(
                        "Could not open output document"
                    )

                runOnUiThread {
                    status.text =
                        "Extracted: $path"
                }

            } catch (t: Throwable) {
                runOnUiThread {
                    status.text =
                        "Extract failed: " +
                        (t.message ?: "Unknown error")
                }
            } finally {
                try {
                    tempFile?.delete()
                } catch (_: Throwable) {
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
