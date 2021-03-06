package nz.co.jedsimson.lgp.examples.kotlin

import kotlinx.coroutines.runBlocking
import nz.co.jedsimson.lgp.core.environment.DefaultValueProviders
import nz.co.jedsimson.lgp.core.environment.Environment
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.config.ConfigurationLoader
import nz.co.jedsimson.lgp.core.environment.constants.GenericConstantLoader
import nz.co.jedsimson.lgp.core.environment.dataset.*
import nz.co.jedsimson.lgp.core.environment.operations.DefaultOperationLoader
import nz.co.jedsimson.lgp.core.evolution.*
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessContexts
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctions
import nz.co.jedsimson.lgp.core.evolution.model.SteadyState
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.ConstantMutationFunctions
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.MicroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.LinearCrossover
import nz.co.jedsimson.lgp.core.evolution.operators.selection.BinaryTournamentSelection
import nz.co.jedsimson.lgp.core.evolution.training.DistributedTrainer
import nz.co.jedsimson.lgp.core.evolution.training.TrainingResult
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleContainer
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.lib.base.BaseProgram
import nz.co.jedsimson.lgp.lib.base.BaseProgramOutputResolvers
import nz.co.jedsimson.lgp.lib.base.BaseProgramSimplifier
import nz.co.jedsimson.lgp.lib.generators.EffectiveProgramGenerator
import nz.co.jedsimson.lgp.lib.generators.RandomInstructionGenerator
import nz.co.jedsimson.lgp.core.environment.events.*
import java.io.File

/*
 * An example of setting up an environment to use LGP to find programs for the function `x^2 + 2x + 2`.
 *
 * This example serves as a good way to learn how to use the system and to ensure that everything
 * is working correctly, as some percentage of the time, perfect individuals should be found.
 */

// A solution for this problem consists of the problem's name and a result from
// running the problem with a `Trainer` impl.
data class SimpleFunctionSolution(
        override val problem: String,
        val result: TrainingResult<Double, Outputs.Single<Double>, Targets.Single<Double>>
) : Solution<Double>

// Define the problem and the necessary components to solve it.
class SimpleFunctionProblem : Problem<Double, Outputs.Single<Double>, Targets.Single<Double>>() {
    override val name = "Simple Quadratic."

    override val description = Description("f(x) = x^2 + 2x + 2\n\trange = [-10:10:0.5]")

    override val configLoader = object : ConfigurationLoader {
        override val information = ModuleInformation("Overrides default configuration for this problem.")

        override fun load(): Configuration {
            val config = Configuration()

            config.initialMinimumProgramLength = 10
            config.initialMaximumProgramLength = 30
            config.minimumProgramLength = 10
            config.maximumProgramLength = 200
            config.operations = listOf(
                    "nz.co.jedsimson.lgp.lib.operations.Addition",
                    "nz.co.jedsimson.lgp.lib.operations.Subtraction",
                    "nz.co.jedsimson.lgp.lib.operations.Multiplication"
            )
            config.constantsRate = 0.5
            config.constants = listOf("0.0", "1.0", "2.0")
            config.numCalculationRegisters = 4
            config.populationSize = 500
            config.generations = 1000
            config.numFeatures = 1
            config.microMutationRate = 0.4
            config.macroMutationRate = 0.6

            return config
        }
    }

    private val config = this.configLoader.load()

    override val constantLoader = GenericConstantLoader(
            constants = config.constants,
            parseFunction = String::toDouble
    )

    val datasetLoader = object : DatasetLoader<Double, Targets.Single<Double>> {
        // x^2 + 2x + 2
        val func = { x: Double -> (x * x) + (2 * x) + 2 }
        val gen = SequenceGenerator()

        override val information = ModuleInformation("Generates samples in the range [-10:10:0.5].")

        override fun load(): Dataset<Double, Targets.Single<Double>> {
            val xs = gen.generate(-10.0, 10.0, 0.5, inclusive = true).map { x ->
                Sample(
                    listOf(Feature(name = "x", value = x))
                )
            }

            val ys = xs.map { x ->
                Targets.Single(this.func(x.features[0].value))
            }

            return Dataset(
                    xs.toList(),
                    ys.toList()
            )
        }
    }

