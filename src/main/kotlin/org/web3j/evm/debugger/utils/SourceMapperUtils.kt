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