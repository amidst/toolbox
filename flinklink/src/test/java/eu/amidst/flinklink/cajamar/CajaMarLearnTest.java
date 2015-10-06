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

package eu.amidst.flinklink.cajamar;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.learning.parametric.bayesian.DataPosteriorAssignment;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.DBNSampler;
import junit.framework.TestCase;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 25/9/15.
 */
public class CajaMarLearnTest extends TestCase {

    public static int NSETS = 3;

    public static int SAMPLESIZE = 2000;
    public static int BATCHSIZE = 1000;

    public static void testCreateDataSets() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("networks/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));
        System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(SAMPLESIZE);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(1);

        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(null);



        DataFlinkWriter.writeDataToARFFFolder(data0, "./datasets/dataFlink/cajaMarSynthetic/data0.arff");
        data0 = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data0.arff");

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();
        System.out.println(list);

        HashSet<Long> noDupSet = new HashSet();
        noDupSet.addAll(list);
        assertEquals(SAMPLESIZE, noDupSet.size());
        System.out.println(noDupSet);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(dataPrev);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, "./datasets/dataFlink/cajaMarSynthetic/data" + i + ".arff");
            dataNew = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data" + i + ".arff");

            dataPrev = dataNew;

            List<Long> listNew = dataNew.getDataSet().map(d -> d.getSequenceID()).collect();
            System.out.println(listNew);

            HashSet<Long> noDupSetNew = new HashSet();
            noDupSetNew.addAll(listNew);
            assertEquals(SAMPLESIZE, noDupSetNew.size());
            System.out.println(noDupSetNew);


            for (Long id : noDupSetNew) {
                assertEquals("ID: "+ id,true,noDupSet.contains(id));
            }

        }
    }
    public static void testJoin() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data0.arff");

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();

        HashSet<Long> noDupSet = new HashSet();
        noDupSet.addAll(list);
        assertEquals(SAMPLESIZE,noDupSet.size());



        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data" + i + ".arff");

            DataSet<Long> dataJoin = data0.getDataSet().join(dataNew.getDataSet()).where(new KeySelector<DynamicDataInstance, Long>() {
                @Override
                public Long getKey(DynamicDataInstance value) throws Exception {
                    return value.getSequenceID();
                }
            }).equalTo(new KeySelector<DynamicDataInstance, Long>() {
                @Override
                public Long getKey(DynamicDataInstance value) throws Exception {
                    return value.getSequenceID();
                }
            }).with(new JoinFunction<DynamicDataInstance, DynamicDataInstance, Long>() {
                @Override
                public Long join(DynamicDataInstance dynamicDataInstance, DynamicDataInstance dataPosterior) throws Exception {
                    return dynamicDataInstance.getSequenceID();
                }
            });

            List<Long> listNew = dataJoin.map(d -> d).collect();
            System.out.println(listNew);

            HashSet<Long> noDupSetNew = new HashSet();
            noDupSetNew.addAll(listNew);
            assertEquals(SAMPLESIZE, noDupSetNew.size());
            System.out.println(noDupSetNew);


            for (Long id : noDupSetNew) {
                assertEquals("ID: "+ id,true,noDupSet.contains(id));
            }
        }
    }

    public static void testUpdate0() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("networks/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));

        CajaMarLearn learn = new CajaMarLearn();
        learn.setMaximumGlobalIterations(1);
        learn.setBatchSize(BATCHSIZE);
        learn.setDAG(dbn.getDynamicDAG());
        learn.setOutput(true);
        learn.initLearning();

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data0.arff");

        System.out.println("Count Data0: "+data0.getDataSet().count());

        System.out.println("--------------- DATA " + 0 + " --------------------------");
        learn.updateModelWithNewTimeSlice(0, data0);

        DataSet<DataPosteriorAssignment> dataPosteriorAssignmentDataSet = learn.getDataPosteriorDataSet();

        System.out.println("Count dataPosteriorAssignmentDataSet: "+ dataPosteriorAssignmentDataSet.count());

        DataFlink<DynamicDataInstance> data1 = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data1.arff");

        System.out.println("Count Data1: "+data1.getDataSet().count());
        //DataSet<CajaMarLearn.DataPosteriorInstance> dataPosteriorInstanceDataSet = learn.joinData(data1);


        DataSet<CajaMarLearn.DataPosteriorInstance> dataPosteriorInstanceDataSet =

        data1.getDataSet().join(dataPosteriorAssignmentDataSet).where(new KeySelector<DynamicDataInstance, Long>() {
            @Override
            public Long getKey(DynamicDataInstance value) throws Exception {
                return value.getSequenceID();
            }
        }).equalTo(new KeySelector<DataPosteriorAssignment, Long>() {
            @Override
            public Long getKey(DataPosteriorAssignment value) throws Exception {
                return value.getPosterior().getId();
            }
        }).with(new JoinFunction<DynamicDataInstance, DataPosteriorAssignment, CajaMarLearn.DataPosteriorInstance>() {
            @Override
            public CajaMarLearn.DataPosteriorInstance join(DynamicDataInstance dynamicDataInstance, DataPosteriorAssignment dataPosterior) throws Exception {
                return new CajaMarLearn.DataPosteriorInstance(dataPosterior,dynamicDataInstance);
            }
        });
        System.out.println("Count dataPosteriorInstanceDataSet: "+dataPosteriorInstanceDataSet.count());
    }

    public static void testUpdate0Learn() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("networks/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));

        System.out.println(dbn.toString());

        CajaMarLearn learn = new CajaMarLearn();
        learn.setMaximumGlobalIterations(10);
        learn.setBatchSize(BATCHSIZE);
        learn.setDAG(dbn.getDynamicDAG());
        learn.setOutput(true);
        learn.initLearning();


        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data0.arff");

        double count = 0;
        for (DataInstance datainstance : data0.getDataSet().collect()) {
            if (datainstance.getValue(dbn.getDynamicVariables().getVariableByName("DEFAULTER"))==1.0
                && datainstance.getValue(dbn.getDynamicVariables().getVariableByName("CREDITCARD"))==0.0)
                count++;
        }
        System.out.println(count);

        System.out.println("--------------- DATA " + 0 + " --------------------------");
        learn.updateModelWithNewTimeSlice(0, data0);

        System.out.println(learn.getLearntDynamicBayesianNetwork().toBayesianNetworkTime0());

        assertEquals(true, dbn.toBayesianNetworkTime0().equalBNs(learn.getLearntDynamicBayesianNetwork().toBayesianNetworkTime0(), 0.5));
    }

    public static void testUpdateN() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("networks/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));

        System.out.println(dbn.toString());

        CajaMarLearn learn = new CajaMarLearn();
        learn.setMaximumGlobalIterations(10);
        learn.setBatchSize(BATCHSIZE);
        learn.setDAG(dbn.getDynamicDAG());
        learn.setOutput(true);
        learn.initLearning();


        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data0.arff");

        System.out.println("--------------- DATA " +0+" --------------------------");
        learn.updateModelWithNewTimeSlice(0,data0);

        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicData(env, "./datasets/dataFlink/cajaMarSynthetic/data"+i+".arff");
            learn.updateModelWithNewTimeSlice(i, dataNew);
            assertEquals(SAMPLESIZE, learn.dataPosteriorDataSet.count());
        }

        System.out.println(learn.getLearntDynamicBayesianNetwork());
        assertEquals(true,dbn.equalDBNs(learn.getLearntDynamicBayesianNetwork(),0.5));
        //learn.getLearntBayesianNetwork()
    }
}