package app.reelstack.data.model

/** Resume wins duplicate entries; equal/missing dates retain the provider's stable order. */
fun combinedWatching(resume: List<LibraryMedia>, nextUp: List<LibraryMedia>): List<LibraryMedia> =
    (resume + nextUp).distinctBy { it.source to it.id }
        .sortedWith(compareByDescending { it.lastActivityEpochMillis ?: Long.MIN_VALUE })
