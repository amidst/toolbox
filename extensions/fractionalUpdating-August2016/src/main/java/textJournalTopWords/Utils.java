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

package textJournalTopWords;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.utils.*;
import eu.amidst.core.utils.Vector;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDA;
import gps.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static eu.amidst.core.utils.Serialization.deepCopy;

/**
 * Created by andresmasegosa on 20/1/17.
 */
public class Utils {

    public static int SEED = 0;

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

    public static Map<Integer,String> loadWords(String filename) throws IOException {
        return Files.lines(Paths.get(filename)).map( line -> Arrays.asList(line.split(","))).collect(Collectors.toConcurrentMap(item -> Integer.parseInt(item.get(1)), item -> item.get(0)));
    }

    public static void printTopics(CompoundVector vector, Map<Integer, String> mapWords) {
        DecimalFormat formateador = new DecimalFormat("#.###");

        for (int j = 0; j < vector.getNumberOfBaseVectors(); j++) {

            ArrayVector arrayVector = (ArrayVector)vector.getVectorByPosition(j);
            double[] array = deepCopy(arrayVector.toArray());


            int[] index = weka.core.Utils.sort(array);
            weka.core.Utils.normalize(array);

            System.out.print("Topic "+j+": ");
            for (int i = index.length-1; i > index.length-100 && i>=0; i--) {
                System.out.print("("+mapWords.get(index[i])+", "+formateador.format(array[index[i]])+"), ");
            }

            System.out.println();

        }
    }


    public static void printTopicsTopWord(int ntop, CompoundVector vector, Map<Integer, String> mapWords) {
        DecimalFormat formateador = new DecimalFormat("#.###");

        for (int j = 0; j < vector.getNumberOfBaseVectors(); j++) {

            ArrayVector arrayVector = (ArrayVector)vector.getVectorByPosition(j);
            double[] array = deepCopy(arrayVector.toArray());


            int[] index = weka.core.Utils.sort(array);
            weka.core.Utils.normalize(array);

            System.out.print("Topic "+j+": ");
            for (int i = index.length-1; i > index.length-ntop-1 && i>=0; i--) {
                //System.out.print("("+mapWords.get(index[i]+1)+", "+formateador.format(array[index[i]])+"), ");
                System.out.print(mapWords.get(index[i])+",");
            }

            System.out.println();

        }
    }

    public static void printTopicsTopWordsProbMass(int ntop, CompoundVector vector, Map<Integer, String> mapWords, double[] vals) {
        DecimalFormat formateador = new DecimalFormat("#.###");

        for (int j = 0; j < vector.getNumberOfBaseVectors(); j++) {

            ArrayVector arrayVector = (ArrayVector)vector.getVectorByPosition(j);
            double[] array = deepCopy(arrayVector.toArray());


            int[] index = weka.core.Utils.sort(array);
            weka.core.Utils.normalize(array);

            double massProb = 0;
            System.out.print("Topic "+j+" "+formateador.format(vals[j])+" ");
            for (int i = index.length-1; i > index.length-ntop-1 && i>=0; i--) {
                System.out.print(mapWords.get(index[i])+" ");
                massProb+=array[index[i]];
                if (massProb>0.95)
                    break;
            }
            massProb = 0;
            System.out.print("Topic "+j+" "+formateador.format(vals[j])+" ");
            for (int i = index.length-1; i > index.length-ntop-1 && i>=0; i--) {
                System.out.print(formateador.format(array[index[i]])+" ");
                massProb+=array[index[i]];
                if (massProb>0.95)
                    break;
            }

            System.out.println();

        }
    }

    public static void printTopicsTopWord(int ntop, CompoundVector vector, Map<Integer, String> mapWords, double[] vals) {
        DecimalFormat formateador = new DecimalFormat("#.##");

        for (int j = 0; j < vector.getNumberOfBaseVectors(); j++) {

            ArrayVector arrayVector = (ArrayVector)vector.getVectorByPosition(j);
            double[] array = deepCopy(arrayVector.toArray());


            int[] index = weka.core.Utils.sort(array);
            weka.core.Utils.normalize(array);

            System.out.print("Topic "+j+" ("+formateador.format(vals[j])+"): ");
            for (int i = index.length-1; i > index.length-ntop-1 && i>=0; i--) {
                //System.out.print("("+mapWords.get(index[i]+1)+", "+formateador.format(array[index[i]])+"), ");
                System.out.print(mapWords.get(index[i])+",");
            }

            System.out.println();

        }
    }


    public static void printTopicsProportions(PlateauLDA plateauLDA) {
        DecimalFormat formateador = new DecimalFormat("#.##");

        Vector sum = plateauLDA.getVMP().getNodes().stream()
                .filter(node -> node.getMainVariable().getName().contains("TopicIndicator_DirichletParameter_"))
                .map(node -> node.getQDist().getNaturalParameters())
                .reduce((a, b) -> {
                    ((Vector) b).sum((Vector) a);
                    return b;
                }).get();
        int totalDocs =  plateauLDA.getReplicatedNodes()
                .filter(node -> node.getMainVariable().getName().contains("TopicIndicator_DirichletParameter_"))
                .mapToInt(node -> 1).sum();

        System.out.print("Topics proportions\t");
        for (int j = 0; j < sum.size(); j++) {
            System.out.print(j+" "+ formateador.format(sum.get(j)/totalDocs)+ " ");
        }
        System.out.println();
        System.out.println();
    }


