package eu.amidst.impSampling2017;

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.ImportanceSamplingCLG_new;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.inference.DynamicMAPInference;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.ecai2016.BankSimulatedDynamicModel3;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * Created by dario on 19/1/17.
 */
public class DistributedISPrecisionBankModel {

    public static void main(String[] args) throws Exception {

        int nTimeSteps = 5;
        int nTimesEvidence = nTimeSteps-1;
        int sampleSizeIS = 1000;
        int seed = 0;

        if (args.length==4) {
            nTimeSteps = Integer.parseInt(args[0]);
            nTimesEvidence = Integer.parseInt(args[1]);
            seed = Integer.parseInt(args[2]);
            sampleSizeIS = (int)Double.parseDouble(args[3]);
        }

        String sampleOutputFile = "/Users/dario/Desktop/salidaGaussian.csv";
//        String sampleOutputFileGM = "/Users/dario/Desktop/salidaGaussianMixture.csv";


        File outputFile = new File(sampleOutputFile);
        outputFile.delete();

//        outputFile = new File(sampleOutputFileGM);
//        outputFile.delete();


//        int nSamplesForLikelihood;





        ExpandedBankModel model = new ExpandedBankModel();

        model.setnTimeSteps(nTimeSteps);
        model.setnTimeStepsEvidence(nTimesEvidence);

        model.setSeed(seed);

        model.buildModel();
        //System.out.println(model.getModel());

        BayesianNetwork bn = model.getModel();

        Assignment evidence = model.generateEvidence();
        //System.out.println(evidence.outputString(bn.getVariables().getListOfVariables()));

        //System.out.println("EVIDENCE: ");
        //System.out.println(hmm_evidence.outputString(model.getVarsEvidence()));


//        nSamplesForLikelihood = 100000;
//
//
//        double likelihood_Gaussian;
//        double likelihood_GaussianMixture;



        /**********************************************
        *    INITIALIZATION
        *********************************************/

        /*
         *  MODEL
         */
        System.out.println("BN with " + nTimeSteps + " time steps");
        //System.out.println(bn);


        /*
         *  CHOOSE VARIABLES OF INTEREST FOR HMM: LAST TIME STEP CONTINUOUS VARS
         */


        List<Variable> varsOfInterest = new ArrayList<>();
        varsOfInterest.add(model.getVarsInterest().get(0));

        System.out.println("VARIABLE OF INTEREST: ");
        varsOfInterest.stream().forEach(var -> System.out.println(var.getName()));


        /*
         *  VARS IN THE EVIDENCE IN THE HMM MODEL (CONTINUOUS VARS EXCLUDING THE LAST TIME STEP)
         */

        //Assignment evidence = model.generateEvidence();
        List<Variable> varsEvidence = model.getVarsEvidence();

        System.out.println("EVIDENCE: ");
        //evidence = new HashMapAssignment();
        System.out.println(evidence.outputString(varsEvidence));



        /**********************************************************************************
         *    EXPERIMENT 1: COMPARING LIKELIHOOD OF POSTERIOR DISTRIBUTIONS
         *********************************************************************************/


        Variable varOfInterest = varsOfInterest.get(0);
        int currentSampleSize = sampleSizeIS;


        /*
         *  OBTAINING POSTERIORS WITH DISTRIBUTED IMPORTANCE SAMPLING
         */
        ImportanceSamplingCLG_new impSamplingLocal = new ImportanceSamplingCLG_new();

        impSamplingLocal.setSeed(seed);
        impSamplingLocal.setModel(bn);
        impSamplingLocal.setSampleSize(currentSampleSize);
        impSamplingLocal.setVariablesOfInterest(varsOfInterest);
        impSamplingLocal.setParallelMode(true);
        impSamplingLocal.setSaveSampleToFile(true, sampleOutputFile);
        impSamplingLocal.setEvidence(evidence);


        // OBTAIN THE POSTERIOR AS A SINGLE GAUSSIAN
        impSamplingLocal.setGaussianMixturePosteriors(false);
        impSamplingLocal.runInference();
        Normal varOfInterestGaussianDistribution = impSamplingLocal.getPosterior(varOfInterest);




        /*
         *  OBTAINING POSTERIORS WITH VMP
         */
        VMP vmp = new VMP();
        vmp.setModel(bn);
        vmp.setEvidence(evidence);
        vmp.runInference();
        Normal VMP_posterior = vmp.getPosterior(varOfInterest);

        String VMP_posterior_output = "/Users/dario/Desktop/VMP_posterior.R";
        FileWriter fileWriter = new FileWriter(VMP_posterior_output, false);
        String line = "VMP_Posterior_mean <- " + VMP_posterior.getMean() + " \nVMP_Posterior_var <- " + VMP_posterior.getVariance() + " \n";
        fileWriter.write(line);
        fileWriter.close();





        /*
         *  SAVE A LARGE SAMPLE FOR ESTIMATING THE LL
         */
        String sampleLL_OutputFile = "/Users/dario/Desktop/salidaLL.csv";

        File outputFile2 = new File(sampleLL_OutputFile);
        outputFile2.delete();


        ImportanceSamplingCLG_new impSamplingLL = new ImportanceSamplingCLG_new();

        impSamplingLL.setSeed(10);
        impSamplingLL.setModel(bn);
        impSamplingLL.setSampleSize(1000000);
        impSamplingLL.setVariablesOfInterest(varsOfInterest);
        impSamplingLL.setParallelMode(true);
        impSamplingLL.setSaveSampleToFile(true, sampleLL_OutputFile);
        impSamplingLL.setEvidence(evidence);


        // OBTAIN THE POSTERIOR AS A SINGLE GAUSSIAN
        impSamplingLL.setGaussianMixturePosteriors(false);
        impSamplingLL.runInference();







        // OBTAIN THE POSTERIOR AS A GAUSSIAN MIXTURE AND THE QUERY RESULT
//        impSamplingLocal.setSeed(0);
//        impSamplingLocal.setGaussianMixturePosteriors(true);
//        impSamplingLocal.setMixtureOfGaussiansInitialVariance(50);
//        impSamplingLocal.setMixtureOfGaussiansNoveltyRate(0.005);
//        impSamplingLocal.setSaveSampleToFile(true, sampleOutputFileGM);
//        impSamplingLocal.runInference();
//        GaussianMixture varOfInterestGaussianMixtureDistribution = impSamplingLocal.getPosterior(varOfInterest);
//
//
//        impSamplingLocal.setSaveSampleToFile(false, "");
//
//
//        Function<Double,Double> posteriorGaussianDensity = (Function<Double,Double> & Serializable) ( x -> -varOfInterestGaussianDistribution.getLogProbability(x));
//        Function<Double,Double> posteriorGaussianMixtureDensity = (Function<Double,Double> & Serializable) ( x -> -varOfInterestGaussianMixtureDistribution.getLogProbability(x));


        /**********************************************************************************
         *    EXPERIMENT 1: COMPARING LIKELIHOOD OF POSTERIOR DISTRIBUTIONS
         *********************************************************************************/

        /*
         *  ESTIMATE LIKELIHOOD OF EACH POSTERIOR
         */
//        impSamplingLocal.setSampleSize(nSamplesForLikelihood);
//        impSamplingLocal.setGaussianMixturePosteriors(false);
//
//        impSamplingLocal.setQuery(varOfInterest, posteriorGaussianDensity);
//        impSamplingLocal.runInference();
//        likelihood_Gaussian = impSamplingLocal.getQueryResult();
//
//        impSamplingLocal.setQuery(varOfInterest, posteriorGaussianMixtureDensity);
//        impSamplingLocal.runInference();
//        likelihood_GaussianMixture = impSamplingLocal.getQueryResult();
//
//
//
//        System.out.println("\n\nVar: " + varOfInterest.getName() + ", conditional=" + bn.getConditionalDistribution(varOfInterest));
//
//        System.out.println("SAMPLE SIZE: " + sampleSizeIS);
//
//        varOfInterestGaussianMixtureDistribution.sortByWeight();
//        System.out.println("Gaussian posterior=        " + varOfInterestGaussianDistribution.toString());
//        System.out.println("GaussianMixture posterior= " + varOfInterestGaussianMixtureDistribution.toString());
//
//        System.out.println(varOfInterestGaussianDistribution.toStringRCode());
//        System.out.println(varOfInterestGaussianMixtureDistribution.toStringRCode());
//
//        System.out.println("Gaussian likelihood=         " + likelihood_Gaussian);
//        System.out.println("GaussianMixture likelihood=  " + likelihood_GaussianMixture);

    }




