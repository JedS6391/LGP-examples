package nz.co.jedsimson.lgp.examples.kotlin

import kotlinx.coroutines.runBlocking
import nz.co.jedsimson.lgp.core.environment.*
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.config.ConfigurationLoader
import nz.co.jedsimson.lgp.core.environment.constants.GenericConstantLoader
import nz.co.jedsimson.lgp.core.environment.dataset.*
import nz.co.jedsimson.lgp.core.environment.events.Event
import nz.co.jedsimson.lgp.core.environment.events.EventDispatcher
import nz.co.jedsimson.lgp.core.environment.events.EventListener
import nz.co.jedsimson.lgp.core.environment.events.EventRegistry
import nz.co.jedsimson.lgp.core.environment.operations.DefaultOperationLoader
import nz.co.jedsimson.lgp.core.evolution.*
import nz.co.jedsimson.lgp.core.evolution.fitness.*
import nz.co.jedsimson.lgp.core.evolution.model.SteadyState
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.ConstantMutationFunctions
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.MicroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.LinearCrossover
import nz.co.jedsimson.lgp.core.evolution.operators.selection.BinaryTournamentSelection
import nz.co.jedsimson.lgp.core.evolution.training.SequentialTrainer
import nz.co.jedsimson.lgp.core.evolution.training.TrainingResult
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleContainer
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator
import nz.co.jedsimson.lgp.core.program.instructions.Arity
import nz.co.jedsimson.lgp.core.program.instructions.Function
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.lib.generators.RandomInstructionGenerator

enum class AntTrailProblemArity(override val number: Int) : Arity {
    None(0)
}

class IfFoodAhead() : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information = ModuleInformation("Instructs the ant simulator to check if there is food in the next position.")
    override val representation = "IfFoodAhead"

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex) = this.representation
}

class MoveForward : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information = ModuleInformation("Instructs the ant simulator to move forward into the next position.")
    override val representation = "MoveForward"

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex) = this.representation
}

class TurnLeft : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information = ModuleInformation("Instructs the ant simulator to turn left.")
    override val representation = "TurnLeft"

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex) = this.representation
}

class TurnRight : Operation<Unit>() {
    override val arity: Arity = AntTrailProblemArity.None
    override val function: Function<Unit> = { }
    override val information = ModuleInformation("Instructs the ant simulator to turn right.")
    override val representation = "TurnRight"

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex) = this.representation
}

/**
 * Responsible for manipulating an [Ant] by executing a sequence of instructions.
 */
