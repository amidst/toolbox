package eu.amidst.sparklink.core.io;

import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.data.DataSparkFromDataFrame;
import org.apache.spark.sql.DataFrame;

/**
 * Created by jarias on 22/06/16.
 */
public class DataSparkLoader {

    public static DataSpark loadSparkDataFrame(DataFrame df) throws Exception {

        return new DataSparkFromDataFrame(df);
    }
}
