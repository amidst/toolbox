package eu.amidst.sparklink.examples.util;

import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.io.DataSparkWriter;
import eu.amidst.sparklink.core.util.BayesianNetworkSampler;
import eu.amidst.sparklink.examples.io.DataStreamWriterExample;
import junit.framework.TestCase;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.junit.Test;

/**
 * Created by rcabanas on 27/09/16.
 */
public class SamplingBNTest extends TestCase {
	@Test
	public void test() throws Exception {
		SamplingBN.main(null);
	}
}

