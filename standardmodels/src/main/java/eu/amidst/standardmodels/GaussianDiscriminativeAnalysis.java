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

/**
 * Created by andresmasegosa on 4/3/16.
 */
public class GaussianDiscriminativeAnalysis extends Model {


    private boolean diagonal = false;


    public GaussianDiscriminativeAnalysis(Attributes attributes) {
        super(attributes);
    }

    @Override
    protected void buildDAG(Attributes attributes) {




        Variable classVar = null; // DEFINIR !!!

        //We create a standard naive Bayes
        Variables vars = new Variables(attributes);
        dag = new DAG(vars);
        dag.getParentSets().stream().filter(w -> w.getMainVar() != classVar).forEach(w -> w.addParent(classVar));

        // if it is not diagonal add the links between the attributes
        if(!isDiagonal()) {

            // completar ...

        }


    }



    /////// Getters and setters

    public boolean isDiagonal() {
        return diagonal;
    }

    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    ////////////

    public static void main(String[] args) {


        DataStream<DataInstance> data = DataStreamLoader.openFromFile("tmp.arff");

        Model model = new GaussianDiscriminativeAnalysis(data.getAttributes());

        System.out.println(model.getDAG());

        model.learnModel(data);

        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(1000)) {
            model.updateModel(batch);
        }


        System.out.println(model.getModel());

    }



}