    public static void printTopicsProportionsNormalized(PlateauLDA plateauLDA) {
        DecimalFormat formateador = new DecimalFormat("#.##");



        Vector sum = plateauLDA.getVMP().getNodes().stream()
                    .filter(node -> node.getMainVariable().getName().contains("TopicIndicator_DirichletParameter_"))
                    .map(node -> node.getQDist().getNaturalParameters())
                    .map(vector -> {
                        Vector newVector = deepCopy(vector);
                        double sumLocal = newVector.sum();
                        newVector.divideBy(sumLocal);
                        return newVector;
                    }).reduce((a, b) -> {
                        ((Vector) b).sum((Vector) a);
                        return b;
                    }).get();
        long totalDocs =  plateauLDA.getReplicatedNodes()
                .filter(node -> node.getMainVariable().getName().contains("TopicIndicator_DirichletParameter_"))
                .count();

        System.out.print("Topics proportions\t");
        for (int j = 0; j < sum.size(); j++) {
            System.out.print(j+" "+ formateador.format(sum.get(j)/totalDocs)+ " ");
        }
        System.out.println();
        System.out.println();
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

    public static void splitNIPSPapers(String[] args) throws IOException {
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/";
        String arrffName = "docword.nips.arff";

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+arrffName);

        List<DataOnMemory<DataInstance>> batches = BatchSpliteratorByID.streamOverDocuments(dataInstances,150).collect(Collectors.toList());


        int count = 0;
        for (DataOnMemory<DataInstance> batch : batches) {
            DataStreamWriter.writeDataToFile(batch,dataPath + "nipsByYear/nips_"+count+".arff");
            count++;
        }

        System.out.println(batches.size());
    }

    public static void splitNIPSTFIDFPapers(String[] args) throws IOException {
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/";
        String arrffName = "docword.nips.tfidf.arff";

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+arrffName);

        List<DataOnMemory<DataInstance>> batches = BatchSpliteratorByID.streamOverDocuments(dataInstances,1).collect(Collectors.toList());


        ArrayList<List<DataInstance>> dataInstanceList = new ArrayList<List<DataInstance>>(10);
        for (int i = 0; i < 10; i++) {
            dataInstanceList.add(new ArrayList<>());
        }
        for (DataOnMemory<DataInstance> batch : batches) {

            double doc_id = batch.getDataInstance(0).getValue(dataInstances.getAttributes().getAttributeByName("SEQUENCE_ID"));

            int year = Math.floorDiv((int)doc_id,150);

            if (year==10)
                year=9;

            dataInstanceList.get(year).addAll(batch.getList());

        }

        for (int i = 0; i < 10; i++) {
            DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<DataInstance>(dataInstances.getAttributes());
            newData.addAll(dataInstanceList.get(i));
            DataStreamWriter.writeDataToFile(newData,dataPath + "nipsTFIDFByYear/nips_"+i+".arff");
        }

        System.out.println(batches.size());
    }


    public static void tf_idf(String[] args) throws IOException {
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/uci-text/";
        String arrffName = "docword.nips.arff";

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+arrffName);

        List<DataOnMemory<DataInstance>> batches = BatchSpliteratorByID.streamOverDocuments(dataInstances,1).collect(Collectors.toList());

        Attribute word = dataInstances.getAttributes().getAttributeByName("word");
        Attribute count = dataInstances.getAttributes().getAttributeByName("count");

        int nTotalDocs = batches.size();

        HashMap<Integer,Double> IDF = new HashMap<>();

        for (DataOnMemory<DataInstance> batch : batches) {
            for (DataInstance dataInstance : batch) {
                if (IDF.containsKey((int)dataInstance.getValue(word)))
                    IDF.put((int)dataInstance.getValue(word),IDF.get((int)dataInstance.getValue(word))+1);
                else
                    IDF.put((int)dataInstance.getValue(word),1.0);
            }
        }

        for (Integer integer : IDF.keySet()) {
            IDF.put(integer,Math.log(nTotalDocs/(double)IDF.get(integer)));
        }



        List<DataInstance> newDataInstances = new ArrayList<>();
        for (DataOnMemory<DataInstance> batch : batches) {

            double nwordsDoc = 0;
            for (DataInstance dataInstance : batch) {
                nwordsDoc += dataInstance.getValue(count);
            }

            for (DataInstance dataInstance : batch) {
                double tf = dataInstance.getValue(count)/nwordsDoc;

                double tf_idf = tf*IDF.get((int)dataInstance.getValue(word));

                if (tf_idf>0.01){
                    newDataInstances.add(dataInstance);
                }
            }


        }


        HashMap<Integer,Double> newWORDS = new HashMap<>();
        for (DataInstance newDataInstance : newDataInstances) {
            newWORDS.put((int)newDataInstance.getValue(word),1.0);
        }

        DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<DataInstance>(dataInstances.getAttributes());
        newData.addAll(newDataInstances);

        DataStreamWriter.writeDataToFile(newData, dataPath+"docword.nips.tfidf.arff");

        Path path = Paths.get(dataPath+"docword.nips.tfidf.arff");
        List<String> fileContent = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));

        for (int i = 0; i < fileContent.size(); i++) {
            if (fileContent.get(i).equals("@attribute word SparseMultinomial 12420")) {
                fileContent.set(i, "@attribute word SparseMultinomial "+newWORDS.keySet().size());
                break;
            }
        }

        Files.write(path, fileContent, StandardCharsets.UTF_8);

        System.out.println(newWORDS.keySet().size());
        System.out.println(BatchSpliteratorByID.streamOverDocuments(newData,1).collect(Collectors.toList()).size());

    }
    public static void main(String[] args) throws IOException {

        Utils.tf_idf(args);
        Utils.splitNIPSTFIDFPapers(args);
    }

}
