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

package eu.amidst.latentvariablemodels.classifiers;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.latentvariablemodels.dynamicmodels.classifiers.DynamicNaiveBayesClassifier;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;
import junit.framework.TestCase;

/**
 * Created by rcabanas on 10/03/16.
 */
public class DynamicNaiveBayesClassifierTest extends TestCase {

    protected DynamicNaiveBayesClassifier dynNaiveBayes;
    DataStream<DynamicDataInstance> data;

    protected void setUp() throws WrongConfigurationException {
        data = DataSetGenerator.generate(1234,500, 2, 10);

        System.out.println(data.getAttributes().toString());

        String classVarName = "DiscreteVar0";

        dynNaiveBayes = new DynamicNaiveBayesClassifier(data.getAttributes());
        dynNaiveBayes.setClassName(classVarName);

        dynNaiveBayes.updateModel(data);
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {

            dynNaiveBayes.updateModel(batch);
        }
        System.out.println(dynNaiveBayes.getModel());
        System.out.println(dynNaiveBayes.getModel().getDynamicDAG().toDAGTime0());
        System.out.println(dynNaiveBayes.getModel().getDynamicDAG().toDAGTimeT());

    }


    //////// test methods

    public void testClassVariable() {
        boolean passedTest = true;

        Variable classVar = dynNaiveBayes.getClassVar();

        // class variable is a multinomial
        boolean isMultinomial = classVar.isMultinomial();

        // and has not parents at t=0 and only its interface copy at time t=T
        boolean noParentsTime0 = dynNaiveBayes.getDynamicDAG().toDAGTime0().getParentSet(classVar).getParents().isEmpty();
        boolean noParentsTimeT = dynNaiveBayes.getDynamicDAG().toDAGTimeT().getParentSet(classVar).getParents().size()==1 && dynNaiveBayes.getDynamicDAG().toDAGTimeT().getParentSet(classVar).getParents().contains(classVar.getInterfaceVariable());
        boolean noParents = noParentsTime0 && noParentsTimeT;

        //all the attributes are their children
        boolean allAttrChildren = dynNaiveBayes.getModel().getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(v-> !v.equals(classVar))
                .allMatch(v -> (dynNaiveBayes.getDynamicDAG().toDAGTime0().getParentSet(v).contains(classVar) && dynNaiveBayes.getDynamicDAG().toDAGTimeT().getParentSet(v).contains(classVar)) );

        assertTrue(isMultinomial && noParents && allAttrChildren);
    }



//    public void testAttributes(){
//        Variable classVar = dynNaiveBayes.getClassVar();
//
//        // the attributes have a single parent
//        boolean numParents = dynNaiveBayes.getModel().getVariables().getListOfVariables().stream()
//                .filter(v-> !v.equals(classVar))
//                .allMatch(v -> dynNaiveBayes.getDAG().getParentSet(v).getNumberOfParents()==1);
//
//        assertTrue(numParents);
//    }

//
//
//    public void testPrediction() {
//
//        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);
//
//
//        double hits = 0;
//
//        for(DataInstance d : dataTest) {
//
//            double realValue = d.getValue(dynNaiveBayes.getClassVar());
//            double predValue;
//
//            d.setValue(dynNaiveBayes.getClassVar(), Utils.missingValue());
//            Multinomial posteriorProb = dynNaiveBayes.predict(d);
//
//
//            double[] values = posteriorProb.getProbabilities();
//            if (values[0]>values[1]) {
//                predValue = 0;
//            }else {
//                predValue = 1;
//
//            }
//
//            if(realValue == predValue) hits++;
//
//
//        }
//
//        assertTrue(hits==10);
//
//
//    }

//
//    public void testNBClassifier() {
//
//        long time = System.nanoTime();
//        dynNaiveBayes.updateModel(data);
//        BayesianNetwork nbClassifier = dynNaiveBayes.getModel();
//        System.out.println(nbClassifier.toString());
//
//        System.out.println("Time: " + (System.nanoTime() - time) / 1000000000.0);
//
//
//        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
//
//        parallelMaximumLikelihood.setWindowsSize(100);
//        parallelMaximumLikelihood.setDAG(nbClassifier.getDAG());
//        parallelMaximumLikelihood.setLaplace(true);
//        parallelMaximumLikelihood.setDataStream(DataSetGenerator.generate(1234,500, 2, 10));
//
//        parallelMaximumLikelihood.runLearning();
//        BayesianNetwork nbML = parallelMaximumLikelihood.getLearntBayesianNetwork();
//        System.out.println(dynNaiveBayes.toString());
//        assertTrue(nbML.equalBNs(nbClassifier, 0.2));
//
//    }



}
