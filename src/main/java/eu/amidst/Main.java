/*
package eu.amidst;


import eu.amidst.core.database.DataStream;
import eu.amidst.core.database.statics.readers.DataStreamReaderFromFile;
import eu.amidst.staticmodelling.models.NaiveBayesClassifier;
import eu.amidst.staticmodelling.models.NaiveBayesClusteringModel;


public class Main {

    public static void learningNaiveBayesClusteringModel(){

        DataStreamReaderFromFile reader = new DataStreamReaderFromFile("./data/data.arff");

        DataStream dataStream = reader.getDataStream();

        NaiveBayesClusteringModel clusteringModel = new NaiveBayesClusteringModel();

        clusteringModel.buildStructure(dataStream.getStaticDataHeader());
        clusteringModel.initLearning();
        clusteringModel.learnModelFromStream(dataStream);

        dataStream.restart();
        clusteringModel.clusterMemberShip(dataStream.nextDataInstance());
    }

    public static void learningNaiveBayes(){

        DataStreamReaderFromFile reader = new DataStreamReaderFromFile("./data/data.arff");

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


package eu.amidst;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;

import java.util.*;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class Main {

 public static void main(String[] args) throws Exception {


     IntStream stream = IntStream.range(0,10);

     OptionalInt optionalInt = stream.filter(i -> i >= 3).findFirst();

     System.out.println(optionalInt.isPresent());
     System.out.println(optionalInt.getAsInt());

     //stream.forEach(System.out::println);

     List<Integer> list = new ArrayList<>(Arrays.asList(1, 10, 3, 7, 5));
     Stream<Integer> streamList = list.stream();
     int a = streamList.filter(x -> {System.out.println("filtered"); return x > 5;}).findFirst().get();
     System.out.println(a);

     //stream.forEach(System.out::println);



     Stream<String> words = Stream.of("Java", "Magazine", "is",
             "the", "best");

     Map<String, Long> letterToCount =
             words.map(w -> w.split(""))
                     .flatMap(Arrays::stream)
                     .collect(Collectors.groupingBy(identity(), counting()));

     System.out.println(letterToCount.toString());


     System.out.println(letterToCount.toString());



     DataStream<DataInstance> data = DataStreamLoader.loadFromFile("datasets/syntheticData.arff");

     Iterator<DataInstance> it = data.iterator();
     for (int i = 0; i < 3; i++) {
         System.out.println(it.next().toString());
     }

 }

}