import eu.amidst.core.distribution.GaussianMixture;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.inference.DistributedImportanceSamplingCLG;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by dario on 19/1/17.
 */
public class DistributedISPrecision {

    public static void main(String[] args) throws Exception {

        int seedBN = 326762;
        int nDiscreteVars = 1000;
        int nContVars = 1000;

        int seedIS = 111235236;
        int sampleSize = 50000;

        int nSamplesForLikelihood = 100000;

        if (args.length!=6) {

            seedBN = 326762;
            nDiscreteVars = 1000;
            nContVars = 1000;

            seedIS = 111235236;
            sampleSize = 50000;

            nSamplesForLikelihood = 1000000;

        }
        else {

            seedBN = Integer.parseInt(args[0]);
            nDiscreteVars = Integer.parseInt(args[1]);
            nContVars = Integer.parseInt(args[2]);

            seedIS = Integer.parseInt(args[3]);
            sampleSize = Integer.parseInt(args[4]);

            nSamplesForLikelihood = Integer.parseInt(args[5]);

        }


        /**********************************************
         *    INITIALIZATION
         *********************************************/

        Assignment evidence = new HashMapAssignment();


        /*
         *  RANDOM GENERATION OF A BAYESIAN NETWORK
         */


        BayesianNetworkGenerator.setSeed(seedBN);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscreteVars, 2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(nContVars);
        BayesianNetworkGenerator.setNumberOfLinks( (int)2.5*(nDiscreteVars+nContVars));
        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();

        System.out.println(bn);


        /*
         *  RANDOM CHOICE OF A CONTINUOUS VARIABLE OF INTEREST
         */
        List<Variable> variableList = bn.getVariables().getListOfVariables();
        Variable varOfInterest = variableList.stream().filter(Variable::isNormal).skip(500).findAny().get();
        System.out.println("Variable of interest: " + varOfInterest.getName());

        List<Variable> varsOfInterestList = new ArrayList<>();
        varsOfInterestList.add(varOfInterest);


        /*
         *  RANDOM GENERATION OF AN EVIDENCE (EXCLUDING VARS OF INTEREST)
         */



        /**********************************************************************************
         *    EXPERIMENT 1: COMPARING LIKELIHOOD OF POSTERIOR DISTRIBUTIONS
         *********************************************************************************/


        /*
         *  OBTAINING POSTERIORS WITH DISTRIBUTED IMPORTANCE SAMPLING
         */

        DistributedImportanceSamplingCLG distributedIS = new DistributedImportanceSamplingCLG();

        distributedIS.setSeed(seedIS);
        distributedIS.setModel(bn);
        distributedIS.setSampleSize(sampleSize);
        distributedIS.setVariablesOfInterest(varsOfInterestList);


        // OBTAIN THE POSTERIOR AS A GAUSSIAN MIXTURE
        distributedIS.setGaussianMixturePosteriors(true);
        distributedIS.runInference();
        GaussianMixture varOfInterestGaussianMixtureDistribution = distributedIS.getPosterior(varOfInterest);

        // OBTAIN THE POSTERIOR AS A SINGLE GAUSSIAN
        distributedIS.setGaussianMixturePosteriors(false);
        distributedIS.runInference();
        Normal varOfInterestGaussianDistribution = distributedIS.getPosterior(varOfInterest);




        /*
         *  LARGE SAMPLE FOR ESTIMATING THE LIKELIHOOD OF EACH POSTERIOR
         */
        BayesianNetworkSampler bnSampler = new BayesianNetworkSampler(bn);
        bnSampler.setSeed(12552);
        Stream<Assignment> sample = bnSampler.sampleWithEvidence(nSamplesForLikelihood, evidence);
        double [] varOfInterestSample = sample.mapToDouble(assignment -> assignment.getValue(varOfInterest)).toArray();


        /*
         *  ESTIMATE LIKELIHOOD OF EACH POSTERIOR
         */
        System.out.println("Var: " + varOfInterest.getName() + ", conditional=" + bn.getConditionalDistribution(varOfInterest));

        System.out.println("Gaussian posterior=" + varOfInterestGaussianDistribution.toString());
        double averageLikelihoodGaussian = Arrays.stream(varOfInterestSample).map(sample1 -> Math.exp(varOfInterestGaussianDistribution.getLogProbability(sample1))).average().getAsDouble();
        System.out.println("Gaussian likelihood= " + averageLikelihoodGaussian);


        System.out.println("GaussianMixture posterior=" + varOfInterestGaussianMixtureDistribution.toString());
        double averageLikelihoodGaussianMixture = Arrays.stream(varOfInterestSample).map(sample1 -> Math.exp(varOfInterestGaussianMixtureDistribution.getLogProbability(sample1))).average().getAsDouble();
        System.out.println("GaussianMixture likelihood= " + averageLikelihoodGaussianMixture);



        /**********************************************************************************
         *    EXPERIMENT 2: COMPARING PROBABILITIES OF QUERIES
         *********************************************************************************/

        double a = -10000; // Lower endpoint of the interval
        double b = 10; // Upper endpoint of the interval

        final double finalA=a;
        final double finalB=b;

        distributedIS = new DistributedImportanceSamplingCLG();

        distributedIS.setSeed(seedIS);
        distributedIS.setModel(bn);
        distributedIS.setSampleSize(sampleSize);
        distributedIS.setVariablesOfInterest(varsOfInterestList);


        // OBTAIN THE POSTERIOR AS A GAUSSIAN MIXTURE
        distributedIS.setGaussianMixturePosteriors(true);
        distributedIS.setQuery(varOfInterest, (Function<Double,Double> & Serializable)(v -> (finalA < v && v < finalB) ? 1.0 : 0.0));

        distributedIS.runInference();


        double queryResult = distributedIS.getQueryResult();

        System.out.println("Var: " + varOfInterest.getName() + ", conditional=" + bn.getConditionalDistribution(varOfInterest));

        System.out.println("Gaussian posterior=" + varOfInterestGaussianDistribution.toString());
        System.out.println("Gaussian likelihood= " + averageLikelihoodGaussian);


        System.out.println("GaussianMixture posterior=" + varOfInterestGaussianMixtureDistribution.toString());
        System.out.println("GaussianMixture likelihood= " + averageLikelihoodGaussianMixture);

        System.out.println("Query: P(" + Double.toString(a) + " < " + varOfInterest.getName() + " < " + Double.toString(b) + ")");
        System.out.println("Probability result: " + queryResult);

        NormalDistribution auxNormal = new NormalDistributionImpl( varOfInterestGaussianDistribution.getMean(), varOfInterestGaussianDistribution.getSd() );
        double probGaussian = auxNormal.cumulativeProbability(finalB) - auxNormal.cumulativeProbability(finalA);

        double [] posteriorGaussianMixtureParameters = varOfInterestGaussianMixtureDistribution.getParameters();
        double probGaussianMixture=0;
        for (int i = 0; i < varOfInterestGaussianMixtureDistribution.getNumberOfComponents(); i++) {
            NormalDistribution auxNormal1 = new NormalDistributionImpl( posteriorGaussianMixtureParameters[1 + i*3], Math.sqrt(posteriorGaussianMixtureParameters[2 + i*3]) );
            probGaussianMixture += posteriorGaussianMixtureParameters[0 + i*3] * ( auxNormal1.cumulativeProbability(finalB) - auxNormal1.cumulativeProbability(finalA) );
        }

        System.out.println("Probability with posterior Gaussian: " + probGaussian);
        System.out.println("Probability with posterior Gaussian Mixture: " + probGaussianMixture);

        System.out.println("Probability estimate with large sample: " + Arrays.stream(varOfInterestSample).map(v -> (finalA < v && v < finalB) ? 1.0 : 0.0).sum()/varOfInterestSample.length);
    }
}
