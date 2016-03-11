/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.standardmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The NaiveBayesClassifier class implements the interface {@link Classifier} and defines a Naive Bayes Classifier.
 */
public class NaiveBayesClassifier extends Classifier{




    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     * @param attributes
     * @throws WrongConfigurationException
     */
    public NaiveBayesClassifier(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
    }


    @Override
    protected void buildDAG() {

        dag = new DAG(vars);
        dag.getParentSets().stream().filter(w -> !w.getMainVar().equals(classVar)).forEach(w -> w.addParent(classVar));

    }

    @Override
    public boolean isValidConfiguration(){
        boolean isValid = true;


        long numFinite = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();


        if(numFinite == 0) {
            isValid = false;
            String errorMsg = "It should contain at least 1 discrete variable and the rest shoud be real";
            this.setErrorMessage(errorMsg);

        }

        return  isValid;

    }


    /////// Getters and setters



    //////////// example of use

    public static void main(String[] args) throws WrongConfigurationException {

        //DataStream<DataInstance> data = DataSetGenerator.generate(1234,1000, 2, 10);

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 2, 10);
       // DataStream<DataInstance> dataTest = DataSetGenerator.generate(1234,100, 2, 10);


        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        NaiveBayesClassifier nb = new NaiveBayesClassifier(data.getAttributes());
        nb.setClassName(classVarName);

        if(nb.isValidConfiguration()) {
            nb.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {

                nb.updateModel(batch);
            }
            System.out.println(nb.getModel());
            System.out.println(nb.getDAG());
        }

        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);

        for(DataInstance d : dataTest) {
            d.setValue(nb.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = nb.predict(d);
            System.out.println(posteriorProb.toString());

        }



    }
}




