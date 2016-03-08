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

package eu.amidst.standardmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;
import org.apache.commons.lang.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 4/3/16.
 */
public class FactorAnalysis extends Model {

    private int numberOfLatentVariables = 5;

    public FactorAnalysis(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
    }

    public void setNumberOfLatentVariables(int numberOfLatentVariables) {
        this.numberOfLatentVariables = numberOfLatentVariables;
    }

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



    @Override
    public boolean isValidConfiguration(){
        throw new NotImplementedException("The method isValidConfiguration() has not been implemented for the class "+this.getClass().getName());
    }

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticDataVerdandeScenario3.arff");

        Model model = new FactorAnalysis(data.getAttributes());

        System.out.println(model.getDAG());

        model.learnModel(data);

//        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(1000)) {
//            model.updateModel(batch);
//        }

        System.out.println(model.getModel());

    }

}
