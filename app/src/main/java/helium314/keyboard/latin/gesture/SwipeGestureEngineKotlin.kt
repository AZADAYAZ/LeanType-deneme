// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.gesture

import android.content.Context
import android.util.Log
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.utils.SuggestionResults

import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// ─── Constants ──────────────────────────────────────────────────────────────

private const val N_PTS = 16
private const val DTW_BAND = 3
private const val FREQ_WEIGHT = 0.05f
private const val USER_BOOST_MAX = 50
private const val PROXIMITY_RADIUS = 0.035f

// ─── Main class ─────────────────────────────────────────────────────────────

class SwipeGestureEngineKotlin {

    // ─── Nested classes ─────────────────────────────────────────────────────

    class IndexEntry(
        val word: String,
        path: FloatArray,
        val frequency: Int
    ) {
        val freqBonus: Float = if (frequency > 0) (kotlin.math.ln(frequency + 1.0) * FREQ_WEIGHT).toFloat() else 0f
        var pathLen: Float = 0f
        private set

        private var path0: Long = 0
            private var path1: Long = 0
                private var path2: Long = 0
                    private var path3: Long = 0

                        init {
                            updatePath(path)
                        }

                        fun updatePath(newPath: FloatArray) {
                            path0 = pack8Bytes(newPath, 0)
                            path1 = pack8Bytes(newPath, 8)
                            path2 = pack8Bytes(newPath, 16)
                            path3 = pack8Bytes(newPath, 24)
                            pathLen = pathLength(newPath)
                        }

                        fun unpackPath(out: FloatArray) {
                            unpack8Bytes(path0, out, 0)
                            unpack8Bytes(path1, out, 8)
                            unpack8Bytes(path2, out, 16)
                            unpack8Bytes(path3, out, 24)
                        }
    }

    class GestureIndex(
        val byFirst: Map<Char, List<IndexEntry>>,
        val charToPos: Array<FloatArray>
    )

    // ─── Companion object (Static Methods & Helpers) ─────────────────────────

