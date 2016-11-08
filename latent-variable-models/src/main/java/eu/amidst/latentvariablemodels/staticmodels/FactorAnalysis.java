/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.latentvariablemodels.staticmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * This class implements Factor Analysis. See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 381
 *
 * Created by andresmasegosa on 4/3/16.
 */
public class FactorAnalysis extends Model<FactorAnalysis> {


    /**
     * Number of continuous latent variables
     */
    private int numberOfLatentVariables;

    /**
     * Constructor from a list of attributes. By default, sets the number of latent
     * variabels to 5.
     * @param attributes list of attributes (i.e. its variables)
     * @throws WrongConfigurationException
     */
    public FactorAnalysis(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
        numberOfLatentVariables = 3;
    }


    /**
     * Sets the number of latent (i.e. hidden) continuous variables in the model
     * @param numberOfLatentVariables positive integer value
     */
    public FactorAnalysis setNumberOfLatentVariables(int numberOfLatentVariables) {
        this.numberOfLatentVariables = numberOfLatentVariables;
        resetModel();
        return this;
    }

    /**
     * Sets the number of latent (i.e. hidden) continuous variables in the model
     */
    public int getNumberOfLatentVariables() {
        return numberOfLatentVariables;
    }

    /**
     * Builds the DAG
     */
    @Override
    protected void buildDAG() {


        List<Variable> observableVariables = new ArrayList<>();
        List<Variable> latentVariables = new ArrayList<>();

        vars.forEach(observableVariables::add);

        IntStream.range(0,numberOfLatentVariables).forEach(i -> {
            Variable latentVar = vars.newGaussianVariable("LatentVar" + i);
            latentVariables.add(latentVar);
        });

        dag = new DAG(vars);

        for (Variable variable : observableVariables) {
            latentVariables.forEach(latentVariable -> dag.getParentSet(variable).addParent(latentVariable));
        }

        IntStream.range(0,numberOfLatentVariables).forEach(i -> {
            Variable latentVarChildren = vars.getVariableByName("LatentVar" + i);
            IntStream.range(0,i).forEach(j -> {
                Variable latentVarParent = vars.getVariableByName("LatentVar" + j);
                dag.getParentSet(latentVarChildren).addParent(latentVarParent);
            });
        });

    }


    /**
     * tests if the attributes passed as an argument in the constructor are suitable
     * @return boolean value with the result of the test.
     */
    @Override
    public boolean isValidConfiguration() {

        boolean isValid  = vars.getListOfVariables().stream()
                .allMatch(Variable::isNormal);

        if(!isValid) {
            String errorMsg = "Invalid configuration: All variables must be real";
            this.setErrorMessage(errorMsg);
        }

        return isValid;
    }




    //// example of use

    public static void main(String[] args) throws WrongConfigurationException {

        int seed=6236;
        int nSamples=5000;
        int nContinuousVars=10;

        DataStream<DataInstance> data = DataSetGenerator.generate(seed,nSamples,0,nContinuousVars);

        Model model = new FactorAnalysis(data.getAttributes());

        System.out.println(model.getDAG());

        model.updateModel(data);

//        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(1000)) {
//            model.updateModel(batch);
//        }

        System.out.println(model.getModel());

        System.out.println(model.getPosteriorDistribution("LatentVar0").toString());


    }

}
