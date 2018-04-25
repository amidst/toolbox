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
 * This class implements a mixture of Factor Analysers. See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 385
 *
 * Created by andresmasegosa on 4/3/16.
 */
public class MixtureOfFactorAnalysers extends Model<MixtureOfFactorAnalysers> {


    /**
     * Number of continuous latent variables
     */
    private int numberOfLatentVariables;

    /**
     * Number of states in the discrete latent variable
     */
    private int numberOfStatesLatentDiscreteVar;


    /**
     * Constructor from a list of attributes. By default, sets the number of  continuous latent
     * variabels to 5 and the number of states of the discrete one to 2.
     * @param attributes list of attributes (i.e. its variables)
     * @throws WrongConfigurationException
     */
    public MixtureOfFactorAnalysers(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
        numberOfLatentVariables = 5;
        numberOfStatesLatentDiscreteVar = 2;
    }


    /**
     * Builds the graph
     */
    @Override
    protected void buildDAG() {


        List<Variable> observableVariables = new ArrayList<>();
        List<Variable> latentVariables = new ArrayList<>();

        vars.forEach(observableVariables::add);

        Variable discreteLatentVar = vars.newMultinomialVariable("DiscreteLatentVar",numberOfStatesLatentDiscreteVar);

        IntStream.range(0,numberOfLatentVariables).forEach(i -> {
            Variable latentVar = vars.newGaussianVariable("LatentVar" + i);
            latentVariables.add(latentVar);
        });

        dag = new DAG(vars);

        for (Variable variable : observableVariables) {
            dag.getParentSet(variable).addParent(discreteLatentVar);
            latentVariables.forEach(latentVariable -> dag.getParentSet(variable).addParent(latentVariable));
        }

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




    //////// Getters and setters ////////

    /**
     * Sets the number of latent variables
     * @param numberOfLatentVariables integer value
     */
    public MixtureOfFactorAnalysers setNumberOfLatentVariables(int numberOfLatentVariables) {
        this.numberOfLatentVariables = numberOfLatentVariables;
        resetModel();
        return this;
    }

    /**
     * Sets the number of states in the discrete latent variable
     * @param numberOfStatesLatentDiscreteVar integer value
     */
    public MixtureOfFactorAnalysers setNumberOfStatesLatentDiscreteVar(int numberOfStatesLatentDiscreteVar) {
        this.numberOfStatesLatentDiscreteVar = numberOfStatesLatentDiscreteVar;
        resetModel();
        return this;
    }


    /**
     * Obtains the number of continuous latent variables
     * @return integer value greater or equal to zero
     */
    public int getNumberOfLatentVariables() {
        return numberOfLatentVariables;
    }


    /**
     * Obtains the number of states in the discrete latent variable
     * @return integer value
     */
    public int getNumberOfStatesLatentDiscreteVar() {
        return numberOfStatesLatentDiscreteVar;
    }



    ////// example of use ///////

    public static void main(String[] args) throws WrongConfigurationException {

        int seed=6236;
        int nSamples=5000;
        int nContinuousVars=10;

        DataStream<DataInstance> data = DataSetGenerator.generate(seed,nSamples,0,nContinuousVars);

        Model model = new MixtureOfFactorAnalysers(data.getAttributes());

        System.out.println(model.getDAG());

        model.updateModel(data);

//        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(1000)) {
//            model.updateModel(batch);
//        }

        System.out.println(model.getModel());

    }

}
