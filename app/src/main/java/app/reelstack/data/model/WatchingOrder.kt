package app.reelstack.data.model

/** Keep playback rows source-local, including when next-up is folded into resume. */
fun watchingBySource(
    resume: List<LibraryMedia>,
    nextUp: List<LibraryMedia> = emptyList(),
): Map<ServiceKind, List<LibraryMedia>> =
    (if (nextUp.isEmpty()) resume else combinedWatching(resume, nextUp)).groupBy { it.source }

/** Resume wins duplicate entries; equal/missing dates retain the provider's stable order. */
fun combinedWatching(resume: List<LibraryMedia>, nextUp: List<LibraryMedia>): List<LibraryMedia> =
    (resume + nextUp).distinctBy { it.source to it.id }
        .sortedWith(compareByDescending { it.lastActivityEpochMillis ?: Long.MIN_VALUE })
