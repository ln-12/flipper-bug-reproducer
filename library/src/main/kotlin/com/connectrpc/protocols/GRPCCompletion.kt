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
import com.connectrpc.Headers
import com.connectrpc.SerializationStrategy
import okio.ByteString

/**
 * Represents the parsed data structure from the GRPC trailers.
 */
internal data class GRPCCompletion(
    // The status code of the response.
    val code: Code,
    // The numerical status parsed from trailers.
    val status: Int?,
    // Message data.
    val message: ByteString,
    // List of error details.
    val errorDetails: List<ConnectErrorDetail>,
    // Set to either message headers (or trailers) where the gRPC status was found.
    val metadata: Headers,
) {
    /**
     * Converts a completion into a [ConnectException] if the completion failed or if a throwable is passed
     * @return a ConnectException on failure, null otherwise
     */
    fun toConnectExceptionOrNull(serializationStrategy: SerializationStrategy, cause: Throwable? = null): ConnectException? {
        if (cause is ConnectException) {
            return cause
        }

        if (cause != null || code != Code.OK) {
            return ConnectException(
                code = code,
                errorDetailParser = serializationStrategy.errorDetailParser(),
                message = message.utf8(),
                exception = cause,
                details = errorDetails,
                metadata = metadata,
            )
        }
        // Successful call.
        return null
    }
}
