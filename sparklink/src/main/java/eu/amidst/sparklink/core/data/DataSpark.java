package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrame;

/**
 * Created by jarias on 22/06/16.
 */
public interface DataSpark {

    DataFrame getDataFrame();

    Attributes getAttributes();

    JavaRDD<DataInstance> getDataSet();

    default JavaRDD<DataOnMemory<DataInstance>> getBatchedDataSet(int batchSize){
        return DataFrameOps.toBatchedRDD(this.getDataSet(), this.getAttributes(), batchSize);
    }

}
