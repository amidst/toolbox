package eu.amidst.huginlink.inference;


import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

/**
 * Created by dario on 15/05/15.
 */
public class ImportanceSamplingExperiments {

    private static Assignment randomEvidence(long seed, double evidenceRatio, BayesianNetwork bn, Variable varInterest) throws UnsupportedOperationException {

        if (evidenceRatio<=0 || evidenceRatio>=1) {
            throw new UnsupportedOperationException("Error: invalid ratio");
        }

        int numVariables = bn.getStaticVariables().getNumberOfVars();

        Random random=new Random(seed); //1823716125
        int numVarEvidence = (int) Math.ceil(numVariables*evidenceRatio); // Evidence on 20% of variables
        //numVarEvidence = 0;
        //List<Variable> varEvidence = new ArrayList<>(numVarEvidence);
        double [] evidence = new double[numVarEvidence];
        Variable aux;
        HashMapAssignment assignment = new HashMapAssignment(2);

        int[] indexesEvidence = new int[numVarEvidence+1];
        indexesEvidence[0]=varInterest.getVarID();
        //System.out.println(variable.getVarID());

        //System.out.println("Evidence:");
        for( int k=0; k<numVarEvidence; k++ ) {
            int varIndex=-1;
            do {
                varIndex = random.nextInt( bn.getNumberOfVars() );
                //System.out.println(varIndex);
                aux = bn.getStaticVariables().getVariableById(varIndex);

                double thisEvidence;
                if (aux.isMultinomial()) {
                    thisEvidence = random.nextInt( aux.getNumberOfStates() );
                }
                else {
                    thisEvidence = random.nextGaussian();
                }
                evidence[k] = thisEvidence;

            } while ( ArrayUtils.contains(indexesEvidence, varIndex) );

            indexesEvidence[k+1]=varIndex;
            //System.out.println(Arrays.toString(indexesEvidence));
            //System.out.println("Variable " + aux.getName() + " = " + evidence[k]);

            assignment.setValue(aux,evidence[k]);
        }
        //System.out.println();
        return assignment;
    }

