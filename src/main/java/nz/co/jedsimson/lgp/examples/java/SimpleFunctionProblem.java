package nz.co.jedsimson.lgp.examples.java;

import kotlin.UninitializedPropertyAccessException;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import nz.co.jedsimson.lgp.core.environment.DefaultValueProvider;
import nz.co.jedsimson.lgp.core.environment.DefaultValueProviders;
import nz.co.jedsimson.lgp.core.environment.Environment;
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade;
import nz.co.jedsimson.lgp.core.environment.config.Configuration;
import nz.co.jedsimson.lgp.core.environment.config.ConfigurationLoader;
import nz.co.jedsimson.lgp.core.environment.constants.ConstantLoader;
import nz.co.jedsimson.lgp.core.environment.constants.GenericConstantLoader;
import nz.co.jedsimson.lgp.core.environment.dataset.*;
import nz.co.jedsimson.lgp.core.environment.operations.DefaultOperationLoader;
import nz.co.jedsimson.lgp.core.environment.operations.OperationLoader;
import nz.co.jedsimson.lgp.core.evolution.Description;
import nz.co.jedsimson.lgp.core.evolution.Problem;
import nz.co.jedsimson.lgp.core.evolution.ProblemNotInitialisedException;
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessContexts;
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunction;
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctions;
import nz.co.jedsimson.lgp.core.evolution.model.SteadyState;
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationOperator;
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.ConstantMutationFunctions;
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.MicroMutationOperator;
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.LinearCrossover;
import nz.co.jedsimson.lgp.core.evolution.operators.selection.TournamentSelection;
import nz.co.jedsimson.lgp.core.evolution.training.DistributedTrainer;
import nz.co.jedsimson.lgp.core.evolution.training.TrainingResult;
import nz.co.jedsimson.lgp.core.modules.*;
import nz.co.jedsimson.lgp.core.program.Outputs;
import nz.co.jedsimson.lgp.lib.base.BaseProgramOutputResolvers;
import nz.co.jedsimson.lgp.lib.generators.EffectiveProgramGenerator;
import nz.co.jedsimson.lgp.lib.generators.RandomInstructionGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * A re-implementation of {@link SimpleFunctionProblem} to showcase Java interoperability.
 */
public class SimpleFunctionProblem extends Problem<Double, Outputs.Single<Double>, Targets.Single<Double>> {

    @NotNull
    public String getName() {
        return "Simple Quadratic.";
    }

    @NotNull
    public Description getDescription() {
        return new Description(
            "f(x) = x^2 + 2x + 2\n\trange = [-10:10:0.5]"
        );
    }

    @NotNull
    public ConfigurationLoader getConfigLoader() {
        return new ConfigurationLoader() {

            @NotNull
            public ModuleInformation getInformation() {
                return new ModuleInformation(
                    "Overrides default configuration for this problem."
                );
            }

            public Configuration load() {
                Configuration config = new Configuration();

                config.setInitialMinimumProgramLength(10);
                config.setInitialMaximumProgramLength(30);
                config.setMinimumProgramLength(10);
                config.setMaximumProgramLength(200);
                config.setOperations(
                    Arrays.asList(
                        "nz.co.jedsimson.lgp.lib.operations.Addition",
                        "nz.co.jedsimson.lgp.lib.operations.Subtraction",
                        "nz.co.jedsimson.lgp.lib.operations.Multiplication"
                    )
                );
                config.setConstantsRate(0.5);
                config.setConstants(Arrays.asList("0.0", "1.0", "2.0"));
                config.setNumCalculationRegisters(4);
                config.setPopulationSize(500);
                config.setGenerations(1000);
                config.setNumFeatures(1);
                config.setMicroMutationRate(0.4);
                config.setMacroMutationRate(0.6);

                return config;
            }

        };
    }

    private Configuration config = this.getConfigLoader().load();

    @NotNull
    public ConstantLoader<Double> getConstantLoader() {
        return new GenericConstantLoader<>(
            this.config.getConstants(),
            Double::parseDouble
        );
    }

    @NotNull
    public OperationLoader<Double> getOperationLoader() {
        return new DefaultOperationLoader<>(
            this.config.getOperations()
        );
    }

    @NotNull
    public DefaultValueProvider<Double> getDefaultValueProvider() {
        return DefaultValueProviders.constantValueProvider(1.0);
    }

    @NotNull
    @Override
    public Function0<FitnessFunction<Double, Outputs.Single<Double>, Targets.Single<Double>>> getFitnessFunctionProvider() {
        return FitnessFunctions::getMSE;
    }

