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

package com.connectrpc.impl

import com.connectrpc.Codec
import com.connectrpc.MethodSpec
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.SerializationStrategy
import com.connectrpc.http.HTTPClientInterface
import com.connectrpc.http.HTTPRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProtocolClientTest {
    private val serializationStrategy: SerializationStrategy = mock { }
    private val codec: Codec<String> = mock { }
    private val httpClient: HTTPClientInterface = mock { }

    @Test
    fun urlConfigurationHostWithTrailingSlashUnary() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = "https://connectrpc.com/",
                serializationStrategy = serializationStrategy,
            ),
        )
        client.unary(
            "input",
            emptyMap(),
            MethodSpec(
                path = "com.connectrpc.SomeService/Service",
                String::class,
                String::class,
            ),
        ) { _ -> }
    }

    @Test
    fun urlConfigurationHostWithoutTrailingSlashUnary() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = "https://connectrpc.com",
                serializationStrategy = serializationStrategy,
            ),
        )
        client.unary(
            "input",
            emptyMap(),
            MethodSpec(
                path = "com.connectrpc.SomeService/Service",
                String::class,
                String::class,
            ),
        ) { _ -> }
    }

    @Test
    fun urlConfigurationHostWithTrailingSlashStreaming() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = "https://connectrpc.com/",
                serializationStrategy = serializationStrategy,
            ),
        )
        CoroutineScope(Dispatchers.IO).launch {
            client.stream(
                emptyMap(),
                MethodSpec(
                    path = "com.connectrpc.SomeService/Service",
                    String::class,
                    String::class,
                ),
            )
        }
    }

    @Test
    fun urlConfigurationHostWithoutTrailingSlashStreaming() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)

        val client = ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = "https://connectrpc.com",
                serializationStrategy = serializationStrategy,
            ),
        )
        CoroutineScope(Dispatchers.IO).launch {
            client.stream(
                emptyMap(),
                MethodSpec(
                    path = "com.connectrpc.SomeService/Service",
                    String::class,
                    String::class,
                ),
            )
        }
    }

    @Test
    fun finalUrlIsValid() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = "https://connectrpc.com",
                serializationStrategy = serializationStrategy,
            ),
        )
        client.unary(
            "",
            emptyMap(),
            MethodSpec(
                path = "com.connectrpc.SomeService/Service",
                String::class,
                String::class,
            ),
        ) {}

        // Use HTTP client to determine and verify the final URL.
        val captor = argumentCaptor<HTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }

    @Test
    fun finalUrlIsValidWithHostEndingInSlash() {
        whenever(codec.encodingName()).thenReturn("testing")
        whenever(codec.serialize(any())).thenReturn(Buffer())
        whenever(serializationStrategy.codec<String>(any())).thenReturn(codec)
        val client = ProtocolClient(
            httpClient = httpClient,
            config = ProtocolClientConfig(
                host = "https://connectrpc.com/",
                serializationStrategy = serializationStrategy,
            ),
        )
        client.unary(
            "",
            emptyMap(),
            MethodSpec(
                path = "com.connectrpc.SomeService/Service",
                String::class,
                String::class,
            ),
        ) {}

        // Use HTTP client to determine and verify the final URL.
        val captor = argumentCaptor<HTTPRequest>()
        verify(httpClient).unary(captor.capture(), any())
        assertThat(captor.firstValue.url.toString()).isEqualTo("https://connectrpc.com/com.connectrpc.SomeService/Service")
    }
}
