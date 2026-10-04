## 2026-08-22 - [LaunchedEffect and Synchronous IO]
**Learning:** In Jetpack Compose, `LaunchedEffect` executes its coroutine block on the Main thread by default. Calling synchronous I/O or heavy CPU-bound operations (like `MediaMetadataRetriever` and bitmap decoding) directly inside `LaunchedEffect` causes severe UI jank.
**Action:** Always wrap heavy synchronous I/O or CPU operations in `withContext(Dispatchers.IO)` before calling them from `LaunchedEffect` or other Compose side effects. Consider using `LruCache` to cache the results of expensive, repeated computations (like color palette extraction from artwork).

## 2024-10-25 - [Missing Key in LazyLists]
**Learning:** In this Jetpack Compose codebase, many LazyColumn and LazyVerticalGrid implementations were missing the `key` parameter. This is a crucial performance optimization, especially when items might be reordered or filtered. Without keys, Compose defaults to using item positions to identify them. When the list changes, Compose might unnecessarily recompose items that just moved or even lose internal state of list items. When `Modifier.animateItem()` is used, as in `HomeScreen.kt`, `key` parameters are absolutely required for the animations to track items correctly.
**Action:** Always verify if Lazy lists provide a `key` parameter, especially if items can be added, removed, or reordered dynamically (like a playlist/queue), or if item-level animations are used. Use unique identifiers like `uri` or ID. For items where duplicates might be present (e.g., song queue or lyrics text), a compound key appending the index (e.g., `song.uri + index`) is necessary to satisfy Compose's unique key constraint.

## 2024-11-13 - [ArtworkCache and Remember]
**Learning:** In Jetpack Compose, state can be remembered incorrectly if key changes are not tracked. In this codebase, initializing `ImageBitmap` with `null` inside `remember` and decoding repeatedly on every recomposition/navigation bypassed the available `ArtworkCache`, causing performance overhead.
**Action:** When working with image loading in Jetpack Compose, always initialize state directly from memory caches like `ArtworkCache.get(uri)` during the `remember` block. Wrap the `remember` with a key (`remember(uri)`) to ensure the cache is hit correctly on item recycle, and short-circuit any `LaunchedEffect` that decodes bitmaps if the cache already populated the bitmap.

## 2024-05-09 - [SQLite Parameter Limit with large sets]
**Learning:** SQLite has a hard limit of 999 parameters for binding variables. When processing large music libraries (which can easily exceed a thousand tracks), passing a large unbounded list directly to a Room query like `@Query("DELETE FROM songs WHERE uri NOT IN (:uris)")` leads to an `SQLiteException: too many SQL variables`.
**Action:** Avoid passing unbounded dynamic lists directly to SQL `IN` or `NOT IN` clauses. To circumvent the limit, compute the exact set of items to act on (e.g. `urisToDelete`) in Kotlin, then slice them into batches using `chunked(900)`, and finally apply the update or delete operation on each chunk separately.

## 2026-09-03 - [Flow Filtering and Main Thread]
**Learning:** In Compose/Flow, combining large dataset flows with rapid user inputs (like search query StateFlows) can cause excessive CPU overhead and UI jank because the filtering executes synchronously on the Main thread for every keystroke.
**Action:** Use `.debounce(300L)` on the query flow to batch typing events. Follow it with `.flowOn(Dispatchers.Default)` after the `combine` block to offload the expensive filtering logic to a background thread before the result reaches `.stateIn`.
