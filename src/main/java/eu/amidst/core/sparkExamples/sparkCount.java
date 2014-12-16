package eu.amidst.core.sparkExamples;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.SparkConf;


/**
 * Created by sigveh on 16/12/14.
 */
public class sparkCount {

    private String appName = "applictaionCount";
    private String master  =   "local";

    public void main(){
        SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
        JavaSparkContext sc = new JavaSparkContext(conf);

    }


}
