## 2026-08-22 - [LaunchedEffect and Synchronous IO]
**Learning:** In Jetpack Compose, `LaunchedEffect` executes its coroutine block on the Main thread by default. Calling synchronous I/O or heavy CPU-bound operations (like `MediaMetadataRetriever` and bitmap decoding) directly inside `LaunchedEffect` causes severe UI jank.
**Action:** Always wrap heavy synchronous I/O or CPU operations in `withContext(Dispatchers.IO)` before calling them from `LaunchedEffect` or other Compose side effects. Consider using `LruCache` to cache the results of expensive, repeated computations (like color palette extraction from artwork).

## 2024-10-25 - [Missing Key in LazyLists]
**Learning:** In this Jetpack Compose codebase, many LazyColumn and LazyVerticalGrid implementations were missing the `key` parameter. This is a crucial performance optimization, especially when items might be reordered or filtered. Without keys, Compose defaults to using item positions to identify them. When the list changes, Compose might unnecessarily recompose items that just moved or even lose internal state of list items. When `Modifier.animateItem()` is used, as in `HomeScreen.kt`, `key` parameters are absolutely required for the animations to track items correctly.
**Action:** Always verify if Lazy lists provide a `key` parameter, especially if items can be added, removed, or reordered dynamically (like a playlist/queue), or if item-level animations are used. Use unique identifiers like `uri` or ID. For items where duplicates might be present (e.g., song queue or lyrics text), a compound key appending the index (e.g., `song.uri + index`) is necessary to satisfy Compose's unique key constraint.

## 2024-11-13 - [ArtworkCache and Remember]
**Learning:** In Jetpack Compose, state can be remembered incorrectly if key changes are not tracked. In this codebase, initializing `ImageBitmap` with `null` inside `remember` and decoding repeatedly on every recomposition/navigation bypassed the available `ArtworkCache`, causing performance overhead.
**Action:** When working with image loading in Jetpack Compose, always initialize state directly from memory caches like `ArtworkCache.get(uri)` during the `remember` block. Wrap the `remember` with a key (`remember(uri)`) to ensure the cache is hit correctly on item recycle, and short-circuit any `LaunchedEffect` that decodes bitmaps if the cache already populated the bitmap.

## 2024-11-13 - [Debounce Flow Inputs]
**Learning:** In Jetpack Compose applications, combining rapid user input (like a search query) directly with a large list of data for filtering causes an O(n) operation to execute on every keystroke, leading to CPU spike and UI jank.
**Action:** When filtering lists based on StateFlow inputs, use the `.debounce(timeMillis)` operator to delay the combination until the user finishes typing. In Kotlin Coroutines, this requires the `@OptIn(FlowPreview::class)` annotation on the class.
