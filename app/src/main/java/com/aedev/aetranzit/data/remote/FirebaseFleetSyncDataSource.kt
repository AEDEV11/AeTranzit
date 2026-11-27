package com.aedev.aetranzit.data.remote

import com.aedev.aetranzit.data.local.entity.BusEntity
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.data.local.entity.RouteEntity
import com.aedev.aetranzit.data.local.entity.TicketEntity
import com.aedev.aetranzit.data.local.entity.TripEntity
import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.data.mapper.RouteStopsJsonCodec
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.model.PassengerType
import com.aedev.aetranzit.domain.model.RouteStop
import com.aedev.aetranzit.domain.model.TicketStatus
import com.aedev.aetranzit.domain.model.TripStatus
import com.aedev.aetranzit.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface FleetSyncRemoteDataSource {
    suspend fun pushCompany(company: CompanyEntity): Result<Unit>
    suspend fun fetchCompanyByJoinCode(joinCode: String): Result<CompanyEntity?>
    suspend fun fetchCompanyById(companyId: String): Result<CompanyEntity?>
    suspend fun fetchUsersByCompany(companyId: String): Result<List<UserEntity>>
    suspend fun fetchBusesByCompany(companyId: String): Result<List<BusEntity>>
    suspend fun fetchRoutesByCompany(companyId: String): Result<List<RouteEntity>>
    suspend fun fetchTripsByCompany(
        companyId: String,
        conductorId: String? = null
    ): Result<List<TripEntity>>
    suspend fun fetchTicketsByCompany(
        companyId: String,
        conductorId: String? = null
    ): Result<List<TicketEntity>>
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
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : FleetSyncRemoteDataSource {

    override suspend fun pushCompany(company: CompanyEntity): Result<Unit> =
        runFirestoreOp("company push") {
            val docRef = firestore.collection(COMPANIES_COLLECTION).document(company.id)
            val existingSnap = docRef.get().await()
            val payload = buildMap<String, Any> {
                put("id", company.id)
                put("name", company.name)
                put("joinCode", company.joinCode)
                put("createdAt", company.createdAt)
                put("updatedAt", company.updatedAt)
                if (!existingSnap.exists()) {
                    firebaseAuth.currentUser?.uid?.let { uid ->
                        put("ownerUid", uid)
                    }
                }
            }
            docRef.set(payload, SetOptions.merge()).await()
        }

    override suspend fun fetchCompanyByJoinCode(joinCode: String): Result<CompanyEntity?> =
        runFirestoreQuery("company join-code lookup") {
            val normalizedCode = joinCode.trim().uppercase(Locale.US)
            if (normalizedCode.isBlank()) {
                return@runFirestoreQuery null
            }
            val querySnap = firestore.collection(COMPANIES_COLLECTION)
                .whereEqualTo("joinCode", normalizedCode)
                .limit(1)
                .get()
                .await()

            val doc = querySnap.documents.firstOrNull()
            if (doc == null || !doc.exists()) {
                null
            } else {
                doc.toCompanyEntity(fallbackCode = normalizedCode)
            }
        }

    override suspend fun fetchCompanyById(companyId: String): Result<CompanyEntity?> =
        runFirestoreQuery("company pull") {
            val cleanId = companyId.trim()
            if (cleanId.isBlank()) return@runFirestoreQuery null
            val doc = firestore.collection(COMPANIES_COLLECTION)
                .document(cleanId)
                .get()
                .await()
            if (!doc.exists()) null else doc.toCompanyEntity()
        }

    override suspend fun fetchUsersByCompany(companyId: String): Result<List<UserEntity>> =
        runFirestoreQuery("company staff pull") {
            val cleanCompanyId = requireCompanyId(companyId)
            val querySnap = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("companyId", cleanCompanyId)
                .get()
                .await()
            querySnap.documents.mapNotNull { it.toUserEntity(cleanCompanyId) }
        }

    override suspend fun fetchBusesByCompany(companyId: String): Result<List<BusEntity>> =
        runFirestoreQuery("fleet buses pull") {
            val cleanCompanyId = requireCompanyId(companyId)
            val querySnap = firestore.collection(BUSES_COLLECTION)
                .whereEqualTo("companyId", cleanCompanyId)
                .get()
                .await()
            querySnap.documents.mapNotNull { it.toBusEntity(cleanCompanyId) }
        }

    override suspend fun fetchRoutesByCompany(companyId: String): Result<List<RouteEntity>> =
        runFirestoreQuery("routes pull") {
            val cleanCompanyId = requireCompanyId(companyId)
            val querySnap = firestore.collection(ROUTES_COLLECTION)
                .whereEqualTo("companyId", cleanCompanyId)
                .get()
                .await()
            querySnap.documents.mapNotNull { it.toRouteEntity(cleanCompanyId) }
        }

    override suspend fun fetchTripsByCompany(
        companyId: String,
        conductorId: String?
    ): Result<List<TripEntity>> = runFirestoreQuery("trips pull") {
        val cleanCompanyId = requireCompanyId(companyId)
        var query = firestore.collection(TRIPS_COLLECTION)
            .whereEqualTo("companyId", cleanCompanyId)
        val cleanConductorId = conductorId?.trim().orEmpty()
        if (cleanConductorId.isNotEmpty()) {
            query = query.whereEqualTo("conductorId", cleanConductorId)
        }
        val querySnap = query.get().await()
        querySnap.documents.mapNotNull { it.toTripEntity(cleanCompanyId) }
    }

    override suspend fun fetchTicketsByCompany(
        companyId: String,
        conductorId: String?
    ): Result<List<TicketEntity>> = runFirestoreQuery("tickets pull") {
        val cleanCompanyId = requireCompanyId(companyId)
        var query = firestore.collection(TICKETS_COLLECTION)
            .whereEqualTo("companyId", cleanCompanyId)
        val cleanConductorId = conductorId?.trim().orEmpty()
        if (cleanConductorId.isNotEmpty()) {
            query = query.whereEqualTo("conductorId", cleanConductorId)
        }
        val querySnap = query.get().await()
        querySnap.documents.mapNotNull { it.toTicketEntity(cleanCompanyId) }
    }

    override suspend fun pushBus(bus: BusEntity): Result<Unit> = runFirestoreOp("bus push") {
        val payload = mapOf(
            "id" to bus.id,
            "busNumber" to bus.busNumber,
            "plateNumber" to bus.plateNumber,
            "totalSeats" to bus.totalSeats,
            "columns" to bus.columns,
            "companyId" to bus.companyId,
            "updatedAt" to bus.updatedAt
        )
        firestore.collection(BUSES_COLLECTION)
            .document(bus.id)
            .set(payload, SetOptions.merge())
            .await()
    }

    override suspend fun deleteBus(busId: String): Result<Unit> = runFirestoreOp("bus delete") {
        firestore.collection(BUSES_COLLECTION)
            .document(busId)
            .delete()
            .await()
    }

    override suspend fun pushRoute(route: RouteEntity): Result<Unit> =
        runFirestoreOp("route push") {
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
                "companyId" to route.companyId,
                "updatedAt" to route.updatedAt
            )
            firestore.collection(ROUTES_COLLECTION)
                .document(route.id)
                .set(payload, SetOptions.merge())
                .await()
        }

    override suspend fun deleteRoute(routeId: String): Result<Unit> =
        runFirestoreOp("route delete") {
            firestore.collection(ROUTES_COLLECTION)
                .document(routeId)
                .delete()
                .await()
        }

    override suspend fun pushTrip(trip: TripEntity): Result<Unit> = runFirestoreOp("trip push") {
        val payload = mapOf(
            "id" to trip.id,
            "busId" to trip.busId,
            "routeId" to trip.routeId,
            "conductorId" to trip.conductorId,
            "companyId" to trip.companyId,
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
    ): Result<Unit> = runFirestoreOp("ticket & seat occupancy push") {
        val ticketPayload = mapOf(
            "id" to ticket.id,
            "tripId" to ticket.tripId,
            "busId" to ticket.busId,
            "conductorId" to ticket.conductorId,
            "companyId" to ticket.companyId,
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

    private fun requireAuthenticatedSession() {
        checkNotNull(firebaseAuth.currentUser) {
            "Firebase Auth session is not signed in. Log in once while online to link your cloud session."
        }
    }

    private fun requireCompanyId(companyId: String): String {
        val clean = companyId.trim()
        require(clean.isNotEmpty()) { "Active companyId is required for cloud sync." }
        return clean
    }

    private inline fun runFirestoreOp(operation: String, block: () -> Unit): Result<Unit> {
        return try {
            requireAuthenticatedSession()
            block()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, operation))
        }
    }

    private inline fun <T> runFirestoreQuery(operation: String, block: () -> T): Result<T> {
        return try {
            requireAuthenticatedSession()
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, operation))
        }
    }

    private fun DocumentSnapshot.toCompanyEntity(
        fallbackCode: String = Company.DEFAULT_JOIN_CODE
    ): CompanyEntity {
        val now = System.currentTimeMillis()
        return CompanyEntity(
            id = getString("id")?.takeIf { it.isNotBlank() } ?: id,
            name = getString("name").orEmpty().ifBlank { Company.DEFAULT_COMPANY_NAME },
            joinCode = getString("joinCode").orEmpty().ifBlank { fallbackCode },
            createdAt = getLong("createdAt") ?: now,
            isSynced = true,
            updatedAt = getLong("updatedAt") ?: now
        )
    }

    private fun DocumentSnapshot.toUserEntity(companyId: String): UserEntity? {
        val email = getString("email")?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
            ?: return null
        val resolvedId = getString("id")?.takeIf { it.isNotBlank() } ?: id
        return UserEntity(
            id = resolvedId,
            name = getString("name").orEmpty().ifBlank { email.substringBefore('@') },
            email = email,
            passwordHash = "",
            role = UserRole.fromRaw(getString("role").orEmpty()).name,
            companyId = getString("companyId")?.takeIf { it.isNotBlank() } ?: companyId,
            isActive = getBoolean("isActive") ?: true,
            mustChangePassword = getBoolean("mustChangePassword") ?: false,
            isSynced = true,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }

    private fun DocumentSnapshot.toBusEntity(companyId: String): BusEntity? {
        val busNumber = getString("busNumber")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val plateNumber = getString("plateNumber")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val totalSeats = (getLong("totalSeats") ?: 45L).toInt()
        val columns = (getLong("columns") ?: 4L).toInt().let { if (it == 5) 5 else 4 }
        return BusEntity(
            id = getString("id")?.takeIf { it.isNotBlank() } ?: id,
            busNumber = busNumber,
            plateNumber = plateNumber,
            totalSeats = totalSeats,
            columns = columns,
            companyId = getString("companyId")?.takeIf { it.isNotBlank() } ?: companyId,
            isSynced = true,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }

    private fun DocumentSnapshot.toRouteEntity(companyId: String): RouteEntity? {
        val origin = getString("origin")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val destination = getString("destination")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val baseFare = getDouble("baseFare") ?: return null
        val rawStopsJson = getString("stopsJson")?.takeIf { it.isNotBlank() }
        val resolvedStopsJson = rawStopsJson ?: run {
            val rawStopsList = get("stops") as? List<*>
            val parsedStops = rawStopsList?.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val stopName = (map["name"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val stopFare = (map["fare"] as? Number)?.toDouble() ?: return@mapNotNull null
                RouteStop(name = stopName, fare = stopFare)
            }.orEmpty()
            RouteStopsJsonCodec.encode(parsedStops)
        }
        return RouteEntity(
            id = getString("id")?.takeIf { it.isNotBlank() } ?: id,
            origin = origin,
            destination = destination,
            baseFare = baseFare,
            stopsJson = resolvedStopsJson,
            companyId = getString("companyId")?.takeIf { it.isNotBlank() } ?: companyId,
            isSynced = true,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }

    private fun DocumentSnapshot.toTripEntity(companyId: String): TripEntity? {
        val busId = getString("busId")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val routeId = getString("routeId")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val conductorId = getString("conductorId")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val startedAt = getLong("startedAt") ?: return null
        return TripEntity(
            id = getString("id")?.takeIf { it.isNotBlank() } ?: id,
            busId = busId,
            routeId = routeId,
            conductorId = conductorId,
            companyId = getString("companyId")?.takeIf { it.isNotBlank() } ?: companyId,
            status = TripStatus.fromRaw(getString("status").orEmpty()).name,
            startedAt = startedAt,
            endedAt = getLong("endedAt"),
            isSynced = true,
            updatedAt = getLong("updatedAt") ?: startedAt
        )
    }

    private fun DocumentSnapshot.toTicketEntity(companyId: String): TicketEntity? {
        val tripId = getString("tripId")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val busId = getString("busId")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val conductorId = getString("conductorId")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val seatNumber = getString("seatNumber")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val destination = getString("destination")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val fareAmount = getDouble("fareAmount") ?: return null
        val cashTendered = getDouble("cashTendered") ?: fareAmount
        val changeAmount = getDouble("changeAmount") ?: (cashTendered - fareAmount).coerceAtLeast(0.0)
        val createdAt = getLong("createdAt") ?: 0L
        return TicketEntity(
            id = getString("id")?.takeIf { it.isNotBlank() } ?: id,
            tripId = tripId,
            busId = busId,
            conductorId = conductorId,
            companyId = getString("companyId")?.takeIf { it.isNotBlank() } ?: companyId,
            seatNumber = seatNumber,
            destination = destination,
            passengerType = PassengerType.fromRaw(getString("passengerType").orEmpty()).name,
            fareAmount = fareAmount,
            cashTendered = cashTendered,
            changeAmount = changeAmount,
            status = TicketStatus.fromRaw(getString("status").orEmpty()).name,
            createdAt = createdAt,
            isSynced = true,
            updatedAt = getLong("updatedAt") ?: createdAt
        )
    }

    private companion object {
        const val COMPANIES_COLLECTION = "companies"
        const val USERS_COLLECTION = "users"
        const val BUSES_COLLECTION = "buses"
        const val ROUTES_COLLECTION = "routes"
        const val TRIPS_COLLECTION = "trips"
        const val TICKETS_COLLECTION = "tickets"
    }
}
