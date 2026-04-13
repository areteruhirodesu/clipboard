package com.areteruhiro.clip

import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.areteruhiro.clip.ui.theme.ClipTheme

data class ClipItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isPinned: Boolean = false
)

class MainActivity : ComponentActivity() {

    private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClipTheme {
                ClipboardApp(clipboardManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardApp(clipboardManager: ClipboardManager) {
    val context = LocalContext.current
    val composeClipboard = LocalClipboardManager.current

    var items by remember { mutableStateOf(listOf<ClipItem>()) }
    var searchQuery by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // クリップボード監視
    DisposableEffect(clipboardManager) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            val text = clipboardManager.primaryClip
                ?.getItemAt(0)
                ?.text
                ?.toString()
                ?.trim()
                ?: return@OnPrimaryClipChangedListener

            if (text.isNotEmpty() && items.none { it.text == text }) {
                items = listOf(ClipItem(text = text)) + items
            }
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        onDispose { clipboardManager.removePrimaryClipChangedListener(listener) }
    }

    // スナックバー表示
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val filtered = items.filter {
        searchQuery.isBlank() || it.text.contains(searchQuery, ignoreCase = true)
    }.sortedByDescending { it.isPinned }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("クリップ履歴") },
                actions = {
                    IconButton(onClick = {
                        items = items.filter { it.isPinned }
                        snackbarMessage = "固定以外を削除しました"
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "全削除")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("検索...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("履歴がありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { item ->
                        ClipItemCard(
                            item = item,
                            onCopy = {
                                composeClipboard.setText(AnnotatedString(item.text))
                                snackbarMessage = "コピーしました"
                            },
                            onPin = {
                                items = items.map {
                                    if (it.id == item.id) it.copy(isPinned = !it.isPinned) else it
                                }
                            },
                            onDelete = {
                                items = items.filter { it.id != item.id }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipItemCard(
    item: ClipItem,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onCopy, onLongClick = onDelete)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.text,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onPin) {
            Icon(
                imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (item.isPinned) "固定解除" else "固定",
                tint = if (item.isPinned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}