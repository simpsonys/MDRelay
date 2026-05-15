package com.simpsonys.mdrelay

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var fileResolver: FileUriResolver
    private lateinit var recentStore: RecentFileStore
    private lateinit var themeStore: ThemeStore
    private var incomingFile by mutableStateOf<OpenedFile?>(null)
    private var incomingText by mutableStateOf<SharedText?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileResolver = FileUriResolver(this)
        recentStore = RecentFileStore(this)
        themeStore = ThemeStore(this)
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
                    incomingFile = fileResolver.open(uri, intent.flags)
                    incomingText = null
                    if (incomingFile == null) toast(this, fileResolver.lastErrorMessage(uri))
                }
            }
            Intent.ACTION_SEND -> {
                val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (stream != null) {
                    incomingFile = fileResolver.open(stream, intent.flags)
                    incomingText = null
                    if (incomingFile == null) toast(this, fileResolver.lastErrorMessage(stream))
                } else {
                    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    if (!text.isNullOrBlank()) {
                        incomingText = SharedText(
                            filename = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                                ?: "Shared text",
                            content = text,
                        )
                        incomingFile = null
                    }
                }
            }
        }
    }
}

@Composable
private fun MdRelayApp(
    initialFile: OpenedFile?,
    initialText: SharedText?,
    fileResolver: FileUriResolver,
    recentStore: RecentFileStore,
    themeStore: ThemeStore,
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
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
    var overflowOpen by remember { mutableStateOf(false) }
    val listState = remember { LazyListState() }
    val recentItems = remember { mutableStateOf(recentStore.load()) }
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
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val opened = fileResolver.open(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (opened != null) {
                loadOpenedFile(
                    opened = opened,
                    context = context,
                    recentStore = recentStore,
                    onRecentChanged = { recentItems.value = it },
                    onLoaded = { applyOpenedFile(opened) },
                )
            } else {
                toast(context, fileResolver.lastErrorMessage(uri))
            }
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
                editMode = false
                showRecent = false
                fullscreen = false
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
                            loadOpenedFile(opened, context, recentStore, { recentItems.value = it }) {
                                applyOpenedFile(opened)
                            }
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
                    onSave = { toast(context, "Save is not supported in MVP. Use Share or Copy.") },
                )

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
                                loadOpenedFile(opened, context, recentStore, { recentItems.value = it }) {
                                    applyOpenedFile(opened)
                                }
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

                if (rawContent.isBlank()) {
                    EmptyScreen(
                        recentItems = recentItems.value,
                        onOpen = { filePicker.launch(arrayOf("text/markdown", "text/plain", "application/json", "application/octet-stream")) },
                        onRecentClick = { item ->
                            val uri = Uri.parse(item.uri)
                            val opened = fileResolver.open(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            if (opened != null) {
                                loadOpenedFile(opened, context, recentStore, { recentItems.value = it }) {
                                    applyOpenedFile(opened)
                                }
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
}

private fun loadOpenedFile(
    opened: OpenedFile,
    context: Context,
    recentStore: RecentFileStore,
    onRecentChanged: (List<RecentFile>) -> Unit,
    onLoaded: () -> Unit,
) {
    onLoaded()
    copyText(context, "MD Relay content", opened.content)
    toast(context, "Content copied")
    recentStore.add(opened.toRecent())
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
                    text = { Text("Save deferred") },
                    leadingIcon = { ToolbarIconImage(ToolbarIcon.Save, "Save deferred") },
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
    onOpen: () -> Unit,
    onRecentClick: (RecentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onOpen) {
            Text("Open file")
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
    fun toRecent(): RecentFile = RecentFile(uri, filename, reference, System.currentTimeMillis())
}

private data class SharedText(
    val filename: String,
    val content: String,
)

private data class RecentFile(
    val uri: String,
    val filename: String,
    val reference: String,
    val lastOpened: Long,
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
        }.onFailure {
            lastError = it.message ?: it.javaClass.simpleName
        }.getOrNull()
    }

    fun lastErrorMessage(uri: Uri): String {
        val name = displayName(uri)
        return "Could not open $name" + lastError?.let { ": $it" }.orEmpty()
    }

    private fun persistPermission(uri: Uri, flags: Int) {
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (takeFlags != 0) {
            runCatching { activity.contentResolver.takePersistableUriPermission(uri, takeFlags) }
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
