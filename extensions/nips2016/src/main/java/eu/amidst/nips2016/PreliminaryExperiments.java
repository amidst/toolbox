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

    public static BayesianNetwork network;

    //public static int[] batchSize = {50, 100, 1000};
    //public static int[] memoryPopulationVI = {50, 100, 1000};
    //public static double[] learningRate = {0.01, 0.1, 0.5};
    //public static double[] deltaValue = {-5, 0, 0.1, 5};

    public static int[] batchSize = {100};
    public static int[] memoryPopulationVI = {1000};
    public static double[] learningRate = {0.51,0.99};
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
    public static PrintWriter writerGamma;

    public static double[] predLLAcum = new double[4];
    public static int iter = 0;



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
        driftSVB.setDAG(network.getDAG());
        driftSVB.initLearning();

        svb = new SVB();
        svb.setWindowsSize(batchSize);
        svb.setSeed(0);
        vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        svb.setDAG(network.getDAG());
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
        populationVI.setDAG(network.getDAG());
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
        stochasticVI.setDAG(network.getDAG());
        stochasticVI.initLearning();

        maximumLikelihoodInit();
    }


    private static void maximumLikelihoodInit(){
        ml = new ParallelMaximumLikelihood();
        ml.setParallelMode(true);
        ml.setDAG(network.getDAG());
        ml.initLearning();
    }


    public static void counts(DataOnMemory<DataInstance> batch) throws Exception{

        predLLAcum[0] = svb.getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        predLLAcum[1] = driftSVB.getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        predLLAcum[2] = stochasticVI.getSVB().getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        predLLAcum[3] = populationVI.getSVB().getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        writerGamma.println(predLLAcum[0]+"\t"+predLLAcum[1]+"\t"+predLLAcum[2]+"\t"+predLLAcum[3]);



    }

    public static void printPredLL(DataOnMemory<DataInstance> batch) throws Exception{


        predLLAcum[0] += svb.predictedLogLikelihood(batch);
        predLLAcum[1] += driftSVB.predictedLogLikelihood(batch);
        predLLAcum[2] += stochasticVI.predictedLogLikelihood(batch);
        predLLAcum[3] += populationVI.predictedLogLikelihood(batch);
        writerPredLL.println(predLLAcum[0]/iter+"\t"+predLLAcum[1]/iter+"\t"+predLLAcum[2]/iter+"\t"+predLLAcum[3]/iter);


    }

    public static void printOutput() throws Exception{

        BayesianNetwork bnML = ml.getLearntBayesianNetwork();
        BayesianNetwork bnSVB = svb.getLearntBayesianNetwork();
        BayesianNetwork bnDriftSVB = driftSVB.getLearntBayesianNetwork();
        BayesianNetwork bnStochasticVI = stochasticVI.getLearntBayesianNetwork();
        BayesianNetwork bnPopulationVI = populationVI.getLearntBayesianNetwork();

        /**
         * Normal A variable
         */
        double meanML = ((Normal)bnML.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
        double meanSVB = ((Normal)bnSVB.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
        double meanDriftSVB = ((Normal)bnDriftSVB.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
        double meanStochasticVI = ((Normal)bnStochasticVI.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
        double meanPopulationVI = ((Normal)bnPopulationVI.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();

        double realMean = ((Normal)network.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
        writerMean.println(realMean+"\t"+meanML+"\t"+meanSVB+"\t"+meanDriftSVB+"\t"+meanStochasticVI+"\t"+meanPopulationVI);

        //writerPopulationSize.println("\t"+"\t"+"\t"+"\t");

        writerLambda.println(driftSVB.getLambdaValue());
    }

    public static void scenarioMixedConceptDrift(int batchSize) throws Exception{

        double predLL = 0;
        for (int i = 0; i < numIter; i++) {

            iter++;

            /**
             * NO CONCEPT DRIFT 1-19
             */

            /**
             * GRADUAL CONCEPT DRIFT 20-30
             */
            if (i>20 && i<30) {
                Normal normal = network.getConditionalDistribution(network.getVariables().getVariableByName("A"));
                normal.setMean(normal.getMean()+new Random(i).nextDouble());
                normal.setVariance(normal.getVariance()+new Random(i).nextDouble());
            }

            /**
             * ABRUPT CONCEPT DRIFT 30-40 (%3)
             */

            if (i>30 && i<40 && i % 3 == 0) {
                network.randomInitialization(new Random(i));
                //System.out.println(network);
                Normal normal = network.getConditionalDistribution(network.getVariables().getVariableByName("A"));
                normal.setMean(normal.getMean()*new Random(i).nextDouble());
                normal.setVariance(normal.getVariance()*new Random(i).nextDouble());

            }

            /**
             * ABRUPT CONCEPT DRIFT 40-70 (%4)
             */
            if (i>40 && i<70 && i % 4 == 0) {
                network.randomInitialization(new Random(i));
                //System.out.println(network);
            }

            /**
             * NO CONCEPT DRIFT 70-90
             */

            /**
             * GRADUAL CONCEPT DRIFT 90-100
             */
            if (i>90 && i<100) {
                Normal normal = network.getConditionalDistribution(network.getVariables().getVariableByName("A"));
                normal.setMean(normal.getMean()+5);
                normal.setVariance(normal.getVariance()+0.1);
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
            sampler.setSeed(i);

            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize).toDataOnMemory();


            if(i>0) {
                //counts(batch);
                printPredLL(batch);

            }

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
             * Outputs: lambda, mean, population size
             */
            printOutput();

        }
    }

    public static void scenarioNoConceptDrift(int batchSize) throws Exception{

        for (int i = 0; i < numIter; i++) {

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
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

        network = BayesianNetworkLoader.loadFromFile("./networks/simulated/Normal.bn");

        System.out.println(network);



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
                        writerPredLL = new PrintWriter(path + "predLL_" + "bs" + batchSize[i] + "_delta" +
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[i] +
                                ".txt", "UTF-8");
                        writerLambda = new PrintWriter(path + "lamda_" + "_bs" + batchSize[i] + "_delta" +
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[i] +
                                ".txt", "UTF-8");
                        writerMean = new PrintWriter(path + "mean_" + "bs" + batchSize[i] + "_delta" +
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[i] +
                                ".txt", "UTF-8");
                        writerGamma = new PrintWriter(path + "gamma_" + "bs" + batchSize[i] + "_delta" +
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[i] +
                                ".txt", "UTF-8");


                        /**
                         * Choose concept drift scenario
                         */
                        scenarioMixedConceptDrift(batchSize[i]);
                        //scenarioNoConceptDrift(batchSize[i]);

                        /**
                         * Close all output files
                         */
                        writerPredLL.close();
                        writerLambda.close();
                        writerMean.close();
                        writerGamma.close();
                    }
                }
            }
        }

        //System.out.println(driftSVB.getLearntBayesianNetwork());

    }
}
