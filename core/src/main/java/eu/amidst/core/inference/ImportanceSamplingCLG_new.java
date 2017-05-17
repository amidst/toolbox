/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.inference;

import eu.amidst.core.distribution.GaussianMixture;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.*;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * This class implements the interface {@link InferenceAlgorithm} and defines the Importance Sampling algorithm.
 * J.M. Hammersley and D.C. Handscomb. Monte Carlo Methods. Methuen and Co, London, UK, 1964.
 *
 * <p> For an example of use follow this link
 * <a href="http://amidst.github.io/toolbox/CodeExamples.html#isexample"> http://amidst.github.io/toolbox/CodeExamples.html#isexample </a>  </p>
 */
public class ImportanceSamplingCLG_new extends ImportanceSampling {

    private static final long serialVersionUID = 3697326793626972639L;

    private List<Variable> variablesAPosteriori;
    private List<Variable> variablesAPosteriori_multinomials;
    private List<Variable> variablesAPosteriori_normals;

    private List<SufficientStatistics> SSOfMultinomialVariables;
    private List<List<StreamingUpdateableGaussianMixture>> SSOfGaussianMixtureVariables;
    private List<List<StreamingUpdateableRobustNormal>> SSOfNormalVariables;

    //private List<StreamingUpdateableRobustNormal> SSOfNormalVariables;


    //private double logProbOfEvidence;

    private boolean parallelMode = true;
    private boolean useGaussianMixtures = false;

    private double mixtureOfGaussiansInitialVariance = 50;
    private double mixtureOfGaussiansNoveltyRate = 0.0001;

    private Function<Double,Double> queryingFunction;
    private Variable queryingVariable;
    private double queryResult = Double.NEGATIVE_INFINITY;

    private boolean saveSampleToFile = false;
    private String saveToFilePath = "";



    private class StreamingUpdateableRobustNormal {

        Variable variable;

//
//        private double log_sum_weights = 0;
//        private double log_sum_positive_terms = 0;
//        private double log_sum_negative_terms = 0;
//        private double log_sum_positive_terms_sq = 0;
//        private double log_sum_negative_terms_sq = 0;

        private double log_sum_weights = Double.NEGATIVE_INFINITY;
        private double log_sum_positive_terms = Double.NEGATIVE_INFINITY;
        private double log_sum_negative_terms = Double.NEGATIVE_INFINITY;
        private double log_sum_positive_terms_sq = Double.NEGATIVE_INFINITY;
        private double log_sum_negative_terms_sq = Double.NEGATIVE_INFINITY;

        public StreamingUpdateableRobustNormal(Variable var) {
            this.variable=var;
        }

        public void update(double dataPoint, double logWeight) {

            double logThisTerm, logThisTerm_sq;
            boolean output=false;

            if(output) {
                System.out.println("NUEVO DATO: " + dataPoint + ", CON PESO: " + logWeight);
            }

            if(dataPoint>0) {
                logThisTerm = logWeight + Math.log(dataPoint);
                log_sum_positive_terms = RobustOperations.robustSumOfLogarithms(log_sum_positive_terms, logThisTerm);

                logThisTerm_sq = logWeight + 2 * Math.log(dataPoint);
                log_sum_positive_terms_sq = RobustOperations.robustSumOfLogarithms(log_sum_positive_terms_sq, logThisTerm_sq);
            }
            else {
                logThisTerm = logWeight + Math.log(-dataPoint);
                log_sum_negative_terms = RobustOperations.robustSumOfLogarithms(log_sum_negative_terms, logThisTerm);

                logThisTerm_sq = logWeight + 2 * Math.log(-dataPoint);
                log_sum_negative_terms_sq = RobustOperations.robustSumOfLogarithms(log_sum_negative_terms_sq, logThisTerm_sq);
            }

            log_sum_weights = (log_sum_weights == Double.NEGATIVE_INFINITY ? logWeight : RobustOperations.robustSumOfLogarithms(log_sum_weights, logWeight));

            if(output) {
                System.out.println("log_sum_p: " + log_sum_positive_terms + ", log_sum_n:" + log_sum_negative_terms);
                System.out.println("log_sum_p_sq: " + log_sum_positive_terms_sq + ", log_sum_n_sq:" + log_sum_negative_terms_sq);
                System.out.println("log_sum_w: " + log_sum_weights);

            }

            if(Double.isNaN(log_sum_positive_terms) || Double.isNaN(log_sum_negative_terms) || Double.isNaN(log_sum_negative_terms_sq) || Double.isNaN(log_sum_positive_terms_sq) || Double.isNaN(log_sum_weights) ) {
                System.out.println("log_sum_p: " + log_sum_positive_terms + ", log_sum_n:" + log_sum_negative_terms);
                System.out.println("log_sum_p_sq: " + log_sum_positive_terms_sq + ", log_sum_n_sq:" + log_sum_negative_terms_sq);
                System.out.println("log_sum_w: " + log_sum_weights);
            }
        }

        public Normal getNormal() {
            double logSumTerms, logSumTerms_sq;
            double meanEstimate, varEstimate;

            if(log_sum_positive_terms > log_sum_negative_terms) {
                logSumTerms = RobustOperations.robustDifferenceOfLogarithms(log_sum_positive_terms, log_sum_negative_terms);
                meanEstimate =   Math.exp( logSumTerms - log_sum_weights );
            } else {
                logSumTerms = RobustOperations.robustDifferenceOfLogarithms(log_sum_negative_terms, log_sum_positive_terms);
                meanEstimate = - Math.exp( logSumTerms - log_sum_weights );
            }

            logSumTerms_sq = RobustOperations.robustSumOfLogarithms(log_sum_positive_terms_sq,log_sum_negative_terms_sq);
            varEstimate = Math.exp(logSumTerms_sq - log_sum_weights);

            varEstimate = varEstimate - Math.pow(meanEstimate,2);
            varEstimate = (varEstimate<0 ? 0.000001 : varEstimate);

            Normal normalDistribution = new Normal(variable);
            normalDistribution.setMean(meanEstimate);
            normalDistribution.setVariance(varEstimate);

            if(Double.isNaN(meanEstimate) || Double.isNaN(varEstimate) || Double.isInfinite(meanEstimate) || Double.isInfinite(varEstimate)) {
                System.out.println("NaN in Normal");
            }

            return normalDistribution;
        }

