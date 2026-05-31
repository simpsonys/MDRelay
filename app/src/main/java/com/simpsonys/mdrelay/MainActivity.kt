package com.simpsonys.mdrelay

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.text.DateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var fileResolver: FileUriResolver
    private lateinit var recentStore: RecentFileStore
    private lateinit var themeStore: ThemeStore
    private lateinit var captureStore: CaptureStore
    private var incomingFile by mutableStateOf<OpenedFile?>(null)
    private var incomingText by mutableStateOf<SharedText?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileResolver = FileUriResolver(this)
        recentStore = RecentFileStore(this)
        themeStore = ThemeStore(this)
        captureStore = CaptureStore(this)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            MdRelayApp(
                initialFile = incomingFile,
                initialText = incomingText,
                fileResolver = fileResolver,
                recentStore = recentStore,
                themeStore = themeStore,
                captureStore = captureStore,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    if (uri.scheme == "mdrelay" && uri.host == "web-open") {
                        handleDeepLinkWebOpen(uri)
                    } else {
                        incomingFile = fileResolver.open(uri, intent.flags)
                        incomingText = null
                        if (incomingFile == null) toast(this, fileResolver.lastErrorMessage(uri))
                    }
                }
            }
            Intent.ACTION_SEND -> {
                val stream = intent.parcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)
                    ?: intent.clipData?.firstUriOrNull()
                if (stream != null) {
                    // TODO: Keep image/PDF bundle capture as a separate backlog item, not part of the outbox slice.
                    incomingFile = fileResolver.open(stream, intent.flags)
                    incomingText = null
                    if (incomingFile == null) toast(this, fileResolver.lastErrorMessage(stream))
                } else {
                    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    if (!text.isNullOrBlank()) {
                        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                        incomingText = SharedText(
                            filename = subject ?: "Shared text",
                            content = text,
                            subject = subject,
                            sourcePackage = extractShareSourcePackage(intent),
                            sharedUrl = detectSharedUrl(text),
                        )
                        incomingFile = null
                    }
                }
            }
        }
    }

    private fun handleDeepLinkWebOpen(uri: Uri) {
        val targetUrlString = uri.getQueryParameter("url")
        if (targetUrlString.isNullOrBlank()) {
            toast(this, "Deep link error: URL parameter is missing")
            return
        }
        val decodedUrlString = runCatching { java.net.URLDecoder.decode(targetUrlString, "UTF-8") }.getOrNull() ?: targetUrlString

        CoroutineScope(Dispatchers.Main).launch {
            val progressToast = Toast.makeText(this@MainActivity, "Downloading remote file...", Toast.LENGTH_SHORT)
            progressToast.show()
            val result = withContext(Dispatchers.IO) {
                downloadWebFile(decodedUrlString)
            }
            progressToast.cancel()
            if (result != null) {
                incomingFile = result
                incomingText = null
                loadOpenedFile(
                    opened = result,
                    context = this@MainActivity,
                    recentStore = recentStore,
                    permissionPersisted = false,
                    onRecentChanged = {},
                    onLoaded = {}
                )
            } else {
                toast(this@MainActivity, "Download failed or invalid URL")
            }
        }
    }

    private fun extractShareSourcePackage(intent: Intent): String? {
        val referrerHost = referrer?.host
        if (!referrerHost.isNullOrBlank()) return referrerHost
        val explicitReferrer = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
        if (!explicitReferrer.isNullOrBlank()) {
            return explicitReferrer.removePrefix("android-app://")
        }
        return null
    }
}

