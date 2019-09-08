package nz.co.jedsimson.lgp.examples.java;

import nz.co.jedsimson.lgp.core.environment.dataset.Targets;
import nz.co.jedsimson.lgp.core.evolution.Problem;
import nz.co.jedsimson.lgp.core.program.Outputs;
import nz.co.jedsimson.lgp.core.evolution.model.EvolutionResult;
import nz.co.jedsimson.lgp.lib.base.BaseProgram;
import nz.co.jedsimson.lgp.lib.base.BaseProgramSimplifier;

import java.util.List;
import java.util.Map;

/**
 * A re-implementation of {@link SimpleFunction} to showcase Java interoperability.
 */
public class SimpleFunction {

    public static void main(String[] args) {
        Problem<Double, Outputs.Single<Double>, Targets.Single<Double>> problem = new SimpleFunctionProblem();

        problem.initialiseEnvironment();
        problem.initialiseModel();

        SimpleFunctionSolution solution = (SimpleFunctionSolution) problem.solve();
        BaseProgramSimplifier<Double, Outputs.Single<Double>> simplifier = new BaseProgramSimplifier<>();

        System.out.println("Results:");
        int run = 0;
        double sum = 0.0;
        List<EvolutionResult<Double, Outputs.Single<Double>>> evaluations = solution.getResult().getEvaluations();

        for (EvolutionResult<Double, Outputs.Single<Double>> evaluation : evaluations) {
            sum += evaluation.getBest().getFitness();

            System.out.println("Run " + (run++ + 1) + " (best fitness = " + evaluation.getBest().getFitness() + ")");
            System.out.println(simplifier.simplify((BaseProgram<Double, Outputs.Single<Double>>) evaluation.getBest()));

            System.out.println("\nStats (last run only):\n");

            int last = evaluation.getStatistics().size() - 1;

            for (Map.Entry<String, Object> datum : evaluation.getStatistics().get(last).getData().entrySet()) {
                System.out.println(datum.getKey() + " = " + datum.getValue());
            }

            System.out.println();
        }

        double averageBestFitness = sum / ((double) solution.getResult().getEvaluations().size());

        System.out.println("Average best fitness: " + averageBestFitness);
    }
}