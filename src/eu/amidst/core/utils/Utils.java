package eu.amidst.core.utils;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public class Utils {


    public static boolean isMissing(double val){
        return Double.isNaN(val);
    }

    public static void accumulatedSumVectors(double[] a, double[] b){
        for (int i=0; i<a.length; i++){
            a[i]+=b[i];
        }
    }
}
