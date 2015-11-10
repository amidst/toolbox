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

package eu.amidst.core;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;

import java.io.IOException;

/**
 * Created by andresmasegosa on 24/6/15.
 */
public class Main {

    public static void main (String[] args) throws IOException, ClassNotFoundException {


        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./datasets/tmp2.arff");

        int cont=0;
        for (DataStream<DataInstance> batch : data.iterableOverBatches(1000)) {
            DataStreamWriter.writeDataToFile(batch,"./datasets/bnaic2015/BCC/Month"+cont+".arff");
            cont++;
        }


        System.out.println(BayesianNetworkLoader.loadFromFile("./networks/outputNB.txt_NB_model.bn"));

        /*
        System.out.println(Integer.parseInt("0008")+"");

        Variables vars = new Variables();
        Variable var = vars.newGaussianVariable("A");
        double mean = 1;
        double variance = 1E-06;
        Normal normal = new Normal(var);
        normal.setMean(mean);
        normal.setVariance(variance);
        EF_Normal efnormal = normal.toEFUnivariateDistribution();
        System.out.println(efnormal.toUnivariateDistribution().toString());

        for (int i = 0; i < 100; i++) {
            efnormal.getNaturalParameters().set(0, mean / variance);
            efnormal.getNaturalParameters().set(1, -1/(2*variance));
            efnormal.fixNumericalInstability();
            efnormal.updateMomentFromNaturalParameters();

            System.out.println(efnormal.toUnivariateDistribution().toString());

            double newvariance = efnormal.getMomentParameters().get(efnormal.EXPECTED_SQUARE) - Math.pow(efnormal.getMomentParameters().get(efnormal.EXPECTED_MEAN),2);
            System.out.println("VAR:" + variance + ", " + newvariance);
            //System.out.println("MEAN:" + mean + ", " + efnormal.getMomentParameters().get(efnormal.EXPECTED_MEAN));
            //System.out.println();
            mean*=10;
            //variance/=10;
        }
        */

    }

}
