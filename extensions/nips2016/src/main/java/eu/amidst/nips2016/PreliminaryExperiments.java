package eu.amidst.nips2016;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.learning.parametric.bayesian.PopulationVI;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.StochasticVI;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.PrintWriter;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 02/05/16.
 */
public class PreliminaryExperiments {

    public static BayesianNetwork oneNormalVarBN;

    //public static int[] batchSize = {50, 100, 1000};
    //public static int[] memoryPopulationVI = {50, 100, 1000};
    //public static double[] learningRate = {0.01, 0.1, 0.5};
    //public static double[] deltaValue = {-5, 0, 0.1, 5};

    public static int[] batchSize = {100};
    public static int[] memoryPopulationVI = {100};
    public static double[] learningRate = {0.1};
    public static double[] deltaValue = {-5};

    public static int numIter = 100;

    public static SVB svb;
    public static DriftSVB driftSVB;
    public static PopulationVI populationVI;
    public static StochasticVI stochasticVI;
    public static ParallelMaximumLikelihood ml;

    public static PrintWriter writerPredLL;
    public static PrintWriter writerLambda ;
    public static PrintWriter writerMean;
    //public static PrintWriter writerPopulationSize;


    public static void initSVBLearners(int batchSize, double deltaValue) {
        driftSVB = new DriftSVB();
        driftSVB.setWindowsSize(batchSize);
        driftSVB.setSeed(0);
        driftSVB.setDelta(deltaValue);
        VMP vmp = driftSVB.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        driftSVB.setDAG(oneNormalVarBN.getDAG());
        driftSVB.initLearning();

        svb = new SVB();
        svb.setWindowsSize(batchSize);
        svb.setSeed(0);
        vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        svb.setDAG(oneNormalVarBN.getDAG());
        svb.initLearning();

    }

    public static void initVILearners(int batchSize, int memoryPopulationVI, double learningRate){

        populationVI = new PopulationVI();
        populationVI.setWindowSize(memoryPopulationVI);
        populationVI.setSeed(0);
        populationVI.setLearningFactor(learningRate);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        populationVI.setDAG(oneNormalVarBN.getDAG());
        populationVI.initLearning();

        stochasticVI = new StochasticVI();
        stochasticVI.setDataSetSize(numIter*batchSize);
        stochasticVI.setBatchSize(batchSize);
        stochasticVI.setSeed(0);
        stochasticVI.setLearningFactor(learningRate);
        vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        stochasticVI.setDAG(oneNormalVarBN.getDAG());
        stochasticVI.initLearning();

        maximumLikelihoodInit();
    }


    private static void maximumLikelihoodInit(){
        ml = new ParallelMaximumLikelihood();
        ml.setParallelMode(true);
        ml.setDAG(oneNormalVarBN.getDAG());
        ml.initLearning();
    }


    public static void printPredLL(DataOnMemory<DataInstance> batch) throws Exception{
        writerPredLL.println(svb.predictedLogLikelihood(batch)+"\t"+driftSVB.predictedLogLikelihood(batch)+"\t"+
                "\t"+stochasticVI.predictedLogLikelihood(batch)+"\t"+populationVI.predictedLogLikelihood(batch));
    }

    public static void printOutput() throws Exception{

        BayesianNetwork bnML = ml.getLearntBayesianNetwork();
        BayesianNetwork bnSVB = svb.getLearntBayesianNetwork();
        BayesianNetwork bnDriftSVB = driftSVB.getLearntBayesianNetwork();

        /**
         * Normal A variable
         */
        double meanML = ((Normal)bnML.getConditionalDistribution(oneNormalVarBN.getVariables().getVariableByName("A"))).getMean();
        double meanSVB = ((Normal)bnSVB.getConditionalDistribution(oneNormalVarBN.getVariables().getVariableByName("A"))).getMean();
        double meanDriftSVB = ((Normal)bnDriftSVB.getConditionalDistribution(oneNormalVarBN.getVariables().getVariableByName("A"))).getMean();

        writerMean.println(meanML+"\t"+meanSVB+"\t"+meanDriftSVB);

        //writerPopulationSize.println("\t"+"\t"+"\t"+"\t");

        writerLambda.println(driftSVB.getLambdaValue());
    }

