package com.theveloper.pixelplay.presentation.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.ExpressiveScrollBar
import com.theveloper.pixelplay.presentation.components.MiniPlayerHeight
import com.theveloper.pixelplay.presentation.components.QueuePlaylistSongItem
import com.theveloper.pixelplay.presentation.utils.LocalAppHapticsConfig
import com.theveloper.pixelplay.presentation.utils.performAppCompatHapticFeedback
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import org.json.JSONArray
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val SONGS_MANUAL_ORDER_PREFS = "songs_manual_order"
private const val SONGS_MANUAL_ORDER_KEY = "order"

private fun loadSongsManualOrder(context: Context): List<String> {
    val raw = context.getSharedPreferences(SONGS_MANUAL_ORDER_PREFS, Context.MODE_PRIVATE)
        .getString(SONGS_MANUAL_ORDER_KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveSongsManualOrder(context: Context, orderedIds: List<String>) {
    val arr = JSONArray()
    orderedIds.forEach { arr.put(it) }
    context.getSharedPreferences(SONGS_MANUAL_ORDER_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(SONGS_MANUAL_ORDER_KEY, arr.toString())
        .apply()
}

/** Saved order first (songs that no longer exist are dropped); songs without a saved slot go to the end. */
private fun applySongsManualOrder(songs: List<Song>, savedIds: List<String>): List<Song> {
    if (savedIds.isEmpty()) return songs
    val byId = songs.associateBy { it.id }
    val ordered = savedIds.mapNotNull { byId[it] }
    val known = ordered.map { it.id }.toHashSet()
    return ordered + songs.filter { it.id !in known }
}

/**
 * Songs tab list used when the "Manual order" sort is selected. It does not use Paging:
 * the full library list is reordered in memory and the order is stored in SharedPreferences.
 */
@Composable
fun LibraryManualOrderSongsList(
    songs: List<Song>,
    playerViewModel: PlayerViewModel,
    bottomBarHeight: Dp,
    onMoreOptionsClick: (Song) -> Unit,
    hasCurrentSong: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current
    val reorderSongCd = stringResource(R.string.playlist_cd_reorder_songs)
    val playerStableState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()

    var savedIds by remember { mutableStateOf(loadSongsManualOrder(context)) }
    val orderedSongs = remember(songs, savedIds) { applySongsManualOrder(songs, savedIds) }
    var localSongs by remember(orderedSongs) { mutableStateOf(orderedSongs) }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            localSongs = localSongs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .padding(
                    start = 12.dp,
                    end = if (listState.canScrollForward || listState.canScrollBackward) 22.dp else 12.dp,
                    bottom = 6.dp
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 26.dp,
                        topEnd = 26.dp,
                        bottomStart = PlayerSheetCollapsedCornerRadius,
                        bottomEnd = PlayerSheetCollapsedCornerRadius
                    )
                ),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = bottomBarHeight + MiniPlayerHeight + 30.dp)
        ) {
            items(
                items = localSongs,
                key = { it.id },
                contentType = { "manual_order_song" }
            ) { song ->
                ReorderableItem(
                    state = reorderableState,
                    key = song.id
                ) { isDragging ->
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.05f else 1f,
                        label = "manualOrderScale"
                    )

                    QueuePlaylistSongItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        onClick = {
                            playerViewModel.playSongs(localSongs, song, "Library")
                        },
                        song = song,
                        isCurrentSong = playerStableState.currentSong?.id == song.id,
                        isPlaying = playerStableState.isPlaying,
                        isDragging = isDragging,
                        onRemoveClick = {},
                        isFromPlaylist = true,
                        isReorderModeEnabled = true,
                        isDragHandleVisible = true,
                        isRemoveButtonVisible = false,
                        onMoreOptionsClick = onMoreOptionsClick,
                        dragHandle = {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .draggableHandle(
                                        onDragStarted = {
                                            performAppCompatHapticFeedback(
                                                view,
                                                appHapticsConfig,
                                                HapticFeedbackConstantsCompat.GESTURE_START
                                            )
                                        },
                                        onDragStopped = {
                                            performAppCompatHapticFeedback(
                                                view,
                                                appHapticsConfig,
                                                HapticFeedbackConstantsCompat.GESTURE_END
                                            )
                                            val ids = localSongs.map { it.id }
                                            saveSongsManualOrder(context, ids)
                                            savedIds = ids
                                        }
                                    )
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragIndicator,
                                    contentDescription = reorderSongCd,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }

        val scrollBarBottomPadding = if (hasCurrentSong) {
            bottomBarHeight + MiniPlayerHeight + 16.dp
        } else {
            bottomBarHeight + 16.dp
        }
        ExpressiveScrollBar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp, top = 16.dp, bottom = scrollBarBottomPadding),
            listState = listState
        )
    }
}
