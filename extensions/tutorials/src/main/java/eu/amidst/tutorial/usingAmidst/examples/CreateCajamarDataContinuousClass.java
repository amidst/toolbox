package eu.amidst.tutorial.usingAmidst.examples;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.tutorial.usingAmidst.Main;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by rcabanas on 20/05/16.
 */
public class CreateCajamarDataContinuousClass {

    public static void main(String[] args) throws Exception{

        int nContinuousAttributes=4;
        int nDiscreteAttributes=1;
        String names[] = {"SEQUENCE_ID", "TIME_ID","Default","Income","Expenses","Balance","TotalCredit"};
        String path = "datasets/simulated/";
        int nSamples=1000;
        int seed = 11234;
        String filename="bank_data_test";




        //Generate random dynamic data
        DataStream<DynamicDataInstance> data  = DataSetGenerator.generate(seed,nSamples,nDiscreteAttributes,nContinuousAttributes);
        List<Attribute> list = new ArrayList<Attribute>();


        //Replace the names
        IntStream.range(0, data.getAttributes().getNumberOfAttributes())
                .forEach(i -> {
                    Attribute a = data.getAttributes().getFullListOfAttributes().get(i);
                    StateSpaceType s = a.getStateSpaceType();
                    Attribute a2 = new Attribute(a.getIndex(), names[i],s);
                    list.add(a2);
                });


        //New list of attributes
        Attributes att2 = new Attributes(list);



        List<DynamicDataInstance> listData = data.stream().collect(Collectors.toList());


        //Datastream with the new attribute names
        DataStream<DynamicDataInstance> data2 =
                new DataOnMemoryListContainer<DynamicDataInstance>(att2,listData);


        //Write to a single file
        DataStreamWriter.writeDataToFile(data2, path+filename+".arff");




    }

}
