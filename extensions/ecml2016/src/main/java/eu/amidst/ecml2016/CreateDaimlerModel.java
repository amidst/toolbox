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

package eu.amidst.ecml2016;

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by andresmasegosa on 31/3/16.
 */
public class CreateDaimlerModel {

    public static void main(String[] args) throws IOException {

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable laneChange = dynamicVariables.newMultinomialDynamicVariable("LaneChange", Arrays.asList("False","True"));

        Variable hiddenManeuverAction = dynamicVariables
                .newMultinomialDynamicVariable("HiddenManeuverAction", Arrays.asList("NoChange","Accelerate","Decelerate"));

        Variable acceleration = dynamicVariables.newGaussianDynamicVariable("Acceleration");

        Variable velocity = dynamicVariables.newGaussianDynamicVariable("Velocity");

        Variable sensorVelocity1 = dynamicVariables.newGaussianDynamicVariable("sensorVelocity1");

        Variable sensorVelocity2 = dynamicVariables.newGaussianDynamicVariable("sensorVelocity2");


        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(laneChange).addParent(laneChange.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(hiddenManeuverAction).addParent(laneChange);
        dynamicDAG.getParentSetTimeT(hiddenManeuverAction).addParent(hiddenManeuverAction.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(acceleration).addParent(laneChange);
        dynamicDAG.getParentSetTimeT(acceleration).addParent(hiddenManeuverAction);


        dynamicDAG.getParentSetTimeT(velocity).addParent(laneChange);
        dynamicDAG.getParentSetTimeT(velocity).addParent(velocity.getInterfaceVariable());
        dynamicDAG.getParentSetTimeT(velocity).addParent(acceleration);

        dynamicDAG.getParentSetTimeT(sensorVelocity1).addParent(velocity);

        dynamicDAG.getParentSetTimeT(sensorVelocity2).addParent(velocity);




        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);

        // LANE CHANGE:
        Multinomial p0 = dbn.getConditionalDistributionTime0(laneChange);
        p0.setProbabilities(new double[]{1.0,0.0});

        Multinomial_MultinomialParents p1 = dbn.getConditionalDistributionTimeT(laneChange);
        p1.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});
        p1.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // HIDDEN MANEUVER ACTION:
        Multinomial_MultinomialParents p2 = dbn.getConditionalDistributionTime0(hiddenManeuverAction);
        p2.getMultinomial(0).setProbabilities(new double[]{0.9,0.05,0.05});
        p2.getMultinomial(1).setProbabilities(new double[]{0.5,0.25,0.25});


        Multinomial_MultinomialParents p3 = dbn.getConditionalDistributionTimeT(hiddenManeuverAction);
        p3.getMultinomial(0).setProbabilities(new double[]{0.9, 0.05, 0.05});
        p3.getMultinomial(1).setProbabilities(new double[]{0.7, 0.15, 0.15});
        p3.getMultinomial(2).setProbabilities(new double[]{0.9, 0.05, 0.05});
        p3.getMultinomial(3).setProbabilities(new double[]{0.5, 0.5,  0});
        p3.getMultinomial(4).setProbabilities(new double[]{0.9, 0.05, 0.05});
        p3.getMultinomial(5).setProbabilities(new double[]{0.5, 0,    0.5});

        // ACCELERATION:
        Normal_MultinomialParents p4 = dbn.getConditionalDistributionTime0(acceleration);
        p4.getNormal(0).setMean(0);
        p4.getNormal(0).setVariance(2);

        p4.getNormal(1).setMean(0);
        p4.getNormal(1).setVariance(2);

        p4.getNormal(2).setMean(0);
        p4.getNormal(2).setVariance(2);

        p4.getNormal(3).setMean(10);
        p4.getNormal(3).setVariance(4);

        p4.getNormal(4).setMean(0);
        p4.getNormal(4).setVariance(2);

        p4.getNormal(5).setMean(-10);
        p4.getNormal(5).setVariance(4);


        Normal_MultinomialParents p5 = dbn.getConditionalDistributionTimeT(acceleration);
        p5.getNormal(0).setMean(0);
        p5.getNormal(0).setVariance(2);

        p5.getNormal(1).setMean(0);
        p5.getNormal(1).setVariance(2);

        p5.getNormal(2).setMean(0);
        p5.getNormal(2).setVariance(2);

        p5.getNormal(3).setMean(10);
        p5.getNormal(3).setVariance(4);

        p5.getNormal(4).setMean(0);
        p5.getNormal(4).setVariance(2);

        p5.getNormal(5).setMean(-10);
        p5.getNormal(5).setVariance(4);

        // VELOCITY:
        Normal_MultinomialNormalParents p6 = dbn.getConditionalDistributionTime0(velocity);
        p6.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p6.getNormal_NormalParentsDistribution(0).setCoeffForParent(acceleration,0);
        p6.getNormal_NormalParentsDistribution(0).setVariance(2);

        p6.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p6.getNormal_NormalParentsDistribution(1).setCoeffForParent(acceleration,0.042);
        p6.getNormal_NormalParentsDistribution(1).setVariance(2);


        Normal_MultinomialNormalParents p7 = dbn.getConditionalDistributionTimeT(velocity);
        p7.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p7.getNormal_NormalParentsDistribution(0).setCoeffForParent(velocity.getInterfaceVariable(),0);
        p7.getNormal_NormalParentsDistribution(0).setCoeffForParent(acceleration,0);
        p7.getNormal_NormalParentsDistribution(0).setVariance(2);

        p7.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p7.getNormal_NormalParentsDistribution(1).setCoeffForParent(velocity.getInterfaceVariable(),1);
        p7.getNormal_NormalParentsDistribution(1).setCoeffForParent(acceleration,0.042);
        p7.getNormal_NormalParentsDistribution(1).setVariance(4);

        // SENSOR VELOCITY 1:
        ConditionalLinearGaussian p8 = dbn.getConditionalDistributionTime0(sensorVelocity1);
        p8.setIntercept(0);
        p8.setCoeffForParent(velocity,1);
        p8.setVariance(2);

        ConditionalLinearGaussian p9 = dbn.getConditionalDistributionTimeT(sensorVelocity1);
        p9.setIntercept(0);
        p9.setCoeffForParent(velocity,1);
        p9.setVariance(2);

        // SENSOR VELOCITY 2:
        ConditionalLinearGaussian p10 = dbn.getConditionalDistributionTime0(sensorVelocity2);
        p10.setIntercept(0);
        p10.setCoeffForParent(velocity,1);
        p10.setVariance(2);

        ConditionalLinearGaussian p11 = dbn.getConditionalDistributionTimeT(sensorVelocity2);
        p11.setIntercept(0);
        p11.setCoeffForParent(velocity,1);
        p11.setVariance(2);

        DynamicBayesianNetworkWriter.saveToFile(dbn, "./networks/DaimlerSimulatedNetwork.dbn");

        System.out.println(dbn);
    }
}