        public double getLogSumWeights() {
            return log_sum_weights;
        }

        protected double [] getLogParameters() {
            return new double[]{log_sum_positive_terms,log_sum_negative_terms,log_sum_positive_terms_sq,log_sum_negative_terms_sq,log_sum_weights};
        }

        protected void setLogParameters(double[] logParameters) {
            // CAREFUL USING THIS METHOD!!
            if(logParameters.length==5) {
                this.log_sum_positive_terms = logParameters[0];
                this.log_sum_negative_terms = logParameters[1];
                this.log_sum_positive_terms_sq = logParameters[2];
                this.log_sum_negative_terms_sq = logParameters[3];
                this.log_sum_weights = logParameters[4];
            }

            if(Double.isNaN(log_sum_positive_terms) || Double.isNaN(log_sum_negative_terms) || Double.isNaN(log_sum_negative_terms_sq) || Double.isNaN(log_sum_positive_terms_sq) || Double.isNaN(log_sum_weights) ) {
                System.out.println("log_sum_p: " + log_sum_positive_terms + ", log_sum_n:" + log_sum_negative_terms);
                System.out.println("log_sum_p_sq: " + log_sum_positive_terms_sq + ", log_sum_n_sq:" + log_sum_negative_terms_sq);
                System.out.println("log_sum_w: " + log_sum_weights);
            }
        }
    }

    private class StreamingUpdateableGaussianMixture {

        private double initial_variance = 50;
        private double tau_novelty = 0.005;

        private Variable variable;

        private int M = 0;
        private List<Double> mean;
        private List<Double> variance;
        private List<Double> componentWeight;
        private List<Double> log_sumProbs;

        private double log_sum_weights = Double.NEGATIVE_INFINITY;

        NormalDistributionImpl normalDistribution;

        public StreamingUpdateableGaussianMixture(Variable variable1) {
            this.variable = variable1;
        }

        public void setInitialVariance(double initial_variance) {
            this.initial_variance = initial_variance;
        }

        public void setNoveltyRate(double tau_novelty) {
            this.tau_novelty = tau_novelty;
        }

        public double getLogSumWeights() {
            return log_sum_weights;
        }

        protected void add(double weight, Normal normal) {

            if(M==0) {
                mean = new ArrayList<>();
                variance = new ArrayList<>();
                componentWeight = new ArrayList<>();
                log_sumProbs = new ArrayList<>();
            }
            // CAREFUL USING THIS METHOD!!
            M=M+1;
            mean.add(normal.getMean());
            variance.add(normal.getVariance());
            componentWeight.add(weight);
            log_sumProbs.add(1.0);
        }


