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

package eu.amidst.cajamareval;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.learning.parametric.ParallelMLMissingData;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.Classifier;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The NaiveBayesClassifier class implements the interface {@link Classifier} and defines a Naive Bayes Classifier.
 * See Murphy, K. P. (2012). Machine learning: a probabilistic perspective. MIT press, page 82.
 */
public class NBIndicatorsClassifier extends Classifier {

    /**
     * Constructor of the classifier which is initialized with the default arguments:
     * the last variable in attributes is the class variable and importance sampling
     * is the inference algorithm for making the predictions.
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException is thrown when the attributes passed are not suitable
     * for such classifier
     */
    public NBIndicatorsClassifier(Attributes attributes) throws WrongConfigurationException {

        //super();
        super(attributes);

        this.vars = new Variables(attributes);
        // Add indicator variables
        List<Variable> initialVariables = this.vars.getListOfVariables();
        int numberOfInitialVariables = initialVariables.size();
        for (int i = 0; i < numberOfInitialVariables; i++) {
            Variable var = initialVariables.get(i);
            if(var.isNormal()) {
                vars.newIndicatorVariable(var,0);
            }
        }
        //vars.getListOfVariables().forEach(var -> System.out.println(var.getName()));
        this.classVar = vars.getListOfVariables().get(numberOfInitialVariables-1);
        if (!this.classVar.isMultinomial()) {
            this.classVar = vars.getListOfVariables().stream().filter(Variable::isMultinomial).findFirst().get();
        }
        //System.out.println("Class: " + classVar.getName());

        this.setLearningAlgorithm(new ParallelMLMissingData());

//        this.getLearningAlgorithm().setWindowsSize(20000);
        this.inferenceAlgoPredict = new ImportanceSampling();

    }

    /**
     * Builds the DAG over the set of variables given with the naive Bayes structure
     */
    @Override
    protected void buildDAG() {

        dag = new DAG(vars);

        // ADD THE CLASS VARIABLE AS A PARENT FOR EVERY VARIABLE
        dag.getParentSets().stream()
                .filter(w -> !w.getMainVar().equals(classVar))
                .filter(w -> !w.getMainVar().isIndicator())
                .forEach(w -> w.addParent(classVar));

        // ADD INDICATOR VARIABLE AS PARENTS OF EVERY CONTINUOUS VARIABLE
        dag.getParentSets().stream()
                .filter(w -> !w.getMainVar().equals(classVar))
                .filter(w -> w.getMainVar().isNormal())
                .forEach(w -> w.addParent(vars.getVariableByName(w.getMainVar().getName() + "_INDICATOR")));

        // ADD INDICATOR VARIABLE AS PARENTS OF EVERY CONTINUOUS VARIABLE
        dag.getParentSets().stream()
                .filter(w -> !w.getMainVar().equals(classVar))
                .filter(w -> w.getMainVar().isNormal())
                .forEach(w -> {
                    Variable var = w.getMainVar();
                    Variable var_aggregated;
                    try {
                        var_aggregated = vars.getVariableByName(var.getName() + "_AG");
                        w.addParent(var_aggregated);
                    } catch (UnsupportedOperationException e) {

                    }
                });
    }


    /**
     * tests if the attributes passed as an argument in the constructor are suitable for this classifier
     * @return boolean value with the result of the test.
     */
    @Override
    public boolean isValidConfiguration(){
        boolean isValid = true;


        long numFinite = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();


        if(numFinite == 0) {
            isValid = false;
            String errorMsg = "It should contain at least 1 discrete variable and the rest should be real";
            this.setErrorMessage(errorMsg);

        }

        return  isValid;

    }


    /////// Getters and setters



    //////////// example of use

    public static void main(String[] args) throws WrongConfigurationException {



        DataStream<DataInstance> data = DataSetGenerator.generate(1234,20000, 2, 3);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        NBIndicatorsClassifier nb = new NBIndicatorsClassifier(data.getAttributes());
        nb.setWindowSize(5000);

        nb.setClassName(classVarName);

        nb.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

            nb.updateModel(batch);
        }
        System.out.println(nb.getModel());
        System.out.println(nb.getDAG());

        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,100);

        double hits = 0;

        for(DataInstance d : dataTest) {

            double realValue = d.getValue(nb.getClassVar());
            double predValue;

            d.setValue(nb.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = nb.predict(d);


            double[] values = posteriorProb.getProbabilities();
            if (values[0]>values[1]) {
                predValue = 0;
            } else {
                predValue = 1;

            }

            if(realValue == predValue) hits++;

            //System.out.println("realValue = "+realValue+", predicted ="+predValue);

        }

        System.out.println("hits="+hits);



    }
}




