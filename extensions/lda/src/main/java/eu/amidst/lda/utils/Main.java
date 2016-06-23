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

package eu.amidst.lda.utils;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.lda.core.BatchSpliteratorByID;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class Main {


    public static void process2(String[] args) throws IOException {
        DataStream<DataInstance> dataInstances = DataStreamLoader.open("/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/docswords-joint.arff");

        double minWord = Double.MAX_VALUE;
        double maxWord = -Double.MAX_VALUE;

        for (DataInstance dataInstance : dataInstances) {
            double word = dataInstance.getValue(dataInstance.getAttributes().getAttributeByName("word"));
            if (minWord>word)
                minWord = word;

            if (maxWord<word)
                maxWord=word;
        }

        System.out.println(minWord);
        System.out.println(maxWord);
    }

    public static void process(String[] args) throws IOException {
        DataStream<DataInstance> dataInstances = DataStreamLoader.open("/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/docswords-joint.arff");

//        List<DataOnMemory<DataInstance>> listA =
//                BatchSpliteratorByID.streamOverDocuments(dataInstances,2).collect(Collectors.toList());


        Stream<DataOnMemory<DataInstance>> stream = BatchSpliteratorByID.streamOverDocuments(dataInstances,2);

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

    public static void shuflle(String[] args) throws IOException {

        //Utils.shuffleData("/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/docword.nips.arff", "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/docword.nips.shuffled.arff");

        DataStream<DataInstance> dataInstances = DataStreamLoader.open("/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/docword.nips.arff");

        List<DataOnMemory<DataInstance>> batches = BatchSpliteratorByID.streamOverDocuments(dataInstances, 1).collect(Collectors.toList());

        Collections.shuffle(batches);

        DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<DataInstance>(dataInstances.getAttributes());

        for (DataOnMemory<DataInstance> batch : batches) {
            for (DataInstance dataInstance : batch) {
                newData.add(dataInstance);
            }
        }

        DataStreamWriter.writeDataToFile(newData,"/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/docword.nips.shuffled.arff");
    }

    public static void shuffleAbstracts(String[] args) throws IOException {
        String path = "/Users/andresmasegosa/Dropbox/amidst_postdoc/abstractByYear/";

        if(args.length>0){
            path = args[0];
        }
        /*  DataOnMemory<DataInstance> dataInstances = DataStreamLoader.loadDataOnMemoryFromFile(path+"abstract_90.arff");

        DataOnMemoryListContainer<DataInstance> container = new DataOnMemoryListContainer<DataInstance>(dataInstances.getAttributes());

        //

        String[] years = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};

        for (String year : years) {
            dataInstances = DataStreamLoader.loadDataOnMemoryFromFile(path+"abstract_"+year+".arff");
            container.addAll(dataInstances.getList());
        }

        DataStreamWriter.writeDataToFile(container,path+"abstracts.all.arff");
        */
        DataOnMemory<DataInstance> container = DataStreamLoader.loadDataOnMemoryFromFile(path+"abstracts.all.arff");

        List<DataOnMemory<DataInstance>> batches = BatchSpliteratorByID.streamOverDocuments(container, 1).collect(Collectors.toList());

        Collections.shuffle(batches);

        DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<DataInstance>(container.getAttributes());

        for (DataOnMemory<DataInstance> batch : batches) {
            newData.addAll(batch.getList());
        }

        DataStreamWriter.writeDataToFile(newData,path+"abstracts.all.shuffle.arff");

    }

    public static void addRandomColumnForExternalShuffle(String[] args) throws IOException{

        String inputPath = "/Users/ana/Desktop/abstracts.all.csv";
        String outputPath = "/Users/ana/Desktop/abstracts.all.shuffled.csv";

        if(args.length>0){
            inputPath = args[0];
            outputPath = args[1];
        }

        int numDocuments = 4;//128804;
        //int[] array = IntStream.range(1, numDocuments +1).toArray();
        List<Integer> randomNumbers = new ArrayList<>();
        for (int i = 1; i <= numDocuments; i++)
        {
            randomNumbers.add(i);
        }
        Collections.shuffle(randomNumbers);

        try (Stream<String> input = Files.lines(Paths.get(inputPath));
             PrintWriter output = new PrintWriter(outputPath, "UTF-8"))
        {
            input.map(s -> {
                String[] line = s.split(",");
                int numDoc = Integer.parseInt(line[0]);
                String newLine = randomNumbers.get(numDoc-1)+","+s;
                return newLine;
            })
                    .forEachOrdered(output::println);
        }

        /**
         * In the command line run the following command to order the documents according to the new random numbers
         * and remove the first column:
         *
         * cat abstracts.all.shuffled.csv | sort | cut -d "," -f 2- > abstracts.all.reallyShuffled.csv
         */


    }


    public static void main(String[] args) throws IOException {

        /**
         * To check number of documents or words in file
         * awk -F ',' 'NR>=10 {print $1}' abstracts.all.shuffled.arff | sort | uniq -c | wc -l
         */
        addRandomColumnForExternalShuffle(args);
    }


}
