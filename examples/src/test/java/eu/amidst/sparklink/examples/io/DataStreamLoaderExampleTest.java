package eu.amidst.sparklink.examples.io;

import eu.amidst.flinklink.examples.extensions.LatentModelsFlink;
import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.io.DataSparkLoader;
import junit.framework.TestCase;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.SQLContext;
import org.junit.Test;

/**
 * Created by rcabanas on 10/06/16.
 */
public class DataStreamLoaderExampleTest extends TestCase {
    @Test
    public void test() throws Exception {
        DataStreamLoaderExample.main(null);
    }
}