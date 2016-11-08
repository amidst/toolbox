package eu.amidst.sparklink.core.util;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.data.DataSparkFromDataStream;
import eu.amidst.sparklink.core.io.DataSparkWriter;
import org.apache.commons.lang.NotImplementedException;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;

/**
 * Created by rcabanas on 30/09/16.
 */
public class FileConverter {

	public static void ARFFtoSparkFormat(String arffFile, String sparkFile, String outputFormat, SQLContext sqlContext, JavaSparkContext jsc) throws Exception {


		throw new NotImplementedException("ARFFtoSparkFormat not implemented yet");

	/*	DataSpark data = new DataSparkFromDataStream(DataStreamLoader.open(arffFile), jsc);

		JavaRDD<DataInstance> rdd = data.getDataSet();

		System.out.println(rdd.getNumPartitions());
		//rdd.collect().forEach(dataInstance -> System.out.println(dataInstance.getValue(data.getAttributes().getAttributeByName("A"))));
		//DataSparkWriter.writeDataToFolder(data, sparkFile, sqlContext, outputFormat);
		System.out.println("");
		*/
	}


	public static void main(String[] args) throws Exception {
		String arffFile = "datasets/simulated/syntheticData.arff";
		String sparkFile = "datasets/simulated/syntheticData.json";


		SparkConf conf = new SparkConf().setAppName("SparkLink!").setMaster("local");
		SparkContext sc = new SparkContext(conf);
		SQLContext sqlContext = new SQLContext(sc);
		JavaSparkContext jsc = new JavaSparkContext(sc);



		ARFFtoSparkFormat(arffFile, sparkFile, "json", sqlContext, jsc);



	}


}
