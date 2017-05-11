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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by dario on 19/1/17.
 */
public class DistributedISPrecisionHMM_2 {

    public static void main(String[] args) throws Exception {

        String sampleOutputFile = "/Users/dario/Desktop/salidaGaussian.csv";
        String sampleOutputFileGM = "/Users/dario/Desktop/salidaGaussianMixture.csv";

        File outputFile = new File(sampleOutputFile);
        outputFile.delete();

        outputFile = new File(sampleOutputFileGM);
        outputFile.delete();

        int sampleSizeIS;

        int nSamplesForLikelihood;

        ExpandedHiddenMarkovModel hmm = new ExpandedHiddenMarkovModel();
        hmm.setnTimeSteps(10);
        hmm.setnStates(3);
        hmm.setnGaussians(1);
        hmm.buildModel();

        System.out.println(hmm.getModel());

        Assignment hmm_evidence = hmm.generateEvidence();
        System.out.println("EVIDENCE: ");
        System.out.println(hmm_evidence.outputString(hmm.getVarsEvidence()));


        sampleSizeIS = 100000;
        nSamplesForLikelihood = 100000;


        double likelihood_Gaussian;
        double likelihood_GaussianMixture;



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
        varsOfInterest.add(bn.getVariables().getVariableByName("GaussianVar0_t" + (hmm.getnTimeSteps()-1 )));

        System.out.println("VARIABLE OF INTEREST: ");
        varsOfInterest.stream().forEach(var -> System.out.println(var.getName()));


        /*
         *  VARS IN THE EVIDENCE IN THE HMM MODEL (CONTINUOUS VARS EXCLUDING THE LAST TIME STEP)
         */

        Assignment evidence = hmm.generateEvidence();
        List<Variable> varsEvidence = hmm.getVarsEvidence();

        System.out.println("EVIDENCE: ");
        //evidence = new HashMapAssignment();
        System.out.println(evidence.outputString(varsEvidence));



        /**********************************************************************************
         *    EXPERIMENT 1: COMPARING LIKELIHOOD OF POSTERIOR DISTRIBUTIONS
         *********************************************************************************/


        Variable varOfInterest = varsOfInterest.get(0);
        int currentSampleSize = sampleSizeIS;


        /*
         *  OBTAINING POSTERIORS WITH DISTRIBUTED IMPORTANCE SAMPLING
         */
        ImportanceSamplingCLG_new impSamplingLocal = new ImportanceSamplingCLG_new();

        impSamplingLocal.setSeed(0);
        impSamplingLocal.setModel(bn);
        impSamplingLocal.setSampleSize(currentSampleSize);
        impSamplingLocal.setVariablesOfInterest(varsOfInterest);
        impSamplingLocal.setParallelMode(false);
        impSamplingLocal.setSaveSampleToFile(true, sampleOutputFile);
        impSamplingLocal.setEvidence(evidence);


        // OBTAIN THE POSTERIOR AS A SINGLE GAUSSIAN
        impSamplingLocal.setGaussianMixturePosteriors(false);
        impSamplingLocal.runInference();
        Normal varOfInterestGaussianDistribution = impSamplingLocal.getPosterior(varOfInterest);


        // OBTAIN THE POSTERIOR AS A GAUSSIAN MIXTURE AND THE QUERY RESULT
        impSamplingLocal.setSeed(0);
        impSamplingLocal.setGaussianMixturePosteriors(true);
        impSamplingLocal.setMixtureOfGaussiansInitialVariance(50);
        impSamplingLocal.setMixtureOfGaussiansNoveltyRate(0.005);
        impSamplingLocal.setSaveSampleToFile(true, sampleOutputFileGM);
        impSamplingLocal.runInference();
        GaussianMixture varOfInterestGaussianMixtureDistribution = impSamplingLocal.getPosterior(varOfInterest);


        impSamplingLocal.setSaveSampleToFile(false, "");


        Function<Double,Double> posteriorGaussianDensity = (Function<Double,Double> & Serializable) ( x -> -varOfInterestGaussianDistribution.getLogProbability(x));
        Function<Double,Double> posteriorGaussianMixtureDensity = (Function<Double,Double> & Serializable) ( x -> -varOfInterestGaussianMixtureDistribution.getLogProbability(x));


        /**********************************************************************************
         *    EXPERIMENT 1: COMPARING LIKELIHOOD OF POSTERIOR DISTRIBUTIONS
         *********************************************************************************/

        /*
         *  ESTIMATE LIKELIHOOD OF EACH POSTERIOR
         */
        impSamplingLocal.setSampleSize(nSamplesForLikelihood);
        impSamplingLocal.setGaussianMixturePosteriors(false);

        impSamplingLocal.setQuery(varOfInterest, posteriorGaussianDensity);
        impSamplingLocal.runInference();
        likelihood_Gaussian = impSamplingLocal.getQueryResult();

        impSamplingLocal.setQuery(varOfInterest, posteriorGaussianMixtureDensity);
        impSamplingLocal.runInference();
        likelihood_GaussianMixture = impSamplingLocal.getQueryResult();



        System.out.println("\n\nVar: " + varOfInterest.getName() + ", conditional=" + bn.getConditionalDistribution(varOfInterest));

        System.out.println("SAMPLE SIZE: " + sampleSizeIS);

        varOfInterestGaussianMixtureDistribution.sortByWeight();
        System.out.println("Gaussian posterior=        " + varOfInterestGaussianDistribution.toString());
        System.out.println("GaussianMixture posterior= " + varOfInterestGaussianMixtureDistribution.toString());

        System.out.println(varOfInterestGaussianDistribution.toStringRCode());
        System.out.println(varOfInterestGaussianMixtureDistribution.toStringRCode());

        System.out.println("Gaussian likelihood=         " + likelihood_Gaussian);
        System.out.println("GaussianMixture likelihood=  " + likelihood_GaussianMixture);

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
                probs_discreteHiddenVar_0[i] = 1.0/nStates;
            }

            double prob_keepState = 1.0/nStates;
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
                    means[i][j] = -50+100*random.nextDouble();
                    vars[i][j] = 5*random.nextDouble();
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


