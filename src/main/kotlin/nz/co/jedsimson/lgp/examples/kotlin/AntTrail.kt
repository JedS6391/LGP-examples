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
import nz.co.jedsimson.lgp.core.evolution.operators.selection.TournamentSelection
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
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.streams.toList

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
        var linearBranching=false
        var skipNextInstruction = false
        var lastMovesMade = -1

        //run until max moves or ideal fitness
        while (this.ant.grid.foodRemaining() != 0 && this.ant.state.movesMade < this.ant.maximumMoves && this.ant.state.movesMade > lastMovesMade) {
            lastMovesMade=this.ant.state.movesMade
            var instructionIterator=0
            var instructionsToSkip= mutableListOf<Int>()
            for (instruction in this.instructions) {
                if (linearBranching){
                    if (skipNextInstruction) {
                        skipNextInstruction = false
                        continue
                    }
                }else{
                    if (instructionIterator in instructionsToSkip) {
                        // A branch was not taken so skip the current instruction
                        instructionIterator++
                        continue
                    }
                }


                val operation = instruction.operation

                // Determine how to manipulate the ant.
                when (operation.javaClass) {
                    IfFoodAhead::class.java -> {
                        // look ahead which nodes in the branch need to be skipped

                        if(linearBranching){
                            if (!this.ant.isFoodAhead()) {
                                skipNextInstruction=true
                            }
                        }
                        else{
                            val tree=buildTreeStructure(instructions,instructionIterator)
                            if (this.ant.isFoodAhead()) {
                                val toSkip=listAllChildren(tree.children[1])
                                instructionsToSkip.addAll(toSkip)
                            }else{
                                val toSkip=listAllChildren(tree.children[0])
                                instructionsToSkip.addAll(toSkip)
                            }
                        }
                    }
                    MoveForward::class.java -> this.ant.moveForward()
                    TurnLeft::class.java -> this.ant.turnLeft()
                    TurnRight::class.java -> this.ant.turnRight()
                }
                instructionIterator++
            }
        }
    }

    class TreeNode<T>(value:T){
        var value:T = value
        var parent:TreeNode<T>? = null

        var children:MutableList<TreeNode<T>> = mutableListOf()

        fun addChild(node:TreeNode<T>){
            children.add(node)
            node.parent = this
        }
        override fun toString(): String {
            var s = "${value}"
            if (!children.isEmpty()) {
                s += " {" + children.map { it.toString() } + " }"
            }
            return s
        }
    }


    private fun buildTreeStructure(instructions:List<Instruction<Unit>>,instructionIterator:Int): TreeNode<Int> {
        var rootNode=TreeNode<Int>(instructionIterator)
        evalNode(instructions,rootNode)
        return rootNode
    }

    private fun evalNode(instructions:List<Instruction<Unit>>,node:TreeNode<Int>): TreeNode<Int>{
        //val children = mutableListOf<IntArray>()
        if(node.value < instructions.size) {
            val operation = instructions[node.value].operation
            if (operation.javaClass == IfFoodAhead::class.java) {
                var childNode1 = TreeNode<Int>(node.value + 1)
                childNode1 = evalNode(instructions, childNode1)
                node.addChild(childNode1)
                //next node is last terminal node of first branch +1
                var childNode2 = TreeNode<Int>(lastChild(childNode1).value + 1)
                childNode2 = evalNode(instructions, childNode2)
                node.addChild(childNode2)
            }
        }
        return node
    }

    private fun lastChild(node:TreeNode<Int>):TreeNode<Int>{
        if(node.children.size>0){
            return lastChild(node.children.last())
        }else{
            return node
        }
    }

    private fun listAllChildren(node:TreeNode<Int>):List<Int>{
        var children = mutableListOf<Int>()
        children.add(node.value)

        if(node.children.size>0){
            for (child in node.children){
                children.addAll(listAllChildren(child))
            }
        }
        return children
    }

    override fun findEffectiveProgram() {

    }

    override fun output(): Outputs.Single<Unit> = Outputs.Single(Unit)
}

class GridProvider private constructor(private val gridData: Array<Array<Grid.Cell>>) {
    fun newGrid(): Grid {
        return Grid(this.gridData.clone())
    }

    companion object Builder {
        fun from(gridData: Array<Array<Grid.Cell>>): GridProvider {
            return GridProvider(gridData)
        }

        fun from(inputStream: InputStream): GridProvider {
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            val header = bufferedReader.readLine()//header containing grid dimensions
            val rows = header.split(" ")[0].toString().trim().toInt()
            val columns = header.split(" ")[1].toString().trim().toInt()

            val gridDataRead = bufferedReader.lines().map { line ->
                var cells=line.map { c ->
                    when (c) {
                        '.' -> Grid.Cell.Empty
                        ' ' -> Grid.Cell.Empty
                        '#' -> Grid.Cell.Food
                        else -> throw Exception("Unexpected grid data character: $c")
                    }
                }.toMutableList()
                for(x in cells.size until columns){
                    cells.add(Grid.Cell.Empty)
                }
                cells.toTypedArray()
            }.toList()

            val gridData = gridDataRead.toMutableList()

            for(y in gridData.size until rows){
                var cells= mutableListOf<Grid.Cell>()
                for(x in 0 until columns){
                    cells.add(Grid.Cell.Empty)
                }
                gridData.add(cells.toTypedArray())
            }

            return GridProvider(gridData.toTypedArray())
        }
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

        /*EventDispatcher.dispatch(AntTrailFitnessEvaluationEvent(
            key = "ant-fitness-evaluation-start",
            details = mapOf(
                "position" to ant.position,
                "ant" to ant.toString()
            )
        ))*/

        program.execute()

        /*EventDispatcher.dispatch(AntTrailFitnessEvaluationEvent(
            key = "ant-fitness-evaluation-end",
            details = mapOf(
                "position" to ant.position,
                "ant" to ant.toString()
            )
        ))*/

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

            config.initialMinimumProgramLength = 3
            config.initialMaximumProgramLength = 40
            config.minimumProgramLength = 3
            config.maximumProgramLength = 60
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
            config.populationSize = 600
            config.generations = 400000
            config.microMutationRate = 0.8
            config.macroMutationRate = 0.2

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
                //BinaryTournamentSelection(environment, tournamentSize = 2)
                TournamentSelection(environment, tournamentSize = 8, numberOfOffspring = 5)
            },
            CoreModuleType.RecombinationOperator to { environment ->
                LinearCrossover(
                    environment,
                    maximumSegmentLength = 15,
                    maximumCrossoverDistance = 6,
                    maximumSegmentLengthDifference = 6
                )
            },
            CoreModuleType.MacroMutationOperator to { environment ->
                MacroMutationOperator(
                    environment,
                    insertionRate = 0.6,
                    deletionRate = 0.4
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
            val runner = SequentialTrainer(environment, model, runs = 10)

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
            val gridProvider = GridProvider.from(
                this::class.java.classLoader.getResourceAsStream("datasets/santafe.trl")
            )

            val problem = AntTrailProblem(
                maximumMoves = 400,
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