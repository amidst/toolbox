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

package eu.amidst.latentvariablemodels.staticmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The AODE class implements the interface {@link Classifier} and defines a (G)AODE Classifier.
 * It can learn from datasets with either all discrete or all continuous attributes.
 *
 * For more details:
 *
 * See Geoffrey I. Webb, Janice R. Boughton, Zhihai Wang: Not So Naive Bayes: Aggregating One-Dependence Estimators.
 * Machine Learning 58(1): 5-24 (2005).
 *
 * See M. Julia Flores, José A. Gámez, Ana M. Martínez, Jose Miguel Puerta: GAODE and HAODE: two proposals based on AODE
 * to deal with continuous variables. ICML 2009: 313-320.
 *
 * Created by ana@cs.aau.dk on 05/03/16.
 */
public class AODE extends Classifier<AODE> {

    private List<Variable> classVariables;
    /**
     * Constructor of a classifier which is initialized with the default arguments:
     * the last variable in attributes is the class variable and importance sampling
     * is the inference algorithm for making the predictions.
     *
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException is thrown when the attributes passed are not suitable
     *                                     for such classifier
     */
    public AODE(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
        this.setLearningAlgorithm(new ParallelMaximumLikelihood());
        classVariables = new ArrayList<>();
    }

    @Override
    protected void buildDAG() {

        int numPredictiveAtts = vars.getNumberOfVars()-1;

        Variable[][] ODEmodels = new Variable[numPredictiveAtts][numPredictiveAtts+1];

        List<Variable> originalVars = Serialization.deepCopy(vars.getListOfVariables());

        //The order of the variables created in the matrix is irrelevant
        //One row corresponds to one ODE
        int varIndex = 0;
        int classIndex = -1;
        for (Variable var : originalVars) {

            if(var.equals(classVar)) {
                classIndex = varIndex;
                classVariables.add(classVar);
            }

            ODEmodels[0][varIndex] = var;
            for (int i = 1; i < numPredictiveAtts; i++) {

                Variable replica;
                if(var.isMultinomial()){
                    replica = vars.newVariable(var.getAttribute(),
                            DistributionTypeEnum.MULTINOMIAL,
                            var.getName() + i);
                }else if (var.isNormal()) {
                    replica = vars.newVariable(var.getAttribute(),
                            DistributionTypeEnum.NORMAL,
                            var.getName() + i);
                }else throw new UnsupportedOperationException("Variables must be multinomial or normal");
                ODEmodels[i][varIndex] = replica;
                if(var.equals(classVar)) {
                    classVariables.add(replica);
                }

            }

            varIndex++;

        }

        dag = new DAG(vars);

        //We choose a superparent index (anyone that is different from the classIndex)
        int superParentIndex = 0;
        if(classIndex==0)
            superParentIndex = 1;


        //We add a link from each class to (all) the other attributes in the same row
        //We add a link from the superparent to all other predictive attributes
        for (int i = 0; i < numPredictiveAtts; i++) {

            for (int j = 0; j < numPredictiveAtts+1; j++) {

                if(j==classIndex)
                    continue;
                if(j!=superParentIndex) //Add Superparent for all children
                    this.dag.getParentSet(ODEmodels[i][j]).addParent(ODEmodels[i][superParentIndex]);
                this.dag.getParentSet(ODEmodels[i][j]).addParent(ODEmodels[i][classIndex]);

            }
        }

    }

    @Override
    public Multinomial predict(DataInstance instance) {
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");

        inferenceAlgoPredict.setModel(this.getModel());
        this.inferenceAlgoPredict.setEvidence(instance);

        System.out.println(instance);

        this.inferenceAlgoPredict.runInference();

        List<Multinomial> posteriors = new ArrayList<>();

        for (Variable classVariable : classVariables) {
             posteriors.add(this.inferenceAlgoPredict.getPosterior(classVariable));
        }

        double[] vectorPosteriors = new double[classVar.getNumberOfStates()];

        for (Multinomial posterior : posteriors) {
            for (int i = 0; i < classVar.getNumberOfStates(); i++) {
                vectorPosteriors[i] += posterior.getParameters()[i];
            }
        }

        posteriors.get(0).setProbabilities(Utils.normalize(vectorPosteriors));
        return posteriors.get(0);
    }

    @Override
    public boolean isValidConfiguration() {

        boolean isValid = true;

        long numFinite = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();

        long numReal = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .count();

        if(numFinite > 1 && numReal > 0) {
            isValid = false;
            String errorMsg = "Invalid configuration: There should be at least 2 discrete variables (root and class)";
            this.setErrorMessage(errorMsg);
        }

        return  isValid;
    }






    public static void main(String[] args) {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 1, 5);

        String classVarName = "DiscreteVar0";

        AODE aode = new AODE(data.getAttributes())
                .setWindowSize(100)
                .setClassName(classVarName);

        //aode.setClassName(classVarName);

        aode.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

            aode.updateModel(batch);
        }
        System.out.println(aode.getModel());
        System.out.println(aode.getDAG());

        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,100);

        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(aode.getClassVar());
            double predValue;

            d.setValue(aode.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = aode.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            }else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;

            System.out.println("realValue = "+realValue+", predicted ="+predValue);

        }

        System.out.println("hits="+hits);


    }

}
