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

package eu.amidst.dVMPJournalExtensionJuly2016;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.io.IOException;

/**
 * Created by andresmasegosa on 27/7/16.
 */
public class Normalize {

    public static void minmax(String[] args) throws IOException {

        DataStream<DataInstance> data = DataStreamLoader.open(args[0]+"_train.arff");

        double[] min = new double[data.getAttributes().getNumberOfAttributes()];
        double[] max = new double[data.getAttributes().getNumberOfAttributes()];

        for (int i = 0; i < max.length; i++) {
            min[i]=Double.MAX_VALUE;
            max[i]=Double.MIN_VALUE;
        }


        for (DataInstance dataInstance : data) {
            for (Attribute attribute : data.getAttributes()) {
                if (dataInstance.getValue(attribute)<min[attribute.getIndex()])
                    min[attribute.getIndex()]=dataInstance.getValue(attribute);

                if (dataInstance.getValue(attribute)>max[attribute.getIndex()])
                    max[attribute.getIndex()]=dataInstance.getValue(attribute);
            }
        }


        data = data.map(dataInstance -> {
            for (Attribute attribute : dataInstance.getAttributes()) {
                dataInstance.setValue(attribute,(dataInstance.getValue(attribute)-min[attribute.getIndex()])/(max[attribute.getIndex()]-min[attribute.getIndex()]));
            }
            return dataInstance;
        });

        DataStreamWriter.writeDataToFile(data,args[1]+"_train.arff");
    }

    public static void main(String[] args) throws Exception {

        DataStream<DataInstance> data = DataStreamLoader.open(args[0]);

        double[] mean = new double[data.getAttributes().getNumberOfAttributes()];
        double[] square = new double[data.getAttributes().getNumberOfAttributes()];
        int count = 0;
        for (int i = 0; i < mean.length; i++) {
            mean[i]=0;
            square[i]=0;
        }


        for (DataInstance dataInstance : data) {
            for (Attribute attribute : data.getAttributes()) {
                    if (attribute.isSpecialAttribute() || attribute.getNumberOfStates()!=-1)
                        continue;
                    mean[attribute.getIndex()]+=dataInstance.getValue(attribute);
                    square[attribute.getIndex()]+=Math.pow(dataInstance.getValue(attribute),2);
            }
            count++;
        }

        final int total = count;
        data = data.map(dataInstance -> {
            for (Attribute attribute : dataInstance.getAttributes()) {
                if (attribute.isSpecialAttribute() || attribute.getNumberOfStates()!=-1)
                    continue;
                double meanAtt = mean[attribute.getIndex()]/total;
                double squareAtt = square[attribute.getIndex()]/total;
                double varAtt = squareAtt-Math.pow(meanAtt,2);
                dataInstance.setValue(attribute,(dataInstance.getValue(attribute)-meanAtt)/Math.sqrt(varAtt));
            }
            return dataInstance;
        });

        DataStreamWriter.writeDataToFile(data,args[1]);


        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.getConfig().disableSysoutLogging();
        env.setParallelism(32);



        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env,args[1],false);

        DataFlinkWriter.writeDataToARFFFolder(dataFlink,args[2]);
    }


}
