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
import eu.amidst.core.inference.messagepassage.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.LocalRandomGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by afa on 10/2/15.
 */

public class ImportanceSampling implements InferenceAlgorithmForBN {

    private BayesianNetwork model;
    private BayesianNetwork samplingModel;
    private int sampleSize;
    private List<Variable> causalOrder;
    public Stream<WeightedAssignment> weightedSampleStream;
    private int seed = 0;
    //TODO The sampling distributions must be restricted to the evidence
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

    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
    }

    public ImportanceSampling() {

    }
    public void setSamplingModel(BayesianNetwork samplingModel_) {
        this.samplingModel = samplingModel_;
        this.causalOrder = Utils.getCausalOrder(samplingModel.getDAG());
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    private WeightedAssignment getWeightedAssignment(Random random) {

        HashMapAssignment samplingAssignment = new HashMapAssignment(1);
        HashMapAssignment modelAssignment = new HashMapAssignment(1);
        double numerator = 1.0;
        double denominator = 1.0;

        for (Variable samplingVar : causalOrder) {
            Variable modelVar = this.model.getStaticVariables().getVariableById(samplingVar.getVarID());
            ConditionalDistribution samplingDistribution = this.samplingModel.getConditionalDistribution(samplingVar);
            UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);

            double simulatedValue;
            simulatedValue = univariateSamplingDistribution.sample(random);
            denominator = denominator/univariateSamplingDistribution.getProbability(simulatedValue);
            UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
            numerator = numerator * univariateModelDistribution.getProbability(simulatedValue);
            modelAssignment.setValue(modelVar,simulatedValue);
            samplingAssignment.setValue(samplingVar, simulatedValue);
        }
        double weight = numerator*denominator;
        WeightedAssignment weightedAssignment = new WeightedAssignment(samplingAssignment,weight);
        return weightedAssignment;
    }

    private void computeWeightedSampleStream() {

        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);
        weightedSampleStream =  IntStream.range(0, sampleSize).mapToObj(i -> getWeightedAssignment(randomGenerator.current()));
        if (parallelMode) weightedSampleStream = weightedSampleStream.parallel();
    }

    @Override
    public void runInference() {
       this.computeWeightedSampleStream();
    }

    @Override
    public void setModel(BayesianNetwork model_) {
        this.model = model_;
    }

    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }

    @Override
    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;
    }

    @Override
    //TODO For continuous variables, instead of returning a Gaussian distributions, we should return a Mixture of Gaussians instead!!!
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {

        Variable samplingVar = this.samplingModel.getStaticVariables().getVariableById(var.getVarID());
        // TODO Could we build this object in a general way for Multinomial and Normal?
        EF_UnivariateDistribution ef_univariateDistribution = samplingVar.newUnivariateDistribution().toEFUnivariateDistribution();

        AtomicInteger dataInstanceCount = new AtomicInteger(0);
        SufficientStatistics sumSS = weightedSampleStream
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(e -> {
                    SufficientStatistics SS = ef_univariateDistribution.getSufficientStatistics(e.assignment);
                    SS.multiplyBy(e.weight);
                    return SS;
                })
                .reduce(SufficientStatistics::sumVector).get();

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

    @Override
    public double getLogProbabilityOfEvidence() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSeed(int seed) {
        this.seed=seed;
    }



    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");

        System.out.println(bn.toString());


        VMP vmp = new VMP();
        vmp.setModel(bn);
        vmp.runInference();

        ImportanceSampling importanceSampling = new ImportanceSampling();
        importanceSampling.setModel(bn);
        importanceSampling.setSamplingModel(vmp.getSamplingModel());
        importanceSampling.setParallelMode(true);
        importanceSampling.setSampleSize(1000000);


        for (Variable var: bn.getStaticVariables()){
            importanceSampling.runInference();
            System.out.println("Posterior of " + var.getName() + ":" + importanceSampling.getPosterior(var).toString());
            System.out.println("Posterior (VMP) of " + var.getName() + ":" + vmp.getPosterior(var).toString());
        }


    }

}
