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

/*
package eu.amidst;


import eu.amidst.core.database.DataStream;
import eu.amidst.core.database.statics.readers.DataStreamReaderFromFile;
import eu.amidst.staticmodelling.models.NaiveBayesClassifier;
import eu.amidst.staticmodelling.models.NaiveBayesClusteringModel;


public class Main {

    public static void learningNaiveBayesClusteringModel(){

        DataStreamReaderFromFile reader = new DataStreamReaderFromFile("./dataTests/data.arff");

        DataStream dataStream = reader.getDataStream();

        NaiveBayesClusteringModel clusteringModel = new NaiveBayesClusteringModel();

        clusteringModel.buildStructure(dataStream.getStaticDataHeader());
        clusteringModel.initLearning();
        clusteringModel.learnModelFromStream(dataStream);

        dataStream.restart();
        clusteringModel.clusterMemberShip(dataStream.nextDataInstance());
    }

    public static void learningNaiveBayes(){

        DataStreamReaderFromFile reader = new DataStreamReaderFromFile("./dataTests/data.arff");

        DataStream dataStream = reader.getDataStream();

        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.setClassVarID(dataStream.getStaticDataHeader().getObservedVariables().size()-1);
        nb.buildStructure(dataStream.getStaticDataHeader());
        nb.initLearning();
        nb.learnModelFromStream(dataStream);

        dataStream.restart();
        nb.predict(dataStream.nextDataInstance());
    }

    public static void main(String[] args) {

        System.out.println("Hello World!");
    }
}*/


package eu.amidst.dynamic.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class Main {

    public static void test1(String[] args) throws Exception {


        IntStream stream = IntStream.range(0, 10);

        OptionalInt optionalInt = stream.filter(i -> i >= 3).findFirst();

        System.out.println(optionalInt.isPresent());
        System.out.println(optionalInt.getAsInt());

        //stream.forEach(System.out::println);

        List<Integer> list = new ArrayList<>(Arrays.asList(1, 10, 3, 7, 5));
        Stream<Integer> streamList = list.stream();
        int a = streamList.filter(x -> {
            System.out.println("filtered");
            return x > 5;
        }).findFirst().get();
        System.out.println(a);

        //stream.forEach(System.out::println);


        Stream<String> words = Stream.of("Java", "Magazine", "is",
                "the", "best");

        Map<String, Long> letterToCount =
                words.map(w -> w.split(""))
                        .flatMap(Arrays::stream)
                        .collect(groupingBy(identity(), counting()));

        System.out.println(letterToCount.toString());


        System.out.println(letterToCount.toString());


        DataStream<DataInstance> data = DataStreamLoader.open("datasets/simulated/syntheticData.arff");

        Iterator<DataInstance> it = data.iterator();
        for (int i = 0; i < 3; i++) {
            System.out.println(it.next().outputString());
        }


        List<String> docs = Arrays.asList("Hola bien", "estoy bien");

        Map<String, Long> wordToCount = docs.parallelStream()
                .flatMap(doc -> Arrays.stream(doc.split(" ")))
                .collect(groupingBy(identity(), counting()));

        System.out.println(wordToCount);


        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/simulated/Asia.bn");
        System.out.println(bn.toString());
        DAG dag = bn.getDAG();

        long nlinks = dag.getParentSets().stream().mapToInt(p -> p.getParents().size()).count();

        Variable var = bn.getVariables().getVariableByName("A");
        dag.getParentSets().stream().filter(parentSet -> parentSet.getParents().contains(var))
                .map(parentSet -> parentSet.getMainVar())
                .collect(Collectors.toList());


    }

    public static void test2(String[] args) throws Exception {
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/dataWeka/Asia.bn");
        DAG dag = bn.getDAG();
        System.out.println(dag.toString());

        Variable var1 = dag.getVariables().getVariableByName("D");
        Variable var2 = dag.getVariables().getVariableByName("A");


        Map<Variable, Long> map = dag.getParentSet(var1).getParents().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        while (!map.containsKey(var2) && !map.entrySet().isEmpty()) {
            map = map.entrySet().parallelStream()
                    .flatMap(entry -> dag.getParentSet(entry.getKey()).getParents().stream().map(v -> new AbstractMap.SimpleEntry<Variable, Long>(v, 1 + entry.getValue())))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (a, b) -> (a < b) ? a : b));
        }

        if (map.containsKey(var2)) {
            System.out.println("Distance from " + var2.getName() + " to " + var1.getName() + " is " + map.get(var2));
            return;
        }

        map = dag.getParentSet(var2).getParents().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));


        while (!map.containsKey(var1) && !map.entrySet().isEmpty()) {
            map = map.entrySet().parallelStream()
                    .flatMap(entry -> dag.getParentSet(entry.getKey()).getParents().stream().map(v -> new AbstractMap.SimpleEntry<Variable, Long>(v, 1 + entry.getValue())))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (a, b) -> (a < b) ? a : b));
        }

        if (map.containsKey(var1)) {
            System.out.println("Distance from " + var1.getName() + " to " + var2.getName() + " is " + map.get(var1));
            return;
        }

    }


    public static void main(String[] args) throws Exception {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/dataWeka/asia.bn");
        DAG dag = bn.getDAG();
        System.out.println(dag.toString());

        Variable var1 = dag.getVariables().getVariableByName("D");
        Variable var2 = dag.getVariables().getVariableByName("A");


        Map<Variable, Long> map = dag.getParentSet(var1).getParents().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        while (!map.containsKey(var2) && !map.entrySet().isEmpty()) {
            map = map.entrySet().parallelStream()
                    .flatMap(entry -> dag.getParentSet(entry.getKey()).getParents().stream().map(v -> new AbstractMap.SimpleEntry<Variable, Long>(v, 1 + entry.getValue())))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (a, b) -> (a < b) ? a : b));
        }

        if (map.containsKey(var2)) {
            System.out.println("Distance from " + var2.getName() + " to " + var1.getName() + " is " + map.get(var2));
            return;
        }


    }
}