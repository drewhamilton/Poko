package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class UserTest {
    @Test
    fun valuey() {
        val alice1 = User("alice", 25)
        val alice2 = User("alice", 25)
        assertEquals(alice1, alice2)
    }
}