        public void update(double dataPoint, double logWeight) {

            this.log_sum_weights = (log_sum_weights == Double.NEGATIVE_INFINITY ? logWeight : RobustOperations.robustSumOfLogarithms(log_sum_weights, logWeight));

            boolean output=false;

            if(output) {
                System.out.println("NUEVO DATO: " + dataPoint + ", CON PESO: " + logWeight);
            }
            // CREATE FIRST COMPONENT
            if (M==0) {
//                System.out.println("NO COMPONENTS, CREATING THE FIRST ONE");
                M=1;

                mean = new ArrayList<>();

                variance = new ArrayList<>();
                componentWeight = new ArrayList<>();
                log_sumProbs = new ArrayList<>();

                mean.add(dataPoint);
                variance.add(initial_variance);
                //log_sumProbs.add(logWeight);
                log_sumProbs.add(-0.001001);
                componentWeight.add(1.0);
            }
            // UPDATE EXISTING COMPONENTS
            else {


                double [] condition = new double[M];
                double [] probs_xj = new double[M];
                double [] probs_jx = new double[M];

                for (int i = 0; i < M; i++) {
                    normalDistribution = new NormalDistributionImpl( mean.get(i), Math.sqrt(variance.get(i)) );
                    double p_xj = normalDistribution.density(dataPoint);
                    probs_xj[i] = p_xj;
                    condition[i] = ( p_xj >= tau_novelty / Math.sqrt(2*Math.PI*variance.get(i)) ) ? 1 : 0;
                    probs_jx[i] = probs_xj[i] * componentWeight.get(i);
                }

                double sumProbsWeights = Arrays.stream(probs_jx).sum();

                for (int i = 0; i < M; i++) {
                    probs_jx[i] = probs_jx[i] / sumProbsWeights;
                }

                // IF DATAPOINT DOES NOT MATCH ANY EXISTING COMPONENT, CREATE A NEW ONE
                if (Arrays.stream(condition).sum()==0) {
//                    System.out.println("THE DATA POINT DOES NOT MATCH ANY EXISTING COMPONENT, CREATE A NEW ONE");
                    M=M+1;

                    mean.add(dataPoint);
                    variance.add(initial_variance);
                    //log_sumProbs.add(logWeight);
                    log_sumProbs.add(-0.001001);
                    componentWeight.add(1.0);

                }
                // ELSE, UPDATE PARAMETERS OF THE COMPONENTS
                else {
//                    System.out.println("UPDATING COMPONENTS PARAMETERS");
                    //TODO: review operations and check robustness
                    for (int i = 0; i < M; i++) {

                        double p_jx = probs_jx[i];
                        double oldMean = mean.get(i);


                        double log_p_jx_weighted = Math.log(p_jx); // + logWeight;    // p_jx_weighted = p_jx * exp(logWeight)
                        log_sumProbs.set(i, RobustOperations.robustSumOfLogarithms(log_sumProbs.get(i), log_p_jx_weighted));

                        double w = Math.exp(log_p_jx_weighted - log_sumProbs.get(i));

                        if(w==0) {
                            continue;
                        }

                        double diff_oldMean = dataPoint - oldMean;
                        double aux = w * diff_oldMean;
                        double newMean = oldMean + aux;


                        double diff_newMean = dataPoint - newMean;
//                        double aux2_new = Math.log(w) + 2 * diff_newMean;
//                        double aux2_old = Math.log(w) + 2 * diff_oldMean;


                        if(output==true) {
                            System.out.println("   p_jx:" + p_jx);
                            System.out.println("   w: " + w);
                            System.out.println("   oldMean: " + oldMean);
                            System.out.println("   diff_oldMean: " + diff_oldMean);
                            System.out.println("   newMean: " + newMean);
                            System.out.println("   diff_newMean: " + diff_newMean);
                        }


                        double oldVar = variance.get(i);

//                        double aux2_plus = w * ( Math.pow(diff_newMean,2) - oldVar);
//                        double aux2_minus = Math.pow(newMean-oldMean,2);
//
//                        double newVar = variance.get(i) + aux2_plus;


//                        double aux2_plus = w * Math.pow(diff_newMean,2);
//                        double aux2_minus = Math.pow(w,2) * Math.pow(diff_oldMean,2);
//
//                        double newVar = (1-w) * oldVar + aux2_plus;


                        double aux2_plus = w * Math.pow(diff_newMean,2);
                        double newVar = (1-w)*oldVar + w * Math.pow(diff_newMean,2);
                        double aux2_minus = Math.pow(w,2) * Math.pow(diff_oldMean,2);

                        if(output==true) {
                            System.out.println("   aux2_plus:" + aux2_plus);
                            System.out.println("   aux2_minus: " + aux2_minus);
                            System.out.println("   newVar: " + (variance.get(i) + aux2_plus - aux2_minus) );
                        }

                        if(newVar > aux2_minus) {
                            newVar = newVar - aux2_minus;
                        }
                        else {
                            newVar = 0.1;
                            System.out.println("Negative variance\n");
                        }

                        mean.set(i,newMean);
                        variance.set(i,newVar);

                    }
//                    System.out.println("COMPONENTS UPDATED");

//
//                    //double sum_sumProbs = log_sumProbs.stream().mapToDouble(Double::doubleValue).sum();
//                    double logSum_log_sumProbs = log_sumProbs.stream().reduce(RobustOperations::robustSumOfLogarithms).get();
//
////                    if(logSum_log_sumProbs!=0.0) {
//                        for (int i = 0; i < M; i++) {
//                            componentWeight.set(i, Math.exp(log_sumProbs.get(i) - logSum_log_sumProbs));
//                        }
////                    }
////                    else {
////                        for (int i = 0; i < M; i++) {
////                            componentWeight.set(i, 1.0/M);
////                        }
////                    }
//
//                    if(output==true) {
//                        System.out.println(Arrays.toString(log_sumProbs.toArray()));
//                        System.out.println(Arrays.toString(componentWeight.toArray()));
//                    }



//
//                    for (int i = 0; i < M; i++) {
//                        componentWeight.set(i, Math.exp(log_sumProbs.get(i) - logSum_log_sumProbs));
//                    }
                }


//                double logSum_log_sumProbs = log_sumProbs.stream().reduce(RobustOperations::robustSumOfLogarithms).get();
//
//                List<Integer> significantComponents = new ArrayList<>();
//
//                double threshold = 1E-16;
//                for (int i = 0; i < M; i++) {
//                    double newWeight = Math.exp(log_sumProbs.get(i) - logSum_log_sumProbs);
//                    componentWeight.set(i, newWeight);
//
////                    if(newWeight > threshold && variance.get(i)>0.0001) {
//                        significantComponents.add(i);
////                    }
//                }
//
//                if(output==true) {
//                    System.out.println("M: " + M + " with " + componentWeight.stream().filter(a -> a < threshold).count() + " negligible");
//                }
//                int newM = significantComponents.size();
//
//                List<Double> newList_logMean = new ArrayList<>();
//                List<Double> newList_logVariance = new ArrayList<>();
//                List<Double> newList_componentWeight = new ArrayList<>();
//                List<Double> newList_log_sumProbs = new ArrayList<>();
//
//                for (int i = 0; i < newM; i++) {
//                    newList_logMean.add(mean.get(significantComponents.get(i)));
//                    newList_logVariance.add(variance.get(significantComponents.get(i)));
//                    newList_componentWeight.add(componentWeight.get(significantComponents.get(i)));
//                    newList_log_sumProbs.add(log_sumProbs.get(significantComponents.get(i)));
//                }
//
//                newList_componentWeight.sort(Double::compareTo);
//
//                M = newM;
//
//                mean = newList_logMean;
//                variance = newList_logVariance;
//                componentWeight = newList_componentWeight;
//                log_sumProbs = newList_log_sumProbs;

                double logSum_log_sumProbs = log_sumProbs.stream().reduce(RobustOperations::robustSumOfLogarithms).get();

                for (int i = 0; i < M; i++) {
                    double newWeight = Math.exp(log_sumProbs.get(i) - logSum_log_sumProbs);
                    componentWeight.set(i, newWeight);
                }


                if(output==true) {
                    System.out.println("LogSumProbs: " + Arrays.toString(log_sumProbs.toArray()));
                    System.out.println("Weights:     " + Arrays.toString(componentWeight.toArray()));
                }
            }

            if(output) {
                System.out.println("COMPONENTES:");
                for (int i = 0; i < M; i++) {
                    System.out.println("Comp. " + i + "; media: " + mean.get(i) + "; var: " + variance.get(i) + "; peso: " + componentWeight.get(i));
                }
                System.out.println();
            }

        }

