package com.silisten.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.silisten.app.SiListenUiState
import com.silisten.app.SiListenViewModel
import com.silisten.app.data.model.MusicPlaylist
import com.silisten.app.data.model.Song

@Composable
internal fun SearchOverlay(
    uiState: SiListenUiState,
    viewModel: SiListenViewModel,
    dark: Boolean,
    autoFocus: Boolean,
    bottomPadding: Dp,
    onClose: () -> Unit,
    onOpenPlaylist: (MusicPlaylist) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xF20A0A0A) else Color(0xF6F7F7F8))
    ) {
        SearchScreen(
            uiState = uiState,
            viewModel = viewModel,
            padding = PaddingValues(bottom = bottomPadding),
            autoFocus = autoFocus,
            onClose = onClose,
            onOpenPlaylist = onOpenPlaylist
        )
    }
}

@Composable
internal fun AddToPlaylistSheet(
    song: Song,
    playlists: List<MusicPlaylist>,
    dark: Boolean,
    isLoading: (MusicPlaylist) -> Boolean,
    onPlaylistClick: (MusicPlaylist) -> Unit
) {
    val titleColor = if (dark) Color.White else Color(0xFF111111)
    val mutedText = if (dark) Color.White.copy(alpha = 0.62f) else Color(0xFF666A70)
    val rowColor = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.86f)
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filteredPlaylists = remember(playlists, query) {
        val keyword = query.trim()
        if (keyword.isBlank()) {
            playlists
        } else {
            playlists.filter { playlist ->
                playlist.title.contains(keyword, ignoreCase = true) ||
                    playlist.subtitle.contains(keyword, ignoreCase = true)
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "加入歌单",
                    color = titleColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "搜索歌单",
                        tint = titleColor
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.title,
                color = mutedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showSearch) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("搜索歌单") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (filteredPlaylists.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (query.isBlank()) "还没有可加入的歌单" else "没有找到歌单",
                    subtitle = if (query.isBlank()) {
                        "登录后同步网易云歌单，或先在网易云音乐里创建一个歌单。"
                    } else {
                        "换个关键词试试。"
                    },
                    dark = dark
                )
            }
        } else {
            items(filteredPlaylists, key = { it.id }) { playlist ->
                val loading = isLoading(playlist)
                Surface(
                    color = rowColor,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(
                        1.dp,
                        if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClick(shape = RoundedCornerShape(22.dp)) {
                            if (!loading) onPlaylistClick(playlist)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = playlist.coverUrl,
                            contentDescription = playlist.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF252525))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                color = titleColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = playlist.subtitle.ifBlank { "网易云歌单" },
                                color = mutedText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = "加入",
                                tint = mutedText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
