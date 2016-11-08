package eu.amidst.multicore;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.utils.DataSetGenerator;

/**
 * Created by rcabanas on 16/06/16.
 */
public class ParallelStreamExample {
    public static void main(String[] args) {

        int batchSize = 100;

        //Generate a random DataStream
        DataStream<DataInstance> data = DataSetGenerator.generate(1234,10000,0,1);

        //Print the value of the attribute GaussianVar0
        Attribute att = data.getAttributes().getAttributeByName("GaussianVar0");
        data.stream()
                .forEach(d->System.out.println(d.getValue(att)));


        //Parallel computation of the sum and the number of instances
        double sum = data
                .parallelStream(batchSize)
                .mapToDouble((d) -> d.getValue(att))
                .reduce((x,y)->x+y)
                .getAsDouble();


        long n =  data
                .parallelStream(batchSize)
                .count();


        //Compute and print the mean value
        double mean = sum/n;
        System.out.println("mean value = "+mean);


    }
}
