/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spaces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import io.element.android.features.invite.api.SeenInvitesStore
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.ui.safety.rememberHideInvitesAvatar
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Inject
class HomeSpacesPresenter(
    private val client: MatrixClient,
    private val seenInvitesStore: SeenInvitesStore,
) : Presenter<HomeSpacesState> {
    @Composable
    override fun present(): HomeSpacesState {
        val hideInvitesAvatar by client.rememberHideInvitesAvatar()
        val spaceRooms by remember {
            client.spaceService.topLevelSpacesFlow.map { it.toImmutableList() }
        }.collectAsState(persistentListOf())

        // Kiber: sum of unread notifications per space. The SDK gives every space the ids of all rooms below it,
        // so one pass over the room summaries covers nested levels without walking the tree ourselves.
        val unreadBySpace by remember {
            combine(
                client.spaceService.spaceFiltersFlow,
                client.roomListService.allRooms.summaries,
            ) { filters, summaries ->
                val unreadByRoom = summaries.associate { it.roomId to it.info.numUnreadNotifications }
                filters.associate { filter ->
                    filter.spaceRoom.roomId to filter.descendants.sumOf { unreadByRoom[it] ?: 0L }
                }.toImmutableMap()
            }
        }.collectAsState(persistentMapOf())

        val seenSpaceInvites by remember {
            seenInvitesStore.seenRoomIds().map { it.toImmutableSet() }
        }.collectAsState(persistentSetOf())

        fun handleEvent(event: HomeSpacesEvent) {
            // when (event) { }
        }

        return HomeSpacesState(
            space = CurrentSpace.Root,
            spaceRooms = spaceRooms,
            seenSpaceInvites = seenSpaceInvites,
            hideInvitesAvatar = hideInvitesAvatar,
            // TODO enable once we can link to the screen to explore public spaces
            canExploreSpaces = false,
            unreadBySpace = unreadBySpace,
            eventSink = ::handleEvent,
        )
    }
}
