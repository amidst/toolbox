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

package text;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.lda.core.BatchSpliteratorByID;
import gps.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 20/1/17.
 */
public class Utils {

    public static <T extends DataInstance> List<DataOnMemory<T>> splitTrainTest(DataStream<T> data, int seed){

        List<DataOnMemory<T>> out = BatchSpliteratorByID.streamOverDocuments(data,1).collect(Collectors.toList());

        if (out.size()< Main.MIN)
            return null;

        Collections.shuffle(out,new Random(seed));

        int limit = (int) ((out.size()*2.0)/3.0);

        DataOnMemoryListContainer<T> train= new
                DataOnMemoryListContainer(data.getAttributes());

        for (int i = 0; i < limit; i++) {
            train.addAll(out.get(i).getList());
        }



        DataOnMemoryListContainer<T> test= new
                DataOnMemoryListContainer(data.getAttributes());

        for (int i = limit; i < out.size(); i++) {
            test.addAll(out.get(i).getList());
        }



        return Arrays.asList(train,test);
    }

    public static void printTopics(CompoundVector vector) {
        for (int j = 0; j < vector.getNumberOfBaseVectors(); j++) {

            ArrayVector arrayVector = (ArrayVector)vector.getVectorByPosition(j);

            int[] index = weka.core.Utils.sort(arrayVector.toArray());

            for (int i = index.length-1; i > index.length-100 && i>=0; i--) {
                System.out.print("("+index[i]+", "+arrayVector.get(index[i])+"), ");
            }

            System.out.println();

        }
    }
    public static void reduceVocab(String[] args) throws IOException {

        String[] years = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYear/";
        String dataPathWrite = "/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYearReducedWords/";


        int WORDS = 100;

        double totalLog = 0;
        for (int year = 0; year < years.length; year++) {

            DataStream<DataInstance> data = DataStreamLoader.open(dataPath + "abstract_" + years[year] + ".arff");

            Attribute word = data.getAttributes().getAttributeByName("word");

            data = data.map( d -> {d.setValue(word,((int)d.getValue(word))%WORDS); return d;});

            DataStreamWriter.writeDataToFile(data,dataPathWrite + "abstract_" + years[year] + ".arff");

            Path path = Paths.get(dataPathWrite + "abstract_" + years[year] + ".arff");
            List<String> fileContent = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));

            for (int i = 0; i < fileContent.size(); i++) {
                if (fileContent.get(i).equals("@attribute word SparseMultinomial 30799")) {
                    fileContent.set(i, "@attribute word SparseMultinomial "+WORDS);
                    break;
                }
            }

            Files.write(path, fileContent, StandardCharsets.UTF_8);
        }



    }

    public static void computSize(String[] args) {

        String[] years = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYear/";



        double totalLog = 0;
        for (int year = 0; year < years.length; year++) {


            DataStream<DataInstance> data= DataStreamLoader.open(dataPath+"abstract_"+years[year]+".arff");

            List<DataOnMemory<DataInstance>> out = BatchSpliteratorByID.streamOverDocuments(data,1).collect(Collectors.toList());

            System.out.println(out.size());


            totalLog+=(int) ((out.size()*2.0)/3.0);
        }

        System.out.println(totalLog);
    }

    public static void main(String[] args) throws IOException {

        Utils.reduceVocab(args);
    }

}
