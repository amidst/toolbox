package eu.amidst.sparklink.core.data;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

import static eu.amidst.core.variables.StateSpaceTypeEnum.REAL;

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

        JavaRDD<double[]> rawRDD = amidstRDD.map(i -> i.toArray());

        // Generate the schema based on the list of attributes and depending on their type:
        List<StructField> fields = new ArrayList<StructField>();

        for (Attribute att: attributes.getFullListOfAttributes()) {

            if (att.getStateSpaceType().getStateSpaceTypeEnum() == REAL)
                fields.add(DataTypes.createStructField(att.getName(), DataTypes.DoubleType, true));
            else
                fields.add(DataTypes.createStructField(att.getName(), DataTypes.StringType, true));

        }
        StructType schema = DataTypes.createStructType(fields);

        // FIXME: Categorical values should be inserted with their correspoding state name
        JavaRDD<Row> rowRDD = rawRDD.map(RowFactory::create);

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
