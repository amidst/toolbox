package eu.amidst.sparklink.core.io;

import eu.amidst.sparklink.core.data.DataSpark;
import eu.amidst.sparklink.core.data.DataSparkFromDataFrame;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

import java.util.Optional;

/**
 * Created by jarias and rcabanas on 22/06/16.
 */
public class DataSparkLoader {

    public static DataSpark loadSparkDataFrame(DataFrame df) throws Exception {

        return new DataSparkFromDataFrame(df);
    }


	/**
	 *
	 * @param sqlContext
	 * @param path
	 * @return
	 * @throws Exception
	 */
    public static DataSpark open(SQLContext sqlContext, String path) throws Exception {

        String formatFile = "";

        //Determine the format of the file
        if (path.endsWith(".json")) {
			formatFile = "json";
		}else if (path.endsWith(".jdbc")) {
			formatFile = "jdbc";
		}else if (path.endsWith(".parquet")) {
			formatFile = "parquet";

		} else {
			throw new IllegalArgumentException("Cannot determine the format of the file: use one built-in sources (json, parquet, jdbc) or explicitily indicate it");
        }

        return open(sqlContext,path,formatFile);
    }

    public static DataSpark open(SQLContext sqlContext, String path, String formatFile) throws Exception {

        //Load the data and store it into an object of class DataFrame
        DataFrame df = sqlContext.read().format(formatFile).load(path);

        //Create an AMIDST object for managing the data
        return loadSparkDataFrame(df);
    }


}
