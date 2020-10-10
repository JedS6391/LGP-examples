package nz.co.jedsimson.lgp.examples.kotlin

import nz.co.jedsimson.lgp.core.environment.DefaultValueProviders
import nz.co.jedsimson.lgp.core.environment.operations.DefaultOperationLoader
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.Arity
import nz.co.jedsimson.lgp.core.program.instructions.Function
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.registers.ArrayRegisterSet
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.lib.base.BaseInstruction

enum class AntTrailProblemArity(override val number: Int) : Arity {
    None(0)
}

class IfFoodAhead() : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information: ModuleInformation
        get() = TODO("not implemented")
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }
}

class MoveForward : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information: ModuleInformation
        get() = TODO("not implemented")
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }
}

class TurnLeft : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information: ModuleInformation
        get() = TODO("not implemented")
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }
}

class TurnRight : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information: ModuleInformation
        get() = TODO("not implemented")
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }
}

/**
 * Responsible for manipulating an [Ant] by executing a sequence of instructions.
 */
class AntTrailProgram(
    override var instructions: MutableList<Instruction<Unit>>,
    override var registers: RegisterSet<Unit>,
    private val ant: Ant
) : Program<Unit, Outputs.Single<Unit>>() {
    override val information: ModuleInformation
        get() = TODO("not implemented")
    override val outputRegisterIndices: List<RegisterIndex>
        get() = TODO("not implemented")

    override fun copy(): Program<Unit, Outputs.Single<Unit>> {
        TODO("not implemented")
    }

    override fun execute() {
        // Reset the ant simulator back to its original state (including the grid).
        this.ant.reset()

        var skipNextInstruction = false

        for (instruction in this.instructions) {
            if (skipNextInstruction ) {
                // A branch was not taken so skip the current instruction
                continue
            }

            val operation = instruction.operation

            // Determine how to manipulate the ant.
            when (operation.javaClass) {
                IfFoodAhead::class.java -> {
                    // The next instruction should be skipped when there is no food ahead,
                    // resulting in an if-else like structure.
                    skipNextInstruction = !this.ant.isFoodAhead()
                }
                MoveForward::class.java -> this.ant.moveForward()
                TurnLeft::class.java -> this.ant.turnLeft()
                TurnRight::class.java -> this.ant.turnRight()
            }
        }
    }

    override fun findEffectiveProgram() {
        TODO("not implemented")
    }

    override fun output(): Outputs.Single<Unit> {
        TODO("not implemented")
    }
}

class AntTrail {
    companion object Main {
        @JvmStatic fun main(args: Array<String>) {
            val grid = Grid(arrayOf(
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Food, Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Food, Grid.Cell.Food, Grid.Cell.Food, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Food, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Food, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Food, Grid.Cell.Food)
            ))
            val ant = Ant(
                grid = grid,
                maximumMoves = 15
            )
            val operationLoader = DefaultOperationLoader<Unit>(
                listOf(
                    "nz.co.jedsimson.lgp.examples.kotlin.IfFoodAhead",
                    "nz.co.jedsimson.lgp.examples.kotlin.MoveForward",
                    "nz.co.jedsimson.lgp.examples.kotlin.TurnLeft",
                    "nz.co.jedsimson.lgp.examples.kotlin.TurnRight"
                )
            )
            val operations = operationLoader.load()
            val registers = ArrayRegisterSet(
                inputRegisters = 0,
                calculationRegisters = 0,
                constants = listOf(),
                defaultValueProvider = DefaultValueProviders.constantValueProvider(Unit)
            )

            val ifFoodAhead = BaseInstruction(operations[0], 0, mutableListOf())
            val moveForward = BaseInstruction(operations[1], 0, mutableListOf())
            val turnLeft = BaseInstruction(operations[2], 0, mutableListOf())
            val turnRight = BaseInstruction(operations[3], 0, mutableListOf())

            val program = AntTrailProgram(
                mutableListOf(turnRight, moveForward, moveForward),
                registers,
                ant
            )

            println(ant.position)

            program.execute()

            println(ant.position)
        }
    }
}