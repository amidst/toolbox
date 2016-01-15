/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

/**
 * This class defines the dynamic learning engine for {@link DynamicBayesianNetwork} models.
 */
public final class DynamicLearningEngine {

    /** Represents the used dynamic parameter learning algorithm, initialized to the {@link DynamicMaximumLikelihood} algorithm. */
    private static DynamicParameterLearningAlgorithm dynamicParameterLearningAlgorithm = DynamicMaximumLikelihood::learnDynamic;

    /** Represents the used dynamic structure learning algorithm, initialized to the dynamic Naive Bayes Structure. */
    private static DynamicStructuralLearningAlgorithm dynamicStructuralLearningAlgorithm = DynamicLearningEngine::dynamicNaiveBayesStructure;

    /**
     * Returns a dynamic Naive Bayes structure for a given {@link DataStream} of {@link DynamicDataInstance}s.
     * @param dataStream a given {@link DataStream} of {@link DynamicDataInstance}s.
     * @return a {@link DynamicDAG} object.
     */
    private static DynamicDAG dynamicNaiveBayesStructure(DataStream<DynamicDataInstance> dataStream){
        DynamicVariables modelHeader = new DynamicVariables(dataStream.getAttributes());
        DynamicDAG dag = new DynamicDAG(modelHeader);
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        dag.getParentSetsTimeT()
                .stream()
                .filter(w-> w.getMainVar()
                        .getVarID()!=classVar.getVarID())
                .forEach(w -> {
                    w.addParent(classVar);
                    w.addParent(modelHeader.getInterfaceVariable(w.getMainVar()));
                });

        return dag;
    }

    /**
     * Sets the dynamic parameter learning Algorithm.
     * @param dynamicParameterLearningAlgorithm a {@link DynamicParameterLearningAlgorithm} object.
     */
    public static void setDynamicParameterLearningAlgorithm(DynamicParameterLearningAlgorithm dynamicParameterLearningAlgorithm) {
        DynamicLearningEngine.dynamicParameterLearningAlgorithm = dynamicParameterLearningAlgorithm;
    }

    /**
     * Sets the dynamic structure learning Algorithm.
     * @param dynamicStructuralLearningAlgorithm a {@link DynamicStructuralLearningAlgorithm} object.
     */
    public static void setDynamicStructuralLearningAlgorithm(DynamicStructuralLearningAlgorithm dynamicStructuralLearningAlgorithm) {
        DynamicLearningEngine.dynamicStructuralLearningAlgorithm = dynamicStructuralLearningAlgorithm;
    }

    /**
     * Learns both the structure and parameters of the dynamic model.
     * @param dataStream a given {@link DataStream} of {@link DynamicDataInstance}s.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork learnDynamicModel(DataStream<DynamicDataInstance> dataStream){
        Stopwatch watch = Stopwatch.createStarted();
        DynamicDAG dag = dynamicStructuralLearningAlgorithm.learn(dataStream);
        System.out.println("Structural Learning : " + watch.stop());

        watch = Stopwatch.createStarted();
        DynamicBayesianNetwork network = dynamicParameterLearningAlgorithm.learn(dag,dataStream);
        System.out.println("Parameter Learning: " + watch.stop());

        return network;
    }
}
