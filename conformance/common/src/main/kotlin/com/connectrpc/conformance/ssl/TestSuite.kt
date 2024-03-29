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

package com.connectrpc.conformance.ssl

/**
 * Test suite for the different conformance testing for
 * protoc-gen-java, protoc-gen-javalite.
 *
 * Primarily to help keep all the suites in sync.
 */
interface TestSuite {
    suspend fun test(tag: String)
    suspend fun emptyUnary()
    suspend fun largeUnary()
    suspend fun serverStreaming()
    suspend fun emptyStream()
    suspend fun customMetadata()
    suspend fun customMetadataServerStreaming()
    suspend fun statusCodeAndMessage()
    suspend fun specialStatus()
    suspend fun timeoutOnSleepingServer()
    suspend fun unimplementedMethod()
    suspend fun unimplementedServerStreamingMethod()
    suspend fun unimplementedService()
    suspend fun unimplementedServerStreamingService()
    suspend fun failUnary()
    suspend fun failServerStreaming()
    suspend fun getUnary()
}

interface UnaryCallbackTestSuite {
    suspend fun test(tag: String)
    suspend fun emptyUnary()
    suspend fun largeUnary()
    suspend fun customMetadata()
    suspend fun statusCodeAndMessage()
    suspend fun specialStatus()
    suspend fun unimplementedMethod()
    suspend fun unimplementedService()
    suspend fun failUnary()
}