    @NotNull
    public ModuleContainer<Double, Outputs.Single<Double>, Targets.Single<Double>> getRegisteredModules() {
        HashMap<RegisteredModuleType, Function1<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>, Module>> modules = new HashMap<>();

        modules.put(CoreModuleType.InstructionGenerator, RandomInstructionGenerator::new);
        modules.put(
            CoreModuleType.ProgramGenerator,
            (environment) -> new EffectiveProgramGenerator<>(
                environment,
                1.0,                                           // sentinelTrueValue
                new ArrayList<>(Collections.singletonList(0)), // outputRegisterIndex
                BaseProgramOutputResolvers.INSTANCE.singleOutput()
            )
        );
        modules.put(
            CoreModuleType.SelectionOperator,
            (environment) -> new TournamentSelection<>(
                environment,
                2,            // tournamentSize
                10,           // numberOfOffspring
                true          // removeWinnersFromPopulation
            )
        );
        modules.put(
            CoreModuleType.RecombinationOperator,
            (environment) -> new LinearCrossover<>(
                environment,
                6,           // maximumSegmentLength
                5,           // maximumCrossoverDistance
                3            // maximumSegmentLengthDifference
            )
        );
        modules.put(
            CoreModuleType.MacroMutationOperator,
            (environment) -> new MacroMutationOperator<>(
                environment,
                0.67,        // insertionRate
                0.33         // deletionRate
            )
        );
        modules.put(
            CoreModuleType.MicroMutationOperator,
            (environment) -> new MicroMutationOperator<>(
                environment,
                0.5,         // registerMutationRate
                0.5,         // operatorMutationRate
                ConstantMutationFunctions.identity()
            )
        );
        modules.put(
            CoreModuleType.FitnessContext,
            FitnessContexts.SingleOutputFitnessContext::new
        );

        return new ModuleContainer<>(modules);
    }

    private DatasetLoader<Double, Targets.Single<Double>> datasetLoader = new DatasetLoader<Double, Targets.Single<Double>>() {

        private Function<Double, Double> func = (x) -> (x * x) + (2 * x) + 2;
        private SequenceGenerator gen = new SequenceGenerator();

        @NotNull
        @Override
        public ModuleInformation getInformation() {
            return new ModuleInformation("Generates samples in the range [-10:10:0.5].");
        }

        @Override
        public Dataset<Double, Targets.Single<Double>> load() {
            Iterator<Double> xs = this.gen.generate(-10.0, 10.0, 0.5, true).iterator();
            List<Sample<Double>> samples = new ArrayList<>();

            for (Iterator<Double> it = xs; it.hasNext(); ) {
                Double x = it.next();
                
                samples.add(
                    new Sample<>(Collections.singletonList(new Feature<>("x", x)))
                );
            }

            List<Targets.Single<Double>> ys = new ArrayList<>();

            for (Sample<Double> sample : samples) {
                Double x = sample.feature("x").getValue();

                ys.add(new Targets.Single<>(this.func.apply(x)));
            }

            return new Dataset<>(samples, ys);
        }
    };

    public void initialiseEnvironment() {
        this.environment = new Environment<>(
            this.getConfigLoader(),
            this.getConstantLoader(),
            this.getOperationLoader(),
            this.getDefaultValueProvider(),
            this.getFitnessFunctionProvider(),
            // resultAggregator
            null,
            // randomStateSeed
            null
        );

        this.environment.setModuleFactory(null);

        this.environment.registerModules(this.getRegisteredModules());
    }

    public void initialiseModel() {
        this.model = new SteadyState<>(this.environment);
    }

    @NotNull
    public SimpleFunctionSolution solve() {
        try {
            DistributedTrainer<Double, Outputs.Single<Double>, Targets.Single<Double>> runner = new DistributedTrainer<>(
                this.environment,
                this.model,
                // runs
                2
            );

            TrainingResult<Double, Outputs.Single<Double>, Targets.Single<Double>> result = runner.train(this.datasetLoader.load());

            return new SimpleFunctionSolution(this.getName(), result);

        } catch (UninitializedPropertyAccessException ex) {
            // The initialisation routines haven't been run.
            try {
                throw new ProblemNotInitialisedException(
                    "The initialisation routines for this problem must be run before it can be solved."
                );
            } catch (ProblemNotInitialisedException e) {
                e.printStackTrace();
            }
        }

        return new SimpleFunctionSolution(this.getName(), null);
    }
}