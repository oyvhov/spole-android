package app.reelstack.ui

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfScope
import app.reelstack.data.model.smartShelfAccountKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal fun ReelstackUiState.smartShelfConnection(shelf: SmartShelf): ServiceConnection? =
    if (shelf.scope == null) libraryConnection else connections.firstOrNull {
        it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank() &&
            it.token.isNotBlank() && shelf.appliesTo(it)
    }

/** Only actual root-library views may seed a new scope, never a nested series or season id. */
internal fun ReelstackUiState.newSmartShelfScope(): SmartShelfScope? {
    val connection = libraryConnection?.takeIf { it.userId.isNotBlank() } ?: return null
    val knownLibraries = libraryShortcuts.map { it.first }.toSet() + libraryChoices.map { it.id }
    val root = libraryPath.firstOrNull()?.first?.takeIf { it in knownLibraries && SmartShelf.idOfPath(it) == null }
    return SmartShelfScope.of(connection, root?.let { setOf(it) } ?: emptySet())
}

internal fun sameSmartShelfQuery(first: SmartShelf, second: SmartShelf): Boolean =
    first.scope == second.scope && first.tags == second.tags && first.genres == second.genres &&
        first.kinds == second.kinds && first.unwatched == second.unwatched &&
        first.requiresBoth == second.requiresBoth && first.sort == second.sort

internal fun sameSmartShelfAccount(first: ServiceConnection?, second: ServiceConnection): Boolean =
    first != null && smartShelfAccountKey(first) == smartShelfAccountKey(second) && first.token == second.token

internal fun sameSmartShelfSource(first: SmartShelf, second: SmartShelf): Boolean =
    first.scope?.source == second.scope?.source && first.scope?.accountKey == second.scope?.accountKey

internal fun ReelstackUiState.ownsSmartShelfEditorResult(start: ReelstackUiState, draft: SmartShelf,
    connection: ServiceConnection, matchLibraries: Boolean = true): Boolean {
    val current = smartShelfEditor?.draft ?: return false
    return activeProfileId == start.activeProfileId && sessionScope == start.sessionScope &&
        current.id == draft.id && (if (matchLibraries) current.scope == draft.scope else sameSmartShelfSource(current, draft)) &&
        sameSmartShelfAccount(smartShelfConnection(current), connection)
}
