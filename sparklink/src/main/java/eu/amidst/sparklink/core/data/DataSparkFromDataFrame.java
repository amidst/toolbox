package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attributes;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;


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
    public JavaRDD<DataInstance> getDataSet(){
        return DataFrameOps.toDataInstanceRDD(data, attributes);
    }

    @Override
    public DataFrame getDataFrame(SQLContext sql) {
        return data;
    }

    @Override
    public Attributes getAttributes() {

        return attributes;
    }
}
