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

package eu.amidst.ecai2016;

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;

/**
 * Created by andresmasegosa on 31/3/16.
 */
public class CreateTemperatureHumidityModel {

    public static void main(String[] args) throws IOException {

        DynamicVariables dynamicVariables = new DynamicVariables();

        //Variable season = dynamicVariables.newMultinomialDynamicVariable("ClassVar", Arrays.asList("Winter","Spring"));
        Variable season = dynamicVariables.newMultinomialDynamicVariable("Season", 2);

        Variable weatherPhenomenon = dynamicVariables
                //.newMultinomialDynamicVariable("WeatherPhenomenon", Arrays.asList("NonPresent", "Present"));
                .newMultinomialDynamicVariable("WeatherPhenomenon", 2);

        Variable temperature = dynamicVariables.newGaussianDynamicVariable("Temperature");

        Variable humidity = dynamicVariables.newGaussianDynamicVariable("Humidity");

        Variable sensorTemperature = dynamicVariables.newGaussianDynamicVariable("sensorTemperature");

        Variable sensorHumidity = dynamicVariables.newGaussianDynamicVariable("sensorHumidity");


        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(sensorHumidity).addParent(humidity);
        dynamicDAG.getParentSetTimeT(sensorHumidity).addParent(temperature);

        dynamicDAG.getParentSetTimeT(sensorTemperature).addParent(temperature);
        dynamicDAG.getParentSetTimeT(sensorTemperature).addParent(humidity);


        dynamicDAG.getParentSetTimeT(humidity).addParent(season);
        dynamicDAG.getParentSetTimeT(humidity).addParent(weatherPhenomenon);
        dynamicDAG.getParentSetTimeT(humidity).addParent(humidity.getInterfaceVariable());
        dynamicDAG.getParentSetTimeT(humidity).addParent(temperature);

        dynamicDAG.getParentSetTimeT(temperature).addParent(season);
        dynamicDAG.getParentSetTimeT(temperature).addParent(weatherPhenomenon);
        dynamicDAG.getParentSetTimeT(temperature).addParent(temperature.getInterfaceVariable());

        dynamicDAG.getParentSetTimeT(season).addParent(season.getInterfaceVariable());

        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);

        Multinomial p0 = dbn.getConditionalDistributionTime0(season);
        p0.setProbabilities(new double[]{0.5,0.5});

        Multinomial_MultinomialParents p1 = dbn.getConditionalDistributionTimeT(season);
        p1.getMultinomial(0).setProbabilities(new double[]{0.9,0.1});
        p1.getMultinomial(1).setProbabilities(new double[]{0.1,0.9});

        Multinomial p2 = dbn.getConditionalDistributionTime0(weatherPhenomenon);
        p2.setProbabilities(new double[]{0.9,0.1});

        Multinomial p3 = dbn.getConditionalDistributionTimeT(weatherPhenomenon);
        p3.setProbabilities(new double[]{0.9,0.1});

        Normal_MultinomialParents p4 = dbn.getConditionalDistributionTime0(temperature);
        p4.getNormal(0).setMean(13);
        p4.getNormal(1).setMean(18);
        p4.getNormal(2).setMean(8);
        p4.getNormal(3).setMean(22);

        p4.getNormal(0).setVariance(2);
        p4.getNormal(1).setVariance(2);
        p4.getNormal(2).setVariance(4);
        p4.getNormal(3).setVariance(4);


        Normal_MultinomialNormalParents p5 = dbn.getConditionalDistributionTimeT(temperature);
        p5.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p5.getNormal_NormalParentsDistribution(0).setCoeffForParent(temperature.getInterfaceVariable(),1.0);
        p5.getNormal_NormalParentsDistribution(0).setVariance(1);

        p5.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p5.getNormal_NormalParentsDistribution(1).setCoeffForParent(temperature.getInterfaceVariable(),1.0);
        p5.getNormal_NormalParentsDistribution(1).setVariance(1);

        p5.getNormal_NormalParentsDistribution(2).setIntercept(8);
        p5.getNormal_NormalParentsDistribution(2).setCoeffForParent(temperature.getInterfaceVariable(),0.0);
        p5.getNormal_NormalParentsDistribution(2).setVariance(4);

