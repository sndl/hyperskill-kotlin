package coffee

import java.util.*
import kotlin.IllegalStateException

object CoffeeMachine {
    var water = 400
    var milk = 540
    var beans = 120
    var cups = 9
    var money = 550

    interface Ingredients {
        val water: Int
        val milk: Int?
        val beans: Int
    }

    data class Resources(override val water: Int, override val milk: Int?, override val beans: Int, val cups: Int) : Ingredients

    enum class Recipe(
            override val water: Int,
            override val milk: Int?,
            override val beans: Int,
            private val price: Int) : Ingredients {
        ESPRESSO(250, null, 16, 4),
        LATTE(350, 75, 20, 7),
        CAPPUCCINO(200, 100, 12, 6);

        private fun lackingIngredients() = when {
            CoffeeMachine.water - water < 0 -> "water"
            CoffeeMachine.milk - milk.valOrZero() < 0 -> "milk"
            CoffeeMachine.beans - beans < 0 -> "beans"
            CoffeeMachine.cups - 1 < 0 -> "cups"
            else -> null
        }

        fun brew(): String? {
            lackingIngredients()?.let {
                return it
            } ?: run {
                CoffeeMachine.water -= water
                milk?.let { CoffeeMachine.milk -= it }
                CoffeeMachine.beans -= beans
                cups -= 1
                money += price
                return null
            }
        }

        companion object {
            fun get(name: String) = valueOf(name.toUpperCase())
        }
    }

    enum class Actions(val action: () -> Unit) {
        BUY({
            val recipe = UserInterface.recipeInput()
            recipe?.let {
                val result = Recipe.get(it).brew()
                UserInterface.buyOutput(result)
            }
        }),
        FILL({
            val resources = UserInterface.fillInput()
            addResources(resources)
        }),
        TAKE({
            UserInterface.takeOutput(money).also { money = 0 }
        }),
        REMAINING({
            UserInterface.remainingOutput()
        });

        fun exec() {
            action()
        }

        companion object {
            fun get(name: String) = valueOf(name.toUpperCase())
        }
    }

    data class RequiredIngredients(override val water: Int, override val milk: Int?, override val beans: Int) : Ingredients {
        constructor(recipe: Recipe) : this(recipe.water, recipe.milk, recipe.beans)
        constructor(recipe: Recipe, cups: Int) : this(recipe.water * cups, recipe.milk?.let { it * cups }, recipe.beans * cups)
    }

    private fun addResources(resources: Resources) {
        water += resources.water
        milk += resources.milk.valOrZero()
        beans += resources.beans
        cups += resources.cups
    }

    object UserInterface {
        private val scanner = Scanner(System.`in`)

        fun buyOutput(result: String?) {
            val output = result?.let {
                "Sorry, not enough $it!"
            } ?: "I have enough resources, making you a coffee!"

            println(output)
        }

        fun remainingOutput() {
            println("""
            The coffee machine has:
            $water of water
            $milk of milk
            $beans of coffee beans
            $cups of disposable cups
            $$money of money
        """.trimIndent())
        }

        fun recipeInput(): String? {
            print("What do you want to buy? 1 - espresso, 2 - latte, 3 - cappuccino, back - to main menu: ")
            return when (scanner.next()) {
                "1" -> "espresso"
                "2" -> "latte"
                "3" -> "cappuccino"
                "back" -> null
                else -> throw IllegalStateException()
            }
        }

        fun fillInput(): Resources {
            print("Write how many ml of water do you want to add: ")
            val water = scanner.nextInt()
            print("Write how many ml of milk do you want to add: ")
            val milk = scanner.nextInt()
            print("Write how many grams of coffee beans do you want to add: ")
            val beans = scanner.nextInt()
            print("Write how many disposable cups of coffee do you want to add: ")
            val cups = scanner.nextInt()

            return Resources(water, milk, beans, cups)
        }

        fun takeOutput(money: Int) {
            println("I gave you $$money")
        }

        // TODO: can be improved by converting UI interaction to state machine
        fun run() {
            while (true) {
                print("Write action (${Actions.values().joinToString { it.name.toLowerCase() }}, exit): ")
                when (val input = scanner.next().toLowerCase()) {
                    "exit" -> return
                    else -> Actions.get(input).exec()
                }
                println()
            }
        }
    }
}

fun main() {
    CoffeeMachine.UserInterface.run()
}

fun Int?.valOrZero() = this ?: 0
