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

package eu.amidst.lda;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class Main {


    public static void main(String[] args) throws IOException {
        DataStream<DataInstance> dataInstances = DataStreamLoader.openFromFile("/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/docswords-joint.arff");

//        List<DataOnMemory<DataInstance>> listA =
//                BatchSpliteratorByID.toFixedBatchStream(dataInstances,2).collect(Collectors.toList());


        Stream<DataOnMemory<DataInstance>> stream = BatchSpliteratorByID.toFixedBatchStream(dataInstances,2);

        Stream<String> lines = Files.lines(Paths.get("/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/idnsfid-joint.txt"));

        Map<Integer, String> map = new HashMap<>();

        lines.forEach(line -> {
            try {
                String[] parts = line.split("\t");
                String year = parts[1].substring(1, 3);

                map.put(Integer.parseInt(parts[0]), year);
            }catch(Exception ex){
                System.out.println(line);
            }

        });


        System.out.println(map.keySet().size());

        String[] years = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};

        for (String year : years) {
            List<DataInstance> list = dataInstances.stream().filter(d ->{
                int id = (int)d.getValue(dataInstances.getAttributes().getSeq_id());
                return map.get(id).compareTo(year)==0;
            }).collect(Collectors.toList());
            DataStreamWriter.writeDataToFile(new DataOnMemoryListContainer<DataInstance>(dataInstances.getAttributes(),list),"./datasets/abstract_"+year+".arff");
            System.out.println(year+" "+list.size());
        }




    }
}