    /**
     * The class constructor.
     * @param args Array of options: "filename variable a b N useVMP" if variable is continuous or "filename variable w N useVMP" for discrete
     */
    public static void main(String[] args) throws Exception {


        //*************************************************
        // READING AND INITIALISING PARAMETERS
        //*************************************************

        String filename=""; //Filename with the Bayesian Network
        String varname=""; // Variable of interest in the BN
        double a=0; // Lower endpoint of the interval
        double b=0; // Upper endpoint of the interval
        int N=0; // Sample size

        int REP = 0; // Number of repetitions of the experiment

        int nDiscrete = 0;
        int nStates = 2;
        int nContin = 0;
        int nLinks = 0;

        // FOR A CONTINUOUS VARIABLE OF INTEREST
        if (args.length==4) {

            String a1 = args[0]; // Number of discrete variables
            String a2 = args[1]; // Number of discrete variables
            String a3 = args[2]; // Sample size for IS in each repetition
            String a4 = args[3]; // Number of repetitions of the experiment

            try {

                nDiscrete = Integer.parseInt(a1);
                nContin = Integer.parseInt(a2);
                N = Integer.parseInt(a3);
                REP = Integer.parseInt(a4);

            }
            catch (NumberFormatException e) {
                System.out.println(e.toString());
            }

        }
        // NO ARGUMENTS, DEFAULT INITIALIZATION
        else if (args.length==0) {

            nDiscrete=20;
            nContin=20;


            N = 1000; // Sample size
            REP = 5;

        }
        else {
            System.out.println("Invalid number of arguments. See comments in main");
            System.exit(1);
        }


        nLinks=2*(nDiscrete+nContin);


        BayesianNetwork bn;
        VMP vmp = new VMP();
        ImportanceSampling importanceSampling = new ImportanceSampling();
        HuginInferenceForBN huginInferenceForBN = new HuginInferenceForBN();


        int seed = 1823716125;
        Random random = new Random(seed);


        double [] timeVMP = new double[REP];
        double [] timeIS = new double[REP];
        double [] timeISVMP = new double[REP];
        double [] timeISexact = new double[REP];
        double [] timeHUGIN = new double[REP];

        double [] probabilitiesVMP = new double[REP];
        double [] probabilitiesIS = new double[REP];
        double [] probabilitiesISVMP = new double[REP];
        double [] probabilitiesISexact = new double[REP];
        double [] probabilitiesHUGIN = new double[REP];

        // DO THE "REP" REPETITIONS OF THE EXPERIMENT
        for(int k=0; k<REP; k++) {

            System.out.println("Repetition " + k);
            //*****************************************************************
            // CREATION OF RANDOM HYBRID BAYESIAN NETWORK
            //*****************************************************************

            filename = "networks/randomlyGeneratedBN.bn";
            BayesianNetworkGenerator.generateBNtoFile(nDiscrete, nStates, nContin, nLinks, seed, filename);
            bn = BayesianNetworkLoader.loadFromFile(filename);
            /*
            System.out.println(bn.toString());
            System.out.println(bn.getDAG().toString());
            */

            //*****************************************************************
            // CHOOSING CONTINUOUS VARIABLE OF INTEREST AND INTERVAL
            //*****************************************************************

            Variable varInterest;
            int varIndex = random.nextInt(bn.getStaticVariables().getNumberOfVars());
            do {
                varInterest = bn.getStaticVariables().getVariableById(varIndex);
                varIndex = random.nextInt(bn.getStaticVariables().getNumberOfVars());
                //System.out.println(varInterest.getName());
            } while (varInterest.isNormal() == false);

            varname = varInterest.getName(); // Variable of interest in the BN

            double middlePoint = random.nextGaussian();
            a = middlePoint - 0.5;
            b = middlePoint + 0.5;

            /*
            System.out.println("Variable of interest: " + varname + " in interval (" + Double.toString(a) + "," + Double.toString(b) + ")");
            System.out.println();
            */

            //*****************************************************************
            // CREATE EVIDENCE
            //*****************************************************************
            double observedVariablesRate = 0.05;

            Assignment evidence = randomEvidence(seed, observedVariablesRate, bn, varInterest);


            //*****************************************************************
            // VMP INFERENCE
            //*****************************************************************
            System.out.println("VMP");
            long timeStartVMP = System.nanoTime();
            vmp.setModel(bn);
            vmp.setEvidence(evidence);
            vmp.runInference();

            UnivariateDistribution posteriorVMP = vmp.getPosterior(varInterest);

            double[] parametersVMP = posteriorVMP.getParameters();
            double vmpMean = parametersVMP[0];
            double vmpVar = parametersVMP[1];

            NormalDistribution auxNormal = new NormalDistribution(vmpMean, vmpVar);
            double probVMP = auxNormal.probability(a, b);

            long timeStopVMP = System.nanoTime();
            double execTimeVMP = (double) (timeStopVMP - timeStartVMP) / 1000000000.0;


            //*****************************************************************
            // IMPORTANCE SAMPLING INFERENCE
            //*****************************************************************
            System.out.println("IS-VMP");
            importanceSampling.setModel(bn);
            //importanceSampling.setSamplingModel(vmp.getSamplingModel());
            importanceSampling.setParallelMode(true);
            importanceSampling.setSampleSize(N);

            importanceSampling.setEvidence(evidence);
            importanceSampling.setSamplingModel(bn);


            // IS WITH VMP POSTERIORS
            long timeStartISVMP = System.nanoTime();

            //importanceSampling.setSamplingModel(vmp.getSamplingModel());

            importanceSampling.runInference(vmp);
            //UnivariateDistribution posteriorISVMP = importanceSampling.getPosterior(varInterest);
            double probISVMP = importanceSampling.runQuery(varInterest, a, b);

            long timeStopISVMP = System.nanoTime();
            double execTimeISVMP = (double) (timeStopISVMP - timeStartISVMP) / 1000000000.0;


            // IS WITH CONDITIONALS
            System.out.println("IS");
            long timeStartIS = System.nanoTime();

            //importanceSampling.setSamplingModel(bn);
            importanceSampling.runInference();
            //UnivariateDistribution posteriorIS = importanceSampling.getPosterior(varInterest);
            double probIS = importanceSampling.runQuery(varInterest, a, b);

            long timeStopIS = System.nanoTime();
            double execTimeIS = (double) (timeStopIS - timeStartIS) / 1000000000.0;


            // IS WITH CONDITIONALS AND HUGE SAMPLE
            System.out.println("IS exact");
            int Nexact = 100000;
            importanceSampling.setSampleSize(Nexact);

            long timeStartISexact = System.nanoTime();

            importanceSampling.setSamplingModel(bn);
            importanceSampling.runInference();
            //UnivariateDistribution posteriorISexact = importanceSampling.getPosterior(varInterest);
            double probISexact = importanceSampling.runQuery(varInterest, a, b);

            long timeStopISexact = System.nanoTime();
            double execTimeISexact = (double) (timeStopISexact - timeStartISexact) / 1000000000.0;


            //*****************************************************************
            // HUGIN INFERENCE
            //*****************************************************************

            /*long timeStartHugin = System.nanoTime();


            huginInferenceForBN.setModel(bn);
            huginInferenceForBN.setEvidence(evidence);
            huginInferenceForBN.runInference();

            UnivariateDistribution posteriorHUGIN = huginInferenceForBN.getPosterior(varInterest);

            double[] parameters = posteriorHUGIN.getParameters();
            double huginMean = parameters[0];
            double huginVar = parameters[1];

            NormalDistribution auxNormal2 = new NormalDistribution(huginMean, huginVar);
            double probHUGIN = auxNormal2.probability(a, b);


            long timeStopHugin = System.nanoTime();
            double execTimeHugin = (double) (timeStopHugin - timeStartHugin) / 1000000000.0;*/


            //*****************************************************************
            // EXECUTION TIMES FOR EACH METHOD
            //*****************************************************************
            /*
            System.out.println("Execution time (VMP):      " + Double.toString(execTimeVMP) + " seconds");
            System.out.println("Execution time (IS-VMP):   " + Double.toString(execTimeISVMP) + " seconds");
            System.out.println("Execution time (IS):       " + Double.toString(execTimeIS) + " seconds");
            System.out.println("Execution time (IS-exact): " + Double.toString(execTimeISexact) + " seconds");
            System.out.println("Execution time (HUGIN):    " + Double.toString(execTimeHugin) + " seconds");
            System.out.println();
            */

            timeVMP[k]=execTimeVMP;
            timeISVMP[k]=execTimeISVMP;
            timeIS[k]=execTimeIS;
            timeISexact[k]=execTimeISexact;
            //timeHUGIN[k]=execTimeHugin;


            //*****************************************************************
            // POSTERIOR DISTRIBUTIONS WITH EACH METHOD
            //*****************************************************************
            /*
            System.out.println("Posterior of " + varInterest.getName() + " (VMP):      " + posteriorVMP.toString());
            System.out.println("Posterior of " + varInterest.getName() + " (IS-VMP):   " + posteriorISVMP.toString());
            System.out.println("Posterior of " + varInterest.getName() + " (IS):       " + posteriorIS.toString());
            System.out.println("Posterior of " + varInterest.getName() + " (IS-exact): " + posteriorISexact.toString());
            System.out.println("Posterior of " + varInterest.getName() + " (HUGIN):    " + posteriorHUGIN.toString());
            System.out.println();
            */

            //*****************************************************************
            // QUERY ABOUT PROBABILITY IN (a,b)
            //*****************************************************************
            /*
            System.out.println("Query: P(" + Double.toString(a) + " < " + varInterest.getName() + " < " + Double.toString(b) + ")");
            System.out.println("Probability (VMP):      " + probVMP);
            System.out.println("Probability (IS-VMP):   " + probISVMP);
            System.out.println("Probability (IS):       " + probIS);
            System.out.println("Probability (IS-exact): " + probISexact);
            System.out.println("Probability (HUGIN):    " + probHUGIN);
            */
            probabilitiesVMP[k]=probVMP;
            probabilitiesISVMP[k]=probISVMP;
            probabilitiesIS[k]=probIS;
            probabilitiesISexact[k]=probISexact;
            //probabilitiesHUGIN[k]=probHUGIN;
        }

        System.out.println("Number of variables" + nDiscrete+nContin);
        System.out.println("Sample size:" + N);
        System.out.println();
        System.out.println("Execution Times: (VMP,IS-VMP,IS,IS-EXACT)");
        System.out.println(Arrays.toString(timeVMP));
        System.out.println(Arrays.toString(timeISVMP));
        System.out.println(Arrays.toString(timeIS));
        System.out.println(Arrays.toString(timeISexact));
        //System.out.println(Arrays.toString(timeHUGIN));
        System.out.println();
        System.out.println("Probabilities: (VMP,IS-VMP,IS,IS-EXACT)");
        System.out.println(Arrays.toString(probabilitiesVMP));
        System.out.println(Arrays.toString(probabilitiesISVMP));
        System.out.println(Arrays.toString(probabilitiesIS));
        System.out.println(Arrays.toString(probabilitiesISexact));
        //System.out.println(Arrays.toString(probabilitiesHUGIN));
        System.out.println();

        double meanTimeVMP = Arrays.stream(timeVMP).average().getAsDouble();
        double meanTimeISVMP = Arrays.stream(timeISVMP).average().getAsDouble();
        double meanTimeIS = Arrays.stream(timeIS).average().getAsDouble();
        double meanTimeISexact = Arrays.stream(timeISexact).average().getAsDouble();
        //double meanTimeHUGIN = Arrays.stream(timeHUGIN).average().getAsDouble();

        /*
        double  [] chi2errorVMP = new double[REP];
        double  [] chi2errorISVMP = new double[REP];
        double  [] chi2errorIS = new double[REP];
        double  [] chi2errorHUGIN = new double[REP];


        for(int k=0; k<REP; k++) {
            chi2errorVMP[k] = Math.pow(probabilitiesVMP[k] - probabilitiesISexact[k],2) /probabilitiesISexact[k];
            chi2errorISVMP[k] = Math.pow(probabilitiesISVMP[k] - probabilitiesISexact[k],2) /probabilitiesISexact[k];
            chi2errorIS[k] = Math.pow(probabilitiesIS[k] - probabilitiesISexact[k],2) /probabilitiesISexact[k];
            chi2errorHUGIN[k] = Math.pow(probabilitiesHUGIN[k] - probabilitiesISexact[k],2) /probabilitiesISexact[k];
        }
        */

        double meanErrorVMP = IntStream.range(0, REP).mapToDouble(k-> Math.pow(probabilitiesVMP[k]-probabilitiesISexact[k],2)/probabilitiesISexact[k]).average().getAsDouble();
        double meanErrorISVMP = IntStream.range(0, REP).mapToDouble(k-> Math.pow(probabilitiesISVMP[k]-probabilitiesISexact[k],2)/probabilitiesISexact[k]).average().getAsDouble();
        double meanErrorIS = IntStream.range(0, REP).mapToDouble(k-> Math.pow(probabilitiesIS[k]-probabilitiesISexact[k],2)/probabilitiesISexact[k]).average().getAsDouble();
        //double meanErrorHUGIN = IntStream.range(0, REP).mapToDouble(k-> Math.pow(probabilitiesHUGIN[k]-probabilitiesISexact[k],2)/probabilitiesISexact[k]).average().getAsDouble();

        System.out.println("Mean time: (VMP,IS-VMP,IS,IS-exact)");
        System.out.println(meanTimeVMP);
        System.out.println(meanTimeISVMP);
        System.out.println(meanTimeIS);
        System.out.println(meanTimeISexact);
        //System.out.println(meanTimeHUGIN);
        System.out.println();
        System.out.println("Mean chi2 error: (VMP,IS-VMP,IS), w.r.t. IS-exact");
        System.out.println(meanErrorVMP);
        System.out.println(meanErrorISVMP);
        System.out.println(meanErrorIS);
        //System.out.println(meanErrorHUGIN);
    }
}