@Composable
private fun MdRelayApp(
    initialFile: OpenedFile?,
    initialText: SharedText?,
    fileResolver: FileUriResolver,
    recentStore: RecentFileStore,
    themeStore: ThemeStore,
    captureStore: CaptureStore,
) {
    var darkTheme by rememberSaveable { mutableStateOf(themeStore.isDark()) }
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppRoot(
                initialFile = initialFile,
                initialText = initialText,
                fileResolver = fileResolver,
                recentStore = recentStore,
                captureStore = captureStore,
                darkTheme = darkTheme,
                onToggleTheme = {
                    darkTheme = !darkTheme
                    themeStore.setDark(darkTheme)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRoot(
    initialFile: OpenedFile?,
    initialText: SharedText?,
    fileResolver: FileUriResolver,
    recentStore: RecentFileStore,
    captureStore: CaptureStore,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val outboxWriter = remember(context) { MarkdownOutboxWriter(context) }
    var filename by rememberSaveable { mutableStateOf("MD Relay") }
    var currentUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var reference by rememberSaveable { mutableStateOf("") }
    var rawContent by rememberSaveable { mutableStateOf("") }
    var fileKind by rememberSaveable { mutableStateOf(FileKind.MARKDOWN.name) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var showToc by rememberSaveable { mutableStateOf(false) }
    var showRecent by rememberSaveable { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var expandedFilename by rememberSaveable { mutableStateOf(false) }
    var showCaptureSettings by rememberSaveable { mutableStateOf(false) }
    var captureSubject by rememberSaveable { mutableStateOf<String?>(null) }
    var captureSourcePackage by rememberSaveable { mutableStateOf<String?>(null) }
    var captureSharedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var saveStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var saveInFlight by rememberSaveable { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    val listState = remember { LazyListState() }
    val recentItems = remember { mutableStateOf(recentStore.load()) }
    val captureSettings = remember { mutableStateOf(captureStore.loadSettings()) }
    val captureDiagnostics = remember { mutableStateOf(captureStore.loadDiagnostics()) }
    val blocks = remember(rawContent, fileKind) {
        if (FileKind.valueOf(fileKind) == FileKind.MARKDOWN) parseMarkdownBlocks(rawContent) else emptyList()
    }
    val tocItems = remember(blocks) { blocks.mapIndexedNotNull { index, block -> block.heading?.copy(blockIndex = index) } }
    val applyOpenedFile: (OpenedFile) -> Unit = { opened ->
        currentUriString = opened.uri
        filename = opened.filename
        reference = opened.reference
        rawContent = opened.content
        fileKind = opened.kind.name
        editMode = false
        showRecent = false
        showToc = false
        fullscreen = false
        captureSubject = null
        captureSourcePackage = null
        captureSharedUrl = null
        saveStatusMessage = null
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val opened = fileResolver.open(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (opened != null) {
                loadOpenedFile(
                    opened = opened,
                    context = context,
                    recentStore = recentStore,
                    permissionPersisted = fileResolver.lastPermissionPersisted(),
                    onRecentChanged = { recentItems.value = it },
                    onLoaded = { applyOpenedFile(opened) },
                )
            } else {
                toast(context, fileResolver.lastErrorMessage(uri))
            }
        }
    }
    val outboxPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val result = captureStore.persistOutboxUri(context, uri)
            captureSettings.value = captureStore.loadSettings()
            captureDiagnostics.value = captureStore.loadDiagnostics()
            saveStatusMessage = if (result.isSuccess) {
                "Outbox folder updated."
            } else {
                "Could not store outbox folder permission."
            }
            toast(context, saveStatusMessage.orEmpty())
        }
    }

    LaunchedEffect(initialFile?.sourceKey, initialText?.content) {
        when {
            initialFile != null -> {
                loadOpenedFile(
                    opened = initialFile,
                    context = context,
                    recentStore = recentStore,
                    onRecentChanged = { recentItems.value = it },
                    onLoaded = { applyOpenedFile(initialFile) },
                )
            }
            initialText != null -> {
                currentUriString = null
                filename = initialText.filename
                reference = ""
                rawContent = initialText.content
                fileKind = FileKind.MARKDOWN.name
                editMode = true
                showRecent = false
                fullscreen = false
                captureSubject = initialText.subject
                captureSourcePackage = initialText.sourcePackage
                captureSharedUrl = initialText.sharedUrl ?: detectSharedUrl(initialText.content)
                saveStatusMessage = null
            }
        }
    }

    BackHandler(enabled = fullscreen) {
        fullscreen = false
    }

    if (fullscreen && !editMode) {
        Box(Modifier.fillMaxSize()) {
            ViewerContent(
                content = rawContent,
                kind = FileKind.valueOf(fileKind),
                blocks = blocks,
                listState = listState,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = { fullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), CircleShape),
            ) {
                ToolbarIconImage(ToolbarIcon.FullscreenExit, "Exit full-screen")
            }
        }
        return
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val wide = maxWidth >= 720.dp
        Row(Modifier.fillMaxSize()) {
            if (wide && (showToc || showRecent)) {
                SidePanel(
                    showRecent = showRecent,
                    recentItems = recentItems.value,
                    tocItems = tocItems,
                    onTocClick = {
                        coroutineScope.launch { listState.animateScrollToItem(it.blockIndex) }
                    },
                    onRecentClick = { item ->
                        val uri = Uri.parse(item.uri)
                        val opened = fileResolver.open(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        if (opened != null) {
                            loadOpenedFile(
                                opened = opened,
                                context = context,
                                recentStore = recentStore,
                                onRecentChanged = { recentItems.value = it },
                            ) { applyOpenedFile(opened) }
                        } else {
                            toast(context, fileResolver.lastErrorMessage(uri))
                        }
                    },
                    onRecentLongPress = { copyReference(context, it.reference) },
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                )
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                TopToolbar(
                    filename = filename,
                    expandedFilename = expandedFilename,
                    editMode = editMode,
                    darkTheme = darkTheme,
                    onFilenameClick = { expandedFilename = !expandedFilename },
                    onFilenameLongPress = { copyReference(context, reference.ifBlank { filename }) },
                    onToggleEdit = {
                        editMode = !editMode
                        if (editMode) fullscreen = false
                    },
                    onShare = { shareContent(context, filename, rawContent, FileKind.valueOf(fileKind)) },
                    onCopyContent = {
                        copyText(context, "MD Relay content", rawContent)
                        toast(context, "Content copied")
                    },
                    onToggleToc = {
                        showToc = !showToc
                        showRecent = false
                    },
                    onFullscreen = {
                        if (!editMode) fullscreen = true else toast(context, "Full-screen is View mode only")
                    },
                    overflowOpen = overflowOpen,
                    onOverflowChange = { overflowOpen = it },
                    onCopyReference = { copyReference(context, reference) },
                    onToggleTheme = onToggleTheme,
                    onShowRecent = {
                        showRecent = !showRecent
                        showToc = false
                    },
                    onOpenFile = { filePicker.launch(arrayOf("text/markdown", "text/plain", "application/json", "application/octet-stream")) },
                    onSave = {
                        coroutineScope.launch {
                            if (saveInFlight) return@launch
                            val settings = captureSettings.value
                            if (settings.outboxTreeUri.isNullOrBlank()) {
                                saveStatusMessage = "Select an outbox folder before saving."
                                showCaptureSettings = true
                                toast(context, saveStatusMessage.orEmpty())
                                return@launch
                            }
                            saveInFlight = true
                            val captureDraft = CaptureDraft(
                                visibleTitle = filename,
                                subject = captureSubject,
                                sourcePackage = captureSourcePackage,
                                sharedUrl = captureSharedUrl ?: detectSharedUrl(rawContent),
                                capturedText = rawContent,
                            )
                            val saveResult = runCatching {
                                outboxWriter.saveMarkdown(
                                    outboxTreeUri = Uri.parse(settings.outboxTreeUri),
                                    draft = captureDraft,
                                )
                            }
                            val savedFile = saveResult.getOrNull()
                            if (savedFile == null) {
                                val message = saveResult.exceptionOrNull()?.message ?: "Unknown save error"
                                captureStore.recordSaveFailure(message)
                                captureDiagnostics.value = captureStore.loadDiagnostics()
                                saveStatusMessage = "Local save failed.\n$message"
                                toast(context, "Local save failed")
                                saveInFlight = false
                                return@launch
                            }

                            captureStore.recordSaveSuccess(savedFile)
                            captureDiagnostics.value = captureStore.loadDiagnostics()
                            currentUriString = savedFile.uri.toString()
                            reference = savedFile.reference
                            filename = savedFile.filename
                            rawContent = savedFile.markdown
                            fileKind = FileKind.MARKDOWN.name
                            editMode = false

                            val statusLines = mutableListOf(
                                "Saved locally.",
                                savedFile.filename,
                            )
                            if (settings.folderSyncEnabled && settings.triggerAfterSave) {
                                kotlinx.coroutines.delay(settings.triggerDelaySeconds.coerceIn(1, 3) * 1_000L)
                                val triggerResult = triggerFolderSync(context, settings.folderPairName)
                                captureStore.recordTriggerResult(settings.folderPairName, triggerResult)
                                captureDiagnostics.value = captureStore.loadDiagnostics()
                                statusLines += "Legacy broadcast sent. Verify FolderSync Instant sync/schedule if upload does not start."
                            } else {
                                statusLines += "FolderSync: use Instant sync / Monitor device folder."
                            }
                            saveStatusMessage = statusLines.joinToString("\n")
                            toast(context, "Saved locally")
                            saveInFlight = false
                        }
                    },
                    onShowCaptureSettings = {
                        showCaptureSettings = true
                    },
                )

                if (!saveStatusMessage.isNullOrBlank()) {
                    StatusPanel(
                        message = saveStatusMessage.orEmpty(),
                        onDismiss = { saveStatusMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (expandedFilename && filename.isNotBlank()) {
                    FileInfoPanel(
                        filename = filename,
                        reference = reference.ifBlank { currentUriString.orEmpty() },
                        onCopyFilename = {
                            copyText(context, "MD Relay filename", filename)
                            toast(context, "Filename copied")
                        },
                        onCopyReference = { copyReference(context, reference.ifBlank { currentUriString.orEmpty() }) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (!wide && showRecent) {
                    RecentPanel(
                        items = recentItems.value,
                        onClick = { item ->
                            val uri = Uri.parse(item.uri)
                            val opened = fileResolver.open(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            if (opened != null) {
                                loadOpenedFile(
                                    opened = opened,
                                    context = context,
                                    recentStore = recentStore,
                                    onRecentChanged = { recentItems.value = it },
                                ) { applyOpenedFile(opened) }
                            } else {
                                toast(context, fileResolver.lastErrorMessage(uri))
                            }
                        },
                        onLongPress = { copyReference(context, it.reference) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    )
                }

                if (rawContent.isBlank() && !editMode) {
                    EmptyScreen(
                        recentItems = recentItems.value,
                        outboxConfigured = !captureSettings.value.outboxTreeUri.isNullOrBlank(),
                        onNewCapture = {
                            filename = "MD Relay"
                            rawContent = ""
                            reference = ""
                            currentUriString = null
                            fileKind = FileKind.MARKDOWN.name
                            editMode = true
                            captureSubject = null
                            captureSourcePackage = null
                            captureSharedUrl = null
                            saveStatusMessage = null
                        },
                        onConfigureCapture = {
                            showCaptureSettings = true
                        },
                        onOpen = { filePicker.launch(arrayOf("text/markdown", "text/plain", "application/json", "application/octet-stream")) },
                        onRecentClick = { item ->
                            val uri = Uri.parse(item.uri)
                            val opened = fileResolver.open(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            if (opened != null) {
                                loadOpenedFile(
                                    opened = opened,
                                    context = context,
                                    recentStore = recentStore,
                                    onRecentChanged = { recentItems.value = it },
                                ) { applyOpenedFile(opened) }
                            } else {
                                toast(context, fileResolver.lastErrorMessage(uri))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else if (editMode) {
                    OutlinedTextField(
                        value = rawContent,
                        onValueChange = { rawContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                    )
                } else {
                    ViewerContent(
                        content = rawContent,
                        kind = FileKind.valueOf(fileKind),
                        blocks = blocks,
                        listState = listState,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (!wide && showToc) {
            CompactTocSheet(
                tocItems = tocItems,
                onDismiss = { showToc = false },
                onClick = {
                    coroutineScope.launch { listState.animateScrollToItem(it.blockIndex) }
                    showToc = false
                },
            )
        }
    }

    if (showCaptureSettings) {
        CaptureSettingsSheet(
            settings = captureSettings.value,
            diagnostics = captureDiagnostics.value,
            onDismiss = { showCaptureSettings = false },
            onSelectOutbox = { outboxPicker.launch(null) },
            onOpenOutbox = {
                val uriString = captureSettings.value.outboxTreeUri
                if (uriString.isNullOrBlank()) {
                    toast(context, "Select an outbox folder first")
                } else if (!openDocumentTree(context, Uri.parse(uriString))) {
                    toast(context, "Could not open outbox folder")
                }
            },
            onFolderSyncEnabledChange = {
                captureStore.updateSettings(captureSettings.value.copy(folderSyncEnabled = it))
                captureSettings.value = captureStore.loadSettings()
            },
            onTriggerAfterSaveChange = {
                captureStore.updateSettings(captureSettings.value.copy(triggerAfterSave = it))
                captureSettings.value = captureStore.loadSettings()
            },
            onTriggerDelayChange = {
                captureStore.updateSettings(captureSettings.value.copy(triggerDelaySeconds = it.coerceIn(1, 3)))
                captureSettings.value = captureStore.loadSettings()
            },
            onFolderPairNameChange = {
                captureStore.updateSettings(captureSettings.value.copy(folderPairName = it))
                captureSettings.value = captureStore.loadSettings()
            },
            onTestFolderSync = {
                val settings = captureSettings.value
                val pairName = settings.folderPairName.ifBlank { DEFAULT_FOLDER_PAIR_NAME }
                val result = triggerFolderSync(context, pairName)
                captureStore.recordTriggerResult(pairName, result)
                captureDiagnostics.value = captureStore.loadDiagnostics()
                toast(context, result.message)
            },
        )
    }
}

private fun loadOpenedFile(
    opened: OpenedFile,
    context: Context,
    recentStore: RecentFileStore,
    permissionPersisted: Boolean = false,
    onRecentChanged: (List<RecentFile>) -> Unit,
    onLoaded: () -> Unit,
) {
    onLoaded()
    copyText(context, "MD Relay content", opened.content)
    toast(context, "Content copied")
    recentStore.add(opened.toRecent(permissionPersisted))
    onRecentChanged(recentStore.load())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopToolbar(
    filename: String,
    expandedFilename: Boolean,
    editMode: Boolean,
    darkTheme: Boolean,
    onFilenameClick: () -> Unit,
    onFilenameLongPress: () -> Unit,
    onToggleEdit: () -> Unit,
    onShare: () -> Unit,
    onCopyContent: () -> Unit,
    onToggleToc: () -> Unit,
    onFullscreen: () -> Unit,
    overflowOpen: Boolean,
    onOverflowChange: (Boolean) -> Unit,
    onCopyReference: () -> Unit,
    onToggleTheme: () -> Unit,
    onShowRecent: () -> Unit,
    onOpenFile: () -> Unit,
    onSave: () -> Unit,
    onShowCaptureSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = filename,
            maxLines = if (expandedFilename) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onFilenameClick, onLongClick = onFilenameLongPress)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            fontWeight = FontWeight.SemiBold,
        )
        ToolbarIconButton(
            icon = if (editMode) ToolbarIcon.Visibility else ToolbarIcon.Edit,
            contentDescription = if (editMode) "View mode" else "Edit mode",
            onClick = onToggleEdit,
        )
        ToolbarIconButton(ToolbarIcon.Share, "Share current content", onShare)
        ToolbarIconButton(ToolbarIcon.ContentCopy, "Copy content", onCopyContent)
        ToolbarIconButton(ToolbarIcon.TableOfContents, "Table of contents", onToggleToc)
        ToolbarIconButton(ToolbarIcon.Fullscreen, "Full-screen viewer", onFullscreen)
        Box {
            ToolbarIconButton(ToolbarIcon.MoreVert, "More actions") { onOverflowChange(true) }
            DropdownMenu(expanded = overflowOpen, onDismissRequest = { onOverflowChange(false) }) {
                DropdownMenuItem(
                    text = { Text("Copy file path/Uri") },
                    leadingIcon = { ToolbarIconImage(ToolbarIcon.Link, "Copy file path/Uri") },
                    onClick = {
                        onOverflowChange(false)
                        onCopyReference()
                    },
                )
                DropdownMenuItem(
                    text = { Text(if (darkTheme) "Light theme" else "Dark theme") },
                    leadingIcon = {
                        ToolbarIconImage(
                            if (darkTheme) ToolbarIcon.LightMode else ToolbarIcon.DarkMode,
                            if (darkTheme) "Light theme" else "Dark theme",
                        )
                    },
                    onClick = {
                        onOverflowChange(false)
                        onToggleTheme()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Recent opened") },
                    leadingIcon = { ToolbarIconImage(ToolbarIcon.History, "Recent opened") },
                    onClick = {
                        onOverflowChange(false)
                        onShowRecent()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Open file") },
                    leadingIcon = { ToolbarIconImage(ToolbarIcon.FolderOpen, "Open file") },
                    onClick = {
                        onOverflowChange(false)
                        onOpenFile()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Capture settings") },
                    leadingIcon = { ToolbarIconImage(ToolbarIcon.Settings, "Capture settings") },
                    onClick = {
                        onOverflowChange(false)
                        onShowCaptureSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Save to outbox") },
                    leadingIcon = { ToolbarIconImage(ToolbarIcon.Save, "Save to outbox") },
                    onClick = {
                        onOverflowChange(false)
                        onSave()
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ToolbarIcon,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        ToolbarIconImage(icon, contentDescription)
    }
}

@Composable
private fun ToolbarIconImage(icon: ToolbarIcon, contentDescription: String) {
    Icon(
        painter = painterResource(id = icon.resId),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun FileInfoPanel(
    filename: String,
    reference: String,
    onCopyFilename: () -> Unit,
    onCopyReference: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "File",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCopyFilename) {
                Text("Copy filename")
            }
            TextButton(onClick = onCopyReference) {
                Text("Copy Uri/path")
            }
        }
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = filename,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (reference.isNotBlank()) {
                    Text(
                        text = reference,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScreen(
    recentItems: List<RecentFile>,
    outboxConfigured: Boolean,
    onNewCapture: () -> Unit,
    onConfigureCapture: () -> Unit,
    onOpen: () -> Unit,
    onRecentClick: (RecentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onNewCapture) {
            Text("New capture")
        }
        Button(onClick = onOpen) {
            Text("Open file")
        }
        TextButton(onClick = onConfigureCapture) {
            Text(if (outboxConfigured) "Outbox configured" else "Configure outbox")
        }
        Text("Recent opened", style = MaterialTheme.typography.titleMedium)
        RecentPanel(
            items = recentItems,
            onClick = onRecentClick,
            onLongPress = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StatusPanel(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(start = 10.dp, top = 6.dp, end = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Capture status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.height(28.dp)) {
                Text("×", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureSettingsSheet(
    settings: CaptureSettings,
    diagnostics: CaptureDiagnostics,
    onDismiss: () -> Unit,
    onSelectOutbox: () -> Unit,
    onOpenOutbox: () -> Unit,
    onFolderSyncEnabledChange: (Boolean) -> Unit,
    onTriggerAfterSaveChange: (Boolean) -> Unit,
    onTriggerDelayChange: (Int) -> Unit,
    onFolderPairNameChange: (String) -> Unit,
    onTestFolderSync: () -> Unit,
) {
    var folderPairName by remember(settings.folderPairName) { mutableStateOf(settings.folderPairName) }
    var triggerDelayText by remember(settings.triggerDelaySeconds) { mutableStateOf(settings.triggerDelaySeconds.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Capture settings", style = MaterialTheme.typography.titleMedium)
            Text("Capture output mode: Local outbox", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Outbox folder: ${settings.outboxTreeUri ?: "Not selected"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSelectOutbox) {
                    Text("Select folder")
                }
                TextButton(onClick = onOpenOutbox) {
                    Text("Open outbox")
                }
            }
            Text(
                text = "Last successful write: ${diagnostics.lastSavedAt?.let(::formatTime) ?: "None"}",
                style = MaterialTheme.typography.bodySmall,
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enable legacy broadcast trigger", style = MaterialTheme.typography.bodyMedium)
                    Text("Optional best-effort broadcast. Recommended: use FolderSync Instant sync / Monitor device folder.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = settings.folderSyncEnabled,
                    onCheckedChange = onFolderSyncEnabledChange,
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Send legacy broadcast after save", style = MaterialTheme.typography.bodyMedium)
                    Text("Best-effort only. Some Android/FolderSync versions may ignore it.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = settings.triggerAfterSave,
                    onCheckedChange = onTriggerAfterSaveChange,
                )
            }

            OutlinedTextField(
                value = folderPairName,
                onValueChange = {
                    folderPairName = it
                    onFolderPairNameChange(it)
                },
                label = { Text("FolderPair name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = triggerDelayText,
                onValueChange = {
                    val digits = it.filter(Char::isDigit).take(1)
                    val parsed = digits.toIntOrNull()?.coerceIn(1, 3)
                    triggerDelayText = parsed?.toString() ?: digits
                    if (parsed != null) onTriggerDelayChange(parsed)
                },
                label = { Text("Trigger delay seconds (1-3)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                text = "Recommended FolderSync setup:\n" +
                    "• Enable Instant sync / Monitor device folder for the MDRelay outbox folder.\n" +
                    "• FolderPair: $DEFAULT_FOLDER_PAIR_NAME\n" +
                    "• Direction: To remote folder\n" +
                    "• Remote: Google Drive/YSDAWAY-LLM-Wiki/inbox/mobile/\n" +
                    "• Sync deletions: OFF\n\n" +
                    "Legacy broadcast trigger is optional and best-effort only. Some Android/FolderSync versions may ignore it.",
                style = MaterialTheme.typography.bodySmall,
            )

            TextButton(onClick = onTestFolderSync) {
                Text("Test legacy broadcast trigger")
            }

            Text("Diagnostics", style = MaterialTheme.typography.titleSmall)
            DiagnosticLine("Last saved file", diagnostics.lastSavedFile ?: "None")
            DiagnosticLine("Last saved at", diagnostics.lastSavedAt?.let(::formatTime) ?: "None")
            DiagnosticLine("Last outbox URI/path", diagnostics.lastOutboxUri ?: "None")
            DiagnosticLine("Last FolderSync trigger at", diagnostics.lastTriggerAt?.let(::formatTime) ?: "None")
            DiagnosticLine("Last FolderSync folderPair", diagnostics.lastTriggerFolderPair ?: "None")
            DiagnosticLine("Last error", diagnostics.lastError ?: "None")
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ViewerContent(
    content: String,
    kind: FileKind,
    blocks: List<MarkdownBlock>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    when (kind) {
        FileKind.JSON -> JsonViewer(content, modifier)
        FileKind.TEXT -> PlainTextViewer(content, modifier)
        FileKind.MARKDOWN -> MarkdownViewer(blocks, listState, modifier)
    }
}

@Composable
private fun PlainTextViewer(content: String, modifier: Modifier = Modifier) {
    SelectionContainer {
        Text(
            text = content,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun JsonViewer(content: String, modifier: Modifier = Modifier) {
    val formatted = remember(content) {
        runCatching {
            val trimmed = content.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> error("Not JSON")
            }
        }.getOrNull()
    }
    SelectionContainer {
        Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (formatted == null) {
                Text(
                    "Invalid JSON or raw JSON text",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = formatted ?: content,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun MarkdownViewer(
    blocks: List<MarkdownBlock>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(blocks) { _, block ->
                MarkdownBlockView(block)
            }
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock) {
    when (block.type) {
        MarkdownBlockType.HEADING -> Text(
            text = block.text,
            fontSize = when (block.level) {
                1 -> 26.sp
                2 -> 22.sp
                3 -> 19.sp
                else -> 17.sp
            },
            fontWeight = FontWeight.Bold,
        )
        MarkdownBlockType.CODE -> {
            if (block.language.equals("mermaid", ignoreCase = true)) {
                MermaidCard(block.text)
            } else {
                CodeBlock(block.text, block.language)
            }
        }
        MarkdownBlockType.QUOTE -> Text(
            text = markdownInline(block.text),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MarkdownBlockType.RULE -> Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        MarkdownBlockType.TABLE -> MarkdownTableView(block.text)
        MarkdownBlockType.LIST, MarkdownBlockType.PARAGRAPH -> Text(
            text = markdownInline(block.text),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        )
    }
}

@Composable
private fun CodeBlock(code: String, language: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(10.dp),
    ) {
        if (!language.isNullOrBlank()) {
            Text(language, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = code,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun MarkdownTableView(rawTable: String) {
    val table = remember(rawTable) { parseMarkdownTable(rawTable) }
    if (table == null) {
        CodeBlock(rawTable, "")
        return
    }
    val columnCount = table.rows.firstOrNull()?.size ?: 0
    if (columnCount == 0) return

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wideTableWidth = (columnCount * 128).dp
        val tableWidth = if (columnCount <= 2 || maxWidth >= wideTableWidth) maxWidth else wideTableWidth
        val columnWeights = when {
            table.isKeyValue && columnCount == 2 -> listOf(0.85f, 2.15f)
            table.isProductOrder && columnCount == 4 -> listOf(2.4f, 1.1f, 0.7f, 1.1f)
            else -> List(columnCount) { 1f }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .width(tableWidth)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
            ) {
                table.rows.forEachIndexed { rowIndex, row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        row.forEachIndexed { cellIndex, cell ->
                            val isHeader = !table.isKeyValue && rowIndex == 0
                            val isKeyColumn = table.isKeyValue && cellIndex == 0
                            Text(
                                text = markdownInline(normalizeTableCellText(cell)),
                                modifier = Modifier
                                    .weight(columnWeights[cellIndex])
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isKeyColumn) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = when {
                                        isHeader -> FontWeight.SemiBold
                                        isKeyColumn -> FontWeight.Medium
                                        else -> FontWeight.Normal
                                    },
                                    textAlign = table.alignments.getOrElse(cellIndex) { TableAlignment.LEFT }.textAlign,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MermaidCard(source: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mermaid diagram source",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = {
                copyText(context, "Mermaid source", source)
                toast(context, "Mermaid copied")
            }) {
                Text("Copy Mermaid")
            }
        }
        Text(
            text = source,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SidePanel(
    showRecent: Boolean,
    recentItems: List<RecentFile>,
    tocItems: List<TocItem>,
    onTocClick: (TocItem) -> Unit,
    onRecentClick: (RecentFile) -> Unit,
    onRecentLongPress: (RecentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showRecent) {
        RecentPanel(recentItems, onRecentClick, onRecentLongPress, modifier)
    } else {
        TocPanel(tocItems, onTocClick, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactTocSheet(
    tocItems: List<TocItem>,
    onDismiss: () -> Unit,
    onClick: (TocItem) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        TocPanel(
            tocItems = tocItems,
            onClick = onClick,
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f),
        )
    }
}

@Composable
private fun TocPanel(
    tocItems: List<TocItem>,
    onClick: (TocItem) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Table of contents",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (onClose != null) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                }
            }
        }
        if (tocItems.isEmpty()) {
            item { Text("No headings", modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.bodySmall) }
        }
        items(tocItems) { item ->
            Text(
                text = item.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(item) }
                    .padding(start = ((item.level - 1) * 12).dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                fontSize = 14.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentPanel(
    items: List<RecentFile>,
    onClick: (RecentFile) -> Unit,
    onLongPress: (RecentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text("Recent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(6.dp))
        }
        if (items.isEmpty()) {
            item { Text("No recent files", modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.bodySmall) }
        }
        items(items) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .combinedClickable(onClick = { onClick(item) }, onLongClick = { onLongPress(item) })
                    .padding(8.dp),
            ) {
                Text(item.filename, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(item.reference, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                Text(formatTime(item.lastOpened), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun markdownInline(text: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        val pattern = Regex("""(`[^`]+`)|(\*\*[^*]+\*\*)|(\*[^*]+\*)|(\[[^\]]+]\([^)]+\))|(!\[[^\]]*]\([^)]+\))""")
        for (match in pattern.findAll(text)) {
            append(text.substring(index, match.range.first))
            val token = match.value
            when {
                token.startsWith("`") -> {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22333333)))
                    append(token.trim('`'))
                    pop()
                }
                token.startsWith("**") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(token.removePrefix("**").removeSuffix("**"))
                    pop()
                }
                token.startsWith("*") -> {
                    pushStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    append(token.removePrefix("*").removeSuffix("*"))
                    pop()
                }
                token.startsWith("![") -> {
                    val label = token.substringAfter("![").substringBefore("]")
                    val url = token.substringAfter("(").substringBeforeLast(")")
                    pushStyle(SpanStyle(color = Color(0xFF1F7A8C), textDecoration = TextDecoration.Underline))
                    append("[Image: $label] $url")
                    pop()
                }
                token.startsWith("[") -> {
                    val label = token.substringAfter("[").substringBefore("]")
                    val url = token.substringAfter("(").substringBeforeLast(")")
                    pushStringAnnotation("URL", url)
                    pushStyle(SpanStyle(color = Color(0xFF1F7A8C), textDecoration = TextDecoration.Underline))
                    append(label)
                    pop()
                    pop()
                }
            }
            index = match.range.last + 1
        }
        append(text.substring(index))
    }
}

private fun parseMarkdownTable(text: String): MarkdownTable? {
    return parseMarkdownTableLines(text.lines())
}

private fun parseMarkdownTableLines(lines: List<String>): MarkdownTable? {
    val tableLines = lines.map { it.trim() }.filter { it.isNotBlank() }
    if (tableLines.size < 2 || !isTableSeparatorLine(tableLines[1])) return null

    val header = splitTableRow(tableLines[0])
    val separator = splitTableRow(tableLines[1])
    if (header.isEmpty() || separator.isEmpty()) return null

    val body = tableLines.drop(2).map { splitTableRow(it) }
    val rawRows = listOf(header) + body
    val columnCount = rawRows.maxOfOrNull { it.size }?.coerceAtLeast(separator.size) ?: return null
    if (columnCount == 0) return null
    val isKeyValue = detectKeyValueTable(rawRows, columnCount)
    val isProductOrder = detectProductOrderTable(header, columnCount)

    val rows = rawRows.map { cells ->
        List(columnCount) { index -> cells.getOrElse(index) { "" } }
    }
    val alignments = List(columnCount) { index ->
        parseTableAlignment(separator.getOrElse(index) { "" })
    }
    return MarkdownTable(
        rows = rows,
        alignments = alignments,
        isKeyValue = isKeyValue,
        isProductOrder = isProductOrder,
    )
}

private fun detectKeyValueTable(rows: List<List<String>>, columnCount: Int): Boolean {
    if (columnCount != 2 || rows.size < 2) return false
    val twoCellRows = rows.count { it.size == 2 }
    if (twoCellRows < rows.size * 0.8f) return false
    val labelLikeRows = rows.count { row ->
        val key = row.getOrNull(0).orEmpty()
        val value = row.getOrNull(1).orEmpty()
        key.isNotBlank() && value.isNotBlank() && key.length <= 24
    }
    return labelLikeRows >= rows.size * 0.6f
}

private fun detectProductOrderTable(header: List<String>, columnCount: Int): Boolean {
    if (columnCount != 4) return false
    val normalizedHeader = header.joinToString(" ").lowercase(Locale.ROOT)
    val hasProduct = listOf("상품명", "상품", "제품명", "product", "item").any { normalizedHeader.contains(it) }
    val hasPrice = listOf("가격", "price").any { normalizedHeader.contains(it) }
    val hasQuantity = listOf("수량", "qty").any { normalizedHeader.contains(it) }
    val hasTotal = listOf("합계", "total").any { normalizedHeader.contains(it) }
    return hasProduct && (hasPrice || hasQuantity || hasTotal)
}

private fun normalizeTableCellText(cell: String): String {
    return cell.replace(Regex("""(?i)<br\s*/?>"""), "\n")
}

private fun isPossibleTableLine(line: String): Boolean {
    return line.contains("|") && splitTableRow(line).size >= 2
}

private fun isTableSeparatorLine(line: String): Boolean {
    val cells = splitTableRow(line)
    return cells.isNotEmpty() && cells.all { cell ->
        Regex("""^:?-{3,}:?$""").matches(cell.replace(" ", ""))
    }
}

private fun splitTableRow(line: String): List<String> {
    return line.trim()
        .trim('|')
        .split("|")
        .map { it.trim() }
}

private fun parseTableAlignment(marker: String): TableAlignment {
    val compact = marker.replace(" ", "")
    return when {
        compact.startsWith(":") && compact.endsWith(":") -> TableAlignment.CENTER
        compact.endsWith(":") -> TableAlignment.RIGHT
        else -> TableAlignment.LEFT
    }
}

private fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val lines = content.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = StringBuilder()
    var inCode = false
    var codeLanguage = ""
    val code = StringBuilder()

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) {
            val type = when {
                text.lines().all { it.trimStart().startsWith("- ") || it.trimStart().startsWith("* ") || Regex("""\d+\.\s+.*""").matches(it.trimStart()) } -> MarkdownBlockType.LIST
                parseMarkdownTable(text) != null -> MarkdownBlockType.TABLE
                else -> MarkdownBlockType.PARAGRAPH
            }
            blocks += MarkdownBlock(type, text)
        }
        paragraph.clear()
    }

    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        if (line.trimStart().startsWith("```")) {
            if (inCode) {
                blocks += MarkdownBlock(MarkdownBlockType.CODE, code.toString().trimEnd(), language = codeLanguage)
                code.clear()
                codeLanguage = ""
                inCode = false
            } else {
                flushParagraph()
                inCode = true
                codeLanguage = line.trim().removePrefix("```").trim()
            }
            index++
            continue
        }
        if (inCode) {
            code.appendLine(line)
            index++
            continue
        }

        if (isPossibleTableLine(line) && index + 1 < lines.size && isTableSeparatorLine(lines[index + 1])) {
            val tableLines = mutableListOf<String>()
            var tableIndex = index
            while (tableIndex < lines.size && isPossibleTableLine(lines[tableIndex])) {
                tableLines += lines[tableIndex]
                tableIndex++
            }
            if (parseMarkdownTableLines(tableLines) != null) {
                flushParagraph()
                blocks += MarkdownBlock(MarkdownBlockType.TABLE, tableLines.joinToString("\n"))
                index = tableIndex
                continue
            }
        }

        val heading = Regex("""^(#{1,6})\s+(.+)$""").find(line)
        when {
            heading != null -> {
                flushParagraph()
                val level = heading.groupValues[1].length
                val title = heading.groupValues[2].trim()
                blocks += MarkdownBlock(
                    type = MarkdownBlockType.HEADING,
                    text = title,
                    level = level,
                    heading = TocItem(level = level, title = title, blockIndex = blocks.size),
                )
            }
            line.trim().matches(Regex("""(-{3,}|\*{3,}|_{3,})""")) -> {
                flushParagraph()
                blocks += MarkdownBlock(MarkdownBlockType.RULE, "")
            }
            line.trimStart().startsWith(">") -> {
                flushParagraph()
                blocks += MarkdownBlock(MarkdownBlockType.QUOTE, line.trimStart().removePrefix(">").trim())
            }
            line.isBlank() -> flushParagraph()
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(line)
            }
        }
        index++
    }
    if (inCode) {
        blocks += MarkdownBlock(MarkdownBlockType.CODE, code.toString().trimEnd(), language = codeLanguage)
    }
    flushParagraph()
    return blocks.ifEmpty { listOf(MarkdownBlock(MarkdownBlockType.PARAGRAPH, content)) }
}

private fun copyReference(context: Context, reference: String) {
    if (reference.isBlank()) {
        toast(context, "No file reference")
        return
    }
    copyText(context, "MD Relay file reference", reference)
    toast(context, if (reference.startsWith("content:")) "Uri copied" else "Path copied")
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareContent(context: Context, filename: String, content: String, kind: FileKind) {
    val type = if (kind == FileKind.MARKDOWN) "text/markdown" else "text/plain"
    val intent = Intent(Intent.ACTION_SEND).apply {
        this.type = type
        putExtra(Intent.EXTRA_SUBJECT, filename)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Share current content"))
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun formatTime(epochMs: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(Date(epochMs))
}

private enum class ToolbarIcon(val resId: Int) {
    Visibility(R.drawable.ic_visibility),
    Edit(R.drawable.ic_edit),
    Share(R.drawable.ic_share),
    ContentCopy(R.drawable.ic_content_copy),
    TableOfContents(R.drawable.ic_format_list_bulleted),
    Fullscreen(R.drawable.ic_fullscreen),
    FullscreenExit(R.drawable.ic_fullscreen_exit),
    MoreVert(R.drawable.ic_more_vert),
    Link(R.drawable.ic_link),
    DarkMode(R.drawable.ic_dark_mode),
    LightMode(R.drawable.ic_light_mode),
    History(R.drawable.ic_history),
    FolderOpen(R.drawable.ic_folder_open),
    Settings(R.drawable.ic_settings),
    Save(R.drawable.ic_save),
}

private enum class FileKind {
    MARKDOWN,
    TEXT,
    JSON,
}

private data class OpenedFile(
    val uri: String,
    val sourceKey: String,
    val filename: String,
    val reference: String,
    val content: String,
    val kind: FileKind,
) {
    fun toRecent(permissionPersisted: Boolean = false): RecentFile =
        RecentFile(uri, filename, reference, System.currentTimeMillis(), permissionPersisted)
}

private data class SharedText(
    val filename: String,
    val content: String,
    val subject: String?,
    val sourcePackage: String?,
    val sharedUrl: String?,
)

private data class CaptureDraft(
    val visibleTitle: String,
    val subject: String?,
    val sourcePackage: String?,
    val sharedUrl: String?,
    val capturedText: String,
)

private data class SavedCaptureFile(
    val uri: Uri,
    val filename: String,
    val reference: String,
    val markdown: String,
)

private data class CaptureSettings(
    val outboxTreeUri: String? = null,
    val folderSyncEnabled: Boolean = true,
    val triggerAfterSave: Boolean = false,
    val triggerDelaySeconds: Int = 2,
    val folderPairName: String = DEFAULT_FOLDER_PAIR_NAME,
)

private data class CaptureDiagnostics(
    val lastSavedFile: String? = null,
    val lastSavedAt: Long? = null,
    val lastOutboxUri: String? = null,
    val lastTriggerAt: Long? = null,
    val lastTriggerFolderPair: String? = null,
    val lastError: String? = null,
)

private data class RecentFile(
    val uri: String,
    val filename: String,
    val reference: String,
    val lastOpened: Long,
    val permissionPersisted: Boolean = false,
)

private enum class MarkdownBlockType {
    HEADING,
    PARAGRAPH,
    LIST,
    CODE,
    QUOTE,
    TABLE,
    RULE,
}

private data class MarkdownBlock(
    val type: MarkdownBlockType,
    val text: String,
    val level: Int = 0,
    val language: String = "",
    val heading: TocItem? = null,
)

private data class MarkdownTable(
    val rows: List<List<String>>,
    val alignments: List<TableAlignment>,
    val isKeyValue: Boolean,
    val isProductOrder: Boolean,
)

private enum class TableAlignment(val textAlign: TextAlign) {
    LEFT(TextAlign.Start),
    CENTER(TextAlign.Center),
    RIGHT(TextAlign.End),
}

private data class TocItem(
    val level: Int,
    val title: String,
    val blockIndex: Int,
)

private class FileUriResolver(private val activity: Activity) {
    private var lastError: String? = null
    private var _lastPermissionPersisted = false

    fun lastPermissionPersisted() = _lastPermissionPersisted

    fun open(uri: Uri, flags: Int): OpenedFile? {
        lastError = null
        return runCatching {
            persistPermission(uri, flags)
            val content = readText(uri)
            val filename = displayName(uri)
            val reference = if (uri.scheme == "file") uri.path.orEmpty() else uri.toString()
            OpenedFile(
                uri = uri.toString(),
                sourceKey = uri.toString() + "#" + System.currentTimeMillis(),
                filename = filename,
                reference = reference,
                content = content,
                kind = detectKind(filename, content),
            )
        }.onFailure { e ->
            lastError = when (e) {
                is SecurityException -> "File permission expired. Please open the file again."
                else -> e.message ?: e.javaClass.simpleName
            }
        }.getOrNull()
    }

    fun lastErrorMessage(uri: Uri): String {
        val permissionExpiredMsg = "File permission expired. Please open the file again."
        if (lastError == permissionExpiredMsg) return permissionExpiredMsg
        val name = displayName(uri)
        return "Could not open $name" + lastError?.let { ": $it" }.orEmpty()
    }

    private fun persistPermission(uri: Uri, flags: Int) {
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        _lastPermissionPersisted = false
        if (takeFlags != 0) {
            _lastPermissionPersisted = runCatching {
                activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }.isSuccess
        }
    }

    private fun readText(uri: Uri): String {
        val bytes = activity.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("No readable input stream")
        return decodeText(bytes)
    }

    private fun decodeText(bytes: ByteArray): String {
        return runCatching {
            val decoder = Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        }.getOrElse {
            Charset.defaultCharset().decode(ByteBuffer.wrap(bytes)).toString()
        }
    }

    private fun displayName(uri: Uri): String {
        if (uri.scheme == "file") return uri.lastPathSegment ?: "Untitled"
        var cursor: Cursor? = null
        return try {
            cursor = activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(0) ?: uri.lastPathSegment ?: "Untitled"
            } else {
                uri.lastPathSegment ?: "Untitled"
            }
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Untitled"
        } finally {
            cursor?.close()
        }
    }

    private fun detectKind(filename: String, content: String): FileKind {
        val lower = filename.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".json") -> FileKind.JSON
            lower.endsWith(".txt") -> FileKind.TEXT
            lower.endsWith(".md") || lower.endsWith(".markdown") -> FileKind.MARKDOWN
            content.trimStart().startsWith("{") || content.trimStart().startsWith("[") -> FileKind.JSON
            else -> FileKind.MARKDOWN
        }
    }
}

private class RecentFileStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("recent_files", Context.MODE_PRIVATE)

    fun load(): List<RecentFile> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                RecentFile(
                    uri = obj.getString("uri"),
                    filename = obj.getString("filename"),
                    reference = obj.getString("reference"),
                    lastOpened = obj.getLong("lastOpened"),
                    permissionPersisted = obj.optBoolean("permissionPersisted", false),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun add(item: RecentFile) {
        val next = (listOf(item) + load().filterNot { it.uri == item.uri }).take(20)
        val array = JSONArray()
        next.forEach {
            array.put(JSONObject().apply {
                put("uri", it.uri)
                put("filename", it.filename)
                put("reference", it.reference)
                put("lastOpened", it.lastOpened)
                put("permissionPersisted", it.permissionPersisted)
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}

private class ThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    fun isDark(): Boolean = prefs.getBoolean("dark", false)

    fun setDark(dark: Boolean) {
        prefs.edit().putBoolean("dark", dark).apply()
    }
}

private class CaptureStore(context: Context) {
    private val prefs = context.getSharedPreferences("capture_settings", Context.MODE_PRIVATE)

    fun loadSettings(): CaptureSettings = CaptureSettings(
        outboxTreeUri = prefs.getString(KEY_OUTBOX_TREE_URI, null),
        folderSyncEnabled = prefs.getBoolean(KEY_FOLDER_SYNC_ENABLED, true),
        triggerAfterSave = prefs.getBoolean(KEY_TRIGGER_AFTER_SAVE, false),
        triggerDelaySeconds = prefs.getInt(KEY_TRIGGER_DELAY_SECONDS, 2).coerceIn(1, 3),
        folderPairName = prefs.getString(KEY_FOLDER_PAIR_NAME, DEFAULT_FOLDER_PAIR_NAME) ?: DEFAULT_FOLDER_PAIR_NAME,
    )

    fun updateSettings(settings: CaptureSettings) {
        prefs.edit()
            .putString(KEY_OUTBOX_TREE_URI, settings.outboxTreeUri)
            .putBoolean(KEY_FOLDER_SYNC_ENABLED, settings.folderSyncEnabled)
            .putBoolean(KEY_TRIGGER_AFTER_SAVE, settings.triggerAfterSave)
            .putInt(KEY_TRIGGER_DELAY_SECONDS, settings.triggerDelaySeconds.coerceIn(1, 3))
            .putString(KEY_FOLDER_PAIR_NAME, settings.folderPairName.ifBlank { DEFAULT_FOLDER_PAIR_NAME })
            .apply()
    }

    fun persistOutboxUri(context: Context, uri: Uri): Result<Unit> {
        return runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            updateSettings(loadSettings().copy(outboxTreeUri = uri.toString()))
            prefs.edit().putString(KEY_LAST_OUTBOX_URI, uri.toString()).apply()
        }.onFailure {
            recordSaveFailure(it.message ?: "Could not persist outbox permission")
        }
    }

    fun loadDiagnostics(): CaptureDiagnostics = CaptureDiagnostics(
        lastSavedFile = prefs.getString(KEY_LAST_SAVED_FILE, null),
        lastSavedAt = prefs.getLong(KEY_LAST_SAVED_AT, 0L).takeIf { it > 0L },
        lastOutboxUri = prefs.getString(KEY_LAST_OUTBOX_URI, null),
        lastTriggerAt = prefs.getLong(KEY_LAST_TRIGGER_AT, 0L).takeIf { it > 0L },
        lastTriggerFolderPair = prefs.getString(KEY_LAST_TRIGGER_FOLDER_PAIR, null),
        lastError = prefs.getString(KEY_LAST_ERROR, null),
    )

    fun recordSaveSuccess(savedFile: SavedCaptureFile) {
        prefs.edit()
            .putString(KEY_LAST_SAVED_FILE, savedFile.filename)
            .putLong(KEY_LAST_SAVED_AT, System.currentTimeMillis())
            .putString(KEY_LAST_OUTBOX_URI, savedFile.reference)
            .remove(KEY_LAST_ERROR)
            .apply()
    }

    fun recordSaveFailure(message: String) {
        prefs.edit().putString(KEY_LAST_ERROR, message).apply()
    }

    fun recordTriggerResult(folderPairName: String, result: FolderSyncTriggerResult) {
        val editor = prefs.edit()
            .putString(KEY_LAST_TRIGGER_FOLDER_PAIR, folderPairName)
        if (result.sent) {
            editor.putLong(KEY_LAST_TRIGGER_AT, System.currentTimeMillis())
        } else {
            editor.putString(KEY_LAST_ERROR, result.message)
        }
        editor.apply()
    }

    private companion object {
        const val KEY_OUTBOX_TREE_URI = "outbox_tree_uri"
        const val KEY_FOLDER_SYNC_ENABLED = "folder_sync_enabled"
        const val KEY_TRIGGER_AFTER_SAVE = "trigger_after_save"
        const val KEY_TRIGGER_DELAY_SECONDS = "trigger_delay_seconds"
        const val KEY_FOLDER_PAIR_NAME = "folder_pair_name"
        const val KEY_LAST_SAVED_FILE = "last_saved_file"
        const val KEY_LAST_SAVED_AT = "last_saved_at"
        const val KEY_LAST_OUTBOX_URI = "last_outbox_uri"
        const val KEY_LAST_TRIGGER_AT = "last_trigger_at"
        const val KEY_LAST_TRIGGER_FOLDER_PAIR = "last_trigger_folder_pair"
        const val KEY_LAST_ERROR = "last_error"
    }
}

private class MarkdownOutboxWriter(private val context: Context) {
    fun saveMarkdown(outboxTreeUri: Uri, draft: CaptureDraft): SavedCaptureFile {
        val timestamp = ZonedDateTime.now()
        val markdown = buildCaptureMarkdown(draft, timestamp)
        val baseName = buildCaptureFilename(timestamp, draft.subject ?: draft.visibleTitle)
        val filename = createUniqueFilename(outboxTreeUri, baseName)
        val targetUri = createDocument(outboxTreeUri, filename)
        context.contentResolver.openOutputStream(targetUri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(markdown)
        } ?: throw FileNotFoundException("Could not open outbox output stream")
        return SavedCaptureFile(
            uri = targetUri,
            filename = filename,
            reference = targetUri.toString(),
            markdown = markdown,
        )
    }

    private fun createUniqueFilename(outboxTreeUri: Uri, baseName: String): String {
        val existingNames = queryChildDisplayNames(context, outboxTreeUri)
        if (baseName !in existingNames) return baseName
        val stem = baseName.removeSuffix(".md")
        var index = 2
        while (true) {
            val candidate = "$stem-${index.toString().padStart(2, '0')}.md"
            if (candidate !in existingNames) return candidate
            index++
        }
    }

    private fun createDocument(outboxTreeUri: Uri, filename: String): Uri {
        val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            outboxTreeUri,
            DocumentsContract.getTreeDocumentId(outboxTreeUri),
        )
        return DocumentsContract.createDocument(
            context.contentResolver,
            treeDocumentUri,
            "text/markdown",
            filename,
        ) ?: error("Could not create $filename in outbox")
    }
}

private data class FolderSyncTriggerResult(
    val sent: Boolean,
    val message: String,
)

private fun triggerFolderSync(context: Context, folderPairName: String): FolderSyncTriggerResult {
    val normalizedName = folderPairName.trim()
    if (normalizedName.isBlank()) {
        return FolderSyncTriggerResult(sent = false, message = "FolderSync trigger not sent: folderPair name is empty.")
    }
    // Send to all candidate packages. Broadcasts to non-installed packages are silently ignored by
    // the OS, so this is safe without package-visibility checks. The <queries> block in the manifest
    // handles detection when we need it; here we always send.
    for (pkg in FOLDERSYNC_PACKAGES) {
        context.sendBroadcast(
            Intent(FOLDERSYNC_ACTION_SYNC).apply {
                setPackage(pkg)
                putExtra(FOLDERSYNC_EXTRA_FOLDERPAIR, normalizedName)
            },
        )
    }
    return FolderSyncTriggerResult(
        sent = true,
        message = "FolderSync trigger sent to ${FOLDERSYNC_PACKAGES.size} candidate package(s). FolderPair: \"$normalizedName\"",
    )
}

private fun queryChildDisplayNames(context: Context, treeUri: Uri): Set<String> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri),
    )
    val names = linkedSetOf<String>()
    context.contentResolver.query(
        childrenUri,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            cursor.getString(0)?.let(names::add)
        }
    }
    return names
}

private fun openDocumentTree(context: Context, treeUri: Uri): Boolean {
    val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri),
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(treeDocumentUri, DocumentsContract.Document.MIME_TYPE_DIR)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(intent)
    }.isSuccess
}

private fun buildCaptureFilename(timestamp: ZonedDateTime, titleCandidate: String?): String {
    val base = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
    val suffix = sanitizeFilenameSegment(titleCandidate)
    return if (suffix.isNullOrBlank()) {
        "${base}_mobile-capture.md"
    } else {
        "${base}_${suffix}.md"
    }
}

internal fun sanitizeFilenameSegment(value: String?): String? {
    if (value.isNullOrBlank()) return null
    // Strip trailing .md extensions (handles .md, .md.md, etc.) before further processing.
    val withoutMd = value.replace(Regex("""(\.md)+$""", RegexOption.IGNORE_CASE), "")
    // If the value looks like a previously-generated capture filename (YYYY-MM-DD_HHmm_<rest>),
    // strip the timestamp prefix so we don't embed old filenames into new ones.
    val withoutTimestampPrefix = withoutMd.replace(Regex("""^\d{4}-\d{2}-\d{2}_\d{4}_"""), "")
    val cleaned = withoutTimestampPrefix
        .substringBefore('\n')
        .substringBefore('?')
        .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "-")
        .replace(Regex("""\s+"""), "-")
        .replace(Regex("-{2,}"), "-")
        .trim('-', '_', '.')
        .take(40)
    return cleaned.takeIf {
        it.isNotBlank() && it.lowercase(Locale.ROOT) !in setOf("md-relay", "shared-text", "mobile-capture")
    }
}

private fun buildCaptureMarkdown(draft: CaptureDraft, timestamp: ZonedDateTime): String {
    val sharedUrl = draft.sharedUrl
    val sourcePackage = draft.sourcePackage
    return buildString {
        appendLine("---")
        appendLine("type: mobile-capture")
        appendLine("source_app: MDRelay")
        appendLine("source_status: raw")
        appendLine("review_status: draft")
        appendLine("sensitivity: personal")
        appendLine("mobile_sync: false")
        appendLine("created: ${timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}")
        appendLine("---")
        appendLine()
        appendLine("## Ingest Intent")
        appendLine()
        appendLine("- Why did I save this?")
        appendLine("- Which life/project/investment decision may this affect?")
        appendLine("- Temporary reference or durable knowledge?")
        appendLine("- Should this become wiki, decision, glossary, roadmap, or archive?")
        appendLine()
        if (sharedUrl != null || sourcePackage != null) {
            appendLine("## Source")
            appendLine()
            if (sharedUrl != null) appendLine("- URL: $sharedUrl")
            if (sourcePackage != null) appendLine("- Shared from: $sourcePackage")
            appendLine()
        }
        appendLine("## Capture")
        appendLine()
        appendLine(draft.capturedText.ifBlank { "(empty capture)" })
    }.trimEnd()
}

internal fun detectSharedUrl(text: String): String? {
    val match = Regex("""https?://\S+""").find(text) ?: return null
    return match.value.trimEnd(')', ']', '}', '.', ',')
}

private fun ClipData.firstUriOrNull(): Uri? {
    return if (itemCount > 0) getItemAt(0)?.uri else null
}

private fun <T : Parcelable> Intent.parcelableExtraCompat(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name) as? T
    }
}

private const val DEFAULT_FOLDER_PAIR_NAME = "YSDAWAY_MDRelay_Outbox_to_Drive"
private const val FOLDERSYNC_ACTION_SYNC = "com.tacit.foldersync.action.SYNC"
private const val FOLDERSYNC_EXTRA_FOLDERPAIR = "folderpair"
private val FOLDERSYNC_PACKAGES = listOf(
    "dk.tacit.android.foldersync.full",
    "dk.tacit.android.foldersync.lite",
)

internal fun extractFilenameFromUrl(urlStr: String): String {
    val decoded = runCatching { java.net.URLDecoder.decode(urlStr, "UTF-8") }.getOrNull() ?: urlStr
    val cleanUrl = decoded.substringBefore('?').substringBefore('#')
    val segment = cleanUrl.substringAfterLast('/')
    
    val sanitized = sanitizeFilenameSegment(segment)
    if (sanitized.isNullOrBlank()) {
        return "web-capture.md"
    }
    
    val lower = segment.lowercase(Locale.ROOT)
    return when {
        lower.endsWith(".json") -> "$sanitized.json"
        lower.endsWith(".txt") -> "$sanitized.txt"
        lower.endsWith(".markdown") -> "$sanitized.markdown"
        else -> "$sanitized.md"
    }
}

private fun downloadWebFile(urlStr: String): OpenedFile? {
    return runCatching {
        val url = java.net.URL(urlStr)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.connect()
        if (conn.responseCode != java.net.HttpURLConnection.HTTP_OK) {
            return null
        }
        val content = conn.inputStream.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
        val filename = extractFilenameFromUrl(urlStr)
        val kind = when {
            filename.endsWith(".json", ignoreCase = true) -> FileKind.JSON
            filename.endsWith(".txt", ignoreCase = true) -> FileKind.TEXT
            else -> FileKind.MARKDOWN
        }
        OpenedFile(
            uri = urlStr,
            filename = filename,
            content = content,
            reference = urlStr,
            kind = kind,
            sourceKey = "web_" + System.currentTimeMillis()
        )
    }.getOrNull()
}
