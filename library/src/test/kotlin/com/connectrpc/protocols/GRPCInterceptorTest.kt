// Copyright 2022-2023 The Connect Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.connectrpc.protocols

import com.connectrpc.Code
import com.connectrpc.ConnectErrorDetail
import com.connectrpc.ConnectException
import com.connectrpc.ErrorDetailParser
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.RequestCompression
import com.connectrpc.SerializationStrategy
import com.connectrpc.StreamResult
import com.connectrpc.compression.GzipCompressionPool
import com.connectrpc.http.HTTPRequest
import com.connectrpc.http.HTTPResponse
import com.connectrpc.http.TracingInfo
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.internal.commonAsUtf8ToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL

class GRPCInterceptorTest {

    private val errorDetailParser: ErrorDetailParser = mock { }
    private val serializationStrategy: SerializationStrategy = mock { }
    private val moshi = Moshi.Builder().build()

    @Before
    fun setup() {
        whenever(serializationStrategy.errorDetailParser()).thenReturn(errorDetailParser)
        whenever(serializationStrategy.serializationName()).thenReturn("encoding_type")
    }

    /*
     * Unary
     */
    @Test
    fun requestHeaders() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                ),
            ),
        )
        assertThat(request.headers[ACCEPT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers[CONTENT_ENCODING]).isNullOrEmpty()
        assertThat(request.headers["key"]).containsExactly("value")
        assertThat(request.contentType).isEqualTo("application/grpc+${serializationStrategy.serializationName()}")
    }

    @Test
    fun requestHeadersCustomUserAgent() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value"), "User-Agent" to listOf("my-custom-user-agent")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                ),
            ),
        )
        // this will only work if we do a case-insensitive lookup of headers
        assertThat(request.headers[USER_AGENT]).isNull()
        assertThat(request.headers["User-Agent"]).containsExactly("my-custom-user-agent")
    }

    @Test
    fun uncompressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = emptyMap(),
                message = "message".commonAsUtf8ToByteArray(),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                ),
            ),
        )
        val (_, message) = Envelope.unpackWithHeaderByte(Buffer().write(request.message!!))
        assertThat(message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcInterceptor.unaryFunction()

        val request = unaryFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = emptyMap(),
                message = "message".commonAsUtf8ToByteArray(),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                ),
            ),
        )
        val (_, message) = Envelope.unpackWithHeaderByte(Buffer().write(request.message!!))
        val decompressed = GzipCompressionPool.decompress(message)
        assertThat(decompressed.readUtf8()).isEqualTo("message")
    }

    @Test
    fun uncompressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),

        )
        val grpcInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcInterceptor.unaryFunction()

        // GRPC has messages coming in as enveloped.
        val message = Buffer().write("message".encodeUtf8())
        val envelopedMessage = Envelope.pack(message)
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                headers = emptyMap(),
                message = envelopedMessage,
                trailers = mapOf(
                    GRPC_STATUS_TRAILER to listOf("${Code.OK.value}"),
                ),
                tracingInfo = null,
            ),
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun compressedResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcInterceptor.unaryFunction()

        val envelopedMessage = Envelope.pack(Buffer().write("message".encodeUtf8()), GzipCompressionPool, 1)
        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                headers = mapOf(GRPC_ENCODING to listOf(GzipCompressionPool.name())),
                message = envelopedMessage,
                trailers = mapOf(
                    GRPC_STATUS_TRAILER to listOf("${Code.OK.value}"),
                ),
                tracingInfo = null,
            ),
        )
        assertThat(response.message.readUtf8()).isEqualTo("message")
    }

    @Test
    fun responseError() {
        val statusDetails = "some_string".encodeUtf8().base64()
        whenever(errorDetailParser.parseDetails(any())).thenReturn(
            listOf(
                ConnectErrorDetail(
                    "type",
                    "value".encodeUtf8(),
                ),
            ),
        )
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcInterceptor.unaryFunction()
        val error = ErrorPayloadJSON(
            "resource_exhausted",
            "no more resources!",
            listOf(
                ErrorDetailPayloadJSON(
                    "type",
                    "value",
                ),
            ),
        )
        val adapter = moshi.adapter(ErrorPayloadJSON::class.java)
        val json = adapter.toJson(error)

        val response = unaryFunction.responseFunction(
            HTTPResponse(
                code = Code.OK,
                message = Buffer().write(json.encodeUtf8()),
                headers = emptyMap(),
                trailers = mapOf(
                    GRPC_STATUS_TRAILER to listOf("${Code.RESOURCE_EXHAUSTED.value}"),
                    GRPC_MESSAGE_TRAILER to listOf("no more resources!"),
                    GRPC_STATUS_DETAILS_TRAILERS to listOf(statusDetails),
                ),
                tracingInfo = null,
            ),
        )
        assertThat(response.cause!!.code).isEqualTo(Code.RESOURCE_EXHAUSTED)
        assertThat(response.cause!!.message).isEqualTo("no more resources!")
        val connectErrorDetail = response.cause!!.details.singleOrNull()!!
        assertThat(connectErrorDetail.type).isEqualTo("type")
        assertThat(connectErrorDetail.payload).isEqualTo("value".encodeUtf8())
    }

    @Test
    fun tracingInfoForwardedOnUnaryResponse() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcWebInterceptor = GRPCInterceptor(config)
        val unaryFunction = grpcWebInterceptor.unaryFunction()

        val result = unaryFunction.responseFunction(
            HTTPResponse(
                Code.UNKNOWN,
                emptyMap(),
                Buffer(),
                emptyMap(),
                TracingInfo(888),
            ),
        )
        assertThat(result.tracingInfo!!.httpStatus).isEqualTo(888)
    }

    /*
     * Streaming
     */
    @Test
    fun streamingRequestHeadersWithCompression() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1000, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "",
                headers = mapOf(
                    // Doesn't get passed as headers.
                    GRPC_ENCODING to listOf("gzip"),
                ),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                ),
            ),
        )
        assertThat(request.contentType).isEqualTo("application/grpc+${serializationStrategy.serializationName()}")
        assertThat(request.headers[USER_AGENT]).containsExactly("grpc-kotlin-connect/dev")
        assertThat(request.headers[GRPC_ENCODING]).containsExactly(GzipCompressionPool.name())
        assertThat(request.headers[GRPC_TE_HEADER]).containsExactly("trailers")
    }

    @Test
    fun streamingRequestHeadersCustomUserAgent() {
        val config = ProtocolClientConfig(
            host = "https://buf.build",
            serializationStrategy = serializationStrategy,
            requestCompression = RequestCompression(1000, GzipCompressionPool),
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "",
                headers = mapOf("User-Agent" to listOf("custom-user-agent")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                ),
            ),
        )
        // this will only work if we do a case-insensitive lookup of headers
        assertThat(request.headers[USER_AGENT]).isNull()
        assertThat(request.headers["User-Agent"]).containsExactly("custom-user-agent")
    }

    @Test
    fun streamingRequestHeadersWithoutAnyCompression() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val request = streamFunction.requestFunction(
            HTTPRequest(
                url = URL(config.host),
                contentType = "content_type",
                headers = mapOf("key" to listOf("value")),
                methodSpec = MethodSpec(
                    path = "",
                    requestClass = Any::class,
                    responseClass = Any::class,
                ),
            ),
        )
        assertThat(request.contentType).isEqualTo("application/grpc+${serializationStrategy.serializationName()}")
        assertThat(request.headers[USER_AGENT]).containsExactly("grpc-kotlin-connect/dev")
        assertThat(request.headers[GRPC_TE_HEADER]).containsExactly("trailers")
    }

    @Test
    fun uncompressedStreamingRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val envelopedMessage = streamFunction.requestBodyFunction(Buffer().write("hello".encodeUtf8()))
        val (_, requestMessage) = Envelope.unpackWithHeaderByte(envelopedMessage)
        assertThat(requestMessage.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun compressedStreamingRequestMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val envelopedMessage = streamFunction.requestBodyFunction(Buffer().write("hello".encodeUtf8()))
        val (_, requestMessage) = Envelope.unpackWithHeaderByte(envelopedMessage, GzipCompressionPool)
        assertThat(requestMessage.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun streamingResponseHeaders() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Headers(
                mapOf(
                    "key" to listOf("value"),
                ),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Headers::class.java)
        val headerResult = result as StreamResult.Headers
        assertThat(headerResult.headers["key"]).containsExactly("value")
    }

    @Test
    fun uncompressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()
        // Send headers for no compression.
        streamFunction.streamResultFunction(StreamResult.Headers(emptyMap()))

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()))
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(envelopedMessage.readByteString()),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun compressedStreamingResponseMessage() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
            compressionPools = listOf(GzipCompressionPool),
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()
        // Send headers for gzip compression.
        streamFunction.streamResultFunction(
            StreamResult.Headers(
                headers = mapOf(
                    GRPC_ENCODING to listOf(GzipCompressionPool.name()),
                ),
            ),
        )

        val envelopedMessage = Envelope.pack(Buffer().write("hello".encodeUtf8()), GzipCompressionPool, 1)
        val result = streamFunction.streamResultFunction(
            StreamResult.Message(
                Buffer().write(envelopedMessage.readByteString()),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Message::class.java)
        val streamMessage = result as StreamResult.Message
        assertThat(streamMessage.message.readUtf8()).isEqualTo("hello")
    }

    @Test
    fun endStreamOnTrailers() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                code = Code.OK,
                trailers = mapOf(
                    GRPC_STATUS_TRAILER to listOf("${Code.OK.value}"),
                    "key" to listOf("value"),
                ),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.code).isEqualTo(Code.OK)
        assertThat(completion.trailers["key"]).containsExactly("value")
    }

    @Test
    fun endStreamForwardsErrors() {
        val config = ProtocolClientConfig(
            host = "https://connectrpc.com",
            serializationStrategy = serializationStrategy,
        )
        val grpcInterceptor = GRPCInterceptor(config)
        val streamFunction = grpcInterceptor.streamFunction()

        val result = streamFunction.streamResultFunction(
            StreamResult.Complete(
                code = Code.UNKNOWN,
                cause = ConnectException(
                    Code.UNKNOWN,
                    message = "error_message",
                ),
            ),
        )

        assertThat(result).isOfAnyClassIn(StreamResult.Complete::class.java)
        val completion = result as StreamResult.Complete
        assertThat(completion.code).isEqualTo(Code.UNKNOWN)
    }
}
