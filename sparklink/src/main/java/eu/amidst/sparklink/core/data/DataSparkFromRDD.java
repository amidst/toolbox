package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.StructType;

/**
 * Created by jarias on 22/06/16.
 */
public class DataSparkFromRDD implements DataSpark {

    private Attributes attributes;
    final private JavaRDD<DataInstance> amidstRDD;
    JavaSparkContext sc;


    public DataSparkFromRDD(JavaSparkContext sc, JavaRDD<DataInstance> input, Attributes atts) {

        this.sc = sc;

        amidstRDD = input;
        attributes = atts;
    }

    @Override
    public DataFrame getDataFrame() {

        // Obtain the schema
        StructType schema = SchemaConverter.getSchema(attributes);

        // Transform the RDD
        JavaRDD<Row> rowRDD = DataFrameOps.toRowRDD(amidstRDD, attributes);

        // Create the DataFrame
        SQLContext sql = new SQLContext(sc);
        return sql.createDataFrame(rowRDD, schema);
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
