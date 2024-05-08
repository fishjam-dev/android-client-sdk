// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: jellyfish/peer_notifications.proto

package jellyfish

@kotlin.jvm.JvmName("-initializepeerMessage")
public inline fun peerMessage(block: jellyfish.PeerMessageKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage =
    jellyfish.PeerMessageKt.Dsl._create(jellyfish.PeerNotifications.PeerMessage.newBuilder()).apply { block() }._build()
public object PeerMessageKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: jellyfish.PeerNotifications.PeerMessage.Builder,
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: jellyfish.PeerNotifications.PeerMessage.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): jellyfish.PeerNotifications.PeerMessage = _builder.build()

        /**
         * <code>.jellyfish.PeerMessage.Authenticated authenticated = 1;</code>
         */
        public var authenticated: jellyfish.PeerNotifications.PeerMessage.Authenticated
            @JvmName("getAuthenticated")
            get() = _builder.getAuthenticated()

            @JvmName("setAuthenticated")
            set(value) {
                _builder.setAuthenticated(value)
            }

        /**
         * <code>.jellyfish.PeerMessage.Authenticated authenticated = 1;</code>
         */
        public fun clearAuthenticated() {
            _builder.clearAuthenticated()
        }

        /**
         * <code>.jellyfish.PeerMessage.Authenticated authenticated = 1;</code>
         * @return Whether the authenticated field is set.
         */
        public fun hasAuthenticated(): kotlin.Boolean {
            return _builder.hasAuthenticated()
        }

        /**
         * <code>.jellyfish.PeerMessage.AuthRequest auth_request = 2;</code>
         */
        public var authRequest: jellyfish.PeerNotifications.PeerMessage.AuthRequest
            @JvmName("getAuthRequest")
            get() = _builder.getAuthRequest()

            @JvmName("setAuthRequest")
            set(value) {
                _builder.setAuthRequest(value)
            }

        /**
         * <code>.jellyfish.PeerMessage.AuthRequest auth_request = 2;</code>
         */
        public fun clearAuthRequest() {
            _builder.clearAuthRequest()
        }

        /**
         * <code>.jellyfish.PeerMessage.AuthRequest auth_request = 2;</code>
         * @return Whether the authRequest field is set.
         */
        public fun hasAuthRequest(): kotlin.Boolean {
            return _builder.hasAuthRequest()
        }

        /**
         * <code>.jellyfish.PeerMessage.MediaEvent media_event = 3;</code>
         */
        public var mediaEvent: jellyfish.PeerNotifications.PeerMessage.MediaEvent
            @JvmName("getMediaEvent")
            get() = _builder.getMediaEvent()

            @JvmName("setMediaEvent")
            set(value) {
                _builder.setMediaEvent(value)
            }

        /**
         * <code>.jellyfish.PeerMessage.MediaEvent media_event = 3;</code>
         */
        public fun clearMediaEvent() {
            _builder.clearMediaEvent()
        }

        /**
         * <code>.jellyfish.PeerMessage.MediaEvent media_event = 3;</code>
         * @return Whether the mediaEvent field is set.
         */
        public fun hasMediaEvent(): kotlin.Boolean {
            return _builder.hasMediaEvent()
        }
        public val contentCase: jellyfish.PeerNotifications.PeerMessage.ContentCase
            @JvmName("getContentCase")
            get() = _builder.getContentCase()

        public fun clearContent() {
            _builder.clearContent()
        }
    }

    @kotlin.jvm.JvmName("-initializeauthenticated")
    public inline fun authenticated(block: jellyfish.PeerMessageKt.AuthenticatedKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage.Authenticated =
        jellyfish.PeerMessageKt.AuthenticatedKt.Dsl._create(jellyfish.PeerNotifications.PeerMessage.Authenticated.newBuilder()).apply { block() }._build()
    public object AuthenticatedKt {
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        @com.google.protobuf.kotlin.ProtoDslMarker
        public class Dsl private constructor(
            private val _builder: jellyfish.PeerNotifications.PeerMessage.Authenticated.Builder,
        ) {
            public companion object {
                @kotlin.jvm.JvmSynthetic
                @kotlin.PublishedApi
                internal fun _create(builder: jellyfish.PeerNotifications.PeerMessage.Authenticated.Builder): Dsl = Dsl(builder)
            }

            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _build(): jellyfish.PeerNotifications.PeerMessage.Authenticated = _builder.build()
        }
    }

    @kotlin.jvm.JvmName("-initializeauthRequest")
    public inline fun authRequest(block: jellyfish.PeerMessageKt.AuthRequestKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage.AuthRequest =
        jellyfish.PeerMessageKt.AuthRequestKt.Dsl._create(jellyfish.PeerNotifications.PeerMessage.AuthRequest.newBuilder()).apply { block() }._build()
    public object AuthRequestKt {
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        @com.google.protobuf.kotlin.ProtoDslMarker
        public class Dsl private constructor(
            private val _builder: jellyfish.PeerNotifications.PeerMessage.AuthRequest.Builder,
        ) {
            public companion object {
                @kotlin.jvm.JvmSynthetic
                @kotlin.PublishedApi
                internal fun _create(builder: jellyfish.PeerNotifications.PeerMessage.AuthRequest.Builder): Dsl = Dsl(builder)
            }

            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _build(): jellyfish.PeerNotifications.PeerMessage.AuthRequest = _builder.build()

            /**
             * <code>string token = 1;</code>
             */
            public var token: kotlin.String
                @JvmName("getToken")
                get() = _builder.getToken()

                @JvmName("setToken")
                set(value) {
                    _builder.setToken(value)
                }

            /**
             * <code>string token = 1;</code>
             */
            public fun clearToken() {
                _builder.clearToken()
            }
        }
    }

    @kotlin.jvm.JvmName("-initializemediaEvent")
    public inline fun mediaEvent(block: jellyfish.PeerMessageKt.MediaEventKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage.MediaEvent =
        jellyfish.PeerMessageKt.MediaEventKt.Dsl._create(jellyfish.PeerNotifications.PeerMessage.MediaEvent.newBuilder()).apply { block() }._build()
    public object MediaEventKt {
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        @com.google.protobuf.kotlin.ProtoDslMarker
        public class Dsl private constructor(
            private val _builder: jellyfish.PeerNotifications.PeerMessage.MediaEvent.Builder,
        ) {
            public companion object {
                @kotlin.jvm.JvmSynthetic
                @kotlin.PublishedApi
                internal fun _create(builder: jellyfish.PeerNotifications.PeerMessage.MediaEvent.Builder): Dsl = Dsl(builder)
            }

            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _build(): jellyfish.PeerNotifications.PeerMessage.MediaEvent = _builder.build()

            /**
             * <code>string data = 1;</code>
             */
            public var data: kotlin.String
                @JvmName("getData")
                get() = _builder.getData()

                @JvmName("setData")
                set(value) {
                    _builder.setData(value)
                }

            /**
             * <code>string data = 1;</code>
             */
            public fun clearData() {
                _builder.clearData()
            }
        }
    }
}

@kotlin.jvm.JvmSynthetic
public inline fun jellyfish.PeerNotifications.PeerMessage.copy(block: jellyfish.PeerMessageKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage =
    jellyfish.PeerMessageKt.Dsl._create(this.toBuilder()).apply { block() }._build()

@kotlin.jvm.JvmSynthetic
public inline fun jellyfish.PeerNotifications.PeerMessage.Authenticated.copy(block: jellyfish.PeerMessageKt.AuthenticatedKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage.Authenticated =
    jellyfish.PeerMessageKt.AuthenticatedKt.Dsl._create(this.toBuilder()).apply { block() }._build()

@kotlin.jvm.JvmSynthetic
public inline fun jellyfish.PeerNotifications.PeerMessage.AuthRequest.copy(block: jellyfish.PeerMessageKt.AuthRequestKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage.AuthRequest =
    jellyfish.PeerMessageKt.AuthRequestKt.Dsl._create(this.toBuilder()).apply { block() }._build()

@kotlin.jvm.JvmSynthetic
public inline fun jellyfish.PeerNotifications.PeerMessage.MediaEvent.copy(block: jellyfish.PeerMessageKt.MediaEventKt.Dsl.() -> kotlin.Unit): jellyfish.PeerNotifications.PeerMessage.MediaEvent =
    jellyfish.PeerMessageKt.MediaEventKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val jellyfish.PeerNotifications.PeerMessageOrBuilder.authenticatedOrNull: jellyfish.PeerNotifications.PeerMessage.Authenticated?
    get() = if (hasAuthenticated()) getAuthenticated() else null

public val jellyfish.PeerNotifications.PeerMessageOrBuilder.authRequestOrNull: jellyfish.PeerNotifications.PeerMessage.AuthRequest?
    get() = if (hasAuthRequest()) getAuthRequest() else null

public val jellyfish.PeerNotifications.PeerMessageOrBuilder.mediaEventOrNull: jellyfish.PeerNotifications.PeerMessage.MediaEvent?
    get() = if (hasMediaEvent()) getMediaEvent() else null
