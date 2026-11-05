package com.marekabaffy.kotlinnoop.integrationtest

import com.marekabaffy.kotlinnoop.annotations.NoOp

@NoOp
public interface Repository {
    public val name: String?
    public fun list(): List<Int>
    public fun emptyList(): MutableList<*>
    public suspend fun load(id: String?): Map<String, Int>
    public fun callback(): suspend (Int) -> String
}

@NoOp
public data class Callbacks(
    val onClick: () -> Unit,
    val onLongClick: (String) -> Int,
)

public interface WithCompanion {
    public fun value(): Int

    @NoOp
    public companion object
}

public class Outer {
    @NoOp
    public interface Inner {
        public fun value(): Int
    }
}