    companion object {

        private val sUserBoost = ConcurrentHashMap<String, Int>()
        private val sUserPaths = ConcurrentHashMap<String, FloatArray>()
        private val sUserBoostCache = FloatArray(USER_BOOST_MAX + 1).apply {
            for (i in indices) {
                this[i] = (kotlin.math.ln(i + 1.0) * 0.08f).toFloat()
            }
        }
        private var sUserDataFile: File? = null

            private fun loadUserData() {
                if (sUserDataFile == null || !sUserDataFile!!.exists()) return
                    synchronized(this) {
                        try {
                            DataInputStream(BufferedInputStream(FileInputStream(sUserDataFile))).use { inp ->
                                val version = inp.readInt()
                                if (version != 1) return
                                    sUserBoost.clear()
                                    val numBoosts = inp.readInt()
                                    for (i in 0 until numBoosts) {
                                        val key = inp.readUTF()
                                        val count = inp.readInt()
                                        sUserBoost[key] = count
                                    }
                                    sUserPaths.clear()
                                    val numPaths = inp.readInt()
                                    for (i in 0 until numPaths) {
                                        val key = inp.readUTF()
                                        val path = FloatArray(N_PTS * 2)
                                        for (j in path.indices) {
                                            path[j] = inp.readFloat()
                                        }
                                        sUserPaths[key] = path
                                    }
                            }
                        } catch (e: IOException) {
                            Log.e("GestureEngineKotlin", "Error loading user data", e)
                        }
                    }
            }

            private fun saveUserData() {
                if (sUserDataFile == null) return
                    try {
                        DataOutputStream(BufferedOutputStream(FileOutputStream(sUserDataFile))).use { out ->
                            out.writeInt(1) // version
                            out.writeInt(sUserBoost.size)
                            for ((key, count) in sUserBoost) {
                                out.writeUTF(key)
                                out.writeInt(count)
                            }
                            out.writeInt(sUserPaths.size)
                            for ((key, path) in sUserPaths) {
                                out.writeUTF(key)
                                for (v in path) out.writeFloat(v)
                            }
                        }
                    } catch (e: IOException) {
                        Log.e("GestureEngineKotlin", "Error saving user data", e)
                    }
            }

            private fun saveUserDataAsync() {
                Thread {
                    synchronized(this) {
                        saveUserData()
                    }
                }.start()
            }

            // ─── Public static API ────────────────────────────────────────────────

            @JvmStatic
            fun initialize(context: Context) {
                if (sUserDataFile != null) return
                    sUserDataFile = File(context.filesDir, "gesture_user_data_kotlin.bin")
                    loadUserData()
                    Log.i("GestureEngineKotlin", "initialize: loaded ${sUserBoost.size} boosted words")
            }

            @JvmStatic
            fun buildIndex(
                facilitator: helium314.keyboard.latin.DictionaryFacilitator,
                keyboard: Keyboard
            ): GestureIndex {
                val charToPos = buildCharToPos(keyboard)
                val byFirst = mutableMapOf<Char, MutableList<IndexEntry>>()

                facilitator.forEachMainDictionaryWord { raw, freqVal ->
                    if (raw == null) return@forEachMainDictionaryWord
                        if (facilitator.isBlacklisted(raw)) return@forEachMainDictionaryWord
                            var freq = freqVal ?: 0
                            val lower = raw.lowercase(Locale.ROOT)
                            val boost = sUserBoost[lower]
                            if (boost != null) freq = (freq + boost * 5).coerceAtMost(255)
                                if (freq < 3) return@forEachMainDictionaryWord
                                    if (lower.isEmpty()) return@forEachMainDictionaryWord
                                        val first = lower[0]
                                        if (first !in 'a'..'z') return@forEachMainDictionaryWord

                                            val userPath = sUserPaths[lower]
                                            val path = if (userPath != null && userPath.size == N_PTS * 2) {
                                                val blended = FloatArray(N_PTS * 2)
                                                val dictPath = wordPath(lower, charToPos)
                                                for (i in blended.indices) {
                                                    blended[i] = dictPath[i] * 0.3f + userPath[i] * 0.7f
                                                }
                                                blended
                                            } else {
                                                wordPath(lower, charToPos)
                                            }

                                            byFirst.getOrPut(first) { mutableListOf() }
                                            .add(IndexEntry(raw, path, freq))
                }

                for (list in byFirst.values) {
                    list.sortByDescending { it.frequency }
                }

                return GestureIndex(byFirst, charToPos)
            }

            @JvmStatic
            fun layoutFingerprint(keyboard: Keyboard): Int =
            Arrays.deepHashCode(buildCharToPos(keyboard))

            @JvmStatic
            fun rankByIndex(
                index: GestureIndex?,
                pointers: InputPointers,
                keyboard: Keyboard,
                maxResults: Int,
                predictionSet: Set<String>?
            ): SuggestionResults {
                val empty = SuggestionResults(1, false, false)
                if (index == null) return empty

                    val n = pointers.pointerSize
                    if (n < 2) return empty

                        val xs = pointers.xCoordinates
                        val ys = pointers.yCoordinates
                        val kw = keyboard.mOccupiedWidth.toFloat()
                        val kh = keyboard.mOccupiedHeight.toFloat()

                        val rawFlat = FloatArray(n * 2)
                        for (i in 0 until n) {
                            rawFlat[2 * i] = xs[i] / kw
                            rawFlat[2 * i + 1] = ys[i] / kh
                        }
                        val inputVec = resampleFlat(rawFlat, n, N_PTS)
                        val inputLen = pathLength(inputVec)

                        val charToPos = index.charToPos
                        val startLetters = nearestLettersFromMap(rawFlat[0], rawFlat[1], charToPos)
                        val endLetters = nearestLettersFromMap(rawFlat[2 * (n - 1)], rawFlat[2 * (n - 1) + 1], charToPos)

                        val candidates = mutableListOf<IndexEntry>()
                        for (first in startLetters) {
                            index.byFirst[first]?.let { candidates.addAll(it) }
                        }
                        if (candidates.isEmpty()) return empty

                            val filtered = mutableListOf<IndexEntry>()
                            for (e in candidates) {
                                val lower = e.word.lowercase(Locale.ROOT)
                                if (lower.isNotEmpty() && endLetters.contains(lower.last())) {
                                    filtered.add(e)
                                }
                            }
                            if (filtered.isEmpty()) filtered.addAll(candidates)

                                val nodeSignature = extractNodeSignature(rawFlat, n, charToPos)
                                if (nodeSignature.isNotEmpty()) {
                                    val lcsFiltered = lcsFilter(filtered, nodeSignature)
                                    if (lcsFiltered.isNotEmpty()) {
                                        filtered.clear()
                                        filtered.addAll(lcsFiltered)
                                    }
                                }

                                if (filtered.isEmpty()) return empty

                                    val m = filtered.size
                                    val scores = FloatArray(m)
                                    val order = IntArray(m)

                                    val candidatePath = FloatArray(N_PTS * 2)
                                    for (i in 0 until m) {
                                        val e = filtered[i]
                                        e.unpackPath(candidatePath)
                                        val dtwSq = dtwDistanceSq(inputVec, candidatePath)
                                        val distance = kotlin.math.sqrt(dtwSq)
                                        val lower = e.word.lowercase(Locale.ROOT)
                                        val userBoostVal = sUserBoost[lower]?.let { sUserBoostCache[it] } ?: 0f
                                        val predBonus = if (predictionSet != null && predictionSet.contains(lower)) 0.15f else 0f
                                        val lenPenalty = -kotlin.math.abs(inputLen - e.pathLen) * 0.4f

                                        val seqMatch = isSequenceMatch(lower, inputVec, charToPos)
                                        val seqPenalty = if (seqMatch) 0f else -0.4f

                                        val score = -distance + e.freqBonus + userBoostVal + predBonus + lenPenalty + seqPenalty
                                        scores[i] = score
                                        order[i] = i
                                    }

                                    for (i in 1 until m) {
                                        val key = order[i]
                                        val ks = scores[key]
                                        var j = i - 1
                                        while (j >= 0 && scores[order[j]] < ks) {
                                            order[j + 1] = order[j]
                                            j--
                                        }
                                        order[j + 1] = key
                                    }

                                    val take = minOf(maxResults, m)
                                    val result = SuggestionResults(take, false, false)
                                    val baseScore = 1_000_000
                                    for (rank in 0 until take) {
                                        val e = filtered[order[rank]]
                                        result.add(SuggestedWordInfo(
                                            e.word, "",
                                            baseScore - rank * 1000,
                                            SuggestedWordInfo.KIND_CORRECTION,
                                            Dictionary.DICTIONARY_USER_TYPED,
                                            SuggestedWordInfo.NOT_AN_INDEX,
                                            SuggestedWordInfo.NOT_A_CONFIDENCE
                                        ))
                                    }
                                    return result
            }

            @JvmStatic
            fun recordAccepted(
                word: String,
                pointers: InputPointers?,
                keyboard: Keyboard?,
                activeIndex: GestureIndex?
            ) {
                if (word.isEmpty()) return
                    val lower = word.lowercase(Locale.ROOT)

                    sUserBoost.merge(lower, 1) { a, b -> (a + b).coerceAtMost(USER_BOOST_MAX) }

                    if (pointers != null && pointers.pointerSize >= 2 && keyboard != null) {
                        val n = pointers.pointerSize
                        val xs = pointers.xCoordinates
                        val ys = pointers.yCoordinates
                        val kw = keyboard.mOccupiedWidth.toFloat()
                        val kh = keyboard.mOccupiedHeight.toFloat()
                        val rawFlat = FloatArray(n * 2)
                        for (i in 0 until n) {
                            rawFlat[2 * i] = xs[i] / kw
                            rawFlat[2 * i + 1] = ys[i] / kh
                        }
                        val path = resampleFlat(rawFlat, n, N_PTS)
                        sUserPaths[lower] = path

                        if (activeIndex != null && lower.isNotEmpty()) {
                            val first = lower[0]
                            val list = activeIndex.byFirst[first]
                            if (list != null) {
                                for (entry in list) {
                                    if (entry.word.lowercase(Locale.ROOT) == lower) {
                                        val blended = FloatArray(N_PTS * 2)
                                        val current = FloatArray(N_PTS * 2)
                                        entry.unpackPath(current)
                                        for (i in blended.indices) {
                                            blended[i] = current[i] * 0.3f + path[i] * 0.7f
                                        }
                                        entry.updatePath(blended)
                                        break
                                    }
                                }
                            }
                        }
                    }

                    saveUserDataAsync()
            }

            @JvmStatic
            fun nearestLetters(x: Int, y: Int, keyboard: Keyboard): List<Char> {
                val kw = keyboard.mOccupiedWidth.toFloat()
                val kh = keyboard.mOccupiedHeight.toFloat()
                return nearestLettersFromMap(x / kw, y / kh, buildCharToPos(keyboard))
            }

            @JvmStatic
            fun hasLoopAtEnd(pointers: InputPointers, keyboard: Keyboard): Boolean {
                val n = pointers.pointerSize
                if (n < 6) return false
                    val xs = pointers.xCoordinates
                    val ys = pointers.yCoordinates

                    val pointsToCheck = (n / 2).coerceAtMost(10).coerceAtLeast(4)
                    val startIdx = n - pointsToCheck

                    var pathLen = 0f
                    for (i in startIdx until n - 1) {
                        val dx = (xs[i + 1] - xs[i]).toFloat()
                        val dy = (ys[i + 1] - ys[i]).toFloat()
                        pathLen += kotlin.math.sqrt(dx * dx + dy * dy)
                    }

                    val startEndX = (xs[n - 1] - xs[startIdx]).toFloat()
                    val startEndY = (ys[n - 1] - ys[startIdx]).toFloat()
                    val displacement = kotlin.math.sqrt(startEndX * startEndX + startEndY * startEndY)

                    val kw = keyboard.mOccupiedWidth.toFloat()
                    if (pathLen < kw * 0.02f) return false

                        return pathLen > 2.0f * displacement
            }

            @JvmStatic
            fun isSequenceMatch(word: String, path: FloatArray, charToPos: Array<FloatArray>): Boolean {
                val n = path.size / 2
                var segmentIdx = 0
                var prevT = -0.01f
                var lastChar = 0.toChar()
                val outT = FloatArray(1)
                for (i in word.indices) {
                    val c = word[i]
                    if (c !in 'a'..'z') continue
                        if (c == lastChar) continue
                            val target = charToPos[c - 'a']
                            if (target[0] == 0f && target[1] == 0f) continue
                                var found = false
                                while (segmentIdx < n - 1) {
                                    val distSq = sqDistanceToSegment(
                                        target[0], target[1],
                                        path[2 * segmentIdx], path[2 * segmentIdx + 1],
                                        path[2 * (segmentIdx + 1)], path[2 * (segmentIdx + 1) + 1],
                                                                     outT
                                    )
                                    if (distSq <= 0.05f) {
                                        val t = outT[0]
                                        if (t > prevT) {
                                            prevT = t
                                            found = true
                                            break
                                        }
                                    }
                                    segmentIdx++
                                    prevT = -0.01f
                                }
                                if (!found) return false
                                    lastChar = c
                }
                return true
            }

            // ─── Stubs ─────────────────────────────────────────────────────────────

            @JvmStatic fun reloadSettings() {}
            @JvmStatic fun syncUserHistory(facilitator: helium314.keyboard.latin.DictionaryFacilitator) {}
            @JvmStatic fun onWordTapped(word: String, facilitator: helium314.keyboard.latin.DictionaryFacilitator) {}
            @JvmStatic fun setPredictionContext(prevWord1: String?, prevWord2: String?) {}
            @JvmStatic fun markLastGestureBackspaced() {}
            @JvmStatic fun getGestureCandidates(word: String): List<String> = emptyList()
            @JvmStatic fun onSuggestionPicked(originalWord: String, newWord: String) {}
            @JvmStatic fun retryWithFallback(maxResults: Int, predictionSet: Set<String>?): SuggestionResults =
            SuggestionResults(1, false, false)

            // ─── Private helper functions (Moved inside Companion Object) ───────

            private fun pathLength(path: FloatArray): Float {
                var len = 0f
                val n = path.size / 2
                for (i in 0 until n - 1) {
                    val dx = path[2 * (i + 1)] - path[2 * i]
                    val dy = path[2 * (i + 1) + 1] - path[2 * i + 1]
                    len += kotlin.math.sqrt(dx * dx + dy * dy)
                }
                return len
            }

            private fun resampleFlat(pts: FloatArray, numPts: Int, n: Int): FloatArray {
                if (numPts == 0) return FloatArray(n * 2)
                    if (numPts == 1) {
                        val r = FloatArray(n * 2)
                        val x = pts[0]
                        val y = pts[1]
                        for (i in 0 until n) {
                            r[2 * i] = x
                            r[2 * i + 1] = y
                        }
                        return r
                    }
                    val cum = FloatArray(numPts)
                    for (i in 1 until numPts) {
                        val dx = pts[2 * i] - pts[2 * (i - 1)]
                        val dy = pts[2 * i + 1] - pts[2 * (i - 1) + 1]
                        cum[i] = cum[i - 1] + kotlin.math.sqrt(dx * dx + dy * dy)
                    }
                    val total = cum[numPts - 1]
                    if (total < 1e-9f) {
                        val r = FloatArray(n * 2)
                        val x = pts[0]
                        val y = pts[1]
                        for (i in 0 until n) {
                            r[2 * i] = x
                            r[2 * i + 1] = y
                        }
                        return r
                    }
                    val result = FloatArray(n * 2)
                    var seg = 0
                    for (i in 0 until n) {
                        val t = total * i / (n - 1)
                        while (seg < numPts - 2 && cum[seg + 1] < t) seg++
                            val segLen = cum[seg + 1] - cum[seg]
                            val alpha = if (segLen > 1e-9f) (t - cum[seg]) / segLen else 0f
                            result[2 * i] = pts[2 * seg] + alpha * (pts[2 * (seg + 1)] - pts[2 * seg])
                            result[2 * i + 1] = pts[2 * seg + 1] + alpha * (pts[2 * (seg + 1) + 1] - pts[2 * seg + 1])
                    }
                    return result
            }

            private fun wordPath(word: String, charToPos: Array<FloatArray>): FloatArray {
                val pts = mutableListOf<FloatArray>()
                var lastX = -1f
                var lastY = -1f
                for (c in word) {
                    val idx = c - 'a'
                    if (idx !in 0..25) continue
                        val p = charToPos[idx]
                        if (pts.isEmpty() || p[0] != lastX || p[1] != lastY) {
                            pts.add(floatArrayOf(p[0], p[1]))
                            lastX = p[0]
                            lastY = p[1]
                        }
                }
                if (pts.size < 2) {
                    val r = FloatArray(N_PTS * 2)
                    val x = pts[0][0]
                    val y = pts[0][1]
                    for (i in 0 until N_PTS) {
                        r[2 * i] = x
                        r[2 * i + 1] = y
                    }
                    return r
                }
                val flat = FloatArray(pts.size * 2)
                for (i in pts.indices) {
                    flat[2 * i] = pts[i][0]
                    flat[2 * i + 1] = pts[i][1]
                }
                return resampleFlat(flat, pts.size, N_PTS)
            }

            private fun dtwDistanceSq(path1: FloatArray, path2: FloatArray): Float {
                val len = N_PTS
                val cost = FloatArray(len * len)
                val infinity = Float.MAX_VALUE

                for (i in 0 until len) {
                    val jMin = (i - DTW_BAND).coerceAtLeast(0)
                    val jMax = (i + DTW_BAND).coerceAtMost(len - 1)
                    for (j in jMin..jMax) {
                        val dx = path1[2 * i] - path2[2 * j]
                        val dy = path1[2 * i + 1] - path2[2 * j + 1]
                        val dist = dx * dx + dy * dy
                        val minPrev = when {
                            i == 0 && j == 0 -> 0f
                            else -> {
                                var m = infinity
                                if (i > 0 && j >= i - DTW_BAND) m = minOf(m, cost[(i - 1) * len + j])
                                    if (j > 0 && i >= j - DTW_BAND) m = minOf(m, cost[i * len + (j - 1)])
                                        if (i > 0 && j > 0) m = minOf(m, cost[(i - 1) * len + (j - 1)])
                                            m
                            }
                        }
                        cost[i * len + j] = dist + minPrev
                    }
                }
                return cost[(len - 1) * len + (len - 1)]
            }

            private fun pack8Bytes(pts: FloatArray, startIndex: Int): Long {
                var value = 0L
                for (i in 0 until 8) {
                    var f = pts[startIndex + i]
                    if (f < 0f) f = 0f
                        else if (f > 1f) f = 1f
                            val b = (f * 255f).toInt() and 0xFF
                            value = value or (b.toLong() shl (i * 8))
                }
                return value
            }

            private fun unpack8Bytes(value: Long, out: FloatArray, startIndex: Int) {
                for (i in 0 until 8) {
                    val b = (value ushr (i * 8)).toInt() and 0xFF
                    out[startIndex + i] = b / 255f
                }
            }

            private fun isAsciiLetter(code: Int): Boolean =
                code in 'a'.code..'z'.code || code in 'A'.code..'Z'.code

                private fun buildCharToPos(keyboard: Keyboard): Array<FloatArray> {
                    val map = Array(26) { FloatArray(2) }
                    val kw = keyboard.mOccupiedWidth.toFloat()
                    val kh = keyboard.mOccupiedHeight.toFloat()
                    for (key in keyboard.sortedKeys) {
                        val code = key.code
                        if (!isAsciiLetter(code)) continue
                            val idx = code.toChar().lowercaseChar() - 'a'
                            val hitBox = key.hitBox
                            map[idx][0] = hitBox.exactCenterX() / kw
                            map[idx][1] = hitBox.exactCenterY() / kh
                    }
                    return map
                }

                private fun nearestLettersFromMap(nx: Float, ny: Float, charToPos: Array<FloatArray>): List<Char> {
                    var minDist = Float.MAX_VALUE
                    val dists = FloatArray(26)
                    for (i in 0..25) {
                        val cx = charToPos[i][0]
                        val cy = charToPos[i][1]
                        if (cx == 0f && cy == 0f) {
                            dists[i] = Float.MAX_VALUE
                            continue
                        }
                        val d = (nx - cx) * (nx - cx) + (ny - cy) * (ny - cy)
                        dists[i] = d
                        if (d < minDist) minDist = d
                    }
                    val results = mutableListOf<Char>()
                    val threshold = minDist + PROXIMITY_RADIUS
                     for (i in 0..25) {
                        if (dists[i] <= threshold) {
                            results.add(('a'.code + i).toChar())
                        }
                    }
                    return results
                }

                private fun lcsFilter(candidates: List<IndexEntry>, nodeSignature: String): List<IndexEntry> {
                    if (nodeSignature.isEmpty()) return candidates
                        val filtered = mutableListOf<IndexEntry>()
                        val minMatch = maxOf(1, (0.75 * nodeSignature.length).toInt())
                        for (e in candidates) {
                            val wordSeq = canonicalNodeSeq(e.word)
                            if (longestCommonSubsequence(nodeSignature, wordSeq) >= minMatch) {
                                filtered.add(e)
                            }
                        }
                        return if (filtered.isNotEmpty()) filtered else candidates
                }

                private fun longestCommonSubsequence(a: String, b: String): Int {
                    val m = a.length
                    val n = b.length
                    val dp = IntArray(n + 1)
                    for (i in 1..m) {
                        var prev = 0
                        for (j in 1..n) {
                            val temp = dp[j]
                            if (a[i - 1] == b[j - 1]) {
                                dp[j] = prev + 1
                            } else {
                                dp[j] = dp[j].coerceAtLeast(dp[j - 1])
                            }
                            prev = temp
                        }
                    }
                    return dp[n]
                }

                private fun canonicalNodeSeq(word: String): String {
                    val sb = StringBuilder()
                    var prev = 0.toChar()
                    for (c in word) {
                        if (c in 'a'..'z' && c != prev) {
                            sb.append(c)
                            prev = c
                        }
                    }
                    return sb.toString()
                }

                private fun extractNodeSignature(rawFlat: FloatArray, numPoints: Int, charToPos: Array<FloatArray>): String {
                    if (numPoints < 2) return ""
                        val sb = StringBuilder()
                        val vecX = FloatArray(numPoints - 1)
                        val vecY = FloatArray(numPoints - 1)
                        for (i in 0 until numPoints - 1) {
                            vecX[i] = rawFlat[2 * (i + 1)] - rawFlat[2 * i]
                            vecY[i] = rawFlat[2 * (i + 1) + 1] - rawFlat[2 * i + 1]
                        }

                        var lastKey = 0.toChar()
                        val startKey = getNearestKey(rawFlat[0], rawFlat[1], charToPos)
                        if (startKey in 'a'..'z') {
                            sb.append(startKey)
                            lastKey = startKey
                        }

                        for (i in 1 until numPoints - 2) {
                            val v1x = vecX[i - 1]
                            val v1y = vecY[i - 1]
                            val v2x = vecX[i]
                            val v2y = vecY[i]

                            val len1Sq = v1x * v1x + v1y * v1y
                            val len2Sq = v2x * v2x + v2y * v2y
                            if (len1Sq == 0f || len2Sq == 0f) continue

                                val dot = v1x * v2x + v1y * v2y
                                val cross = v1x * v2y - v1y * v2x
                                val angle = kotlin.math.atan2(kotlin.math.abs(cross), dot) * 180.0 / kotlin.math.PI
                                if (angle >= 45f) {
                                    val key = getNearestKey(rawFlat[2 * i], rawFlat[2 * i + 1], charToPos)
                                    if (key in 'a'..'z' && key != lastKey) {
                                        sb.append(key)
                                        lastKey = key
                                    }
                                }
                        }

                        val endKey = getNearestKey(rawFlat[2 * (numPoints - 1)], rawFlat[2 * (numPoints - 1) + 1], charToPos)
                        if (endKey in 'a'..'z' && endKey != lastKey) {
                            sb.append(endKey)
                        }
                        return sb.toString()
                }

                private fun getNearestKey(x: Float, y: Float, charToPos: Array<FloatArray>): Char {
                    var minDist = Float.MAX_VALUE
                    var best = 0.toChar()
                    for (i in 0..25) {
                        val cx = charToPos[i][0]
                        val cy = charToPos[i][1]
                        if (cx == 0f && cy == 0f) continue
                            val d = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                            if (d < minDist) {
                                minDist = d
                                best = ('a'.code + i).toChar()
                            }
                    }
                    return best
                }

                private fun sqDistanceToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float, outT: FloatArray): Float {
                    val dx = bx - ax
                    val dy = by - ay
                    val segmentLenSq = dx * dx + dy * dy
                    if (segmentLenSq < 1e-9f) {
                        outT[0] = 0f
                        return (px - ax) * (px - ax) + (py - ay) * (py - ay)
                    }
                    var t = ((px - ax) * dx + (py - ay) * dy) / segmentLenSq
                    if (t < 0f) t = 0f
                        else if (t > 1f) t = 1f
                            outT[0] = t
                            val closestX = ax + t * dx
                            val closestY = ay + t * dy
                            return (px - closestX) * (px - closestX) + (py - closestY) * (py - closestY)
                }
    }
}
