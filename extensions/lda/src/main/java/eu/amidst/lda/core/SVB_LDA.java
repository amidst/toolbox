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

package eu.amidst.lda.core;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class SVB_LDA {
    public static void main(String[] args) {

        DataStream<DataInstance> dataInstances = DataStreamLoader.open("/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYear/abstract_90.arff");

        //DataOnMemory<DataInstance> dataInstances = DataStreamLoader.loadDataOnMemoryFromFile("/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYear/abstract_90.arff");

        SVB svb = new SVB();

        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(),"word","count");
        plateauLDA.setNTopics(10);
        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(10);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(0.1);

        svb.setPlateuStructure(plateauLDA);
        svb.setOutput(true);

        svb.initLearning();

        //System.out.println(dataInstances.getNumberOfDataInstances());

        //svb.updateModel(dataInstances);

        BatchSpliteratorByID.streamOverDocuments(dataInstances, 500).sequential().forEach(batch -> {
            System.out.println("Batch: "+ batch.getNumberOfDataInstances());
            svb.updateModel(batch);
        });


    }

}