        p5.getNormal_NormalParentsDistribution(3).setIntercept(22);
        p5.getNormal_NormalParentsDistribution(3).setCoeffForParent(temperature.getInterfaceVariable(),0.0);
        p5.getNormal_NormalParentsDistribution(3).setVariance(4);


        Normal_MultinomialNormalParents p6 = dbn.getConditionalDistributionTime0(humidity);
        p6.getNormal_NormalParentsDistribution(0).setIntercept(60);
        p6.getNormal_NormalParentsDistribution(0).setCoeffForParent(temperature,0.2);
        p6.getNormal_NormalParentsDistribution(0).setVariance(5);

        p6.getNormal_NormalParentsDistribution(1).setIntercept(70);
        p6.getNormal_NormalParentsDistribution(1).setCoeffForParent(temperature,0.2);
        p6.getNormal_NormalParentsDistribution(1).setVariance(5);

        p6.getNormal_NormalParentsDistribution(2).setIntercept(50);
        p6.getNormal_NormalParentsDistribution(2).setCoeffForParent(temperature,0.4);
        p6.getNormal_NormalParentsDistribution(2).setVariance(10);

        p6.getNormal_NormalParentsDistribution(3).setIntercept(80);
        p6.getNormal_NormalParentsDistribution(3).setCoeffForParent(temperature,0.1);
        p6.getNormal_NormalParentsDistribution(3).setVariance(10);

        Normal_MultinomialNormalParents p7 = dbn.getConditionalDistributionTimeT(humidity);
        p7.getNormal_NormalParentsDistribution(0).setIntercept(0);
        p7.getNormal_NormalParentsDistribution(0).setCoeffForParent(humidity.getInterfaceVariable(),1);
        p7.getNormal_NormalParentsDistribution(0).setCoeffForParent(temperature,0.2);
        p7.getNormal_NormalParentsDistribution(0).setVariance(1);

        p7.getNormal_NormalParentsDistribution(1).setIntercept(0);
        p7.getNormal_NormalParentsDistribution(1).setCoeffForParent(humidity.getInterfaceVariable(),1);
        p7.getNormal_NormalParentsDistribution(1).setCoeffForParent(temperature,0.2);
        p7.getNormal_NormalParentsDistribution(1).setVariance(1);

        p7.getNormal_NormalParentsDistribution(2).setIntercept(50);
        p7.getNormal_NormalParentsDistribution(2).setCoeffForParent(humidity.getInterfaceVariable(),0);
        p7.getNormal_NormalParentsDistribution(2).setCoeffForParent(temperature,0.4);
        p7.getNormal_NormalParentsDistribution(2).setVariance(10);

        p7.getNormal_NormalParentsDistribution(3).setIntercept(80);
        p7.getNormal_NormalParentsDistribution(3).setCoeffForParent(humidity.getInterfaceVariable(),0);
        p7.getNormal_NormalParentsDistribution(3).setCoeffForParent(temperature,0.1);
        p7.getNormal_NormalParentsDistribution(3).setVariance(10);


        ConditionalLinearGaussian p8 = dbn.getConditionalDistributionTime0(sensorTemperature);
        p8.setIntercept(0);
        p8.setCoeffForParent(temperature,1.0);
        p8.setCoeffForParent(humidity,0.05);
        p8.setVariance(2);

        ConditionalLinearGaussian p9 = dbn.getConditionalDistributionTimeT(sensorTemperature);
        p9.setIntercept(0);
        p9.setCoeffForParent(temperature,1.0);
        p9.setCoeffForParent(humidity,0.05);
        p9.setVariance(2);


        ConditionalLinearGaussian p10 = dbn.getConditionalDistributionTime0(sensorHumidity);
        p10.setIntercept(0);
        p10.setCoeffForParent(humidity,1.0);
        p10.setCoeffForParent(temperature,0.05);
        p10.setVariance(2);

        ConditionalLinearGaussian p11 = dbn.getConditionalDistributionTimeT(sensorHumidity);
        p11.setIntercept(0);
        p11.setCoeffForParent(humidity,1.0);
        p11.setCoeffForParent(temperature,0.05);
        p11.setVariance(2);

        DynamicBayesianNetworkWriter.save(dbn, "./networks/TemparatureHumidtyNetworks.dbn");

        System.out.println(dbn);
    }
}