    private static class ExpandedBankModel {

        private int nTimeSteps = 10;
        private int nTimeStepsEvidence = 8;

        private BayesianNetwork bn;

        private Random random;
        private List<Variable> varsEvidence;
        private List<Variable> varsInterest;


        public void setnTimeSteps(int nTimeSteps) {
            this.nTimeSteps = nTimeSteps;
        }

        public void setnTimeStepsEvidence(int nTimeStepsEvidence) {
            this.nTimeStepsEvidence = nTimeStepsEvidence;
        }

        public int getnTimeSteps() {
            return nTimeSteps;
        }

        public List<Variable> getVarsEvidence() {
            return varsEvidence;
        }

        public List<Variable> getVarsInterest() {
            return varsInterest;
        }

        public BayesianNetwork getModel() {
            return bn;
        }

        public ExpandedBankModel() {
            random = new Random(0);
        }

        public void setSeed(long seed) {
            random.setSeed(seed);
        }

        public void buildModel() {

            BankSimulatedDynamicModel3 model = new BankSimulatedDynamicModel3();

            model.generateModel();
            //model.printDAG();

            model.setSeed(random.nextInt());
            model.generateEvidence(nTimeSteps);

            DynamicBayesianNetwork DBNmodel = model.getModel();
            DynamicMAPInference dynMAP = new DynamicMAPInference();

            List<DynamicAssignment> evidence = model.getEvidenceNoClass();

            Variable MAPVariable = model.getClassVariable();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setEvidence(evidence);

            BayesianNetwork staticModel = dynMAP.getUnfoldedStaticModel();
            Assignment staticEvidence = dynMAP.getUnfoldedEvidence();

            //System.out.println(staticModel.toString());
            //System.out.println(staticEvidence.outputString(staticModel.getVariables().getListOfVariables()));

            this.bn = staticModel;

            List<Variable> varsContinuous = this.bn.getVariables().getListOfVariables()
                    .stream()
                    .filter(var -> var.isNormal())
                    .collect(Collectors.toList());

            varsEvidence = varsContinuous.stream().filter(variable -> variable.getName().contains("Local")).collect(Collectors.toList());

            List<Variable> varsEvidence2 = new ArrayList<>();
            for (int i = 0; i < varsEvidence.size(); i++) {
                Variable variable = varsEvidence.get(i);

                StringTokenizer stringTokenizer = new StringTokenizer(variable.getName(), "_");
                stringTokenizer.nextToken();
                String timeStep = stringTokenizer.nextToken();
                timeStep = timeStep.substring(1);
                int timeStep1 = Integer.parseInt(timeStep);
                if (timeStep1 < nTimeStepsEvidence) {
                    varsEvidence2.add(variable);
                }
            }
            varsEvidence = varsEvidence2;

            varsInterest = varsContinuous.stream().filter(variable -> variable.getName().equals("OveralIncomes_t" + Integer.toString(nTimeSteps-1))).collect(Collectors.toList());
        }

        public Assignment generateEvidence() {

            BankSimulatedDynamicModel3 model = new BankSimulatedDynamicModel3();

            model.generateModel();
            //model.printDAG();

            model.setSeed(random.nextInt());
            model.generateEvidence(nTimeSteps);

            List<DynamicAssignment> evidence = model.getEvidenceNoClass();

            DynamicBayesianNetwork DBNmodel = model.getModel();
            DynamicMAPInference dynMAP = new DynamicMAPInference();

            Variable MAPVariable = model.getClassVariable();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setEvidence(evidence);

            Assignment staticEvidence = dynMAP.getUnfoldedEvidence();
            this.bn.getVariables().getListOfVariables().stream().filter(variable -> !varsEvidence.contains(variable)).forEach(variable -> staticEvidence.setValue(variable,Double.NaN));

            return staticEvidence;
        }
    }
}


