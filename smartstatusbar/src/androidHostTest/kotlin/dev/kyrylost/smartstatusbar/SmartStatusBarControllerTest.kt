package dev.kyrylost.smartstatusbar

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmartStatusBarControllerTest {

    private val state = WindowState()
    private val controller = SmartStatusBarController(state::requestUpdate)

    @Test
    fun requestBeforeSubscription_isNotLost() = runTest {
        controller.requestUpdate()

        assertEquals(Unit, state.requests.first())
    }

    @Test
    fun multipleRequestsBeforeSubscription_areConflatedIntoOne() = runTest {
        repeat(5) { controller.requestUpdate() }

        assertEquals(Unit, state.requests.first())
        // nothing else is buffered
        assertNull(withTimeoutOrNull(100) { state.requests.first() })
    }

    @Test
    fun requestAfterSubscription_isDelivered() = runTest {
        val received = mutableListOf<Unit>()

        backgroundScope.launch { state.requests.toList(received) }
        runCurrent()
        assertEquals(0, received.size)

        controller.requestUpdate()
        runCurrent()
        assertEquals(1, received.size)

        controller.requestUpdate()
        runCurrent()
        assertEquals(2, received.size)
    }

    @Test
    fun noRequest_noEmission() = runTest {
        assertNull(withTimeoutOrNull(100) { state.requests.first() })
    }

    @Test
    fun controllersOfOneWindow_shareRequests() = runTest {
        SmartStatusBarController(state::requestUpdate).requestUpdate()

        assertEquals(Unit, state.requests.first())
    }
}