    public static void scenarioMixedConceptDrift(int batchSize) throws Exception{

        for (int i = 0; i < numIter; i++) {

            /**
             * GRADUAL CONCEPT DRIFT 1-19
             */
            if (i>0 && i<20) {
                Normal normal = oneNormalVarBN.getConditionalDistribution(oneNormalVarBN.getVariables().getVariableByName("A"));
                normal.setMean(normal.getMean()+5);
                normal.setVariance(normal.getVariance()+0.5);
            }

            /**
             * NO CONCEPT DRIFT 20-30
             */

            /**
             * ABRUPT CONCEPT DRIFT 30-40 (%3)
             */

            if (i>30 && i<40 && i % 3 == 0) {
                oneNormalVarBN.randomInitialization(new Random(i));
                //System.out.println(oneNormalVarBN);
            }

            /**
             * ABRUPT CONCEPT DRIFT 40-70 (%4)
             */
            if (i>40 && i<70 && i % 4 == 0) {
                oneNormalVarBN.randomInitialization(new Random(i));
                //System.out.println(oneNormalVarBN);
            }

            /**
             * NO CONCEPT DRIFT 70-90
             */

            /**
             * GRADUAL CONCEPT DRIFT 90-100
             */
            if (i>90 && i<100) {
                Normal normal = oneNormalVarBN.getConditionalDistribution(oneNormalVarBN.getVariables().getVariableByName("A"));
                normal.setMean(normal.getMean()-5);
                normal.setVariance(normal.getVariance()-1);
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(i);

            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize).toDataOnMemory();


            if(i>0)
                printPredLL(batch);

            /**
             * Update with all different learning techniques
             */
            driftSVB.updateModelWithConceptDrift(batch);
            svb.updateModel(batch);
            populationVI.updateModel(batch);
            stochasticVI.updateModel(batch);

            /* Learn maximum likelihood to get the real means*/
            ml.updateModel(batch);

            /**
             * Outputs: predLL, lambda, mean, population size
             */
            printOutput();

        }
    }

    public static void scenarioNoConceptDrift(int batchSize) throws Exception{

        for (int i = 0; i < numIter; i++) {

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(i);

            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize).toDataOnMemory();


            if(i>0)
                printPredLL(batch);

            /**
             * Update with all different learning techniques
             */
            driftSVB.updateModelWithConceptDrift(batch);
            svb.updateModel(batch);
            populationVI.updateModel(batch);
            stochasticVI.updateModel(batch);

            BayesianNetwork bnSVB = svb.getLearntBayesianNetwork();
            BayesianNetwork bnStochasticVI = stochasticVI.getLearntBayesianNetwork();

            /* Learn maximum likelihood to get the real means*/
            ml.updateModel(batch);

            /**
             * Outputs: predLL, lambda, mean, population size
             */
            printOutput();

        }
    }

    public static void main(String[] args) throws Exception{

        /**
         * Parameters to test: batchSize,
         *                     memory in populationIV,
         *                     delta in initLearning of DriftSVB
         *                     learningRate in StochasticVB & populationIV
         */

        oneNormalVarBN = BayesianNetworkLoader.loadFromFile("./networks/simulated/Normal.bn");

        System.out.println(oneNormalVarBN);

        for (int i = 0; i < batchSize.length; i++) {

            for (int j = 0; j < deltaValue.length; j++) {

                for (int k = 0; k < memoryPopulationVI.length; k++) {

                    for (int l = 0; l < learningRate.length; l++) {

                        /**
                         * Define Learning VI techniques
                         */
                        initSVBLearners(batchSize[i], deltaValue[j]);
                        initVILearners(batchSize[i], memoryPopulationVI[k], learningRate[l]);


                        /**
                         * Output files for predLL, lambda, mean, population size
                         */
                        String path = "extensions/nips2016/doc-Experiments/preliminaryExperiments/";
                        writerPredLL = new PrintWriter(path + "predLL_" + "bs_" + batchSize[i] + "delta_" +
                                deltaValue[j]+ "mem_" + memoryPopulationVI[k] + "lr_" + learningRate[i] +
                                ".txt", "UTF-8");
                        writerLambda = new PrintWriter(path + "lamda_" + "bs_" + batchSize[i] + "delta_" +
                                deltaValue[j]+ "mem_" + memoryPopulationVI[k] + "lr_" + learningRate[i] +
                                ".txt", "UTF-8");
                        writerMean = new PrintWriter(path + "mean_" + "bs_" + batchSize[i] + "delta_" +
                                deltaValue[j]+ "mem_" + memoryPopulationVI[k] + "lr_" + learningRate[i] +
                                ".txt", "UTF-8");
                        //writerPopulationSize = new PrintWriter("populationSize.txt", "UTF-8");


                        /**
                         * Choose concept drift scenario
                         */
                        //scenarioMixedConceptDrift(batchSize[i]);
                        scenarioNoConceptDrift(batchSize[i]);

                        /**
                         * Close all output files
                         */
                        writerPredLL.close();
                        writerLambda.close();
                        writerMean.close();
                        //writerPopulationSize.close();
                    }
                }
            }
        }

        //System.out.println(driftSVB.getLearntBayesianNetwork());

    }
}
