/*
 * Copyright 2021 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.evm.debugger.utils

object SourceMapperUtils {

    fun resolveOffsetAtLine(textFile: String): Map<Int, Pair<Pair<Int, Int>, String>> {
        val map = mutableMapOf<Int, Pair<Pair<Int, Int>, String>>()
        textFile.lines().forEachIndexed { index, s ->
            if (index == 0) {
                map.putIfAbsent(index, Pair(Pair(index, s.length), s))
            } else {
                val previousLineEndOffset = map[index - 1]!!.first.second
                map.putIfAbsent(index, Pair(Pair(previousLineEndOffset + 1, previousLineEndOffset + 1 + s.length), s))
            }
        }
        return map
    }


}