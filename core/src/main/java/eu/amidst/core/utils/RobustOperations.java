package eu.amidst.core.utils;

/**
 * Created by dario on 23/6/16.
 */
public class RobustOperations {

    public static double robustSumOfLogarithmsWithZeros(double log_x1, double log_x2) {
        double result;

        if(log_x1!=0 && log_x2!=0) {

            double aux_max = Math.max(log_x1,log_x2);
            double aux_min = Math.min(log_x1,log_x2);

            double tail;
            double aux = Math.exp(aux_min-aux_max);
            if (aux<0.5) {
                tail = Math.log1p( aux );
            }
            else {
                tail = Math.log( 1 + aux );
            }
//            tail = Math.log( 1+aux );

            //double tail = Math.log1p( Math.exp(aux_min-aux_max) );
            result = aux_max + (Double.isFinite(tail) ? tail : 0);
        }
        else if (log_x1==0) {
            result=log_x2;
        }
        else if (log_x1 == Double.NEGATIVE_INFINITY) {
            result=log_x2;
        }
        else {
            result=log_x1;
        }
        return result;
    }

    public static double robustDifferenceOfLogarithmsWithZeros(double log_x1, double log_x2) {
        double result;
        if(log_x1!=0 && log_x2!=0) {

            double aux_max = Math.max(log_x1,log_x2);
            double aux_min = Math.min(log_x1,log_x2);

            double tail;
            double aux = Math.exp(aux_min-aux_max);
//            if (aux<0.5) {
//                tail = Math.log1p( -aux );
//            }
//            else {
//                tail = Math.log( 1 - aux );
//            }

            tail = Math.log( 1 - aux );
//            tail = Math.log( 1+aux );

            //double tail = Math.log1p( Math.exp(aux_min-aux_max) );
            result = aux_max + (Double.isFinite(tail) ? tail : 0);
        }
        else if (log_x1==0) {
            result=log_x2;
        }
        else {
            result=log_x1;
        }
        return result;
    }

    public static double robustSumOfLogarithms(double log_x1, double log_x2) {
        double result;

        if(log_x1 == Double.NEGATIVE_INFINITY && log_x2 == Double.NEGATIVE_INFINITY) {
            return Double.NEGATIVE_INFINITY;
        }
        double aux_max = Math.max(log_x1,log_x2);
        double aux_min = Math.min(log_x1,log_x2);

        result = aux_max + Math.log1p( Math.exp(aux_min-aux_max) );

        return result;
    }

    public static double robustDifferenceOfLogarithms(double log_x1, double log_x2) {
        double result;

        double aux_max = Math.max(log_x1,log_x2);
        double aux_min = Math.min(log_x1,log_x2);

        result = aux_max + Math.log1p( -Math.exp(aux_min-aux_max) );

        return result;
    }


    public static ArrayVector robustSumOfMultinomialLogSufficientStatistics(ArrayVector ss1, ArrayVector ss2) {
        double[] ss1_values = ss1.toArray();
        double[] ss2_values = ss2.toArray();
        double[] ss_result = new double[ss1_values.length];

        for (int i = 0; i < ss_result.length; i++) {

            double log_a = ss1_values[i];
            double log_b = ss2_values[i];

//            if(log_a!=0 && log_b!=0) {
//
//                double aux_max = Math.max(log_a,log_b);
//                double aux_min = Math.min(log_a,log_b);
//
//                double tail = Math.log1p( Math.exp(aux_min-aux_max) );
//                //System.out.println(tail);
//                ss_result[i] = aux_max + (Double.isFinite(tail) ? tail : 0);
//            }
//            else if (log_a==0) {
//                ss_result[i]=log_b;
//            }
//            else {
//                ss_result[i]=log_a;
//            }

            ss_result[i] = robustSumOfLogarithmsWithZeros(log_a,log_b);
        }
        return new ArrayVector(ss_result);
    }

    public static ArrayVector robustNormalizationOfLogProbabilitiesVector(ArrayVector ss) {

        //System.out.println("ROBUST NORMALIZATION OF: \n\n" + Arrays.toString(ss.toArray()));
        double log_sumProbabilities = 0;
        double [] logProbabilities = ss.toArray();

        for (int i = 0; i < logProbabilities.length; i++) {


            double log_b = logProbabilities[i];
//
//            double log_a = log_sumProbabilities;
//            if(log_a!=0 && log_b!=0) {
//
//                double aux_max = Math.max(log_a,log_b);
//                double aux_min = Math.min(log_a,log_b);
//
//                double tail = Math.log1p( Math.exp(aux_min-aux_max) );
//
//                log_sumProbabilities = aux_max + (Double.isFinite(tail) ? tail : 0);
//            }
//            else if (log_a==0) {
//                log_sumProbabilities=log_b;
//            }
//            else {
//                log_sumProbabilities=log_a;
//            }
            log_sumProbabilities = robustSumOfLogarithmsWithZeros(log_sumProbabilities, log_b);
        }

        //System.out.println("logSUM: " + log_sumProbabilities);

        //System.out.println("ROBUST NORMALIZATION RESULT: \n\n" + Arrays.toString((new ArrayVector(ss.toArray())).toArray()));

        double [] normalizedProbabilities = new double[logProbabilities.length];

        for (int i = 0; i < logProbabilities.length; i++) {
            double result = Math.exp( logProbabilities[i]-log_sumProbabilities );
            normalizedProbabilities[i] = (0<=result && result<=1) ? result : 0;
        }

        //System.out.println(Arrays.toString(normalizedProbabilities));

        return new ArrayVector(normalizedProbabilities);
    }

    private static ArrayVector robustSumOfNormalSufficientStatistics(ArrayVector ss1, ArrayVector ss2) {
        return new ArrayVector(new double[]{ss1.get(0)});

    }
}