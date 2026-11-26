package com.aedev.aetranzit.data.mapper

import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.domain.model.Route
import com.aedev.aetranzit.domain.model.RouteStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMapperTest {

    @Test
    fun `encode and decode round-trip preserves intermediate stops and special characters`() {
        val stops = listOf(
            RouteStop(name = "Bocaue \"Toll\" Exit", fare = 65.5),
            RouteStop(name = "San Fernando / NLEX", fare = 140.0),
            RouteStop(name = "Dasmariñas", fare = 88.25)
        )

        val encoded = RouteStopsJsonCodec.encode(stops)
        val decoded = RouteStopsJsonCodec.decode(encoded)

        assertEquals(stops, decoded)
    }

    @Test
    fun `decode handles empty, blank, and malformed JSON safely`() {
        assertTrue(RouteStopsJsonCodec.decode("").isEmpty())
        assertTrue(RouteStopsJsonCodec.decode("   ").isEmpty())
        assertTrue(RouteStopsJsonCodec.decode("[]").isEmpty())
        assertTrue(RouteStopsJsonCodec.decode("not-valid-json").isEmpty())
    }

    @Test
    fun `Route toEntity and RouteEntity toDomain preserve all fields`() {
        val domain = Route(
            id = "route-1",
            origin = "Cubao",
            destination = "Dau",
            baseFare = 195.0,
            stops = listOf(
                RouteStop("Bocaue", 65.0),
                RouteStop("San Fernando", 140.0)
            ),
            isSynced = true,
            updatedAt = 1_700_000_000L
        )

        val entity: RouteEntity = domain.toEntity()
        val roundTrip = entity.toDomain()

        assertEquals(domain, roundTrip)
        assertEquals("Cubao → Dau", roundTrip.displayName)
    }
}
