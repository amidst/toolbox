package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrame;

/**
 * Created by jarias on 22/06/16.
 */
public class DataSparkFromRDD implements DataSpark {

    private Attributes attributes;
    final private JavaRDD<DataInstance> amidstRDD;

    public DataSparkFromRDD(JavaRDD<DataInstance> input, Attributes atts) {

        amidstRDD = input;
        attributes = atts;
    }

    @Override
    public DataFrame getDataFrame() {
        return null;
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public JavaRDD<DataOnMemory<DataInstance>> getBatchedDataSet(int batchSize) {
        return DataFrameOps.toBatchedRDD(amidstRDD, attributes, batchSize);
    }
}
