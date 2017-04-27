package eu.amidst.impSampling2017;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.ImportanceSamplingCLG_new;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.inference.DistributedImportanceSamplingCLG;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by dario on 19/1/17.
 */
public class DistributedISPrecisionHMM {

    public static void main(String[] args) throws Exception {

        int seedBN;
        int nDiscreteVars;
        int nContVars;

        int seedIS;
        int minimumSampleSize;

        double evidenceVarsRatio;

        int nSamplesForLikelihood;

        ExpandedHiddenMarkovModel hmm = new ExpandedHiddenMarkovModel();
        hmm.setnTimeSteps(5);
        hmm.setnStates(5);
        hmm.setnGaussians(3);
        hmm.buildModel();

        System.out.println(hmm.getModel());

        Assignment hmm_evidence = hmm.generateEvidence();
        System.out.println("EVIDENCE: ");
        System.out.println(hmm_evidence.outputString(hmm.getVarsEvidence()));


        if (args.length!=7) {

            seedBN = 326762;
            nDiscreteVars = 500;
            nContVars = 500;

            seedIS = 111235236;
            minimumSampleSize = 1000;

            evidenceVarsRatio = 0.3;

            nSamplesForLikelihood = 10000;

        }
        else {

            seedBN = Integer.parseInt(args[0]);
            nDiscreteVars = Integer.parseInt(args[1]);
            nContVars = Integer.parseInt(args[2]);

            seedIS = Integer.parseInt(args[3]);
            minimumSampleSize = Integer.parseInt(args[4]);

            evidenceVarsRatio = Double.parseDouble(args[5]);

            nSamplesForLikelihood = Integer.parseInt(args[6]);

        }

        final int numberOfRepetitions = 3;
        final int numberOfSampleSizes = 3;

        int[] sampleSizes = new int[numberOfSampleSizes];
        sampleSizes[0]=minimumSampleSize;
        for (int i = 1; i < numberOfSampleSizes; i++) {
            sampleSizes[i]=10*sampleSizes[i-1];
        }

        final double link2VarsRatio = 2;

        System.out.println("\n\n\n");

        System.out.println("DISTRIBUTED IMPORTANCE SAMPLING PRECISION EXPERIMENTS");
        System.out.println("Parameters:");
        System.out.println("Bayesian Network with  " + nDiscreteVars + " discrete vars and " + nContVars + " continuous vars");
        System.out.println("(BN generated with seed " + seedBN + " and number of links " + (int) (link2VarsRatio*(nDiscreteVars + nContVars)) + ")");
        System.out.println();
        System.out.println("Ratio of variables in the evidence: " + evidenceVarsRatio);
        System.out.println();
        System.out.println("Seed for ImportanceSampling: " + seedIS);
        System.out.println("Sample sizes for IS: " + Arrays.toString(sampleSizes));
        System.out.println();
        System.out.println("Number of samples for estimating likelihood and actual probability of interval: " + nSamplesForLikelihood);

        System.out.println("\n\n\n");


        double[][] likelihood_Gaussian = new double[numberOfSampleSizes][numberOfRepetitions];
        double[][] likelihood_GaussianMixture = new double[numberOfSampleSizes][numberOfRepetitions];

//        double[][] probability_LargeSample = new double[numberOfSampleSizes][numberOfRepetitions];
//        double[][] probability_Query = new double[numberOfSampleSizes][numberOfRepetitions];
//        double[][] probability_Gaussian = new double[numberOfSampleSizes][numberOfRepetitions];
//        double[][] probability_GaussianMixture = new double[numberOfSampleSizes][numberOfRepetitions];


        for (int rep = 0; rep < numberOfRepetitions; rep++) {

            /**********************************************
             *    INITIALIZATION
             *********************************************/

            /*
             *  HIDDEN MARKOV MODEL
             */
            BayesianNetwork bn = hmm.getModel();
            System.out.println(bn);


            /*
             *  CHOOSE VARIABLES OF INTEREST FOR HMM: LAST TIME STEP CONTINUOUS VARS
             */

            List<Variable> varsOfInterest = new ArrayList<>();
            varsOfInterest.add(bn.getVariables().getVariableByName("discreteHiddenVar_t4"));

            System.out.println("VARIABLE OF INTEREST: ");
            varsOfInterest.stream().forEach(var -> System.out.println(var.getName()));


            /*
             *  VARS IN THE EVIDENCE IN THE HMM MODEL (CONTINUOUS VARS EXCLUDING THE LAST TIME STEP)
             */

            Assignment evidence = hmm.generateEvidence();
            List<Variable> varsEvidence = hmm.getVarsEvidence();

            //evidence = new HashMapAssignment();
            System.out.println("EVIDENCE: ");
            //System.out.println(evidence.outputString(varsEvidence));



             /*
             *  LARGE SAMPLE FOR ESTIMATING THE LIKELIHOOD OF EACH POSTERIOR
             */
//            BayesianNetworkSampler bnSampler = new BayesianNetworkSampler(bn);
//            bnSampler.setSeed(12552);
//            Stream<Assignment> sample = bnSampler.sampleWithEvidence(nSamplesForLikelihood, evidence);
//            double[] varOfInterestSample = sample.mapToDouble(assignment -> assignment.getValue(varOfInterest)).toArray();



            /**********************************************************************************
             *    EXPERIMENT 1: COMPARING LIKELIHOOD OF POSTERIOR DISTRIBUTIONS
             *********************************************************************************/

            for (int ss = 0; ss < numberOfSampleSizes; ss++) {

                Variable varOfInterest = varsOfInterest.get(0);

                int currentSampleSize = sampleSizes[ss];

                /*
                 *  OBTAINING POSTERIORS WITH DISTRIBUTED IMPORTANCE SAMPLING
                 */
                DistributedImportanceSamplingCLG distributedIS = new DistributedImportanceSamplingCLG();

                distributedIS.setSeed(seedIS+ss+rep);
                distributedIS.setModel(bn);
                distributedIS.setSampleSize(currentSampleSize);
                distributedIS.setVariablesOfInterest(varsOfInterest);

                distributedIS.setEvidence(evidence);

                // OBTAIN THE POSTERIOR AS A SINGLE GAUSSIAN
                distributedIS.setGaussianMixturePosteriors(false);

                distributedIS.runInference();
                Multinomial varOfInterestGaussianDistribution = distributedIS.getPosterior(varOfInterest);


                // OBTAIN THE POSTERIOR AS A GAUSSIAN MIXTURE
                distributedIS.setGaussianMixturePosteriors(true);
                distributedIS.setMixtureOfGaussiansInitialVariance(5);

                // AND ALSO QUERY THE PROBABILITY OF THE VARIABLE BEING IN A CERTAIN INTERVAL
//
//                double a = -3; // Lower endpoint of the interval
//                double b = +3; // Upper endpoint of the interval
//
//                final double finalA = a;
//                final double finalB = b;
//                distributedIS.setQuery(varOfInterest, (Function<Double, Double> & Serializable) (v -> (finalA < v && v < finalB) ? 1.0 : 0.0));


                distributedIS.runInference();

                // GET THE POSTERIOR AS A GAUSSIANMIXTURE AND THE QUERY RESULT

//                double probQuery = distributedIS.getQueryResult();
                Multinomial varOfInterestGaussianMixtureDistribution = distributedIS.getPosterior(varOfInterest);


                Function<Double,Double> posteriorGaussianDensity = (Function<Double,Double> & Serializable) varOfInterestGaussianDistribution::getProbability;
                Function<Double,Double> posteriorGaussianMixtureDensity = (Function<Double,Double> & Serializable) varOfInterestGaussianMixtureDistribution::getProbability;


                /**********************************************************************************
                 *    EXPERIMENT 1: COMPARING LIKELIHOOD OF POSTERIOR DISTRIBUTIONS
                 *********************************************************************************/

                /*
                 *  ESTIMATE LIKELIHOOD OF EACH POSTERIOR
                 */
                distributedIS.setSampleSize(nSamplesForLikelihood);
                distributedIS.setGaussianMixturePosteriors(false);

                distributedIS.setQuery(varOfInterest, posteriorGaussianDensity);
                distributedIS.runInference();
                double averageLikelihoodGaussian = distributedIS.getQueryResult();

                distributedIS.setQuery(varOfInterest, posteriorGaussianMixtureDensity);
                distributedIS.runInference();
                double averageLikelihoodGaussianMixture = distributedIS.getQueryResult();




                /*
                 *  ESTIMATE LIKELIHOOD OF EACH POSTERIOR
                 */
                ImportanceSamplingCLG_new importanceSamplingCLG_new = new ImportanceSamplingCLG_new();
                importanceSamplingCLG_new.setModel(bn);
                importanceSamplingCLG_new.setSeed(seedIS+ss+rep);
                importanceSamplingCLG_new.setSampleSize(currentSampleSize);
                importanceSamplingCLG_new.setSampleSize(nSamplesForLikelihood);
                importanceSamplingCLG_new.setGaussianMixturePosteriors(true);
                importanceSamplingCLG_new.setVariablesOfInterest(varsOfInterest);

                importanceSamplingCLG_new.setEvidence(evidence);

                importanceSamplingCLG_new.setQuery(varOfInterest, posteriorGaussianDensity);
                importanceSamplingCLG_new.runInference();
                double averageLikelihoodGaussianCLG = distributedIS.getQueryResult();


//                double averageLikelihoodGaussian = Arrays.stream(varOfInterestSample).map(sample1 -> Math.exp(varOfInterestGaussianDistribution.getLogProbability(sample1))).average().getAsDouble();
//                double averageLikelihoodGaussianMixture = Arrays.stream(varOfInterestSample).map(sample1 -> Math.exp(varOfInterestGaussianMixtureDistribution.getLogProbability(sample1))).average().getAsDouble();

                //System.out.println("\n\nVar: " + varOfInterest.getName() + ", conditional=" + bn.getConditionalDistribution(varOfInterest));
                //System.out.println("Gaussian posterior=" + varOfInterestGaussianDistribution.toString());
                //System.out.println("Gaussian likelihood= " + averageLikelihoodGaussian);
                //System.out.println("GaussianMixture posterior=" + varOfInterestGaussianMixtureDistribution.toString());
                //System.out.println("GaussianMixture likelihood= " + averageLikelihoodGaussianMixture);


                System.out.println("\n\nVar: " + varOfInterest.getName() + ", conditional=" + bn.getConditionalDistribution(varOfInterest));

                System.out.println("Gaussian posterior=        " + varOfInterestGaussianDistribution.toString());
                System.out.println("GaussianMixture posterior= " + varOfInterestGaussianMixtureDistribution.toString());


                System.out.println("Gaussian likelihood=         " + averageLikelihoodGaussian);
                System.out.println("GaussianMixture likelihood=  " + averageLikelihoodGaussianCLG);





//                /**********************************************************************************
//                 *    EXPERIMENT 2: COMPARING PROBABILITIES OF QUERIES
//                 *********************************************************************************/
//
//                NormalDistribution auxNormal = new NormalDistributionImpl(varOfInterestGaussianDistribution.getMean(), varOfInterestGaussianDistribution.getSd());
//                double probGaussian = auxNormal.cumulativeProbability(finalB) - auxNormal.cumulativeProbability(finalA);
//
//                double[] posteriorGaussianMixtureParameters = varOfInterestGaussianMixtureDistribution.getParameters();
//                double probGaussianMixture = 0;
//                for (int i = 0; i < varOfInterestGaussianMixtureDistribution.getNumberOfComponents(); i++) {
//                    NormalDistribution auxNormal1 = new NormalDistributionImpl(posteriorGaussianMixtureParameters[1 + i * 3], Math.sqrt(posteriorGaussianMixtureParameters[2 + i * 3]));
//                    probGaussianMixture += posteriorGaussianMixtureParameters[0 + i * 3] * (auxNormal1.cumulativeProbability(finalB) - auxNormal1.cumulativeProbability(finalA));
//                }
//
//                System.out.println("Query: P(" + Double.toString(a) + " < " + varOfInterest.getName() + " < " + Double.toString(b) + ")");
//                System.out.println("Probability estimate with query:            " + probQuery);
//
//                System.out.println("Probability with posterior Gaussian:        " + probGaussian);
//                System.out.println("Probability with posterior GaussianMixture: " + probGaussianMixture);
//
//                double probLargeSample = Arrays.stream(varOfInterestSample).map(v -> (finalA < v && v < finalB) ? 1.0 : 0.0).sum() / varOfInterestSample.length;
//                System.out.println("Probability estimate with large sample:     " + probLargeSample);


                /**********************************************************************************
                 *    STORE THE RESULTS
                 *********************************************************************************/

                likelihood_Gaussian[ss][rep] = averageLikelihoodGaussian;
                likelihood_GaussianMixture[ss][rep] = averageLikelihoodGaussianMixture;


//                probability_Query[ss][rep] = probQuery;
//                probability_Gaussian[ss][rep] = probGaussian;
//                probability_GaussianMixture[ss][rep] = probGaussianMixture;
//                probability_LargeSample[ss][rep] = probLargeSample;

//            distributedIS = new DistributedImportanceSamplingCLG();
//
//            distributedIS.setSeed(seedIS);
//            distributedIS.setModel(bn);
//            distributedIS.setSampleSize(currentSampleSize);
//            distributedIS.setVariablesOfInterest(varsOfInterestList);
//
//
//            distributedIS.setGaussianMixturePosteriors(true);
//            distributedIS.setQuery(varOfInterest, (Function<Double, Double> & Serializable) (v -> (finalA < v && v < finalB) ? 1.0 : 0.0));
//            double probQuery = distributedIS.getQueryResult();
//            distributedIS.runInference();
//
//
//            double probQuery = distributedIS.getQueryResult();

            }
        }

        /**********************************************************************************
         *    SUMMARIZE AND SHOW THE RESULTS
         *********************************************************************************/

        for (int ss = 0; ss < numberOfSampleSizes; ss++) {
            System.out.println("SAMPLE SIZE: " + sampleSizes[ss]);

            System.out.println("Likelihood Gaussians:        " + Arrays.toString(likelihood_Gaussian[ss]));
            System.out.println("Likelihood GaussianMixtures: " + Arrays.toString(likelihood_GaussianMixture[ss]));

            System.out.println("Mean Likelihood Gaussians:        " + Arrays.stream(likelihood_Gaussian[ss]).average().getAsDouble());
            System.out.println("Mean Likelihood GaussianMixtures: " + Arrays.stream(likelihood_GaussianMixture[ss]).average().getAsDouble());

//            System.out.println("Prob Large sample:    " + Arrays.toString(probability_LargeSample[ss]));
//            System.out.println("Prob Query:           " + Arrays.toString(probability_Query[ss]));
//            System.out.println("Prob Gaussian:        " + Arrays.toString(probability_Gaussian[ss]));
//            System.out.println("Prob GaussianMixture: " + Arrays.toString(probability_GaussianMixture[ss]));

        }


    }

