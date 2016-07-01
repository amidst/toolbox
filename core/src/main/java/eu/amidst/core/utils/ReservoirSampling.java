/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.utils;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;

import java.util.Random;

//TODO Be careful with use of "add(int pos, element)" of List!!!!!!

/**
 * This class defines the Reservoir Sampling.
 */
public class ReservoirSampling {

    /**
     * Samples {@link DataOnMemory} from a {@link DataStream} and a given number of samples.
     * @param numberOfSamples a given number of samples
     * @param dataStream a {@link DataStream} object.
     * @return a {@link DataOnMemory} object.
     */
    public static DataOnMemory samplingNumberOfSamples(int numberOfSamples, DataStream<? extends DataInstance> dataStream){

        Random random = new Random(0);
        DataOnMemoryListContainer<DataInstance> dataOnMemoryList = new DataOnMemoryListContainer(dataStream.getAttributes());
        int count = 0;

        for (DataInstance instance : dataStream){
            if (count<numberOfSamples) {
                dataOnMemoryList.add(instance);
            }else{
                int r = random.nextInt(count+1);
                if (r < numberOfSamples)
                    dataOnMemoryList.set(r,instance);
            }
            count++;
        }
        return dataOnMemoryList;
    }

    /**
     * Samples {@link DataOnMemory} from a {@link DataStream} and a given number of GB.
     * @param numberOfGB a given number of GB
     * @param dataStream a {@link DataStream} object.
     * @return a {@link DataOnMemory} object.
     */
    public static DataOnMemory samplingNumberOfGBs(double numberOfGB, DataStream<? extends DataInstance> dataStream) {
        double numberOfBytesPerSample = dataStream.getAttributes().getFullListOfAttributes().size()*8.0;

        //We assume an overhead of 10%.
        int numberOfSamples = (int) ((1-0.1)*numberOfGB*1073741824.0/numberOfBytesPerSample);

        return samplingNumberOfSamples(numberOfSamples,dataStream);
    }

    public static void main(String[] args) throws Exception {
        DataStream<DataInstance> data = DataSetGenerator.generate(15,1000,5,5);
        DataOnMemory<DataInstance> dataOnMemory = ReservoirSampling.samplingNumberOfSamples(1000, data);
    }

}
