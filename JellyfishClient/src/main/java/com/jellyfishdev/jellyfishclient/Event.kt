package com.jellyfishdev.jellyfishclient

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

internal val gson = Gson()

// convert a data class to a map
internal fun <T> T.serializeToMap(): Map<String, Any?> {
    return convert()
}

// convert a map to a data class
internal inline fun <reified T> Map<String, Any?>.toDataClass(): T {
    return convert()
}

// convert an object of type I to type O
internal inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<O>() {}.type)
}

internal sealed class SendableEvent

internal data class AuthRequest(val type: String, val data: Data) : SendableEvent() {
    data class Data(val type: String, val token: String)
    constructor(token: String) : this("controlMessage", Data("authRequest", token))
}

internal data class SendableMediaEvent(val type: String, val data: String) : SendableEvent() {
    constructor(data: String) : this("mediaEvent", data)
}

internal enum class ReceivableEventType {
    @SerializedName("controlMessage")
    ControlMessage,

    @SerializedName("authenticated")
    Authenticated,

    @SerializedName("unauthenticated")
    Unauthenticated,

    @SerializedName("mediaEvent")
    ReceivableMediaEvent,
}

internal data class BaseReceivableEvent(val type: ReceivableEventType)

internal data class BaseControlEvent(val type: ReceivableEventType, val data: Data) : ReceivableEvent() {
    data class Data(val type: ReceivableEventType)
}

internal class CustomEvent<Event : ReceivableEvent>(val type: ReceivableEventType, val data: Event)

internal data class AuthenticatedEvent(val type: ReceivableEventType) : ReceivableEvent()
internal data class UnauthenticatedEvent(val type: ReceivableEventType) : ReceivableEvent()
internal data class ReceivableMediaEvent(val type: ReceivableEventType, val data: String) : ReceivableEvent()

internal sealed class ReceivableEvent {
    companion object {
        fun decode(event: String): ReceivableEvent? {
            try {
                val type = object : TypeToken<Map<String, Any?>>() {}.type

                val payload: Map<String, Any?> = gson.fromJson(event, type)

                val eventBase: BaseReceivableEvent = payload.toDataClass()

                return when (eventBase.type) {
                    ReceivableEventType.ControlMessage -> {
                        val controlEventBase = payload.toDataClass<BaseControlEvent>()

                        return when (controlEventBase.data.type) {
                            ReceivableEventType.Authenticated ->
                                payload.toDataClass<CustomEvent<AuthenticatedEvent>>().data
                            ReceivableEventType.Unauthenticated ->
                                payload.toDataClass<CustomEvent<UnauthenticatedEvent>>().data
                            else -> null
                        }
                    }
                    ReceivableEventType.ReceivableMediaEvent ->
                        payload.toDataClass<ReceivableMediaEvent>()
                    else -> null
                }
            } catch (e: JsonParseException) {
                return null
            }
        }
    }
}