        public GaussianMixture getGaussianMixture() {

            double [] parameters = new double[3*M];

            for (int i = 0; i < M; i++) {
                parameters[3 * i + 0] = componentWeight.get(i);
                parameters[3 * i + 1] = mean.get(i);
                parameters[3 * i + 2] = variance.get(i); // (variance.get(i)<0 ? 0.000001 : variance.get(i));
            }
            GaussianMixture result = new GaussianMixture(variable);
            result.setParameters(parameters);

            return result;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(BayesianNetwork model_) {
        super.setModel(model_);

//        this.model = Serialization.deepCopy(model_);



        //setSamplingModel(model_);
//        this.samplingModel = model;
//        this.causalOrder = Utils.getTopologicalOrder(model.getDAG());
//        this.sameSamplingModel=true;

//        evidence=null;
//        weightedSampleStream=null;
//        this.weightedSampleList=null;

        if(this.model.getVariables().getListOfVariables().stream().anyMatch(variable -> !(variable.isMultinomial() || variable.isNormal()))) {
            throw new UnsupportedOperationException("All the Variable objects must be Multinomial or Normal in a CLG network");
        }

        variablesAPosteriori = model.getVariables().getListOfVariables();
        SSOfMultinomialVariables = new ArrayList<>(variablesAPosteriori.size());

        variablesAPosteriori.stream().forEachOrdered(variable -> {

            EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();
            ArrayVector arrayVector = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
            SSOfMultinomialVariables.add(arrayVector);
        });

        this.variablesAPosteriori_multinomials = variablesAPosteriori.stream().filter(Variable::isMultinomial).collect(Collectors.toList());
        this.variablesAPosteriori_normals = variablesAPosteriori.stream().filter(Variable::isNormal).collect(Collectors.toList());

        this.initializeSufficientStatistics();

    }


//    public Stream<Assignment> getSamples() {
//
//        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
//
//        IntStream weightedSampleStream = IntStream.range(0, sampleSize).parallel();
//
//        if (!parallelMode) {
//            weightedSampleStream = weightedSampleStream.sequential();
//        }
//
//        return weightedSampleStream.mapToObj(i -> {
//            WeightedAssignment weightedSample = generateSample(randomGenerator.current());
//            return weightedSample.getAssignment();
//        });
//    }

    public void setGaussianMixturePosteriors(boolean useGaussianMixtures) {
        this.useGaussianMixtures = useGaussianMixtures;

        this.initializeSufficientStatistics();
    }

//    public List<SufficientStatistics> getSSMultinomialVariablesAPosteriori() {
//        return SSOfMultinomialVariables;
//    }


    public void setSaveSampleToFile(boolean saveSampleToFile, String path) {
        this.saveSampleToFile = saveSampleToFile;
        this.saveToFilePath = path;
    }

    public void setVariablesOfInterest(List<Variable> variablesAPosterior) throws InvalidObjectException {

        if (variablesAPosterior.stream().anyMatch(variable -> !model.getVariables().getListOfVariables().contains(variable))) {
            throw new InvalidObjectException("All variables in the list must be included in the Bayesian network");
        }
        this.variablesAPosteriori = variablesAPosterior;
        this.variablesAPosteriori_multinomials = variablesAPosteriori.stream().filter(Variable::isMultinomial).collect(Collectors.toList());
        this.variablesAPosteriori_normals = variablesAPosteriori.stream().filter(Variable::isNormal).collect(Collectors.toList());

        this.initializeSufficientStatistics();

    }


    public void setMixtureOfGaussiansInitialVariance(double mixtureOfGaussiansInitialVariance) {
        this.mixtureOfGaussiansInitialVariance = mixtureOfGaussiansInitialVariance;
        if(useGaussianMixtures) {
            for (int i = 0; i < SSOfGaussianMixtureVariables.size(); i++) {
                this.SSOfGaussianMixtureVariables.get(i).forEach(object -> object.setInitialVariance(this.mixtureOfGaussiansInitialVariance));
            }

        }
    }

    public void setMixtureOfGaussiansNoveltyRate(double mixtureOfGaussiansNoveltyRate) {
        this.mixtureOfGaussiansNoveltyRate = mixtureOfGaussiansNoveltyRate;
        if(useGaussianMixtures) {
            for (int i = 0; i < SSOfGaussianMixtureVariables.size(); i++) {
                this.SSOfGaussianMixtureVariables.get(i).forEach(object -> object.setNoveltyRate(this.mixtureOfGaussiansNoveltyRate));
            }
        }
    }

    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
        this.initializeSufficientStatistics();
    }

    private void initializeSufficientStatistics() {
        // INITIALIZE SUFFICIENT STATISTICS FOR MULTINOMIAL VARIABLES
        this.SSOfMultinomialVariables = new ArrayList<>();

        this.variablesAPosteriori.stream().filter(Variable::isMultinomial).forEachOrdered(variable -> {

            EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();
            ArrayVector arrayVector = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());

            this.SSOfMultinomialVariables.add(arrayVector);
        });

        int parallelism=1;
        if(parallelMode) {
            parallelism = Runtime.getRuntime().availableProcessors();
        }

