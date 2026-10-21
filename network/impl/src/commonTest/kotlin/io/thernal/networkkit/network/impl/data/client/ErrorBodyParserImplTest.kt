package io.thernal.networkkit.network.impl.data.client

import io.thernal.networkkit.network.api.domain.ErrorBody
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ErrorBodyParserImplTest {
    private val parser = ErrorBodyParserImpl()

    @Test
    fun `members it does not read are kept as extras`() {
        val body = parser.parse(
            status = 400,
            body = """{"error": "WRONG_CODE", "attemptsLeft": 3, "requestId": "r1"}""",
        )

        val expected = ErrorBody(
            error = "WRONG_CODE",
            extras = mapOf(
                "attemptsLeft" to JsonPrimitive(3),
                "requestId" to JsonPrimitive("r1"),
            ),
        )
        assertEquals(expected = expected, actual = body)
    }

    @Test
    fun `a body whose known keys have another shape is kept whole in extras`() {
        val body = parser.parse(
            status = 400,
            body = """{"code": "WRONG_CODE", "detail": "Check the code"}""",
        )

        val expected = ErrorBody(
            extras = mapOf(
                "code" to JsonPrimitive("WRONG_CODE"),
                "detail" to JsonPrimitive("Check the code"),
            ),
        )
        assertEquals(expected = expected, actual = body)
    }

    @Test
    fun `anything but a non-empty object is not an error body`() {
        listOf("", "{}", "[1]", "<html>").forEach { text ->
            assertNull(parser.parse(status = 500, body = text))
        }
    }
}