    override val operationLoader = DefaultOperationLoader<Double>(
            operationNames = config.operations
    )

    override val defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)

    override val fitnessFunctionProvider = {
        FitnessFunctions.MSE
    }

    override val registeredModules = ModuleContainer<Double, Outputs.Single<Double>, Targets.Single<Double>>(
            modules = mutableMapOf(
                    CoreModuleType.InstructionGenerator to { environment ->
                        RandomInstructionGenerator(environment)
                    },
                    CoreModuleType.ProgramGenerator to { environment ->
                        EffectiveProgramGenerator(
                            environment,
                            sentinelTrueValue = 1.0,
                            outputRegisterIndices = listOf(0),
                            outputResolver = BaseProgramOutputResolvers.singleOutput()
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
                            // Use identity func. since the probabilities
                            // of other micro mutations mean that we aren't
                            // modifying constants.
                            constantMutationFunc = ConstantMutationFunctions.identity<Double>()
                        )
                    },
                    CoreModuleType.FitnessContext to { environment ->
                        FitnessContexts.SingleOutputFitnessContext(environment)
                    }
            )
    )

    override fun initialiseEnvironment() {
        this.environment = Environment(
            this.configLoader,
            this.constantLoader,
            this.operationLoader,
            this.defaultValueProvider,
            this.fitnessFunctionProvider,
            ResultAggregators.BufferedResultAggregator(
                ResultOutputProviders.CsvResultOutputProvider("results.csv")
            )
        )

        this.environment.registerModules(this.registeredModules)
    }

    override fun initialiseModel() {
        this.model = SteadyState(this.environment)
    }

    override fun solve(): SimpleFunctionSolution {
        try {
            /*
            // This is an example of training sequentially in an asynchronous manner.
            val runner = SequentialTrainer(environment, model, runs = 2)

            return runBlocking {
                val job = runner.trainAsync(
                    this@SimpleFunctionProblem.datasetLoader.load()
                )

                job.subscribeToUpdates { println("training progress = ${it.progress}%") }

                val result = job.result()

                SimpleFunctionSolution(this@SimpleFunctionProblem.name, result)
            }
            */

            val traceEvents = mutableListOf<DiagnosticEvent.Trace>()

            EventRegistry.register(object : EventListener<DiagnosticEvent.Trace> {
                override fun handle(event: DiagnosticEvent.Trace) {
                    traceEvents += event
                }
            })

            val runner = DistributedTrainer(environment, model, runs = 2)

            return runBlocking {
                val job = runner.trainAsync(
                    this@SimpleFunctionProblem.datasetLoader.load()
                )

                job.subscribeToUpdates { println("training progress = ${it.progress}") }

                val result = job.result()

                SimpleFunctionSolution(this@SimpleFunctionProblem.name, result)
            }

        } catch (ex: UninitializedPropertyAccessException) {
            // The initialisation routines haven't been run.
            throw ProblemNotInitialisedException(
                    "The initialisation routines for this problem must be run before it can be solved."
            )
        }
    }
}

class SimpleFunction {
    companion object Main {
        @JvmStatic fun main(args: Array<String>) {
            System.setProperty("LGP.LogLevel", "debug")

            // Create a new problem instance, initialise it, and then solve it.
            val problem = SimpleFunctionProblem()
            problem.initialiseEnvironment()
            problem.initialiseModel()
            val solution = problem.solve()
            val simplifier = BaseProgramSimplifier<Double, Outputs.Single<Double>>()

            println("Results:")

            solution.result.evaluations.forEachIndexed { run, res ->
                println("Run ${run + 1} (best fitness = ${res.best.fitness})")
                println(simplifier.simplify(res.best as BaseProgram<Double, Outputs.Single<Double>>))

                println("\nStats (last run only):\n")

                for ((k, v) in res.statistics.last().data) {
                    println("$k = $v")
                }
                println("")
            }

            val avgBestFitness = solution.result.evaluations.map { eval ->
                eval.best.fitness
            }.sum() / solution.result.evaluations.size

            println("Average best fitness: $avgBestFitness")
        }
    }
}

