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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 4/3/16.
 */
public class GaussianDiscriminativeAnalysis extends Model {


    private boolean diagonal;
    private Variable classVar = null;



    public GaussianDiscriminativeAnalysis(Attributes attributes) {
        super(attributes);


        Variables vars = new Variables(attributes);

        // default parameters
        classVar = vars.getListOfVariables().get(vars.getNumberOfVars()-1);
        diagonal = false;

    }



    @Override
    protected void buildDAG(Attributes attributes) {





        //We create a standard naive Bayes
        Variables vars = new Variables(attributes);


        dag = new DAG(vars);

        dag.getParentSets().stream().filter(w -> w.getMainVar() != classVar).forEach(w -> w.addParent(classVar));




        // if it is not diagonal add the links between the attributes
        if(!isDiagonal()) {
            List<Variable> attrVars = vars.getListOfVariables().stream().filter(v -> !v.equals(classVar)).collect(Collectors.toList());

            for (int i=0; i<attrVars.size()-1; i++){
                for(int j=i+1; j<attrVars.size(); j++) {
                    // Add the links
                    dag.getParentSet(attrVars.get(i)).addParent(attrVars.get(j));



                }

            }


        }


    }



    /////// Getters and setters

    public boolean isDiagonal() {
        return diagonal;
    }

    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    public Variable getClassVar() {
        return classVar;
    }

    public void setClassVar(Variable classVar) {
        this.classVar = classVar;
    }


    public void setClassVar(int indexVar) {
        Variables vars = new Variables(attributes);
        classVar = vars.getListOfVariables().get(indexVar);
    }


    ////////////

    public static void main(String[] args) {


        String file = "datasets/tmp2.arff";
        //file = "datasets/WasteIncineratorSample.arff";
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(file);

        GaussianDiscriminativeAnalysis gda = new GaussianDiscriminativeAnalysis(data.getAttributes());

        gda.setDiagonal(true);
        gda.setClassVar(3);

        System.out.println(gda.getDAG());

        gda.learnModel(data);

        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            System.out.println("update model");
            gda.updateModel(batch);
        }


        System.out.println(gda.getModel());

    }



}
