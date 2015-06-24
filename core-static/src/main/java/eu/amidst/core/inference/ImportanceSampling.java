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
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Created by afa on 10/2/15.
 */

public class ImportanceSampling implements InferenceAlgorithm {

    private BayesianNetwork model;
    private BayesianNetwork samplingModel;
    private int sampleSize;
    private List<Variable> causalOrder;
    public Stream<ImportanceSampling.WeightedAssignment> weightedSampleList;
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
        return getWeightedAssignment(random, new VMP());
    }

    private WeightedAssignment getWeightedAssignment(Random random, VMP vmp) {

        HashMapAssignment samplingAssignment = new HashMapAssignment(1);
        HashMapAssignment modelAssignment = new HashMapAssignment(1);
        double numerator = 1.0;
        double denominator = 1.0;

        for (Variable samplingVar : causalOrder) {

            Variable modelVar = this.model.getStaticVariables().getVariableById(samplingVar.getVarID());
            double simulatedValue;

            if( evidence!=null && !Double.isNaN(evidence.getValue(samplingVar))) {
                //System.out.println("Evidence: " + samplingVar.getName () + " = " + this.evidence.getValue(samplingVar) );
                simulatedValue=evidence.getValue(samplingVar);

                //UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
                //numerator = numerator * univariateModelDistribution.getProbability(simulatedValue);
            }
            else {

                ConditionalDistribution samplingDistribution = this.samplingModel.getConditionalDistribution(samplingVar);
                UnivariateDistribution univariateSamplingDistribution;

                if (vmp.getNumberOfIterations()==0 ) {
                    univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);
                }
                else {

                    univariateSamplingDistribution = vmp.getPosterior(samplingVar);
                    //System.out.println(univariateSamplingDistribution.outputString());
                }

                simulatedValue = univariateSamplingDistribution.sample(random);
                denominator = denominator / univariateSamplingDistribution.getProbability(simulatedValue);

                UnivariateDistribution univariateModelDistribution = this.model.getConditionalDistribution(modelVar).getUnivariateDistribution(modelAssignment);
                numerator = numerator * univariateModelDistribution.getProbability(simulatedValue);

            }

            //ConditionalDistribution samplingDistribution = this.samplingModel.getConditionalDistribution(samplingVar);
            //UnivariateDistribution univariateSamplingDistribution = samplingDistribution.getUnivariateDistribution(samplingAssignment);

            modelAssignment.setValue(modelVar,simulatedValue);
            samplingAssignment.setValue(samplingVar, simulatedValue);
        }
        double weight = numerator*denominator;
        return new WeightedAssignment(samplingAssignment,weight);
    }

    private void computeWeightedSampleStream() {
        this.computeWeightedSampleStream(new VMP());
    }

    private void computeWeightedSampleStream(VMP vmp) {

        LocalRandomGenerator randomGenerator = new LocalRandomGenerator(seed);

        IntStream auxIntStream = IntStream.range(0, sampleSize);
        if (parallelMode) {
            auxIntStream.parallel();
        }
        else {
            auxIntStream.sequential();
        }

        if(vmp.getNumberOfIterations()==0) {
            //weightedSampleList = auxIntStream.mapToObj(i -> getWeightedAssignment(randomGenerator.current())).collect(Collectors.toList());
            weightedSampleList = auxIntStream.mapToObj(i -> getWeightedAssignment(randomGenerator.current()));
            //weightedSampleList =  IntStream.range(0, sampleSize).mapToObj(i -> getWeightedAssignment(randomGenerator.current()));

        }
        else {
            //weightedSampleList = auxIntStream.mapToObj(i -> getWeightedAssignment(randomGenerator.current(), vmp)).collect(Collectors.toList());
            weightedSampleList = auxIntStream.mapToObj(i -> getWeightedAssignment(randomGenerator.current(), vmp));
        }
    }

    @Override
    public double getExpectedValue(Variable var, Function<Double,Double> function) {
        List<Double> sum = weightedSampleList
                .map(ws ->Arrays.asList(ws.weight, ws.weight*function.apply(ws.assignment.getValue(var))))
                .reduce(Arrays.asList(0.0, 0.0), (e1, e2) -> Arrays.asList(e1.get(0) + e2.get(0), e1.get(1) + e2.get(1)));

        return sum.get(1)/sum.get(0);
    }

    public double runQuery(Variable continuousVarInterest, double a, double b) {

        double probInterest;

        if ( a >=b ) {
            System.out.println("Error: a must be less than b");
            System.exit(1);
        }
        if (evidence!=null && !Double.isNaN(evidence.getValue(continuousVarInterest))) {

            System.out.println("Error: quering about a variable with evidence");
            System.exit(1);
        }


        //double sumWeights;
        double sumWeightsSuccess;
        double sumAllWeights;

        if (parallelMode) {
            //sumWeights = weightedSampleList.stream().parallel()
            //        .mapToDouble(ws -> ws.weight).sum();
            //sumWeightsSuccess = weightedSampleList.stream().parallel()
            /*sumWeightsSuccess = weightedSampleList
                    .filter(ws -> (ws.assignment.getValue(continuousVarInterest)>a) && (ws.assignment.getValue(continuousVarInterest)<b))
                    .mapToDouble(ws -> ws.weight).sum();
            sumAllWeights = weightedSampleList.parallel()
                    .mapToDouble(ws -> ws.weight).sum();*/
            List<Double> sum = weightedSampleList.map(ws ->
                    { if ((ws.assignment.getValue(continuousVarInterest)>a && ws.assignment.getValue(continuousVarInterest)<b))
                        return Arrays.asList(ws.weight, ws.weight);
                    else
                        return Arrays.asList(ws.weight , 0.0);
                    }
            ).reduce(Arrays.asList(0.0,0.0), (e1,e2) -> Arrays.asList(e1.get(0)+e2.get(0),e1.get(1)+e2.get(1)));
            sumWeightsSuccess = sum.get(1);
            sumAllWeights=sum.get(0);
        }
        else {
            //sumWeights = weightedSampleList.stream().sequential()
            //        .mapToDouble(ws -> ws.weight).sum();
            /*sumWeightsSuccess = weightedSampleList.sequential()
                    .filter(ws -> (ws.assignment.getValue(continuousVarInterest)>a && ws.assignment.getValue(continuousVarInterest)<b))
                    .mapToDouble(ws -> ws.weight).sum();
            sumAllWeights = weightedSampleList.sequential()
                    .mapToDouble(ws -> ws.weight).sum();*/
            List<Double> sum = weightedSampleList.map(ws ->
                    { if ((ws.assignment.getValue(continuousVarInterest)>a && ws.assignment.getValue(continuousVarInterest)<b))
                        return Arrays.asList(ws.weight, ws.weight);
                    else
                        return Arrays.asList(ws.weight , 0.0);
                    }
            ).reduce(Arrays.asList(0.0,0.0), (e1,e2) -> Arrays.asList(e1.get(0)+e2.get(0),e1.get(1)+e2.get(1)));
            sumWeightsSuccess = sum.get(1);
            sumAllWeights=sum.get(0);
        }

        //System.out.println(sumWeights);
        //System.out.println(meanWeightsSuccess);
        //System.out.println(meanAllWeights);

        probInterest = sumWeightsSuccess/sumAllWeights;

        return probInterest;
    }

    public double runQuery(Variable discreteVarInterest, int w) {

        double probInterest;

        if( w<0 || w>= discreteVarInterest.getNumberOfStates()) {
            return 0;
        }
        if (evidence!=null && !Double.isNaN(evidence.getValue(discreteVarInterest))) {

            System.out.println("Error: quering about a Variable with evidence");
            System.exit(1);
        }


        //double sumWeights;
        double sumWeightsSuccess;
        double sumAllWeights;

        if (parallelMode) {
            //sumWeights = weightedSampleList.stream().parallel()
            //        .mapToDouble(ws -> ws.weight).sum();
            sumWeightsSuccess = weightedSampleList.parallel()
                    .filter(ws -> new Double(ws.assignment.getValue(discreteVarInterest)).compareTo((double)w)==0)
                    .mapToDouble(ws -> ws.weight).sum();
            sumAllWeights = weightedSampleList.parallel()
                    .mapToDouble(ws -> ws.weight).sum();
        }
        else {
            //sumWeights = weightedSampleList.stream().sequential()
            //        .mapToDouble(ws -> ws.weight).sum();
            sumWeightsSuccess = weightedSampleList.sequential()
                    .filter(ws -> new Double(ws.assignment.getValue(discreteVarInterest)).compareTo((double)w)==0)
                    .mapToDouble(ws -> ws.weight).sum();
            sumAllWeights = weightedSampleList.sequential()
                    .mapToDouble(ws -> ws.weight).sum();
        }

        probInterest = sumWeightsSuccess/sumAllWeights;

        return probInterest;

    }

    @Override
    public void runInference() {
       this.computeWeightedSampleStream();
    }

    public void runInference(VMP vmp) {
        this.computeWeightedSampleStream(vmp);
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

        SufficientStatistics sumSS = weightedSampleList
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
        importanceSampling.setSampleSize(1000);


        for (Variable var: bn.getStaticVariables()){
            importanceSampling.runInference();
            System.out.println("Posterior of " + var.getName() + ":" + importanceSampling.getPosterior(var).toString());
            System.out.println("Posterior (VMP) of " + var.getName() + ":" + vmp.getPosterior(var).toString());
        }

        ImportanceSampling importanceSampling2 = new ImportanceSampling();
        importanceSampling2.setModel(bn);
        importanceSampling2.setSamplingModel(vmp.getSamplingModel());
        importanceSampling2.setParallelMode(true);
        importanceSampling2.setSampleSize(100);


        // Including evidence:
        Variable variable1 = importanceSampling2.causalOrder.get(1);  // causalOrder: X,B,D,A,S,L,T,E
        Variable variable2 = importanceSampling2.causalOrder.get(2);

        int var1value=0;
        int var2value=1;

        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + " and Variable " + variable2.getName() + " = " + var2value);
        System.out.println();

        HashMapAssignment assignment = new HashMapAssignment(2);

        assignment.setValue(variable1,var1value);
        assignment.setValue(variable2,var2value);

        importanceSampling2.setEvidence(assignment);

        for (Variable var: bn.getStaticVariables()) {
            importanceSampling.runInference(vmp);
            importanceSampling2.runInference();
            System.out.println("Posterior of " + var.getName() + "  (IS w/o Evidence) :" + importanceSampling.getPosterior(var).toString());
            System.out.println("Posterior of " + var.getName() + "  (IS w. Evidence) :" + importanceSampling2.getPosterior(var).toString());
            System.out.println("Posterior of " + var.getName() + " (VMP) :" + vmp.getPosterior(var).toString());
        }

    }

}
