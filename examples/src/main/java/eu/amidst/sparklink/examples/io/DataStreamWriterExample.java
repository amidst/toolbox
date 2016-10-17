package eu.amidst.sparklink.examples.io;

import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.io.DataSparkWriter;
import eu.amidst.sparklink.core.util.BayesianNetworkSampler;
import eu.amidst.sparklink.core.util.DataSetGenerator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;

/**
 * Created by rcabanas on 30/09/16.
 */
public class DataStreamWriterExample {

	public static void main(String[] args) throws Exception {

		//Setting up spark
		SparkConf conf = new SparkConf().setAppName("SparkLink!").setMaster("local");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		SQLContext sqlContext = new SQLContext(jsc);


		//Generate random data
		int seed = 1234;
		int nInstances = 1000;
		int nDiscreteAtts=3;
		int nContinuousAttributes = 2;

		DataSpark data = DataSetGenerator
				.generate(	jsc,
							seed,
							nInstances,
							nDiscreteAtts,
							nContinuousAttributes );


		// Save it as a json and parquet file
		DataSparkWriter.writeDataToFolder(data, "datasets/simulated/randomData.json", sqlContext);
		DataSparkWriter.writeDataToFolder(data, "datasets/simulated/randomData.parquet", sqlContext);

	}



}




