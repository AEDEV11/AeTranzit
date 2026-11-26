package com.aedev.aetranzit.data.remote

import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.mapper.RouteStopsJsonCodec
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface FleetSyncRemoteDataSource {
    suspend fun pushBus(bus: BusEntity): Result<Unit>
    suspend fun deleteBus(busId: String): Result<Unit>
    suspend fun pushRoute(route: RouteEntity): Result<Unit>
    suspend fun deleteRoute(routeId: String): Result<Unit>
    suspend fun pushTrip(trip: TripEntity): Result<Unit>
    suspend fun pushTicketAndOccupiedSeats(
        ticket: TicketEntity,
        occupiedSeats: List<String>
    ): Result<Unit>
}

@Singleton
class FirebaseFleetSyncDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : FleetSyncRemoteDataSource {

    override suspend fun pushBus(bus: BusEntity): Result<Unit> = runFirestoreOp {
        val payload = mapOf(
            "id" to bus.id,
            "busNumber" to bus.busNumber,
            "plateNumber" to bus.plateNumber,
            "totalSeats" to bus.totalSeats,
            "columns" to bus.columns,
            "updatedAt" to bus.updatedAt
        )
        firestore.collection(BUSES_COLLECTION)
            .document(bus.id)
            .set(payload, SetOptions.merge())
            .await()
    }

    override suspend fun deleteBus(busId: String): Result<Unit> = runFirestoreOp {
        firestore.collection(BUSES_COLLECTION)
            .document(busId)
            .delete()
            .await()
    }

    override suspend fun pushRoute(route: RouteEntity): Result<Unit> = runFirestoreOp {
        val stopsMaps = RouteStopsJsonCodec.decode(route.stopsJson).map { stop ->
            mapOf("name" to stop.name, "fare" to stop.fare)
        }
        val payload = mapOf(
            "id" to route.id,
            "origin" to route.origin,
            "destination" to route.destination,
            "baseFare" to route.baseFare,
            "stops" to stopsMaps,
            "stopsJson" to route.stopsJson,
            "updatedAt" to route.updatedAt
        )
        firestore.collection(ROUTES_COLLECTION)
            .document(route.id)
            .set(payload, SetOptions.merge())
            .await()
    }

    override suspend fun deleteRoute(routeId: String): Result<Unit> = runFirestoreOp {
        firestore.collection(ROUTES_COLLECTION)
            .document(routeId)
            .delete()
            .await()
    }

    override suspend fun pushTrip(trip: TripEntity): Result<Unit> = runFirestoreOp {
        val payload = mapOf(
            "id" to trip.id,
            "busId" to trip.busId,
            "routeId" to trip.routeId,
            "conductorId" to trip.conductorId,
            "status" to trip.status,
            "startedAt" to trip.startedAt,
            "endedAt" to trip.endedAt,
            "updatedAt" to trip.updatedAt
        )
        firestore.collection(TRIPS_COLLECTION)
            .document(trip.id)
            .set(payload, SetOptions.merge())
            .await()
    }

    override suspend fun pushTicketAndOccupiedSeats(
        ticket: TicketEntity,
        occupiedSeats: List<String>
    ): Result<Unit> = runFirestoreOp {
        val ticketPayload = mapOf(
            "id" to ticket.id,
            "tripId" to ticket.tripId,
            "busId" to ticket.busId,
            "conductorId" to ticket.conductorId,
            "seatNumber" to ticket.seatNumber,
            "destination" to ticket.destination,
            "passengerType" to ticket.passengerType,
            "fareAmount" to ticket.fareAmount,
            "cashTendered" to ticket.cashTendered,
            "changeAmount" to ticket.changeAmount,
            "status" to ticket.status,
            "createdAt" to ticket.createdAt,
            "updatedAt" to ticket.updatedAt
        )
        val tripSeatPayload = mapOf(
            "occupiedSeats" to occupiedSeats,
            "updatedAt" to ticket.updatedAt
        )
        firestore.runTransaction { transaction ->
            val tripRef = firestore.collection(TRIPS_COLLECTION).document(ticket.tripId)
            val ticketRef = firestore.collection(TICKETS_COLLECTION).document(ticket.id)
            transaction.get(tripRef)
            transaction.set(ticketRef, ticketPayload, SetOptions.merge())
            transaction.set(tripRef, tripSeatPayload, SetOptions.merge())
            null
        }.await()
    }

    private inline fun runFirestoreOp(block: () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val BUSES_COLLECTION = "buses"
        const val ROUTES_COLLECTION = "routes"
        const val TRIPS_COLLECTION = "trips"
        const val TICKETS_COLLECTION = "tickets"
    }
}
