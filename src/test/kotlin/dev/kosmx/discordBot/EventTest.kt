package dev.kosmx.discordBot

import org.junit.jupiter.api.Test


class EventTest {

    @Test
    fun testEvents() {
        val event = Event<(String) -> TypedEventResult<Int>>()



        event.subscribe(5) {
            if (it == "World") {
                println("Hello $it"); 2
            } else {
                null
            }
        }
        event.subscribe(10) {s ->
            if (s.isNotEmpty()) {
                println("string is $s")
                1
            } else null
        }
        event.subscribe(7) {
            if (it == "Hello") {
                println("$it World"); 3
            } else {
                null
            }
        }

        assert(event("Test").t == 1)
        assert(event("World").t == 2)
        assert(event("Hello").t == 3)
        assert(!event("").consume)

    }
}