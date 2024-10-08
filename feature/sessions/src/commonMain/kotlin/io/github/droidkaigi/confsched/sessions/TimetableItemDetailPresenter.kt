package io.github.droidkaigi.confsched.sessions

import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import conference_app_2024.feature.sessions.generated.resources.bookmarked_successfully
import conference_app_2024.feature.sessions.generated.resources.view_bookmark_list
import io.github.droidkaigi.confsched.compose.SafeLaunchedEffect
import io.github.droidkaigi.confsched.model.Lang
import io.github.droidkaigi.confsched.model.SessionsRepository
import io.github.droidkaigi.confsched.model.TimetableItem
import io.github.droidkaigi.confsched.model.TimetableItemId
import io.github.droidkaigi.confsched.model.TimetableSessionType.NORMAL
import io.github.droidkaigi.confsched.model.localSessionsRepository
import io.github.droidkaigi.confsched.sessions.TimetableItemDetailEvent.Bookmark
import io.github.droidkaigi.confsched.sessions.TimetableItemDetailEvent.FavoriteListNavigated
import io.github.droidkaigi.confsched.sessions.TimetableItemDetailEvent.SelectDescriptionLanguage
import io.github.droidkaigi.confsched.sessions.TimetableItemDetailScreenUiState.Loaded
import io.github.droidkaigi.confsched.sessions.TimetableItemDetailScreenUiState.Loading
import io.github.droidkaigi.confsched.ui.UserMessageResult.ActionPerformed
import io.github.droidkaigi.confsched.ui.providePresenterDefaults
import io.github.droidkaigi.confsched.ui.rememberNavigationArgument
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource

sealed interface TimetableItemDetailEvent {
    data class Bookmark(val timetableItem: TimetableItem) : TimetableItemDetailEvent
    data class SelectDescriptionLanguage(val language: Lang) : TimetableItemDetailEvent
    data object FavoriteListNavigated : TimetableItemDetailEvent
}

@Composable
fun timetableItemDetailPresenter(
    events: SharedFlow<TimetableItemDetailEvent>,
    sessionsRepository: SessionsRepository = localSessionsRepository(),
    timetableItemIdArg: String = rememberNavigationArgument(
        key = timetableItemDetailScreenRouteItemIdParameterName,
        initialValue = "",
    ),
): TimetableItemDetailScreenUiState = providePresenterDefaults<TimetableItemDetailScreenUiState> { userMessageStateHolder ->
    val timetableItemId = TimetableItemId(timetableItemIdArg)
    val timetableItemStateWithBookmark by rememberUpdatedState(
        sessionsRepository
            .timetableItemWithBookmark(timetableItemId),
    )
    var selectedDescriptionLanguage by remember { mutableStateOf<Lang?>(null) }
    var shouldGoToFavoriteList by remember { mutableStateOf(false) }
    val bookmarkedSuccessfullyString = stringResource(SessionsRes.string.bookmarked_successfully)
    val viewBookmarkListString = stringResource(SessionsRes.string.view_bookmark_list)

    SafeLaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is Bookmark -> {
                    val timetableItemWithBookmark = timetableItemStateWithBookmark
                    val timetableItem =
                        timetableItemWithBookmark?.first ?: return@collect
                    sessionsRepository.toggleBookmark(timetableItem.id)
                    val oldBookmarked = timetableItemWithBookmark.second
                    if (!oldBookmarked) {
                        val result = userMessageStateHolder.showMessage(
                            message = bookmarkedSuccessfullyString,
                            actionLabel = viewBookmarkListString,
                            duration = Short,
                        )
                        if (result == ActionPerformed) {
                            shouldGoToFavoriteList = true
                        }
                    }
                }

                is SelectDescriptionLanguage -> {
                    selectedDescriptionLanguage = event.language
                }

                is FavoriteListNavigated -> {
                    shouldGoToFavoriteList = false
                }
            }
        }
    }
    SafeLaunchedEffect(timetableItemStateWithBookmark?.first) {
        val timetableItem = timetableItemStateWithBookmark?.first ?: return@SafeLaunchedEffect
        if (selectedDescriptionLanguage == null) {
            selectedDescriptionLanguage = Lang.valueOf(timetableItem.language.langOfSpeaker)
        }
    }
    val timetableItemStateWithBookmarkValue = timetableItemStateWithBookmark
        ?: return@providePresenterDefaults Loading(timetableItemId, userMessageStateHolder)
    val (timetableItem, bookmarked) = timetableItemStateWithBookmarkValue
    Loaded(
        timetableItem = timetableItem,
        timetableItemDetailSectionUiState = TimetableItemDetailSectionUiState(timetableItem),
        isBookmarked = bookmarked,
        isLangSelectable = timetableItem.sessionType == NORMAL,
        currentLang = selectedDescriptionLanguage,
        roomThemeKey = timetableItem.room.getThemeKey(),
        timetableItemId = timetableItemId,
        userMessageStateHolder = userMessageStateHolder,
        shouldGoToFavoriteList = shouldGoToFavoriteList,
    )
}
