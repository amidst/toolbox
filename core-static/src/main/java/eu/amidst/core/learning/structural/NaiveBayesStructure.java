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

package eu.amidst.core.learning.structural;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 9/6/15.
 */
public class NaiveBayesStructure implements StructuralLearningAlgorithm {

    DataStream<DataInstance> dataStream;
    DAG dag;

    @Override
    public void initLearning() {

    }

    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {
        return 0;
    }

    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        dataStream=data;
    }

    @Override
    public void runLearning() {
        Variables modelHeader = new Variables(dataStream.getAttributes());
        dag = new DAG(modelHeader);
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));
    }

    @Override
    public DAG getDAG() {
        return dag;
    }

    @Override
    public void setParallelMode(boolean parallelMode) {

    }

}