        // INITIALIZE "SUFFICIENT STATISTICS" FOR NORMAL VARIABLES, TO BUILD A NORMAL OR A GAUSSIAN MIXTURE
        if(useGaussianMixtures) {
            this.SSOfGaussianMixtureVariables = new ArrayList<>();

            for (int i = 0; i < parallelism; i++) {
                ArrayList arrayList = new ArrayList();

                this.variablesAPosteriori.stream().filter(Variable::isNormal).forEachOrdered(variable -> {
                    StreamingUpdateableGaussianMixture streamingPosteriorDistribution = new StreamingUpdateableGaussianMixture(variable);
                    streamingPosteriorDistribution.setInitialVariance(this.mixtureOfGaussiansInitialVariance);
                    streamingPosteriorDistribution.setNoveltyRate(this.mixtureOfGaussiansNoveltyRate);
                    arrayList.add(streamingPosteriorDistribution);
                });

                this.SSOfGaussianMixtureVariables.add(arrayList);
            }


        }
        else {
            this.SSOfNormalVariables = new ArrayList<>();
//            this.variablesAPosteriori.stream().filter(Variable::isNormal).forEachOrdered(variable -> {
//                StreamingUpdateableRobustNormal streamingPosteriorDistribution = new StreamingUpdateableRobustNormal(variable);
//                this.SSOfNormalVariables.add(streamingPosteriorDistribution);
//            });

            for (int i = 0; i < parallelism; i++) {
                ArrayList arrayList = new ArrayList();

                this.variablesAPosteriori.stream().filter(Variable::isNormal).forEachOrdered(variable -> {
                    StreamingUpdateableRobustNormal streamingPosteriorDistribution = new StreamingUpdateableRobustNormal(variable);
                    arrayList.add(streamingPosteriorDistribution);
                });

                this.SSOfNormalVariables.add(arrayList);
            }
        }
    }

    //
//    /**
//     * Returns a {@link Stream} containing the drawn samples after running the inference.
//     * @return a {@link Stream} of {@link HashMapAssignment} objects.
//     */
//    public Stream<Assignment> getSamples() {
//
//        if(keepDataOnMemory) {
//            weightedSampleStream = weightedSampleList.stream().sequential();
//        }
//        if(parallelMode) {
//            weightedSampleStream.parallel();
//        }
//        return weightedSampleStream.map(wsl -> wsl.assignment);
//    }

