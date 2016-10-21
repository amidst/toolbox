package eu.amidst.sparklink.examples.io;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import org.apache.spark.SparkContext;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;
import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.io.DataSparkLoader;

/**
 * Created by rcabanas on 10/06/16.
 */
public class DataStreamLoaderExample {
    public static void main(String[] args) throws Exception {
        SparkConf conf = new SparkConf().setAppName("SLink!").setMaster("local");
        SparkContext sc = new SparkContext(conf);
        SQLContext sqlContext = new SQLContext(sc);

        //Path to dataset
        String path ="datasets/simulated/WI_samples.json";

		//Create an AMIDST object for managing the data
        DataSpark dataSpark = DataSparkLoader.open(sqlContext, path);


		//Print all the instances in the dataset
        dataSpark.collectDataStream()
                .forEach(
                        dataInstance -> System.out.println(dataInstance)
                );


    }
}