class AntTrailProgram(
    override var instructions: MutableList<Instruction<Unit>>,
    override var registers: RegisterSet<Unit>,
    val ant: Ant
) : Program<Unit, Outputs.Single<Unit>>() {
    override val information = ModuleInformation("Simulates the movement of an ant by executing a set of instructions.")
    override val outputRegisterIndices = listOf<RegisterIndex>()

    override fun copy(): Program<Unit, Outputs.Single<Unit>> = AntTrailProgram(
        instructions = this.instructions.map(Instruction<Unit>::copy).toMutableList(),
        registers = this.registers.copy(),
        ant = this.ant.copy()
    )

    override fun execute() {
        var skipNextInstruction = false

        //run until max moves or ideal fitness
        while (this.ant.grid.foodRemaining() != 0 && this.ant.state.movesMade < this.ant.maximumMoves) {
            for (instruction in this.instructions) {
                if (skipNextInstruction) {
                    skipNextInstruction = false
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
    }

    override fun findEffectiveProgram() {

    }

    override fun output(): Outputs.Single<Unit> = Outputs.Single(Unit)
}

class GridProvider(private val gridData: Array<Array<Grid.Cell>>) {
    fun newGrid(): Grid {
        return Grid(this.gridData.clone())
    }
}

class AntTrailProgramGenerator(
    environment: EnvironmentFacade<Unit, Outputs.Single<Unit>, Targets.Single<Unit>>,
    private val maximumMoves: Int,
    private val gridProvider: GridProvider
) : ProgramGenerator<Unit, Outputs.Single<Unit>, Targets.Single<Unit>>(
    environment,
    instructionGenerator = environment.moduleFactory.instance(CoreModuleType.InstructionGenerator)
) {
    private val random = this.environment.randomState

    override val information = ModuleInformation("Responsible for generating ant simulator programs.")

    override fun generateProgram(): Program<Unit, Outputs.Single<Unit>> {
        val length = this.random.randInt(
            this.environment.configuration.initialMinimumProgramLength,
            this.environment.configuration.initialMaximumProgramLength
        )

        val instructions = mutableListOf<Instruction<Unit>>()

        val branchInitialisationRate = this.environment.configuration.branchInitialisationRate

        for (i in 1..length) {
            val instruction = when {
                random.nextDouble() < branchInitialisationRate -> this.instructionGenerator.next().first { instruction ->
                    instruction.operation is IfFoodAhead
                }
                else -> this.instructionGenerator.generateInstruction()
            }

            instructions.add(instruction)
        }

        return AntTrailProgram(
            instructions = instructions,
            registers = this.environment.registerSet.copy(),
            ant = Ant(
                grid = this.gridProvider.newGrid(),
                maximumMoves = this.maximumMoves
            )
        )
    }
}

class AntTrailFitnessEvaluationEvent(val key: String, val details: Map<String, Any>) : Event()

class AntTrailFitnessContext(
  environment: EnvironmentFacade<Unit, Outputs.Single<Unit>, Targets.Single<Unit>>
) : FitnessContext<Unit, Outputs.Single<Unit>, Targets.Single<Unit>>(environment) {
    override val information = ModuleInformation("Evaluates the fitness of an ant simulation based on how it has moved through the grid.")

    override fun fitness(
        program: Program<Unit, Outputs.Single<Unit>>,
        fitnessCases: List<FitnessCase<Unit, Targets.Single<Unit>>>
    ): Double {
        val ant = (program as AntTrailProgram).ant

        ant.reset()

        EventDispatcher.dispatch(AntTrailFitnessEvaluationEvent(
            key = "ant-fitness-evaluation-start",
            details = mapOf(
                "position" to ant.position,
                "ant" to ant.toString()
            )
        ))

        program.execute()

        EventDispatcher.dispatch(AntTrailFitnessEvaluationEvent(
            key = "ant-fitness-evaluation-end",
            details = mapOf(
                "position" to ant.position,
                "ant" to ant.toString()
            )
        ))

        // Ant performance is measured in terms of how much food is remaining
        program.fitness = ant.grid.foodRemaining().toDouble()

        return program.fitness
    }

}

class AntTrailFitnessFunction : SingleOutputFitnessFunction<Unit>() {
    override fun fitness(
        outputs: List<Outputs.Single<Unit>>,
        cases: List<FitnessCase<Unit, Targets.Single<Unit>>>
    ): Double {
        TODO("not implemented")
    }
}

class AntTrailSolution(val trainingResult: TrainingResult<Unit, Outputs.Single<Unit>, Targets.Single<Unit>>) : Solution<Unit> {
    override val problem = "Ant trail"
}

class AntTrailProblem(
    val maximumMoves: Int,
    val gridProvider: GridProvider
) : Problem<Unit, Outputs.Single<Unit>, Targets.Single<Unit>>() {
    override val name = "Ant trail."

    override val description = Description("Ant trail problem")

    override val configLoader = object : ConfigurationLoader {
        override val information = ModuleInformation("Overrides default configuration for this problem.")

        override fun load(): Configuration {
            val config = Configuration()

            config.initialMinimumProgramLength = 5
            config.initialMaximumProgramLength = 20
            config.minimumProgramLength = 5
            config.maximumProgramLength = 40
            config.operations = listOf(
                "nz.co.jedsimson.lgp.examples.kotlin.IfFoodAhead",
                "nz.co.jedsimson.lgp.examples.kotlin.MoveForward",
                "nz.co.jedsimson.lgp.examples.kotlin.TurnLeft",
                "nz.co.jedsimson.lgp.examples.kotlin.TurnRight"
            )
            // The input or calculation registers aren't actually used but we need at least 1 of each
            // to ensure instructions can be generated using the built-in instruction generator.
            config.numFeatures = 1
            config.numCalculationRegisters = 1
            config.constantsRate = 0.0
            config.constants = listOf()
            config.populationSize = 10
            config.generations = 50
            config.microMutationRate = 0.5
            config.macroMutationRate = 0.5

            return config
        }
    }

    private val config = this.configLoader.load()

    override val constantLoader = GenericConstantLoader(
        constants = config.constants,
        parseFunction = { }
    )

    val datasetLoader = object : DatasetLoader<Unit, Targets.Single<Unit>> {
        override val information = ModuleInformation("")

        override fun load(): Dataset<Unit, Targets.Single<Unit>> {
            return Dataset(
                listOf(),
                listOf()
            )
        }
    }

    override val operationLoader = DefaultOperationLoader<Unit>(
        operationNames = config.operations
    )

    override val defaultValueProvider = DefaultValueProviders.constantValueProvider(Unit)

    override val fitnessFunctionProvider = { AntTrailFitnessFunction() }

    override val registeredModules = ModuleContainer<Unit, Outputs.Single<Unit>, Targets.Single<Unit>>(
        modules = mutableMapOf(
            CoreModuleType.InstructionGenerator to { environment ->
                RandomInstructionGenerator(environment)
            },
            CoreModuleType.ProgramGenerator to { environment ->
                AntTrailProgramGenerator(
                    environment,
                    maximumMoves,
                    gridProvider
                )
            },
            CoreModuleType.SelectionOperator to { environment ->
                BinaryTournamentSelection(environment, tournamentSize = 2)
            },
            CoreModuleType.RecombinationOperator to { environment ->
                LinearCrossover(
                    environment,
                    maximumSegmentLength = 6,
                    maximumCrossoverDistance = 5,
                    maximumSegmentLengthDifference = 3
                )
            },
            CoreModuleType.MacroMutationOperator to { environment ->
                MacroMutationOperator(
                    environment,
                    insertionRate = 0.67,
                    deletionRate = 0.33
                )
            },
            CoreModuleType.MicroMutationOperator to { environment ->
                MicroMutationOperator(
                    environment,
                    registerMutationRate = 0.5,
                    operatorMutationRate = 0.5,
                    constantMutationFunc = ConstantMutationFunctions.identity()
                )
            },
            CoreModuleType.FitnessContext to { environment ->
                AntTrailFitnessContext(environment)
            }
        )
    )

    override fun initialiseEnvironment() {
        val specification = EnvironmentSpecification(
            this.configLoader,
            this.constantLoader,
            this.operationLoader,
            this.defaultValueProvider,
            this.fitnessFunctionProvider
        )
        this.environment = Environment(specification)

        this.environment.registerModules(this.registeredModules)
    }

    override fun initialiseModel() {
        this.model = SteadyState(this.environment)
    }

    override fun solve(): AntTrailSolution {
        try {
            val runner = SequentialTrainer(environment, model, runs = 2)

            return runBlocking {
                val job = runner.trainAsync(
                    this@AntTrailProblem.datasetLoader.load()
                )

                job.subscribeToUpdates { println("training progress = ${it.progress}%") }

                val result = job.result()

                AntTrailSolution(result)
            }
        } catch (ex: UninitializedPropertyAccessException) {
            // The initialisation routines haven't been run.
            throw ProblemNotInitialisedException(
                "The initialisation routines for this problem must be run before it can be solved."
            )
        }
    }
}

class AntTrail {
    companion object Main {
        @JvmStatic fun main(args: Array<String>) {
            val gridProvider = GridProvider(arrayOf(
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Food, Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Food, Grid.Cell.Food, Grid.Cell.Food, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Food, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Food, Grid.Cell.Empty),
                arrayOf(Grid.Cell.Empty, Grid.Cell.Empty, Grid.Cell.Food, Grid.Cell.Food)
            ))

            val problem = AntTrailProblem(
                maximumMoves = 60,
                gridProvider = gridProvider
            )

            problem.initialiseEnvironment()
            problem.initialiseModel()

            EventRegistry.register(object : EventListener<AntTrailFitnessEvaluationEvent> {
                override fun handle(event: AntTrailFitnessEvaluationEvent) {
                    println(event.key)
                    println(event.details)
                }
            })

            val solution = problem.solve()

            solution.trainingResult.evaluations.forEachIndexed { evaluation, result ->
                println("Best program (evaluation ${evaluation + 1})")
                println("fitness = ${result.best.fitness.toInt()}")
                println("instructions = ${result.best.instructions}")
            }
        }
    }
}