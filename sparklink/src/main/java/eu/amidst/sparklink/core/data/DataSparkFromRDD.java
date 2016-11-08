package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.StructType;

import java.io.Serializable;

/**
 * Created by jarias on 22/06/16.
 */
public class DataSparkFromRDD implements DataSpark {

    private Attributes attributes;
    final private JavaRDD<DataInstance> amidstRDD;

    public DataSparkFromRDD(JavaRDD<DataInstance> input, Attributes atts) {

        // FIXME: is this a good idea?
        amidstRDD = input.cache();
        attributes = atts;
    }

    @Override
    public DataFrame getDataFrame(SQLContext sql) {

        // Obtain the schema
        StructType schema = SchemaConverter.getSchema(attributes);

        // Transform the RDD
        JavaRDD<Row> rowRDD = DataFrameOps.toRowRDD(amidstRDD, attributes);

        // Create the DataFrame
        return sql.createDataFrame(rowRDD, schema);
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    @Override
    public JavaRDD<DataInstance> getDataSet() {
        return amidstRDD;
    }
}
