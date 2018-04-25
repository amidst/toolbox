package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

import java.util.List;

/**
 * Created by jarias on 22/06/16.
 */
public interface DataSpark {

    DataFrame getDataFrame(SQLContext sql);

    Attributes getAttributes();

    JavaRDD<DataInstance> getDataSet();

    default JavaRDD<DataOnMemory<DataInstance>> getBatchedDataSet(int batchSize){
        return DataFrameOps.toBatchedRDD(this.getDataSet(), this.getAttributes(), batchSize);
    }

    default DataStream<DataInstance> collectDataStream() {

        List<DataInstance> local = getDataSet().collect();

        return new DataOnMemoryListContainer<DataInstance>(getAttributes(), local);
    }
}
