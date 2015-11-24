/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.classifiers;

import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The NaiveBayesClassifier class implements the interface {@link Classifier} and defines a Naive Bayes Classifier.
 */
public class NaiveBayesClassifier implements Classifier{

    /** Represents the name of the class variable. */
    String className;

    /** Represents the ID of the class variable. */
    String classVarID;

    /** Represents the Naive Bayes Classifier, which is considered as a {@link BayesianNetwork}. */
    BayesianNetwork bnModel;

    /** Represents the parallel mode, which is initialized as true. */
    boolean parallelMode = true;

    /** Represents the inference algorithm. */
    InferenceAlgorithm predictions;

    /** Represents the class variable */
    Variable classVar;

    /**
     * Creates a new NaiveBayesClassifier.
     */
    public NaiveBayesClassifier(){
        predictions=new VMP();
        predictions.setSeed(0);
    }

    /**
     * Returns whether the parallel mode is supported or not.
     * @return true if the parallel mode is supported.
     */
    public boolean isParallelMode() {
        return parallelMode;
    }

    /**
     * Sets the parallel mode for this NaiveBayesClassifier.
     * @param parallelMode boolean equals to true if the parallel mode is supported, and false otherwise.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified.
     * @return an array of doubles containing the estimated membership probabilities of the data instance for each class label.
     */
    @Override
    public double[] predict(DataInstance instance) {
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");
        this.predictions.setEvidence(instance);
        this.predictions.runInference();
        Multinomial multinomial = this.predictions.getPosterior(classVar);
        return multinomial.getParameters();
    }

    /**
     * Returns the name of the class variable.
     * @return the name of the class variable.
     */
    @Override
    public String getClassName() {
        return className;
    }

    /**
     * Sets the ID of the class variable.
     * @param className the ID of the class variable.
     */
    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns this NaiveBayesClassifier considered as a Bayesian network model.
     * @return a BayesianNetwork.
     */
    public BayesianNetwork getBNModel() {
        return bnModel;
    }

    /**
     * Trains this NaiveBayesClassifier using the given data streams.
     * @param dataStream a data stream {@link DataStream}.
     */
    @Override
    public void learn(DataStream<DataInstance> dataStream){
        this.learn(dataStream,1000);
    }


    /**
     * Trains this NaiveBayesClassifier using the given data streams.
     * @param dataStream a data stream {@link DataStream}.
     * @param batchSize the size of the batch for the parallel ML algorithm.
     */
    public void learn(DataStream<DataInstance> dataStream, int batchSize){
        DAG dag = DAGGenerator.getNaiveBayesStructure(dataStream.getAttributes(), this.className);
        BayesianNetwork naiveBayes = new BayesianNetwork(dag);
        classVar = naiveBayes.getVariables().getVariableByName(this.className);

        EF_BayesianNetwork ef_naiveBayes = new EF_BayesianNetwork(naiveBayes);

        AtomicDouble dataInstanceCount = new AtomicDouble(0); //Initial count


        PartialSufficientSatistics result =
                        dataStream
                        .parallelStream(batchSize)
                        .peek(a -> {if (dataInstanceCount.addAndGet(1)%batchSize==1) System.out.println("Data Instance:"+dataInstanceCount.toString());})
                        .map(dataInstance -> computeCountSufficientStatistics(ef_naiveBayes, dataInstance))
                        .reduce(PartialSufficientSatistics::sumNonStateless).get();

        PartialSufficientSatistics initSS = PartialSufficientSatistics.createInitPartialSufficientStatistics(ef_naiveBayes);
        result.sum(initSS);
        result.normalize();
        SufficientStatistics finalSS = ef_naiveBayes.createZeroSufficientStatistics();
        finalSS.sum(result.getCompoundVector());
        ef_naiveBayes.setMomentParameters(finalSS);


        bnModel = ef_naiveBayes.toBayesianNetwork(dag);

        this.predictions.setModel(bnModel);

    }

    private static PartialSufficientSatistics computeCountSufficientStatistics(EF_BayesianNetwork bn, DataInstance dataInstance){
        List<CountVector> list = bn.getDistributionList().stream().map(dist -> {
            if (Utils.isMissingValue(dataInstance.getValue(dist.getVariable())))
                    return new CountVector();

            for (Variable var: dist.getConditioningVariables())
                if (Utils.isMissingValue(dataInstance.getValue(var)))
                    return new CountVector();

            return new CountVector(dist.getSufficientStatistics(dataInstance));
        }).collect(Collectors.toList());

        return new PartialSufficientSatistics(list);
    }


    static class PartialSufficientSatistics {

        List<CountVector> list;

        public PartialSufficientSatistics(List<CountVector> list) {
            this.list = list;
        }

        public static PartialSufficientSatistics createInitPartialSufficientStatistics(EF_BayesianNetwork ef_bayesianNetwork){
            return new PartialSufficientSatistics(ef_bayesianNetwork.getDistributionList().stream().map(w -> new CountVector(w.createInitSufficientStatistics())).collect(Collectors.toList()));
        }

        public static PartialSufficientSatistics createZeroPartialSufficientStatistics(EF_BayesianNetwork ef_bayesianNetwork){
            return new PartialSufficientSatistics(ef_bayesianNetwork.getDistributionList().stream().map(w -> new CountVector(w.createZeroSufficientStatistics())).collect(Collectors.toList()));
        }

        public void normalize(){
            list.stream().forEach(a -> a.normalize());
        }

        public void sum(PartialSufficientSatistics a){
            for (int i = 0; i < this.list.size(); i++) {
                this.list.get(i).sum(a.list.get(i));
            }
        }

        public static PartialSufficientSatistics sumNonStateless(PartialSufficientSatistics a, PartialSufficientSatistics b) {
            for (int i = 0; i < b.list.size(); i++) {
                b.list.get(i).sum(a.list.get(i));
            }
            return b;
        }

        public CompoundVector getCompoundVector(){
            List<Vector> ssList = this.list.stream().map(a -> a.sufficientStatistics).collect(Collectors.toList());
            return new CompoundVector(ssList);
        }
    }

    static class CountVector {

        SufficientStatistics sufficientStatistics;
        int count;

        public CountVector() {
            count=0;
            sufficientStatistics=null;
        }

        public CountVector(SufficientStatistics sufficientStatistics) {
            this.sufficientStatistics = sufficientStatistics;
            this.count=1;
        }

        public void normalize(){
            this.sufficientStatistics.divideBy(count);
        }

        public void sum(CountVector a){
            if (a.sufficientStatistics==null)
                return;

            this.count+=a.count;

            if (this.sufficientStatistics==null) {
                this.sufficientStatistics = a.sufficientStatistics;
            }else{
                this.sufficientStatistics.sum(a.sufficientStatistics);
            }
        }
    }

}