    private void updatePosteriorDistributions(Assignment sample, double logWeight, int parallelProcessIndex) {

        variablesAPosteriori_multinomials.stream().forEach(variable -> {

            int j = variablesAPosteriori_multinomials.indexOf(variable);

            EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();

            ArrayVector SSposterior = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
            SSposterior.copy(SSOfMultinomialVariables.get(j));

            ArrayVector newSSposterior;

            ArrayVector SSsample = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
            SSsample.copy(ef_univariateDistribution.getSufficientStatistics(sample));

            if(evidence!=null) {
                SSsample.multiplyBy(logWeight);
            }


            newSSposterior = RobustOperations.robustSumOfMultinomialLogSufficientStatistics(SSposterior, SSsample);

            SSOfMultinomialVariables.set(j,newSSposterior);

        });


        variablesAPosteriori_normals.stream().forEach(variable -> {

            int j = variablesAPosteriori_normals.indexOf(variable);

            double dataPoint = sample.getValue(variable);

            if(useGaussianMixtures) {
                SSOfGaussianMixtureVariables.get(parallelProcessIndex).get(j).update(dataPoint, logWeight);
            }
            else {
                SSOfNormalVariables.get(parallelProcessIndex).get(j).update(dataPoint, logWeight);
            }
        });


        if(queryingVariable!=null) {
            double queryingVariableValue = sample.getValue(queryingVariable);
            double queryingValueResult = this.queryingFunction.apply(queryingVariableValue);

            if (queryingValueResult != 0) {
                // System.out.println(queryResult);
                this.queryResult = RobustOperations.robustSumOfLogarithms(this.queryResult, logWeight + Math.log(queryingValueResult));
            }
        }

        if (saveSampleToFile) {
            try {
                FileWriter fileWriter = new FileWriter(saveToFilePath, true);
                String line = sample.getValue(variablesAPosteriori.get(0)) + "; " + logWeight + "\n";
                fileWriter.write(line);
                fileWriter.close();

            } catch (Exception e) {
                System.out.println("ERROR EN SALIDA DE IS");
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

//        int nVarsAPosteriori = variablesAPosteriori.size();
//
//        IntStream.range(0,nVarsAPosteriori).forEach(i -> {
//
//            Variable variable = variablesAPosteriori.get(i);
//
//
//            if(variable.isMultinomial()) {
//
//                EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();
//
////            SufficientStatistics SSposterior = SSOfMultinomialVariables.get(i);
////            SufficientStatistics SSsample = ef_univariateDistribution.getSufficientStatistics(sample);
//
//                ArrayVector SSposterior = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
//                SSposterior.copy(SSOfMultinomialVariables.get(i));
//
//                ArrayVector newSSposterior;
//
//                ArrayVector SSsample = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
//                SSsample.copy(ef_univariateDistribution.getSufficientStatistics(sample));
//
//                if(evidence!=null) {
//                    SSsample.multiplyBy(logWeight);
//                }
//
////                ArrayVector SS = new ArrayVector(ef_univariateDistribution.getMomentParameters().size());
////                SS.copy(ef_univariateDistribution.getSufficientStatistics(sample));
//
////                System.out.println(Arrays.toString(SSposterior.toArray()));
////                System.out.println(Arrays.toString(SSsample.toArray()));
////                System.out.println(Arrays.toString(SS.toArray()));
////                System.out.println();
//
//                newSSposterior = RobustOperations.robustSumOfMultinomialLogSufficientStatistics(SSposterior, SSsample);
//
//                SSOfMultinomialVariables.set(i,newSSposterior);
//            }
//            else if(variable.isNormal()) {
//
//                double dataPoint = sample.getValue(variable);
//                SSOfGaussianMixtureVariables.get(i).update(dataPoint, logWeight);
//
//            }
//            else {
//                throw new UnsupportedOperationException("ImportanceSamplingCLG.updatePosteriorDistributions() variable distribution not supported");
//            }

//        });
    }

    public void setQuery(Variable var, Function<Double,Double> function) {
        if( var!=null && !this.variablesAPosteriori.contains(var) ) {
            throw new UnsupportedOperationException("The querying variable must be a variable of interest");
        }

        this.queryingFunction = function;
        this.queryingVariable = var;
        this.queryResult = Double.NEGATIVE_INFINITY;
    }

    public double getQueryResult() {
        return queryResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedValue(Variable var, Function<Double,Double> function) {

//        List<Double> sum = weightedSampleStream
//                .map(ws -> Arrays.asList(Math.exp(ws.logWeight), Math.exp(ws.logWeight) * function.apply(ws.assignment.getValue(var))))
//                .filter(array -> (Double.isFinite(array.get(0)) && Double.isFinite(array.get(1)) ))
//                .reduce(Arrays.asList(new Double(0.0), new Double(0.0)), (e1, e2) -> Arrays.asList(e1.get(0) + e2.get(0), e1.get(1) + e2.get(1)));
//
//        return sum.get(1)/sum.get(0);


        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        IntStream weightedSampleStream = IntStream.range(0, sampleSize).parallel();

        if (!parallelMode) {
            weightedSampleStream = weightedSampleStream.sequential();
        }

        double[] logSumWeights = weightedSampleStream
                .mapToObj(i -> {
                    WeightedAssignment weightedSample = generateSample(randomGenerator.current());

                    return new double[]{Math.exp(weightedSample.getWeight()), Math.exp(weightedSample.getWeight()) * function.apply(weightedSample.getAssignment().getValue(var))};
                })
                .filter(doubleArray -> Double.isFinite(doubleArray[0]) && Double.isFinite(doubleArray[1]))
                .reduce(((doubleArray1, doubleArray2) -> {
                    double [] result = new double[2];
//                    result[0] = robustSumOfLogarithms(doubleArray1[0],doubleArray2[0]);
//                    result[1] = robustSumOfLogarithms(doubleArray1[1],doubleArray2[1]);
                    result[0] = (doubleArray1[0] + doubleArray2[0]);
                    result[1] = (doubleArray1[1] + doubleArray2[1]);
                    return result;
                })).get();

        System.out.println(logSumWeights[1]);
        System.out.println(logSumWeights[0]);
        return logSumWeights[1]/logSumWeights[0];
    }

    /**
    * {@inheritDoc}
    */
    @Override
    //TODO For continuous variables, instead of returning a Gaussian distributions, we should return a Mixture of Gaussians!!
    public <E extends UnivariateDistribution> E getPosterior(Variable variable) {

        if(variable.isMultinomial()) {

            EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();

            ArrayVector sumSS = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
            sumSS.copy(SSOfMultinomialVariables.get(variablesAPosteriori_multinomials.indexOf(variable)));
            //System.out.println(Arrays.toString(sumSS.toArray()));

            sumSS = RobustOperations.robustNormalizationOfLogProbabilitiesVector(sumSS);

            ef_univariateDistribution.setMomentParameters((SufficientStatistics)sumSS);
            Multinomial posteriorDistribution = ef_univariateDistribution.toUnivariateDistribution();
            posteriorDistribution.setProbabilities(Utils.normalize(posteriorDistribution.getParameters()));

            return (E) posteriorDistribution;
        }


//         TODO Could we build this object in a general way for Multinomial and Normal?
        else {
            //(variable.isNormal())

//            throw new UnsupportedOperationException("ImportanceSamplingCLG.getPosterior() not supported yet for non-multinomial distributions");
            int j = variablesAPosteriori_normals.indexOf(variable);

            if(useGaussianMixtures) {
                List<StreamingUpdateableGaussianMixture> auxList = new ArrayList<>();
                for (int i = 0; i < SSOfGaussianMixtureVariables.size(); i++) {
                    auxList.add(SSOfGaussianMixtureVariables.get(i).get(j));
                }
                return (E) auxList.stream().reduce(this::reduceGaussianMixture).get().getGaussianMixture();
                //return (E) SSOfGaussianMixtureVariables.get(0).get(j).getGaussianMixture();
            }
            else {
                List<StreamingUpdateableRobustNormal> auxList = new ArrayList<>();
                for (int i = 0; i < SSOfNormalVariables.size(); i++) {
                    auxList.add(SSOfNormalVariables.get(i).get(j));
                }
                return (E) auxList.stream().reduce(this::reduceNormal).get().getNormal();
            }
        }


    }

    protected StreamingUpdateableRobustNormal reduceNormal(StreamingUpdateableRobustNormal distr1, StreamingUpdateableRobustNormal distr2) {
        double [] paramDistr1 = distr1.getLogParameters();
        double [] paramDistr2 = distr2.getLogParameters();
        double [] paramNewDistr = new double[5];

        for (int i = 0; i < paramDistr1.length; i++) {
            if (Double.isNaN(paramDistr1[i])) {
                paramNewDistr[i] = paramDistr2[i];
            }
            else if(Double.isNaN(paramDistr2[i])) {
                paramNewDistr[i] = paramDistr1[i];
            }
            else {
                paramNewDistr[i] = RobustOperations.robustSumOfLogarithms(paramDistr1[i], paramDistr2[i]);
            }
        }

        boolean containNaNs = Arrays.stream(paramNewDistr).anyMatch(Double::isNaN);
        StreamingUpdateableRobustNormal newDistr = new StreamingUpdateableRobustNormal(distr1.getNormal().getVariable());
        newDistr.setLogParameters(paramNewDistr);
        return newDistr;
    }

    private StreamingUpdateableGaussianMixture reduceGaussianMixture(StreamingUpdateableGaussianMixture distr1, StreamingUpdateableGaussianMixture distr2) {

        double[] paramDistr1 = distr1.getGaussianMixture().getParameters();
        double[] paramDistr2 = distr2.getGaussianMixture().getParameters();

        StreamingUpdateableGaussianMixture newDistr = new StreamingUpdateableGaussianMixture(distr1.variable);

        double logWeightDistr1 = distr1.getLogSumWeights();
        double logWeightDistr2 = distr2.getLogSumWeights();

        double logSumWeights = RobustOperations.robustSumOfLogarithms(logWeightDistr1,logWeightDistr2);

        for (int i = 0; i < (paramDistr1.length)/3; i++) {
            double weightThisTerm = paramDistr1[3*i+0];
            double meanThisTerm =   paramDistr1[3*i+1];
            double varThisTerm =    paramDistr1[3*i+2];

            double relativeWeight = Math.exp(logWeightDistr1 - logSumWeights);
            double newWeight = relativeWeight * weightThisTerm;

            if(newWeight > 0.0001) {
                Normal normal = new Normal(distr1.getGaussianMixture().getVariable());
                normal.setMean(meanThisTerm);
                normal.setVariance(varThisTerm);
                newDistr.add(newWeight, normal);
            }
        }

        for (int i = 0; i < (paramDistr2.length)/3; i++) {
            double weightThisTerm = paramDistr2[3*i+0];
            double meanThisTerm =   paramDistr2[3*i+1];
            double varThisTerm =    paramDistr2[3*i+2];

            double relativeWeight = Math.exp(logWeightDistr2 - logSumWeights);
            double newWeight = relativeWeight * weightThisTerm;

            if(newWeight > 0.0001) {
                Normal normal = new Normal(distr1.getGaussianMixture().getVariable());
                normal.setMean(meanThisTerm);
                normal.setVariance(varThisTerm);
                newDistr.add(newWeight, normal);
            }
        }

        return newDistr;
    }
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    //TODO For continuous variables, instead of returning a Gaussian distributions, we should return a Mixture of Gaussians!!
//    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
//
//        Variable samplingVar = this.samplingModel.getVariables().getVariableByName(var.getName());
//        EF_UnivariateDistribution ef_univariateDistribution = samplingVar.newUnivariateDistribution().toEFUnivariateDistribution();
//
//        // TODO Could we build this object in a general way for Multinomial and Normal?
//        if (!samplingVar.isMultinomial()) {
//            throw new UnsupportedOperationException("ImportanceSamplingCLG.getPosterior() not supported yet for non-multinomial distributions");
//
//        }
//
//        if(keepDataOnMemory) {
//            weightedSampleStream = weightedSampleList.stream().sequential();
//        } else {
//            computeWeightedSampleStream(false);
//        }
//
//        if(parallelMode) {
//            weightedSampleStream.parallel();
//        }
//
//        if (!keepDataOnMemory) {
//            weightedSampleList = weightedSampleStream.collect(Collectors.toList());
//        }
//
////        long samples0 = weightedSampleList.stream().mapToInt(weightedAssignment -> (int)weightedAssignment.assignment.getValue(var)).filter(i -> i==0).count();
////        long samples1 = weightedSampleList.stream().mapToInt(weightedAssignment -> (int)weightedAssignment.assignment.getValue(var)).filter(i -> i==1).count();
//
////        System.out.println("Values 0:" +  samples0);
////        System.out.println("Values 1:" +  samples1);
////
////        System.out.println("Rate 0:" +  ((double)samples0)/(samples0+samples1));
////        System.out.println("Rate 1:" +  ((double)samples1)/(samples0+samples1));
//        ArrayVector sumSS = weightedSampleStream
//                .map(e -> {
//                    ArrayVector SS = new ArrayVector(ef_univariateDistribution.getMomentParameters().size());
//                    SS.copy(ef_univariateDistribution.getSufficientStatistics(e.assignment));
//                    SS.multiplyBy(e.logWeight);  //Remember: logWeight is actually log-logWeight
//
////                    if(e.logWeight>=0) {
////                        System.out.println(Arrays.toString(((ArrayVector) ef_univariateDistribution.getSufficientStatistics(e.assignment)).toArray()));
////                        System.out.println(e.logWeight);
////                    }
//                    return SS;
//                })
//                .reduce(this::robustSumOfMultinomialLogSufficientStatistics).get();
//
//
//        sumSS = robustNormalizationOfLogProbabilitiesVector(sumSS);
//
//        ef_univariateDistribution.setMomentParameters((SufficientStatistics)sumSS);
//
//        Distribution posteriorDistribution = ef_univariateDistribution.toUnivariateDistribution();
//        //((Multinomial) posteriorDistribution).setProbabilities(Utils.normalize(sumSS.toArray()));
//
//        return (E)posteriorDistribution;
//    }


    private void runInferenceForGaussianMixtures() {

        int parallelism = 1;
        IntStream parallelProcesses;

        if (parallelMode) {
            parallelism = Runtime.getRuntime().availableProcessors();
            parallelProcesses = IntStream.range(0, parallelism).parallel();
        }
        else {
            parallelProcesses = IntStream.range(0, 1).sequential();
        }

        int subsampleSize = sampleSize/parallelism;

        double logSumWeights = parallelProcesses.mapToDouble(parallelProcessIndex -> {
            IntStream weightedSampleStream = IntStream.range(0, subsampleSize).sequential();

            return weightedSampleStream
                   .mapToDouble(i -> {
                       LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed + i);
                       WeightedAssignment weightedSample = generateSample(randomGenerator.current());
                       //System.out.println(weightedSample.assignment.outputString());
                       //System.out.println(weightedSample.logWeight);
                       updatePosteriorDistributions(weightedSample.getAssignment(),weightedSample.getWeight(),parallelProcessIndex);
                       //updateLogProbabilityOfEvidence(weightedSample.logWeight);
                       //logProbOfEvidence = robustSumOfLogarithms(logProbOfEvidence,weightedSample.logWeight-Math.log(sampleSize));
                       //System.out.println(weightedSample.logWeight);
                       //System.out.println(logProbOfEvidence);

                       return weightedSample.getWeight();

                   }).reduce(RobustOperations::robustSumOfLogarithms).getAsDouble();

        }).reduce(RobustOperations::robustSumOfLogarithms).getAsDouble();

        this.logSumWeights = logSumWeights;

        if (evidence!=null) {
            logProbEvidence = logSumWeights - Math.log(sampleSize);
        }
        else {
            logProbEvidence = 0;
        }


        this.queryResult = Math.exp(this.queryResult - this.logSumWeights);



        //        return weightedSampleStream.mapToDouble(ws -> ws.logWeight - Math.log(sampleSize)).reduce(this::robustSumOfLogarithms).getAsDouble();


        //logProbOfEvidence = robustSumOfLogarithms(logProbOfEvidence,-Math.log(sampleSize));



//        if(saveDataOnMemory_) {
//            weightedSampleList = weightedSampleStream.collect(Collectors.toList());
//            //weightedSampleList.forEach(weightedAssignment -> System.out.println("Weight: " + weightedAssignment.logWeight + " for assignment " + weightedAssignment.assignment.getValue(this.model.getVariables().getListOfVariables().get(0)) + " prob " + model.getLogProbabiltyOf(weightedAssignment.assignment)));
//        }


    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {

        this.queryResult = Double.NEGATIVE_INFINITY;

//        if(useGaussianMixtures) {
//            this.runInferenceForGaussianMixtures();
//            return;
//        }

        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);

        IntStream weightedSampleStream = IntStream.range(0, sampleSize).parallel();

        if (!parallelMode) {
            weightedSampleStream = weightedSampleStream.sequential();
        }

//            weightedSampleStream = IntStream.range(0, sampleSize).parallel()
//                    .mapToObj(i -> generateSample(randomGenerator.current()));
//        } else {
//            weightedSampleStream = IntStream.range(0, sampleSize).sequential()
//                    .mapToObj(i -> generateSample(randomGenerator.current()));
//        }

        double maxLogWeight = IntStream.range(0, 1000).mapToObj(k -> generateSample(randomGenerator.current())).mapToDouble(w -> w.getWeight()).max().getAsDouble();


        double logSumWeights = weightedSampleStream

                .mapToObj( i -> generateSample(randomGenerator.current()))
                .filter(w -> (maxLogWeight-w.getWeight())<15)
                .mapToDouble(weightedSample ->{
                    updatePosteriorDistributions(weightedSample.getAssignment(),weightedSample.getWeight(),0);
                    return weightedSample.getWeight();

                }).reduce(RobustOperations::robustSumOfLogarithms).getAsDouble();
//                .mapToDouble(i -> {
//
//
//
//                    WeightedAssignment weightedSample = generateSample(randomGenerator.current());
//
//
//
//                    //System.out.println(weightedSample.assignment.outputString());
//                    //System.out.println(weightedSample.logWeight);
//                    updatePosteriorDistributions(weightedSample.getAssignment(),weightedSample.getWeight(),0);
//                    //updateLogProbabilityOfEvidence(weightedSample.logWeight);
//
//
//                    //logProbOfEvidence = robustSumOfLogarithms(logProbOfEvidence,weightedSample.logWeight-Math.log(sampleSize));
//
//                    //System.out.println(weightedSample.logWeight);
//
//                    //System.out.println(logProbOfEvidence);
//
//                    return weightedSample.getWeight();
//                }).reduce(RobustOperations::robustSumOfLogarithms).getAsDouble();

        //        return weightedSampleStream.mapToDouble(ws -> ws.logWeight - Math.log(sampleSize)).reduce(this::robustSumOfLogarithms).getAsDouble();


        //logProbOfEvidence = robustSumOfLogarithms(logProbOfEvidence,-Math.log(sampleSize));

        this.logSumWeights = logSumWeights;

        if (evidence!=null) {
            this.logProbEvidence = logSumWeights - Math.log(sampleSize);
        }
        else {
            this.logProbEvidence = 0;
        }


        this.queryResult = Math.exp(this.queryResult - this.logSumWeights);

//        if(saveDataOnMemory_) {
//            weightedSampleList = weightedSampleStream.collect(Collectors.toList());
//            //weightedSampleList.forEach(weightedAssignment -> System.out.println("Weight: " + weightedAssignment.logWeight + " for assignment " + weightedAssignment.assignment.getValue(this.model.getVariables().getListOfVariables().get(0)) + " prob " + model.getLogProbabiltyOf(weightedAssignment.assignment)));
//        }


    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        // this.oldMain();

        int nMultinomialVars = 50;
        int nGaussianVars = 50;

        BayesianNetworkGenerator.setNumberOfMultinomialVars(nMultinomialVars,2);
        BayesianNetworkGenerator.setNumberOfGaussianVars(nGaussianVars);
        BayesianNetworkGenerator.setNumberOfLinks((int)1.5*(nMultinomialVars+nGaussianVars));
        BayesianNetworkGenerator.setSeed(9463);
        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
        System.out.println(bn);


        ImportanceSamplingCLG_new importanceSamplingCLG = new ImportanceSamplingCLG_new();
        importanceSamplingCLG.setModel(bn);
        //importanceSamplingCLG.setSamplingModel(vmp.getSamplingModel());


        Assignment evidence = new HashMapAssignment();
        for (int i = 1; i < nGaussianVars; i++) {
            evidence.setValue(bn.getVariables().getVariableByName("GaussianVar" + Integer.toString(i)),0);
        }
        importanceSamplingCLG.setEvidence(evidence);


        importanceSamplingCLG.setParallelMode(true);
        importanceSamplingCLG.setSampleSize(100000);
        importanceSamplingCLG.setSeed(23623);

        Variable gaussianVar0 = bn.getVariables().getVariableByName("GaussianVar0");

        List<Variable> varsPosteriori = new ArrayList<>();
        varsPosteriori.add(gaussianVar0);
        importanceSamplingCLG.setVariablesOfInterest(varsPosteriori);

        importanceSamplingCLG.runInference();


        System.out.println(gaussianVar0.getName() + " conditional distribution: \n" + bn.getConditionalDistribution(gaussianVar0).toString());
        System.out.println(gaussianVar0.getName() + " posterior distribution: \n" + importanceSamplingCLG.getPosterior(gaussianVar0).toString());
        //System.out.println(importanceSamplingCLG.getPosterior(bn.getVariables().getVariableByName("GaussianVar1")).toString());
        //System.out.println(importanceSamplingCLG.getPosterior(bn.getVariables().getVariableByName("GaussianVar2")).toString());

    }

}
