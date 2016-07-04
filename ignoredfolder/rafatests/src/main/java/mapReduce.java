import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.variables.Variable;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by rcabanas on 15/06/16.
 */
public class mapReduce {
    public static void main(String[] args) {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,10000,0,1);


        Attribute att = data.getAttributes().getAttributeByName("GaussianVar0");
        data.stream().forEach(d->System.out.println(d.getValue(att)));


        List<Double> list = new ArrayList<Double>();

        for(int i=0; i<100; i++) {
            list.add((double) i);
        }

        // list.stream().forEach(d -> System.out.println(d));

        list.parallelStream()
                .forEach(d -> System.out.println(d));

        double sum = list.parallelStream().reduce((x,y)->x+y).get().doubleValue();


        long t1 = System.nanoTime();


        //Parallel computation of the mean from the values in a stream
        sum = data.parallelStream(100).mapToDouble((d) -> d.getValue(att)).reduce((x,y)->x+y).getAsDouble();
        long n =  data.parallelStream(100).count();
     /*
        sum = data.stream().mapToDouble((d) -> d.getValue(att)).reduce((x,y)->x+y).getAsDouble();
        long n =  data.stream().count();

        */

        double mean = sum/n;
        long t2 = System.nanoTime();
        System.out.println("mean value = "+mean+", time ="+(t2-t1)/1000);




    }
}
