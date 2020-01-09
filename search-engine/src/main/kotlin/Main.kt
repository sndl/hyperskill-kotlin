package search

import java.io.File
import java.util.*
import kotlin.IllegalArgumentException
import kotlin.system.exitProcess

fun MutableList<Int>.addIfNotExists(value: Int) {
    if (!this.contains(value)) this.add(value)
}

class SearchEngine(filePath: String) {
    enum class SearchStrategy { ALL, ANY, NONE }

    val entries = File(filePath).readLines()
    private val invertedIndex = run {
        val result = mutableMapOf<String, MutableList<Int>>()

        entries.forEachIndexed { index, entry ->
            entry.split(" ").forEach { word ->
                result.putIfAbsent(word.normalize(), mutableListOf(index))?.addIfNotExists(index)
            }
        }

        // Converting collections to immutable
        result.toMap().mapValues { it.value.toList() }
    }

    fun find(strategy: SearchStrategy, terms: List<String>): List<String> {
        fun getEntriesByTerms(terms: List<String>) = terms.flatMap { term ->
            invertedIndex[term.normalize()]?.map { entries[it] } ?: emptyList()
        }.distinct()

        return when (strategy) {
            SearchStrategy.ANY -> getEntriesByTerms(terms)
            SearchStrategy.ALL -> terms.flatMap {
                invertedIndex[it] ?: emptyList()
            }.groupingBy { it }.eachCount().filter { it.value == terms.size }.map {
                entries[it.key]
            }
            SearchStrategy.NONE -> entries.subtract(getEntriesByTerms(terms)).toList()
        }
    }

    private fun String.normalize() = this.toLowerCase()
}

class SearchEngineCLI(private val engine: SearchEngine) {
    enum class State { MENU, FIND, FIND_ALL, EXIT }

    private val scanner = Scanner(System.`in`)
    private var state = State.MENU

    fun run() {
        while (true) {
            try {
                when (state) {
                    State.MENU -> menu()
                    State.FIND -> {
                        find()
                        state = State.MENU
                    }
                    State.FIND_ALL -> {
                        findAll()
                        state = State.MENU
                    }
                    State.EXIT -> exit()
                }
            } catch (e: IllegalArgumentException) {
                println("\nIncorrect option! Try again.")
            }
            println()
        }
    }

    private fun menu() {
        val help = """
            === Menu ===
            1. Find a person
            2. Print all people
            0. Exit
        """.trimIndent()

        println(help)

        state = when (scanner.next()) {
            "1" -> State.FIND
            "2" -> State.FIND_ALL
            "0" -> State.EXIT
            else -> throw IllegalArgumentException()
        }
    }

    private fun findAll() {
        val help = "=== List of people ==="

        println(help)
        engine.entries.forEach { println(it) }
    }

    private fun find() {
        fun searchStrategy(): SearchEngine.SearchStrategy {
            val strategies = SearchEngine.SearchStrategy.values().joinToString(", ")
            val help = "Select a matching strategy: $strategies"
            println(help)

            return SearchEngine.SearchStrategy.valueOf(scanner.next().toUpperCase())
        }

        fun searchTerms(): List<String> {
            val help = "Enter a name or email to search all suitable people."
            println(help)

            // Additional nextLine() due to scanner problems
            scanner.nextLine()
            return scanner.nextLine().trim().split(" ")
        }

        val strategy = searchStrategy()
        println()
        val terms = searchTerms()
        val result = engine.find(strategy, terms)

        if (result.isNotEmpty()) {
            println("${result.size} persons found:")
            result.forEach { println(it) }
        } else {
            println("No matching people found.")
        }
    }

    private fun exit() {
        println("Bye!")
        exitProcess(0)
    }
}

fun main() {
    val engine = SearchEngine(loadResourcePath("example.txt"))
    val cli = SearchEngineCLI(engine)

    cli.run()
}

fun loadResourcePath(resource: String) = object {}.javaClass.classLoader.getResource(resource)!!.path
