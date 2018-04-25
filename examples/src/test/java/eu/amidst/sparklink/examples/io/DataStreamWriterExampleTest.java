package eu.amidst.sparklink.examples.io;

import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.io.DataSparkWriter;
import eu.amidst.sparklink.core.util.DataSetGenerator;
import junit.framework.TestCase;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.junit.Test;

/**
 * Created by rcabanas on 30/09/16.
 */
public class DataStreamWriterExampleTest extends TestCase {
	@Test
	public void test() throws Exception {
		DataStreamWriterExample.main(null);
	}
}