    private static class ExpandedHiddenMarkovModel {

        private int nStates = 2;
        private int nGaussians = 1;

        private int nTimeSteps = 10;

        private Variables variables;
        private DAG dag;
        private BayesianNetwork bn;

        private Random random;
        private List<Variable> varsEvidence;
        private List<Variable> varsInterest;


        public void setnStates(int nStates) {
            this.nStates = nStates;
        }

        public void setnGaussians(int nGaussians) {
            this.nGaussians = nGaussians;
        }

        public void setnTimeSteps(int nTimeSteps) {
            this.nTimeSteps = nTimeSteps;
        }

        public int getnTimeSteps() {
            return nTimeSteps;
        }

        public int getnGaussians() {
            return nGaussians;
        }

        public int getnStates() {
            return nStates;
        }

        public List<Variable> getVarsEvidence() {
            return varsEvidence;
        }

        public List<Variable> getVarsInterest() {
            return varsInterest;
        }

        public BayesianNetwork getModel() {
            return bn;
        }

        public ExpandedHiddenMarkovModel() {
            this.variables = new Variables();
        }

        public void buildModel() {

            random = new Random(0);

            double[] probs_discreteHiddenVar_0 = new double[nStates];
            for (int i = 0; i < nStates; i++) {
                probs_discreteHiddenVar_0[i]=(double)1/nStates;
            }

            double prob_keepState = 0.8;
            double[][] probs_discreteHiddenVars = new double[nStates][nStates];
            for (int i = 0; i < nStates; i++) {
                for (int j = 0; j < nStates; j++) {
                    if(i==j) {
                        probs_discreteHiddenVars[i][j] = prob_keepState;
                    }
                    else {
                        probs_discreteHiddenVars[i][j] = (1-prob_keepState)/(nStates-1);
                    }
                }
            }

            double[][] means = new double[nGaussians][nStates];
            double[][] vars= new double[nGaussians][nStates];

            for (int i = 0; i < nGaussians; i++) {
                for (int j = 0; j < nStates; j++) {
                    means[i][j] = -10+20*random.nextDouble();
                    vars[i][j] = random.nextDouble()+2;
                }
            }


            for (int i = 0; i < nTimeSteps; i++) {
                Variable discreteHiddenVar = this.variables.newMultionomialVariable("discreteHiddenVar_t" + i, this.getnStates());
                for (int j = 0; j < nGaussians; j++) {
                    Variable gaussian = this.variables.newGaussianVariable("GaussianVar" + j + "_t" + i);
                }
            }

            dag = new DAG(this.variables);

            Variable discreteHiddenVar0 = this.dag.getVariables().getVariableByName("discreteHiddenVar_t0");

            for (int i = 0; i < nTimeSteps; i++) {

                Variable discreteHiddenVar_current = this.variables.getVariableByName("discreteHiddenVar_t" + i);

                if(i>0) {
                    Variable discreteHiddenVar_previous = this.variables.getVariableByName("discreteHiddenVar_t" + (i - 1));
                    dag.getParentSet(discreteHiddenVar_current).addParent(discreteHiddenVar_previous);
                }

                for (int j = 0; j < nGaussians; j++) {
                    Variable gaussian = this.variables.getVariableByName("GaussianVar" + j + "_t" + i);
                    dag.getParentSet(gaussian).addParent(discreteHiddenVar_current);
                }
            }

            bn = new BayesianNetwork(dag);
            bn.randomInitialization(random);

            ConditionalDistribution cdist = bn.getConditionalDistribution(discreteHiddenVar0);
            ((Multinomial) cdist).setProbabilities(probs_discreteHiddenVar_0);

            for (int i = 0; i < nTimeSteps; i++) {

                if(i>0) {
                    Variable discreteHiddenVar_current = this.variables.getVariableByName("discreteHiddenVar_t" + i);
                    cdist = bn.getConditionalDistribution(discreteHiddenVar_current);
                    for (int k = 0; k < nStates; k++) {
                        Multinomial component = ((Multinomial_MultinomialParents) cdist).getMultinomial(k);
                        component.setProbabilities(probs_discreteHiddenVars[k]);
                    }
                }

                for (int j = 0; j < nGaussians; j++) {
                    Variable gaussian = this.variables.getVariableByName("GaussianVar" + j + "_t" + i);
                    cdist = bn.getConditionalDistribution(gaussian);

                    for (int k = 0; k < nStates; k++) {
                        ((Normal_MultinomialParents) cdist).getNormal(k).setMean(means[j][k]);
                        ((Normal_MultinomialParents) cdist).getNormal(k).setVariance(vars[j][k]);
                    }
                }
            }

            varsEvidence = this.bn.getVariables().getListOfVariables()
                    .stream()
                    .filter(var -> !var.getName().contains("_t" + (nTimeSteps-1)) && !var.getName().contains("_t" + (nTimeSteps-2)) && var.isNormal())
                    .collect(Collectors.toList());

            varsInterest = this.bn.getVariables().getListOfVariables()
                    .stream()
                    .filter(var -> ( var.getName().contains("_t" + (nTimeSteps-1)) || var.getName().contains("_t" + (nTimeSteps-2)) ) && var.isNormal())
                    .collect(Collectors.toList());
        }

        public Assignment generateEvidence() {


            BayesianNetworkSampler bayesianNetworkSampler = new BayesianNetworkSampler(bn);
            bayesianNetworkSampler.setSeed(random.nextInt());
            DataStream<DataInstance> fullSample = bayesianNetworkSampler.sampleToDataStream(1);

            Assignment evidence = new HashMapAssignment(varsEvidence.size());
            varsEvidence.forEach(variable -> evidence.setValue(variable, fullSample.stream().findFirst().get().getValue(variable)));

            return evidence;
        }
    }
}


