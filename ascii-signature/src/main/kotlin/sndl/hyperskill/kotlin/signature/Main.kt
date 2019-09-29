package sndl.hyperskill.kotlin.signature

import java.io.File
import java.util.*

class Ascii(fontFile: File, val padding: Int, val border: String) {
    inner class AsciiChar(val char: Char, val width: Int, val graphic: Array<String>)
    inner class AsciiString(val chars: Array<AsciiChar?>, val width: Int) {
        fun print(borderLength: Int) {
            // Adding padding spaces to each side
            val leftPadding = (borderLength - width) / 2 + padding
            val rightPadding = borderLength - width - (borderLength - width) / 2 + padding

            for (i in 0 until fontHeight) {
                print(border); repeat(leftPadding) { print(' ') }

                chars.forEach { char ->
                    char?.let {
                        print(it.graphic[i])
                    } ?: repeat(spaceWidth) { print(' ') }
                }

                repeat(rightPadding) { print(' ') }; print(border)
                println()
            }
        }
    }

    private val scanner = Scanner(fontFile)
    private val fontHeight = scanner.nextInt()
    private val charNumber = scanner.nextInt()
    private val charList = Array(charNumber) {
        AsciiChar(
                char = scanner.next().first(),
                width = scanner.nextInt().also { scanner.nextLine() },
                graphic = Array(fontHeight) { scanner.nextLine() }
        )
    }
    private val spaceWidth = getChar('a').width

    private fun getChar(char: Char) = charList.find { it.char == char }
            ?: throw IllegalStateException("Char not found: $char")

    fun getString(str: String): AsciiString {
        var length = 0
        val arr = Array(str.length) {
            if (str[it] == ' ') {
                length += spaceWidth
                null
            } else {
                getChar(str[it]).also { length += it.width }
            }
        }

        return AsciiString(arr, length)
    }
}

fun main() {
    fun withBorder(maxLength: Int, padding: Int, border: String, body: () -> Unit) {
        val borderLength = maxLength + 2 * padding+ 2 * border.length

        repeat(borderLength) { print(border.first()) }; println()
        body()
        repeat(borderLength) { print(border.first()) }; println()
    }

    val padding = 2
    val border = "88"
    val scanner = Scanner(System.`in`)
    val roman = Ascii(File(loadResourcePath("roman.txt")), padding, border)
    val medium = Ascii(File(loadResourcePath("medium.txt")), padding, border)

    print("Enter name and surname: ")
    val fullname = scanner.nextLine()
    print("Enter person's status: ")
    val status = scanner.nextLine()

    val fullnameAscii = roman.getString(fullname)
    val statusAscii = medium.getString(status)
    val maxLength = maxOf(fullnameAscii.width, statusAscii.width)

    withBorder(maxLength, padding, border) {
        fullnameAscii.print(maxLength)
        statusAscii.print(maxLength)
    }
}

fun loadResourcePath(resource: String) = object {}.javaClass.classLoader.getResource(resource)!!.path
