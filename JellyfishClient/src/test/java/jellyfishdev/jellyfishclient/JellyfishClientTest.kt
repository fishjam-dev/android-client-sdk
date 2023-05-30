import android.content.Context
import com.jellyfishdev.jellyfishclient.Config
import com.jellyfishdev.jellyfishclient.JellyfishClient
import com.jellyfishdev.jellyfishclient.JellyfishClientListener
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import jellyfish.PeerNotifications.PeerMessage
import jellyfish.PeerNotifications.PeerMessage.MediaEvent
import jellyfishdev.jellyfishclient.WebsocketMock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.membraneframework.rtc.MembraneRTC
import org.membraneframework.rtc.MembraneRTCListener

class JellyfishClientTest {
    private val context = mockk<Context>()
    private val webrtcListener = slot<MembraneRTCListener>()
    private val webrtcClient = mockk<MembraneRTC>(relaxed = true)

    private lateinit var websocketMock: WebsocketMock
    private lateinit var jellyfishClientListener: JellyfishClientListener
    private lateinit var client: JellyfishClient

    private val url = "ws://localhost:4000/socket/peer/websocket"
    private val token = "auth"
    private val authRequest = PeerMessage
        .newBuilder()
        .setAuthRequest(PeerMessage.AuthRequest.newBuilder().setToken(token))
        .build()

    private val authenticated = PeerMessage
        .newBuilder()
        .setAuthenticated(PeerMessage.Authenticated.newBuilder())
        .build()

    init {
        mockkObject(MembraneRTC)
        every { MembraneRTC.create(context, capture(webrtcListener), any()) } returns webrtcClient
    }

    @Before fun initMocksAndConnect() {
        websocketMock = WebsocketMock()
        jellyfishClientListener = mockk(relaxed = true)
        client = JellyfishClient(context, jellyfishClientListener)

        client.connect(Config(websocketUrl = url, token = token))
        websocketMock.open()
        verify { jellyfishClientListener.onSocketOpen() }
        websocketMock.expect(authRequest)
    }

    @Test fun authenticates() {
        websocketMock.sendToClient(authenticated)
        verify { jellyfishClientListener.onAuthSuccess() }
    }

    @Test fun cleansUp() {
        client.cleanUp()
        verify { webrtcClient.disconnect() }
        websocketMock.expectClosed()
        verify { jellyfishClientListener.onDisconnected() }
    }

    @Test fun receivesAndSendsMediaEvents() {
        val sdpOfferMediaEvent = PeerMessage
            .newBuilder()
            .setMediaEvent(MediaEvent.newBuilder().setData("sdpOffer"))
            .build()

        val joinMediaEvent = PeerMessage
            .newBuilder()
            .setMediaEvent(MediaEvent.newBuilder().setData("join"))
            .build()

        websocketMock.sendToClient(authenticated)

        verify { jellyfishClientListener.onAuthSuccess() }

        webrtcListener.captured.onSendMediaEvent("join")

        websocketMock.expect(joinMediaEvent)

        websocketMock.sendToClient(sdpOfferMediaEvent)

        verify { webrtcClient.receiveMediaEvent("sdpOffer") }
    }

    @Test fun callsOnSocketError() {
        websocketMock.error()
        verify { jellyfishClientListener.onSocketError(any(), any()) }
    }

    @Test fun callsOnSocketClosed() {
        websocketMock.close()
        verify { jellyfishClientListener.onSocketClose(any(), any()) }
    }

    @After fun confirmVerified() {
        confirmVerified(jellyfishClientListener)
        confirmVerified(webrtcClient)
        websocketMock.confirmVerified()
    }
}
