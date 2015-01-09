package eu.amidst.core.utils;

import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 28/08/14.
 */
public final class Utils {

    private Utils(){
        //Not called
    }

    public static double missingValue(){
        return Double.NaN;
    }

    public static boolean isMissing(double val){
        return Double.isNaN(val);
    }

    public static void accumulatedSumVectors(double[] a, double[] b){
        for (int i=0; i<a.length; i++){
            a[i]+=b[i];
        }
    }

    public static int maxIndex(double[] vals){
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i=0; i<vals.length; i++){
            if (vals[i]>max) {
                max = vals[i];
                index = i;
            }
        }
        return index;
    }

    public static double[] normalize(double[] vals) {
        double sum = 0;
        for (int i=0; i<vals.length; i++) {
            sum+=vals[i];
        }

        for (int i=0; i<vals.length; i++) {
            vals[i] /= sum;
        }

        return vals;

    }

    public static double[] newNormalize(double[] vals) {
        double sum = 0;
        for (int i=0; i<vals.length; i++) {
            sum+=vals[i];
        }

        double[] normalizedVals = new double[vals.length];

        for (int i=0; i<vals.length; i++) {
            normalizedVals[i] = vals[i]/sum;
        }

        return normalizedVals;

    }

    public static double[] logs2probs(double[] vals){
        double max = vals[Utils.maxIndex(vals)];
        double[] normalizedVals = new double[vals.length];
        for (int i=0; i<vals.length; i++) {
            normalizedVals[i] = Math.exp(vals[i]+max);
        }
        return Utils.normalize(normalizedVals);
    }

    public static boolean isLinkCLG(Variable child, Variable parent){
        return !(child.getDistributionType()== DistType.MULTINOMIAL && parent.getDistributionType()==DistType.GAUSSIAN);
    }


}
