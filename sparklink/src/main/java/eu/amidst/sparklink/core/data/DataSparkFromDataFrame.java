package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attributes;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrame;


/**
 * Created by jarias on 20/06/16.
 */


public class DataSparkFromDataFrame implements DataSpark {

    private final DataFrame data;

    private Attributes attributes;

    public DataSparkFromDataFrame(DataFrame d) throws Exception {

        data = d.cache();
        attributes = SchemaConverter.getAttributes(data);
    }

    @Override
    public JavaRDD<DataOnMemory<DataInstance>> getBatchedDataSet(int batchSize){

        // Each batch correspond to a particular partition
        // Lazily convert the DataFrame into an RDD:
        JavaRDD<DataInstance> instanceRDD = DataFrameOps.toDataInstanceRDD(data, attributes);

        JavaRDD<DataOnMemory<DataInstance>> batchedRDD = DataFrameOps.toBatchedRDD(instanceRDD, attributes, batchSize);

        return batchedRDD;
    }

    @Override
    public DataFrame getDataFrame() {
        return data;
    }

    @Override
    public Attributes getAttributes() {

        return attributes;
    }
}
