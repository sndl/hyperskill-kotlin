package converter

import java.lang.IllegalStateException
import java.util.*

interface Measurement {
    data class Names(val single: String, val plural: String, val short: String, val additional: List<String>? = null) {
        val values = listOf(single, plural, short) + additional.orEmpty()
    }

    data class ConvertResult(val value: Double, val unitName: String) {
        fun asString() = "$value $unitName"
    }

    val names: Names

    fun convert(value: Double, to: Measurement): ConvertResult

    fun pickName(value: Double): String = if (value == 1.0) names.single else names.plural

    interface Companion {
        fun findByName(name: String): Measurement?
    }
}

interface ScaledMeasurement : Measurement {
    val scale: Double
    val referenceMeasurement: Measurement?

    override fun convert(value: Double, to: Measurement): Measurement.ConvertResult {
        check(this.javaClass == to.javaClass) { "Conversion from ${this.names.plural} to ${to.names.plural} is impossible" }
        check(value > 0) { "${this.javaClass.simpleName.capitalize()} shouldn't be negative" }
        val result = referenceMeasurement?.let { reference ->
            if (to != reference) convert(value, reference).value / (to as ScaledMeasurement).scale else value * this.scale
        } ?: value / (to as ScaledMeasurement).scale

        return Measurement.ConvertResult(result, to.pickName(result))
    }
}

enum class Length(
        override val names: Measurement.Names,
        override val scale: Double,
        override val referenceMeasurement: Measurement?) : ScaledMeasurement {
    METER(Measurement.Names("meter", "meters", "m"), 1.0, null),
    KILOMETER(Measurement.Names("kilometer", "kilometers", "km"), 1000.0, METER),
    CENTIMETER(Measurement.Names("centimeter", "centimeters", "cm"), 0.01, METER),
    MILLIMETER(Measurement.Names("millimeter", "millimeters", "mm"), 0.001, METER),
    MILE(Measurement.Names("mile", "miles", "mi"), 1609.35, METER),
    YARD(Measurement.Names("yard", "yards", "yd"), 0.9144, METER),
    FOOT(Measurement.Names("foot", "feet", "ft"), 0.3048, METER),
    INCH(Measurement.Names("inch", "inches", "in"), 0.0254, METER);

    companion object : Measurement.Companion {
        override fun findByName(name: String) = values().find { name.toLowerCase() in it.names.values }
    }
}

enum class Weight(
        override val names: Measurement.Names,
        override val scale: Double,
        override val referenceMeasurement: Measurement?) : ScaledMeasurement {
    GRAM(Measurement.Names("gram", "grams", "g"), 1.0, null),
    KILOGRAM(Measurement.Names("kilogram", "kilograms", "kg"), 1000.0, GRAM),
    MILLIGRAM(Measurement.Names("milligram", "milligrams", "mg"), 0.001, GRAM),
    POUND(Measurement.Names("pound", "pounds", "lb"), 453.592, GRAM),
    OUNCE(Measurement.Names("ounce", "ounces", "oz"), 28.3495, GRAM);

    companion object : Measurement.Companion {
        override fun findByName(name: String) = values().find { name.toLowerCase() in it.names.values }
    }
}

enum class Temperature(override val names: Measurement.Names, val formula: (value: Double, to: Temperature) -> Measurement.ConvertResult) : Measurement {
    CELSIUS(Measurement.Names("degree Celsius", "degrees Celsius", "c", listOf("dc", "celsius")),
            { value, to ->
                val result = when (to) {
                    CELSIUS -> value
                    FAHRENHEIT -> value * 9 / 5 + 32
                    KELVIN -> value + 273.15
                }
                Measurement.ConvertResult(result, to.pickName(result))
            }),
    FAHRENHEIT(Measurement.Names("degree Fahrenheit", "degrees Fahrenheit", "f", listOf("df", "fahrenheit")),
            { value, to ->
                val result = when (to) {
                    FAHRENHEIT -> value
                    CELSIUS -> (value - 32) * 5 / 9
                    KELVIN -> (value + 459.67) * 5 / 9
                }
                Measurement.ConvertResult(result, to.pickName(result))
            }),
    KELVIN(Measurement.Names("Kelvin", "Kelvins", "k"),
            { value, to ->
                val result = when (to) {
                    KELVIN -> value
                    FAHRENHEIT -> value * 9 / 5 - 459.67
                    CELSIUS -> value - 273.15
                }
                Measurement.ConvertResult(result, to.pickName(result))
            });

    override fun convert(value: Double, to: Measurement): Measurement.ConvertResult {
        check(this.javaClass == to.javaClass) { "Conversion from ${this.names.plural} to ${to.names.plural} is impossible" }
        val result = this.formula(value, (to as Temperature))
        return result
    }

    companion object : Measurement.Companion {
        override fun findByName(name: String) = values().find { name.toLowerCase() in it.names.values.map { it.toLowerCase() } }
    }
}

fun main() {
    fun findByName(name: String): Measurement? {
        listOf(Length, Weight, Temperature).forEach {
            val result = it.findByName(name)
            if (result != null) return result
        }
        return null
    }

    val scanner = Scanner(System.`in`)

    while (true) {
        print("Enter what you want to convert (or exit): ")
        val input = scanner.next()
        if (input == "exit") {
            break
        }

        val value = input.toDouble()
        val from = scanner.nextMeasurement()
        scanner.next() // skipping to/in part
        val to = scanner.nextMeasurement()

        val fromMeasurement = findByName(from)
        val toMeasurement = findByName(to)

        if (fromMeasurement != null && toMeasurement != null) {
            val output =
                    try {
                        "$value ${fromMeasurement.pickName(value)} is ${fromMeasurement.convert(value, toMeasurement).asString()}"
                    } catch (e: IllegalStateException) {
                        e.message
                    }
            println(output)
        } else {
            println("Conversion from ${fromMeasurement?.names?.plural ?: "???"} to ${toMeasurement?.names?.plural
                    ?: "???"} is impossible.")
            continue
        }
    }
}

fun Scanner.nextMeasurement(): String = if (this.hasNext("(?i)degree(s)?")) "${this.next()} ${this.next()}" else this.next()
