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

import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.*;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math.complex.Complex;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * This class implements the interface {@link InferenceAlgorithm} and defines the Importance Sampling algorithm.
 * J.M. Hammersley and D.C. Handscomb. Monte Carlo Methods. Methuen and Co, London, UK, 1964.
 *
 * <p> For an example of use follow this link
 * <a href="http://amidst.github.io/toolbox/CodeExamples.html#isexample"> http://amidst.github.io/toolbox/CodeExamples.html#isexample </a>  </p>
 */
public class ImportanceSamplingRobust implements InferenceAlgorithm, Serializable {

    private static final long serialVersionUID = 8587756877237341367L;

    private BayesianNetwork model;
    private BayesianNetwork samplingModel;
    private boolean sameSamplingModel;

    private List<Variable> causalOrder;

    private int seed = 0;
    private int sampleSize = 1000;

    private Stream<ImportanceSamplingRobust.WeightedAssignment> weightedSampleStream;

    private List<Variable> variablesAPosteriori;
    private List<SufficientStatistics> SSvariablesAPosteriori;

    private Assignment evidence;
    private double logProbOfEvidence;

    private boolean parallelMode = true;


    private class WeightedAssignment {
        private HashMapAssignment assignment;
        private double logWeight;

        public WeightedAssignment(HashMapAssignment assignment_, double weight_){
            this.assignment = assignment_;
            this.logWeight = weight_;
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("[ ");

            for (Map.Entry<Variable, Double> entry : this.assignment.entrySet()) {
                str.append(entry.getKey().getName() + " = " + entry.getValue());
                str.append(", ");
            }
            str.append("Weight = " + logWeight + " ]");
            return str.toString();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {
        this.seed=seed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(BayesianNetwork model_) {
        this.model = Serialization.deepCopy(model_);
        //setSamplingModel(model_);
        this.samplingModel = model;
        this.causalOrder = Utils.getTopologicalOrder(model.getDAG());
        this.sameSamplingModel=true;

        evidence=null;
        weightedSampleStream=null;

        variablesAPosteriori = model.getVariables().getListOfVariables();
        SSvariablesAPosteriori = new ArrayList<>(variablesAPosteriori.size());

        variablesAPosteriori.stream().forEachOrdered(variable -> {

            EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();
            ArrayVector arrayVector = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
            SSvariablesAPosteriori.add(arrayVector);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;
        weightedSampleStream=null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }

    public BayesianNetwork getSamplingModel() {
        return this.samplingModel;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbabilityOfEvidence() {

//        if(keepDataOnMemory) {
//            weightedSampleStream = weightedSampleList.stream().sequential();
//        } else {
//            computeWeightedSampleStream(false);
//        }
//        if(parallelMode) {
//            weightedSampleStream.parallel();
//        }

//        //return Math.log(weightedSampleStream.mapToDouble(ws -> Math.exp(ws.logWeight)).filter(Double::isFinite).average().getAsDouble());
//        return weightedSampleStream.mapToDouble(ws -> ws.logWeight - Math.log(sampleSize)).reduce(this::robustSumOfLogarithms).getAsDouble();

        return logProbOfEvidence;

    }


    /**
     * Sets the sampling model for this ImportanceSampling.
     * @param samplingModel_ a {@link BayesianNetwork} model according to which samples will be simulated.
     */
    public void setSamplingModel(BayesianNetwork samplingModel_) {
        this.samplingModel = new BayesianNetwork(samplingModel_.getDAG(),
                Serialization.deepCopy(samplingModel_.getConditionalDistributions()));
        this.causalOrder = Utils.getTopologicalOrder(samplingModel.getDAG());

        if (this.samplingModel.equalBNs(this.model,1E-10)) {
            this.sameSamplingModel=true;
        }
        else {
            this.sameSamplingModel=false;
        }
    }

    /**
     * Sets the number of samples to be drawn from the sampling model.
     * @param sampleSize an {@code int} that represents the number of samples.
     */
    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }


    public void setVariablesAPosteriori(List<Variable> variablesAPosterior) {


        this.variablesAPosteriori = variablesAPosterior;
        this.SSvariablesAPosteriori = new ArrayList<>();

        variablesAPosterior.stream().forEachOrdered(variable -> {

            EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();
            ArrayVector arrayVector = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());

            SSvariablesAPosteriori.add(arrayVector);
        });
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

    private void updatePosteriorDistributions(Assignment sample, double logWeight) {

        int nVarsAPosteriori = variablesAPosteriori.size();

        IntStream.range(0,nVarsAPosteriori).forEach(i -> {

            Variable variable = variablesAPosteriori.get(i);
            EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();

//            SufficientStatistics SSposterior = SSvariablesAPosteriori.get(i);
//            SufficientStatistics SSsample = ef_univariateDistribution.getSufficientStatistics(sample);

            ArrayVector SSposterior = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
            SSposterior.copy(SSvariablesAPosteriori.get(i));

            ArrayVector newSSposterior;

            if(variable.isMultinomial()) {

                ArrayVector SSsample = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
                SSsample.copy(ef_univariateDistribution.getSufficientStatistics(sample));

                if(evidence!=null) {
                    SSsample.multiplyBy(logWeight);
                }

//                ArrayVector SS = new ArrayVector(ef_univariateDistribution.getMomentParameters().size());
//                SS.copy(ef_univariateDistribution.getSufficientStatistics(sample));

//                System.out.println(Arrays.toString(SSposterior.toArray()));
//                System.out.println(Arrays.toString(SSsample.toArray()));
//                System.out.println(Arrays.toString(SS.toArray()));
//                System.out.println();

                newSSposterior = robustSumOfMultinomialSufficientStatistics(SSposterior, SSsample);
            }
            else {
//                if (variable.isNormal() ) {
//
//                    double global_shift = SSposterior.get(2);
//
//                    //ArrayVector SSsample = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics()+1);
//
//                    SufficientStatistics SSsample = ef_univariateDistribution.getSufficientStatistics(sample);
//
//                    double coef1 = ef_univariateDistribution.getSufficientStatistics(sample).get(0);
//                    double coef2 = Math.pow(coef1,2);
//                    double shift = 0;
//
//
//                    if(coef1<=global_shift) {
//                        shift = coef1-1;
//                    }
//                    double log_aux = Math.log(coef1 - global_shift - shift);
//
//                    double[] SScoefs = new double[]{log_aux, 2*log_aux, shift};
//
//                    ArrayVector AVsample = new ArrayVector(SScoefs);
//                    //AVsample.multiplyBy(logWeight);
//                    AVsample.sumConstant(logWeight);
//
////                ArrayVector SS = new ArrayVector(ef_univariateDistribution.getMomentParameters().size());
////                SS.copy(ef_univariateDistribution.getSufficientStatistics(sample));
//
//                    newSSposterior = robustSumOfNormalSufficientStatistics(SSposterior, AVsample);
//                }
//                else {
                    throw new UnsupportedOperationException("ImportanceSamplingRobust.updatePosteriorDistributions() works only for multinomials");
//              }
            }
            SSvariablesAPosteriori.set(i,newSSposterior);

        });
    }

    private WeightedAssignment generateSampleSameModel(Random random) {

        HashMapAssignment sample = new HashMapAssignment(this.model.getNumberOfVars());;

        double logWeight = 0.0;

        for (Variable samplingVar : causalOrder) {

            double simulatedValue;

            ConditionalDistribution samplingDistribution = this.model.getConditionalDistribution(samplingVar);
            UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(sample);

            if( evidence!=null && !Double.isNaN(evidence.getValue(samplingVar))) {
                simulatedValue=evidence.getValue(samplingVar);
                logWeight = logWeight + univariateSamplingDistribution.getLogProbability(simulatedValue);
            }
            else {
                simulatedValue = univariateSamplingDistribution.sample(random);
            }
            //logWeight = logWeight + univariateSamplingDistribution.getLogProbability(simulatedValue);
            sample.setValue(samplingVar,simulatedValue);
        }
        //double logWeight = Math.exp(logWeight);
        //System.out.println(logWeight);



        return new WeightedAssignment(sample,logWeight);
    }

    private WeightedAssignment generateSample(Random random) {

        if(this.sameSamplingModel) {
            return generateSampleSameModel(random);
        }

        HashMapAssignment samplingAssignment = new HashMapAssignment(1);
        HashMapAssignment modelAssignment = new HashMapAssignment(1);
        double numerator = 0.0;
        double denominator = 0.0;

        for (Variable samplingVar : causalOrder) {

            Variable modelVar = this.model.getVariables().getVariableById(samplingVar.getVarID());
            double simulatedValue;

            if( evidence!=null && !Double.isNaN(evidence.getValue(samplingVar))) {
                simulatedValue=evidence.getValue(samplingVar);

                UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
                numerator = numerator + univariateModelDistribution.getLogProbability(simulatedValue);
            }
            else {
                ConditionalDistribution samplingDistribution = this.samplingModel.getConditionalDistribution(samplingVar);
                UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);

                simulatedValue = univariateSamplingDistribution.sample(random);
                denominator = denominator + univariateSamplingDistribution.getLogProbability(simulatedValue);

                UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
                numerator = numerator + univariateModelDistribution.getLogProbability(simulatedValue);

            }
            modelAssignment.setValue(modelVar,simulatedValue);
            samplingAssignment.setValue(samplingVar, simulatedValue);
        }
        double logWeight = numerator-denominator;
        return new WeightedAssignment(samplingAssignment,logWeight);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedValue(Variable var, Function<Double,Double> function) {

//        if(keepDataOnMemory) {
//            weightedSampleStream = weightedSampleList.stream().sequential();
//        }else{
//            computeWeightedSampleStream(false);
//        }

        if(parallelMode) {
            weightedSampleStream.parallel();
        }
        List<Double> sum = weightedSampleStream
                .map(ws -> Arrays.asList(Math.exp(ws.logWeight), Math.exp(ws.logWeight) * function.apply(ws.assignment.getValue(var))))
                .filter(array -> (Double.isFinite(array.get(0)) && Double.isFinite(array.get(1)) ))
                .reduce(Arrays.asList(new Double(0.0), new Double(0.0)), (e1, e2) -> Arrays.asList(e1.get(0) + e2.get(0), e1.get(1) + e2.get(1)));

        return sum.get(1)/sum.get(0);
    }

    private double robustSumOfLogarithms(double log_x1, double log_x2) {
        double result;
        if(log_x1!=0 && log_x2!=0) {

            double aux_max = Math.max(log_x1,log_x2);
            double aux_min = Math.min(log_x1,log_x2);

            double tail;
            double aux = Math.exp(aux_min-aux_max);
            if (aux<0.5) {
                tail = Math.log1p( aux );
            }
            else {
                tail = Math.log( 1+aux );
            }
//            tail = Math.log( 1+aux );

            //double tail = Math.log1p( Math.exp(aux_min-aux_max) );
            result = aux_max + (Double.isFinite(tail) ? tail : 0);
        }
        else if (log_x1==0) {
            result=log_x2;
        }
        else {
            result=log_x1;
        }
        return result;
    }

    private ArrayVector robustSumOfMultinomialSufficientStatistics(ArrayVector ss1, ArrayVector ss2) {
        double[] ss1_values = ss1.toArray();
        double[] ss2_values = ss2.toArray();
        double[] ss_result = new double[ss1_values.length];

        for (int i = 0; i < ss_result.length; i++) {

            double log_a = ss1_values[i];
            double log_b = ss2_values[i];

//            if(log_a!=0 && log_b!=0) {
//
//                double aux_max = Math.max(log_a,log_b);
//                double aux_min = Math.min(log_a,log_b);
//
//                double tail = Math.log1p( Math.exp(aux_min-aux_max) );
//                //System.out.println(tail);
//                ss_result[i] = aux_max + (Double.isFinite(tail) ? tail : 0);
//            }
//            else if (log_a==0) {
//                ss_result[i]=log_b;
//            }
//            else {
//                ss_result[i]=log_a;
//            }

            ss_result[i] = robustSumOfLogarithms(log_a,log_b);
        }
        return new ArrayVector(ss_result);
    }

    private ArrayVector robustSumOfNormalSufficientStatistics(ArrayVector ss1, ArrayVector ss2) {
        return new ArrayVector(new double[]{ss1.get(0)});

    }

    private ArrayVector robustNormalizationOfLogProbabilitiesVector(ArrayVector ss) {

        //System.out.println("ROBUST NORMALIZATION OF: \n\n" + Arrays.toString(ss.toArray()));
        double log_sumProbabilities = 0;
        double [] logProbabilities = ss.toArray();

        for (int i = 0; i < logProbabilities.length; i++) {


            double log_b = logProbabilities[i];
//
//            double log_a = log_sumProbabilities;
//            if(log_a!=0 && log_b!=0) {
//
//                double aux_max = Math.max(log_a,log_b);
//                double aux_min = Math.min(log_a,log_b);
//
//                double tail = Math.log1p( Math.exp(aux_min-aux_max) );
//
//                log_sumProbabilities = aux_max + (Double.isFinite(tail) ? tail : 0);
//            }
//            else if (log_a==0) {
//                log_sumProbabilities=log_b;
//            }
//            else {
//                log_sumProbabilities=log_a;
//            }
            log_sumProbabilities = robustSumOfLogarithms(log_sumProbabilities, log_b);
        }

        //System.out.println("logSUM: " + log_sumProbabilities);

        //System.out.println("ROBUST NORMALIZATION RESULT: \n\n" + Arrays.toString((new ArrayVector(ss.toArray())).toArray()));

        double [] normalizedProbabilities = new double[logProbabilities.length];

        for (int i = 0; i < logProbabilities.length; i++) {
            double result = Math.exp( logProbabilities[i]-log_sumProbabilities );
            normalizedProbabilities[i] = (0<=result && result<=1) ? result : 0;
        }

        //System.out.println(Arrays.toString(normalizedProbabilities));

        return new ArrayVector(normalizedProbabilities);
    }

    /**
    * {@inheritDoc}
    */
    @Override
    //TODO For continuous variables, instead of returning a Gaussian distributions, we should return a Mixture of Gaussians!!
    public <E extends UnivariateDistribution> E getPosterior(Variable variable) {

        EF_UnivariateDistribution ef_univariateDistribution = variable.newUnivariateDistribution().toEFUnivariateDistribution();

        // TODO Could we build this object in a general way for Multinomial and Normal?
        if (!variable.isMultinomial()) {
            throw new UnsupportedOperationException("ImportanceSamplingRobust.getPosterior() not supported yet for non-multinomial distributions");

        }

        ArrayVector sumSS = new ArrayVector(ef_univariateDistribution.sizeOfSufficientStatistics());
        sumSS.copy(SSvariablesAPosteriori.get(variablesAPosteriori.indexOf(variable)));
        //System.out.println(Arrays.toString(sumSS.toArray()));

        sumSS = robustNormalizationOfLogProbabilitiesVector(sumSS);

        ef_univariateDistribution.setMomentParameters((SufficientStatistics)sumSS);
        Multinomial posteriorDistribution = ef_univariateDistribution.toUnivariateDistribution();
        posteriorDistribution.setProbabilities(Utils.normalize(posteriorDistribution.getParameters()));

        return (E)posteriorDistribution;
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
//            throw new UnsupportedOperationException("ImportanceSamplingRobust.getPosterior() not supported yet for non-multinomial distributions");
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
//                .reduce(this::robustSumOfMultinomialSufficientStatistics).get();
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


   /* */

//    private void computeWeightedSampleStream(boolean saveDataOnMemory_) {
//
//        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
//        if (parallelMode) {
//            weightedSampleStream = IntStream.range(0, sampleSize).parallel()
//                    .mapToObj(i -> generateSample(randomGenerator.current()));
//        } else {
//            weightedSampleStream = IntStream.range(0, sampleSize).sequential()
//                    .mapToObj(i -> generateSample(randomGenerator.current()));
//        }
//
//        if(saveDataOnMemory_) {
//            weightedSampleList = weightedSampleStream.collect(Collectors.toList());
//            //weightedSampleList.forEach(weightedAssignment -> System.out.println("Weight: " + weightedAssignment.logWeight + " for assignment " + weightedAssignment.assignment.getValue(this.model.getVariables().getListOfVariables().get(0)) + " prob " + model.getLogProbabiltyOf(weightedAssignment.assignment)));
//        }
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {

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

        double logSumWeights = weightedSampleStream.mapToDouble(i -> {
            WeightedAssignment weightedSample = generateSample(randomGenerator.current());
            updatePosteriorDistributions(weightedSample.assignment,weightedSample.logWeight);
            //updateLogProbabilityOfEvidence(weightedSample.logWeight);


            //logProbOfEvidence = robustSumOfLogarithms(logProbOfEvidence,weightedSample.logWeight-Math.log(sampleSize));

            //System.out.println(weightedSample.logWeight);

            //System.out.println(logProbOfEvidence);

            return weightedSample.logWeight;

        }).reduce(this::robustSumOfLogarithms).getAsDouble();

        //        return weightedSampleStream.mapToDouble(ws -> ws.logWeight - Math.log(sampleSize)).reduce(this::robustSumOfLogarithms).getAsDouble();


        //logProbOfEvidence = robustSumOfLogarithms(logProbOfEvidence,-Math.log(sampleSize));

        if (evidence!=null) {
            logProbOfEvidence = logSumWeights - Math.log(sampleSize);
        }
        else {
            logProbOfEvidence = 0;
        }

//        if(saveDataOnMemory_) {
//            weightedSampleList = weightedSampleStream.collect(Collectors.toList());
//            //weightedSampleList.forEach(weightedAssignment -> System.out.println("Weight: " + weightedAssignment.logWeight + " for assignment " + weightedAssignment.assignment.getValue(this.model.getVariables().getListOfVariables().get(0)) + " prob " + model.getLogProbabiltyOf(weightedAssignment.assignment)));
//        }


    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(60,2);
        BayesianNetworkGenerator.setNumberOfLinks(100);
        BayesianNetworkGenerator.setSeed(1);
        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
        System.out.println(bn);


        ImportanceSamplingRobust importanceSampling = new ImportanceSamplingRobust();
        importanceSampling.setModel(bn);
        //importanceSampling.setSamplingModel(vmp.getSamplingModel());

        importanceSampling.setParallelMode(true);
        importanceSampling.setSampleSize(5000);
        importanceSampling.setSeed(57457);

        List<Variable> causalOrder = importanceSampling.causalOrder;
        Variable varPosterior = causalOrder.get(0);

        List<Variable> variablesPosteriori = new ArrayList<>(1);
        variablesPosteriori.add(varPosterior);
        importanceSampling.setVariablesAPosteriori( variablesPosteriori);



        importanceSampling.runInference();
//

//
////        for (Variable var: causalOrder) {
////            System.out.println("Posterior (IS) of " + var.getName() + ":" + importanceSampling.getPosterior(var).toString());
////        }
//

        System.out.println("Posterior (IS) of " + varPosterior.getName() + ":" + importanceSampling.getPosterior(varPosterior).toString());
        System.out.println(bn.getConditionalDistribution(varPosterior).toString());
        System.out.println("Log-Prob. of Evidence: " + importanceSampling.getLogProbabilityOfEvidence());



        // Including evidence:
        Variable variableEvidence = causalOrder.get(1);

        int varEvidenceValue=0;

        System.out.println("Evidence: Variable " + variableEvidence.getName() + " = " + varEvidenceValue);
        System.out.println();

        HashMapAssignment assignment = new HashMapAssignment(1);

        assignment.setValue(variableEvidence,varEvidenceValue);


        importanceSampling.setEvidence(assignment);

        long time_start = System.nanoTime();
        importanceSampling.runInference();
        long time_end = System.nanoTime();
        double execution_time = (((double)time_end)-time_start)/1E9;
        System.out.println("Execution time: " + execution_time + " s");

        System.out.println("Posterior of " + varPosterior.getName() + " (IS with Evidence) :" + importanceSampling.getPosterior(varPosterior).toString());


        System.out.println("Log-Prob. of Evidence: " + importanceSampling.getLogProbabilityOfEvidence());
        System.out.println("Prob of Evidence: " + Math.exp(importanceSampling.getLogProbabilityOfEvidence()));



//        Complex complex = new Complex(1,0);
//        Complex log_complex = complex.log();
//        System.out.println(log_complex.getReal() + " + 1i * " + log_complex.getImaginary());
//
//
//        Complex complex1 = new Complex(0.5,-0.3);
//        Complex complex2 = new Complex(0.2,0.1);
//
//        Complex sum = complex1.add(complex2);
//        System.out.println(sum.getReal() + " + 1i * " + sum.getImaginary());


    }

}
