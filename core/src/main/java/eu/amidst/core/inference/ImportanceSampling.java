/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.inference;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.LocalRandomGenerator;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * This class implements the interface {@link InferenceAlgorithm} and defines the Importance Sampling algorithm.
 * J.M. Hammersley and D.C. Handscomb. Monte Carlo Methods. Methuen and Co, London, UK, 1964.
 *
 * <p> For an example of use follow this link
 * <a href="http://amidst.github.io/toolbox/CodeExamples.html#isexample"> http://amidst.github.io/toolbox/CodeExamples.html#isexample </a>  </p>
 */
public class ImportanceSampling implements InferenceAlgorithm, Serializable {

    private static final long serialVersionUID = 8587756877237341367L;

    private BayesianNetwork model;
    private BayesianNetwork samplingModel;
    private int sampleSize = 100;
    private List<Variable> causalOrder;

    private boolean keepDataOnMemory = true;
    private List<ImportanceSampling.WeightedAssignment> weightedSampleList;
    private Stream<ImportanceSampling.WeightedAssignment> weightedSampleStream;
    private int seed = 0;
    private Assignment evidence;
    private boolean parallelMode = true;

    private class WeightedAssignment {
        private HashMapAssignment assignment;
        private double weight;

        public WeightedAssignment(HashMapAssignment assignment_, double weight_){
            this.assignment = assignment_;
            this.weight = weight_;
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("[ ");

            for (Map.Entry<Variable, Double> entry : this.assignment.entrySet()) {
                str.append(entry.getKey().getName() + " = " + entry.getValue());
                str.append(", ");
            }
            str.append("Weight = " + weight + " ]");
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
        this.model = model_;
        this.setSamplingModel(model_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;
    }

    /**
     * Sets the sampling model for this ImportanceSampling.
     * @param samplingModel_ a {@link BayesianNetwork} model according to which samples will be taken.
     */
    public void setSamplingModel(BayesianNetwork samplingModel_) {
        this.samplingModel = new BayesianNetwork(samplingModel_.getDAG(),
                Serialization.deepCopy(samplingModel_.getConditionalDistributions()));
        this.causalOrder = Utils.getTopologicalOrder(samplingModel.getDAG());
    }

    /**
     * Sets the number of samples to be drawn from the sampling model.
     * @param sampleSize an {@code int} that represents the number of samples.
     */
    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public void setKeepDataOnMemory(boolean keepDataOnMemory) {
        this.keepDataOnMemory = keepDataOnMemory;
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

        if(keepDataOnMemory) {
            weightedSampleStream = weightedSampleList.stream().sequential();
        }else{
            computeWeightedSampleStream(false);
        }
        if(parallelMode) {
            weightedSampleStream.parallel();
        }

        return Math.log(weightedSampleStream.mapToDouble(ws -> ws.weight).average().getAsDouble());
    }

    /**
     * Returns a {@link Stream} containing the drawn samples after running the inference.
     * @return a {@link Stream} of {@link HashMapAssignment} objects.
     */
    public Stream<Assignment> getSamples() {

        if(keepDataOnMemory) {
            weightedSampleStream = weightedSampleList.stream().sequential();
        }
        if(parallelMode) {
            weightedSampleStream.parallel();
        }
        return weightedSampleStream.map(wsl -> wsl.assignment);
    }


    private WeightedAssignment getWeightedAssignment(Random random) {

        HashMapAssignment samplingAssignment = new HashMapAssignment(1);
        HashMapAssignment modelAssignment = new HashMapAssignment(1);
        double numerator = 1.0;
        double denominator = 1.0;

        for (Variable samplingVar : causalOrder) {

            Variable modelVar = this.model.getVariables().getVariableById(samplingVar.getVarID());
            double simulatedValue;

            if( evidence!=null && !Double.isNaN(evidence.getValue(samplingVar))) {
                simulatedValue=evidence.getValue(samplingVar);

                UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
                numerator = numerator * univariateModelDistribution.getProbability(simulatedValue);
            }
            else {
                ConditionalDistribution samplingDistribution = this.samplingModel.getConditionalDistribution(samplingVar);
                UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);

                simulatedValue = univariateSamplingDistribution.sample(random);
                denominator = denominator / univariateSamplingDistribution.getProbability(simulatedValue);

                UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
                numerator = numerator * univariateModelDistribution.getProbability(simulatedValue);

            }
            modelAssignment.setValue(modelVar,simulatedValue);
            samplingAssignment.setValue(samplingVar, simulatedValue);
        }
        double weight = numerator*denominator;
        return new WeightedAssignment(samplingAssignment,weight);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedValue(Variable var, Function<Double,Double> function) {

        if(keepDataOnMemory) {
            weightedSampleStream = weightedSampleList.stream().sequential();
        }else{
            computeWeightedSampleStream(false);
        }

        if(parallelMode) {
            weightedSampleStream.parallel();
        }
        List<Double> sum = weightedSampleStream
                .map(ws -> Arrays.asList(ws.weight, ws.weight * function.apply(ws.assignment.getValue(var))))
                .reduce(Arrays.asList(new Double(0.0), new Double(0.0)), (e1, e2) -> Arrays.asList(e1.get(0) + e2.get(0), e1.get(1) + e2.get(1)));

        return sum.get(1)/sum.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //TODO For continuous variables, instead of returning a Gaussian distributions, we should return a Mixture of Gaussians!!
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {

        Variable samplingVar = this.samplingModel.getVariables().getVariableByName(var.getName());
        // TODO Could we build this object in a general way for Multinomial and Normal?
        EF_UnivariateDistribution ef_univariateDistribution = samplingVar.newUnivariateDistribution().toEFUnivariateDistribution();

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        if(keepDataOnMemory) {
            weightedSampleStream = weightedSampleList.stream().sequential();
        }else{
            computeWeightedSampleStream(false);
        }

        if(parallelMode) {
            weightedSampleStream.parallel();
        }


        SufficientStatistics sumSS = weightedSampleStream
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(e -> {
                    SufficientStatistics SS = ef_univariateDistribution.getSufficientStatistics(e.assignment);
                    SS.multiplyBy(e.weight);
                    return SS;
                })
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        sumSS.divideBy(dataInstanceCount.get());

        ef_univariateDistribution.setMomentParameters(sumSS);

        Distribution posteriorDistribution = ef_univariateDistribution.toUnivariateDistribution();

        //Normalize Multinomial distributions
        if(var.isMultinomial()) {
            ((Multinomial) posteriorDistribution).
                    setProbabilities(Utils.normalize(((Multinomial) posteriorDistribution).getProbabilities()));
        }

        return (E)posteriorDistribution;
    }

    private void computeWeightedSampleStream(boolean saveDataOnMemory_) {

        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        if (parallelMode) {
            weightedSampleStream = IntStream.range(0, sampleSize).parallel()
                    .mapToObj(i -> getWeightedAssignment(randomGenerator.current()));
        } else {
            weightedSampleStream = IntStream.range(0, sampleSize).sequential()
                    .mapToObj(i -> getWeightedAssignment(randomGenerator.current()));
        }

        if(saveDataOnMemory_){
                weightedSampleList = weightedSampleStream.collect(Collectors.toList());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {
        if(keepDataOnMemory) computeWeightedSampleStream(true);
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");

        System.out.println(bn.toString());


        VMP vmp = new VMP();
        vmp.setModel(bn);
        vmp.runInference();

        ImportanceSampling importanceSampling = new ImportanceSampling();
        importanceSampling.setModel(bn);
        //importanceSampling.setSamplingModel(vmp.getSamplingModel());

        importanceSampling.setParallelMode(true);
        importanceSampling.setSampleSize(100);
        importanceSampling.setSeed(57457);
        importanceSampling.setKeepDataOnMemory(true);
        importanceSampling.runInference();

        List<Variable> causalOrder = importanceSampling.causalOrder;

        for (Variable var: causalOrder) {
            System.out.println("Posterior (IS) of " + var.getName() + ":" + importanceSampling.getPosterior(var).toString());
            System.out.println("Posterior (VMP) of " + var.getName() + ":" + vmp.getPosterior(var).toString());
        }



        // Including evidence:
        Variable variable1 = causalOrder.get(1);  // causalOrder: A,S,L,T,E,X,B,D
        Variable variable2 = causalOrder.get(2);

        int var1value=0;
        int var2value=0;

        System.out.println();
        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + " and Variable " + variable2.getName() + " = " + var2value);
        System.out.println();

        HashMapAssignment assignment = new HashMapAssignment(2);

        assignment.setValue(variable1,var1value);
        assignment.setValue(variable2,var2value);

        importanceSampling.setEvidence(assignment);
        importanceSampling.runInference();

        for (Variable var: causalOrder) {
            System.out.println("Posterior of " + var.getName() + " (IS with Evidence) :" + importanceSampling.getPosterior(var).toString());
            //System.out.println("Posterior of " + var.getName() + " (VMP) :" + vmp.getPosterior(var).toString());
        }

        System.out.printf("Prob. of Evidence: " + Math.exp(importanceSampling.getLogProbabilityOfEvidence()));
    }

}
