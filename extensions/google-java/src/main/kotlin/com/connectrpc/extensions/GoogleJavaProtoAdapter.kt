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

package com.connectrpc.extensions

import com.connectrpc.CODEC_NAME_PROTO
import com.connectrpc.Codec
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Internal
import okio.Buffer
import okio.BufferedSource
import java.io.IOException
import kotlin.reflect.KClass

/**
 * Adapter to use Google's protobuf-java runtime for
 * deserializing and serializing data types.
 */
internal class GoogleJavaProtoAdapter<E : GeneratedMessageV3>(
    clazz: KClass<E>,
) : Codec<E> {
    /**
     * Casting assumes the user is using Google's GeneratedMessageV3 type.
     */
    @Suppress("UNCHECKED_CAST")
    private val instance by lazy {
        Internal.getDefaultInstance(clazz.java as Class<GeneratedMessageV3>)
    }

    override fun encodingName(): String {
        return CODEC_NAME_PROTO
    }

    /**
     * Casting assumes the user is using Google's GeneratedMessageV3 type.
     * The builder returns a GeneratedMessageV3 but it is assumed to be used
     * with the assumption that the generic E is the underlying type.
     */
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(source: BufferedSource): E {
        return instance.parserForType.parseFrom(source.inputStream()) as E
    }

    override fun serialize(message: E): Buffer {
        return Buffer().write(message.toByteArray())
    }

    override fun deterministicSerialize(message: E): Buffer {
        return try {
            val result = Buffer()
            val output = CodedOutputStream.newInstance(result.outputStream())
            output.useDeterministicSerialization()
            message.writeTo(output)
            output.checkNoSpaceLeft()
            result
        } catch (e: IOException) {
            throw RuntimeException("deterministic serialization failed", e)
        }
    }
}
