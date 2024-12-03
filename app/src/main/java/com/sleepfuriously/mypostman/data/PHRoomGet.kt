package com.sleepfuriously.mypostman.data

data class PHRoomGet(
    val errors: PHErrorResponse,
    val `data`: List<RoomData>
)

data class RoomData(
    val id: String,
    val id_v1: String,
    val children: List<RoomChildren>,
    val services: List<RoomService>,
    val metadata: RoomMetadata,
    val type: String
)

data class RoomService(
    val rid: String,
    val rtype: String
)

data class RoomChildren(
    val rid: String,
    val rtype: String
)

data class RoomMetadata(
    val name: String,
    val archetype: String
)