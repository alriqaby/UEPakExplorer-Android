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
            layoutDirection = View.LAYOUT_DIRECTION_LTR
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

        themeButton = ImageButton(this).apply {
            scaleType = ImageView.ScaleType.CENTER
            setPadding(dp(9), dp(9), dp(9), dp(9))
            contentDescription = "Toggle light and dark mode"

            setOnClickListener {
                darkMode = !darkMode
                applyTheme()
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
                        chooseOutput()
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

        showExtractionDialog(paths.size)

        Thread {
            var extracted = 0
            var failed = 0
            val failures = mutableListOf<String>()

            for ((index, path) in paths.withIndex()) {
                try {
                    val fileUri = createFileForPakPath(
                        treeUri,
                        path
                    )

                    val pfd = contentResolver.openFileDescriptor(
                        fileUri,
                        "w"
                    ) ?: error("Could not create output file")

                    val fd = pfd.detachFd()

                    try {
                        val result = NativePak.extract(
                            path,
                            fd
                        )

                        if (!result.startsWith("Extracted:")) {
                            error(result)
                        }

                        extracted++
                    }

                } catch (t: Throwable) {
                    failed++
                    failures.add(
                        "${path.substringAfterLast('/')} : ${t.message ?: "Unknown error"}"
                    )
                }

                val completed = index + 1

                runOnUiThread {
                    extractionProgress?.progress = completed

                    extractionTitle?.text =
                        "Extracting files..."

                    extractionDetails?.text =
                        "$completed / ${paths.size}\n" +
                        "Success: $extracted    Failed: $failed"
                }
            }

            runOnUiThread {
                extractionProgress?.progress = paths.size

                extractionTitle?.text =
                    if (failed == 0) "Extraction complete"
                    else "Extraction finished with errors"

                val failureText =
                    if (failed == 0) {
                        "All $extracted files extracted successfully."
                    } else {
                        "$extracted succeeded, $failed failed."
                    }

                extractionDetails?.text = failureText

                status.text =
                    if (failed == 0) {
                        "Extracted $extracted files successfully"
                    } else {
                        "Extraction finished: $extracted succeeded, $failed failed"
                    }

                extractionDialog?.setOnDismissListener {
                    extractionDialog = null
                    extractionProgress = null
                    extractionTitle = null
                    extractionDetails = null
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
                }, 1400)
            }
        }.start()
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
