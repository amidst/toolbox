import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.GaussianMixture;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.learning.parametric.bayesian.DataPosterior;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.inference.DistributedImportanceSamplingCLG;
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import org.apache.flink.api.java.DataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

            nSamplesForLikelihood = 100000;

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
         *  OBTAINING POSTERIORS WITH DISTRIBUTED VMP
         */

        eu.amidst.flinklink.core.utils.BayesianNetworkSampler bnSamplerFlink = new eu.amidst.flinklink.core.utils.BayesianNetworkSampler(bn);

        DataFlink<DataInstance> dataFlink = bnSamplerFlink.sampleToDataFlink(sampleSize);

        dVMP dvmp = new dVMP();
        dvmp.setDAG(bn.getDAG());
        dvmp.setOutput(false);

        dvmp.setSeed(236236);
        dvmp.setDataFlink(dataFlink);
        dvmp.runLearning();

        DataSet<DataPosterior> posterior = dvmp.computePosterior(varsOfInterestList);

        Normal varOfInterestGaussianDistributionDVMP = (Normal)posterior.collect().get(0).getPosterior(varOfInterest);



        /*
         *  HUGE SAMPLE FOR ESTIMATING THE LIKELIHOOD OF EACH POSTERIOR
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



    }
}
