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
        SparkConf conf = new SparkConf().setAppName("SparkLink!").setMaster("local");;
        SparkContext sc = new SparkContext(conf);
        SQLContext sqlContext = new SQLContext(sc);

        //Paths to datasets
        String path = "datasets/simulated/syntheticData.arff";
        DataFrame df = sqlContext.read().format("parquet").load(path);


        DataSpark dataSpark = DataSparkLoader.loadSparkDataFrame(df);

        dataSpark.collectDataStream().forEach(dataInstance -> System.out.println(dataInstance) );


    }
}
