package com.drivetheory.cbt.data.seed

import android.content.Context
import com.drivetheory.cbt.domain.model.Question
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class QuestionSeedLoader(private val context: Context) {
    fun loadFromAssets(path: String = "seed/questions_seed.json"): List<Question> {
        return runCatching { readJsonArray(path) }.getOrDefault(emptyList())
    }

    fun loadAllFromAssets(dir: String = "seed"): List<Question> {
        val map = LinkedHashMap<String, Question>()
        // List files recursively under dir; do not abort on individual file errors
        val files = runCatching { listAssetFiles(dir) }.getOrElse { emptyList() }
        if (files.isEmpty()) return loadFromAssets()

        val csv = files.filter { it.endsWith(".csv", ignoreCase = true) }.sorted()
        val jsonl = files.filter { it.endsWith(".jsonl", ignoreCase = true) }.sorted()
        val json = files.filter { it.endsWith(".json", ignoreCase = true) }.sorted()

        csv.forEach { full ->
            runCatching { readCsv(full) }
                .onSuccess { list -> list.forEach { q -> map[q.id] = q } }
                .onFailure { e -> Log.d("SeedLoader", "CSV load failed: $full -> ${e.message}") }
        }
        jsonl.forEach { full ->
            runCatching { readJsonLines(full) }
                .onSuccess { list -> list.forEach { q -> map[q.id] = q } }
                .onFailure { e -> Log.d("SeedLoader", "JSONL load failed: $full -> ${e.message}") }
        }
        json.forEach { full ->
            runCatching { readJsonArray(full) }
                .onSuccess { list -> list.forEach { q -> map[q.id] = q } }
                .onFailure { e -> Log.d("SeedLoader", "JSON load failed: $full -> ${e.message}") }
        }
        return if (map.isNotEmpty()) map.values.toList() else loadFromAssets()
    }

    fun loadForVehicle(vehicle: String, dir: String = "seed"): List<Question> {
        val files = runCatching { listAssetFiles(dir) }.getOrElse { emptyList() }
        if (files.isEmpty()) return emptyList()
        val matcher = fileMatcher(vehicle)
        val selected = files.filter { f -> matcher(f) }
        if (selected.isEmpty()) {
            // fallback: load all and filter by category field if present
            val all = loadAllFromAssets(dir)
            val key = vehicle.lowercase()
            return all.filter { q ->
                val c = q.category?.lowercase().orEmpty()
                when (key) {
                    "car", "cars" -> listOf("car","cars","lightvehicle","light vehicles").any { c.contains(it) }
                    "motorcycle", "bike", "bikes" -> listOf("motor","motorcycle","bike","bikes").any { c.contains(it) }
                    "lorry", "lorries", "truck", "trucks" -> listOf("lorry","lorries","truck","trucks","hgv").any { c.contains(it) }
                    "buscoach", "bus", "buses", "coach", "coaches" -> listOf("bus","buses","coach","coaches","psv").any { c.contains(it) }
                    else -> false
                }
            }
        }
        val map = LinkedHashMap<String, Question>()
        selected.sorted().forEach { full ->
            when {
                full.endsWith(".csv", true) -> runCatching { readCsv(full) }.onSuccess { it.forEach { q -> map[q.id] = q } }
                full.endsWith(".jsonl", true) -> runCatching { readJsonLines(full) }.onSuccess { it.forEach { q -> map[q.id] = q } }
                full.endsWith(".json", true) -> runCatching { readJsonArray(full) }.onSuccess { it.forEach { q -> map[q.id] = q } }
            }
        }
        return map.values.toList()
    }

    private fun fileMatcher(vehicle: String): (String) -> Boolean {
        val v = vehicle.lowercase()
        val car = listOf("car","cars","lightvehicle")
        val moto = listOf("motor","motorcycle","bike","bikes")
        val lorry = listOf("lorry","lorries","truck","trucks","hgv")
        val bus = listOf("bus","buses","coach","coaches","psv")
        return { path ->
            val name = path.substringAfterLast('/')
            val tokens = name.lowercase()
            when {
                v in listOf("car","cars") -> car.any { tokens.contains(it) }
                v in listOf("motorcycle","bike","bikes") -> moto.any { tokens.contains(it) }
                v in listOf("lorry","lorries","truck","trucks") -> lorry.any { tokens.contains(it) }
                v in listOf("buscoach","bus","buses","coach","coaches") -> bus.any { tokens.contains(it) }
                else -> false
            }
        }
    }

    private fun listAssetFiles(dir: String): List<String> {
        val out = mutableListOf<String>()
        val children = context.assets.list(dir) ?: return out
        children.forEach { name ->
            val full = if (dir.isEmpty()) name else "$dir/$name"
            val sub = context.assets.list(full)
            if (sub != null && sub.isNotEmpty()) {
                out.addAll(listAssetFiles(full))
            } else {
                out.add(full)
            }
        }
        return out
    }

    private fun readJsonArray(path: String): List<Question> {
        val json = context.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
        val type = object : TypeToken<List<SeedQuestion>>() {}.type
        val seed: List<SeedQuestion> = Gson().fromJson(json, type)
        return seed.map { it.toDomain() }
    }

    private fun readJsonLines(path: String): List<Question> {
        val gson = Gson()
        val lines = context.assets.open(path).use { it.reader(Charsets.UTF_8).readLines() }
        val out = ArrayList<Question>(lines.size)
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                try {
                    val sq: SeedQuestion = gson.fromJson(trimmed, SeedQuestion::class.java)
                    out.add(sq.toDomain())
                } catch (_: JsonSyntaxException) {
                    // skip malformed lines
                }
            }
        }
        return out
    }

    private fun readCsv(path: String): List<Question> {
        val reader = context.assets.open(path).bufferedReader(Charsets.UTF_8)
        val lines = reader.use { it.readLines() }
        if (lines.isEmpty()) return emptyList()
        val header = splitCsv(lines.first()).map { normalize(it) }
        val rows = lines.drop(1)
        val out = ArrayList<Question>(rows.size)
        rows.forEachIndexed { idx, raw ->
            if (raw.isBlank()) return@forEachIndexed
            val cols = splitCsv(raw)
            val byName = header.indices.associate { i -> header[i] to (cols.getOrNull(i) ?: "").trim() }
            val id = (firstOf(header, listOf("id","questionid","qid"))?.let { byName[it] } ?: "")
                .ifBlank { "csv_${idx+1}" }
            val text = (firstOf(header, listOf("text","question","questiontext"))?.let { byName[it] } ?: "").trim()
            val options = extractOptions(header, byName)
            val correctIndex = extractCorrectIndex(byName, options)
            val category = firstOf(header, listOf("category","topic","subject"))?.let { byName[it]?.ifBlank { null } }
            val difficultyStr = firstOf(header, listOf("difficulty","level","hardness"))?.let { byName[it] } ?: ""
            val difficulty = difficultyStr.toIntOrNull()
            if (text.isNotBlank() && options.isNotEmpty() && correctIndex in options.indices) {
                out.add(Question(id = id, text = text, options = options, correctIndex = correctIndex, category = category, difficulty = difficulty))
            }
        }
        return out
    }

    private fun splitCsv(line: String): List<String> {
        val out = ArrayList<String>()
        var i = 0
        val sb = StringBuilder()
        var inQuotes = false
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') { // escaped quote
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { out.add(sb.toString()); sb.clear() }
                    else -> sb.append(c)
                }
            }
            i++
        }
        out.add(sb.toString())
        return out.map { it.trim() }
    }

    private fun normalize(name: String) = name.trim().lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun firstOf(header: List<String>, keys: List<String>): String? = keys.firstOrNull { header.contains(it) }

    private fun extractOptions(header: List<String>, byName: Map<String, String>): List<String> {
        data class Opt(val order: Int, val value: String)
        val opts = mutableListOf<Opt>()
        val numPatterns = listOf("option", "answer", "ans", "choice", "opt")
        header.forEach headerLoop@{ h ->
            val v = byName[h]?.trim().orEmpty()
            if (v.isEmpty()) return@headerLoop
            var matched = false
            // Numbered headers e.g., option1, answer2, ans3, choice4, opt5
            numPatterns.forEach numLoop@{ prefix ->
                val m = Regex("^${prefix}(\\d+)").find(h)
                if (m != null) {
                    opts.add(Opt(m.groupValues[1].toInt(), v))
                    matched = true
                    return@numLoop
                }
            }
            if (!matched) {
                // Lettered headers e.g., A, B, C, or optionA, answerB, etc.
                val mLet = Regex("^(?:option|answer|ans|choice|opt)?([a-j])$").find(h)
                if (mLet != null) {
                    opts.add(Opt(letterIndex(mLet.groupValues[1]) + 1, v))
                }
            }
        }
        if (opts.isEmpty()) {
            // Try plain letters a-j as a last resort
            ("abcdefghij").forEachIndexed { i, ch ->
                val key = ch.toString()
                val value = byName[key]
                if (!value.isNullOrBlank()) opts.add(Opt(i + 1, value))
            }
        }
        return opts.sortedBy { it.order }.map { it.value }.filter { it.isNotBlank() }
    }

    private fun extractCorrectIndex(byName: Map<String, String>, options: List<String>): Int {
        val keys = listOf(
            "correctindex","answerindex","correct","answer","correctoption",
            "correctanswer","answerletter","answernumber","right","rightanswer","rightoption"
        )
        val raw = keys.firstNotNullOfOrNull { byName[it] }?.trim().orEmpty()
        if (raw.isEmpty()) return 0
        raw.toIntOrNull()?.let { n ->
            return if (n in 0 until options.size) n else (n - 1).coerceIn(0, options.size - 1)
        }
        if (raw.length == 1 && raw[0].isLetter()) {
            val idx = letterIndex(raw).coerceIn(0, options.size - 1)
            return idx
        }
        val matchIdx = options.indexOfFirst { it.equals(raw, ignoreCase = true) }
        return if (matchIdx >= 0) matchIdx else 0
    }

    private fun letterIndex(s: String): Int {
        val ch = s.lowercase()[0]
        return (ch - 'a').coerceAtLeast(0)
    }

    private data class SeedQuestion(
        val id: String,
        val text: String,
        val options: List<String>,
        val correctIndex: Int,
        val category: String? = null,
        val difficulty: Int? = null,
    ) {
        fun toDomain() = Question(id, text, options, correctIndex, category, difficulty)
    }
}
