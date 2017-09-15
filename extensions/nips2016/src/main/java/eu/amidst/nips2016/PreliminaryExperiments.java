package eu.amidst.nips2016;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.learning.parametric.bayesian.PopulationVI;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.StochasticVI;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.PrintWriter;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 02/05/16.
 */
public class PreliminaryExperiments {

    public static boolean mixture = true;

    public static BayesianNetwork network;

    public static int[] batchSize = {1000};
    public static int[] memoryPopulationVI = {1000};
    public static double[] learningRate = {0.8};
    public static double[] deltaValue = {100};

    public static int numIter = 150;

    public static SVB svb;
    public static DriftSVB driftSVB;
    public static PopulationVI populationVI;
    public static StochasticVI stochasticVI;
    public static ParallelMaximumLikelihood ml;

    public static PrintWriter writerPredLL;
    public static PrintWriter writerLambda ;
    public static PrintWriter writerMean;
    public static PrintWriter writerGamma;

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
        populationVI.setMemorySize(memoryPopulationVI);
        populationVI.setBatchSize(batchSize);
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


    public static void printCounts() throws Exception{

        double[] outputs = new double[4];
        outputs[0] = svb.getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        outputs[1] = driftSVB.getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        outputs[2] = stochasticVI.getSVB().getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        outputs[3] = populationVI.getSVB().getPlateuStructure().getNonReplictedNodes().findFirst().get().getQDist().getNaturalParameters().get(0);
        writerGamma.println(outputs[0]+"\t"+outputs[1]+"\t"+outputs[2]+"\t"+outputs[3]);



    }

    public static void printPredLL(DataOnMemory<DataInstance> batch) throws Exception{

        double[] outputs = new double[4];
        outputs[0] += svb.predictedLogLikelihood(batch);
        outputs[1] += driftSVB.predictedLogLikelihood(batch);
        outputs[2] += stochasticVI.predictedLogLikelihood(batch);
        outputs[3] += populationVI.predictedLogLikelihood(batch);
        writerPredLL.println(outputs[0]/iter+"\t"+outputs[1]/iter+"\t"+outputs[2]/iter+"\t"+outputs[3]/iter);
    }

    public static void printOutput() throws Exception{

        BayesianNetwork bnML = ml.getLearntBayesianNetwork();
        BayesianNetwork bnSVB = svb.getLearntBayesianNetwork();
        BayesianNetwork bnDriftSVB = driftSVB.getLearntBayesianNetwork();
        BayesianNetwork bnStochasticVI = stochasticVI.getLearntBayesianNetwork();
        BayesianNetwork bnPopulationVI = populationVI.getLearntBayesianNetwork();

        double[] meanML=new double[2], meanSVB=new double[2], meanDriftSVB=new double[2], meanStochasticVI=new double[2],
                meanPopulationVI=new double[2], realMean=new double[2];
        /**
         * Normal A variable
         */
        if(!mixture) {
            meanML[0] = ((Normal) bnML.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
            meanSVB[0] = ((Normal) bnSVB.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
            meanDriftSVB[0] = ((Normal) bnDriftSVB.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
            meanStochasticVI[0] = ((Normal) bnStochasticVI.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
            meanPopulationVI[0] = ((Normal) bnPopulationVI.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
            realMean[0] = ((Normal)network.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getMean();
        }else{
            for (int i = 0; i < 2; i++) {
                meanML[i] = ((Normal_MultinomialParents) bnML.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(i).getMean();
                meanSVB[i] = ((Normal_MultinomialParents) bnSVB.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(i).getMean();
                meanDriftSVB[i] = ((Normal_MultinomialParents) bnDriftSVB.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(i).getMean();
                meanStochasticVI[i] = ((Normal_MultinomialParents) bnStochasticVI.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(i).getMean();
                meanPopulationVI[i] = ((Normal_MultinomialParents) bnPopulationVI.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(i).getMean();
                realMean[i] = ((Normal_MultinomialParents)network.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(i).getMean();
            }
        }

        String means = "";
        for (int i = 0; i < 2; i++) {
            if(i!=0)
                means += "\t";
            means += realMean[i]+"\t"+meanML[i]+"\t"+meanSVB[i]+"\t"+meanDriftSVB[i]+"\t"+meanStochasticVI[i]+"\t"+meanPopulationVI[i];
        }


        writerMean.println(means);

        writerLambda.println(driftSVB.getLambdaMomentParameter());
    }

    public static void introduceAbruptConceptDrift(){

        //Single Gaussian, without parents
        if(!mixture){
            Normal normal = network.getConditionalDistribution(network.getVariables().getVariableByName("A"));
            normal.setMean(normal.getMean()*new Random(iter).nextDouble());
            normal.setVariance(normal.getVariance()*new Random(iter).nextDouble());
        }else{
            Normal normal0 = ((Normal_MultinomialParents)network.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(0);
            normal0.setMean(normal0.getMean()*new Random(iter).nextDouble());
            normal0.setVariance(normal0.getVariance()*new Random(iter).nextDouble());
        }
    }

    public static void introduceSmoothConceptDrift(){

        //Single Gaussian, without parents
        if(!mixture){
            Normal normal = network.getConditionalDistribution(network.getVariables().getVariableByName("A"));
            //normal.setMean(normal.getMean()+new Random(iter).nextDouble());
            //normal.setVariance(normal.getVariance()+new Random(iter).nextDouble());
            normal.setMean(normal.getMean()+0.2);
            normal.setVariance(normal.getVariance()+0.2);
        }else{
            Normal normal0 = ((Normal_MultinomialParents)network.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(0);
            //Normal normal1 = ((Normal_MultinomialParents)network.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(1);
            //normal.setMean(normal.getMean()+new Random(iter).nextDouble());
            //normal.setVariance(normal.getVariance()+new Random(iter).nextDouble());
            normal0.setMean(normal0.getMean()+0.2);
            normal0.setVariance(normal0.getVariance()+0.2);
        }
    }

    public static void randomInitialization(){
        if(!mixture){
            network.randomInitialization(new Random(iter));
        }else{
            Normal normal0 = ((Normal_MultinomialParents)network.getConditionalDistribution(network.getVariables().getVariableByName("A"))).getNormal(0);
            normal0.randomInitialization(new Random(iter));
        }
    }

    /**
     * scenario 1 for concept drift: includes some random concept drift
     * @param batchSize
     * @throws Exception
     */
    public static void scenario1MixedConceptDrift(int batchSize) throws Exception{

        for (int i = 0; i < numIter; i++) {

            /**
             * NO CONCEPT DRIFT 1-19
             */

            /**
             * GRADUAL CONCEPT DRIFT 20-30
             */
            if (i>20 && i<30) {
                introduceSmoothConceptDrift();
            }

            /**
             * ABRUPT CONCEPT DRIFT 30-40 (%3)
             */

            if (i>30 && i<40 && i % 3 == 0) {
                introduceAbruptConceptDrift();
            }

            /**
             * RANDOM "ABRUPT" CONCEPT DRIFT 40-70 (%4)
             */
            if (i>40 && i<70 && i % 4 == 0) {
                //introduceAbruptConceptDrift();
                randomInitialization();
            }

            /**
             * NO CONCEPT DRIFT 70-90
             */

            /**
             * GRADUAL CONCEPT DRIFT 90-100
             */
            if (i>90 && i<numIter) {
                introduceSmoothConceptDrift();
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
            sampler.setSeed(i);

            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize).toDataOnMemory();


            if(i>0) {
                printCounts();
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

            iter++;

        }
    }

    /**
     * scenario 2 for concept drift: no random concept drift
     * @param batchSize
     * @throws Exception
     */

    public static void scenario2MixedConceptDrift(int batchSize) throws Exception{

        for (int i = 0; i < numIter; i++) {

            /**
             * NO CONCEPT DRIFT 1-19
             */

            /**
             * GRADUAL CONCEPT DRIFT 20-30
             */
            if (i>20 && i<30) {
                introduceSmoothConceptDrift();
            }

            /**
             * ABRUPT CONCEPT DRIFT 30-40 (%3)
             */

            if (i>30 && i<40 && i % 3 == 0) {
                introduceAbruptConceptDrift();
            }

            /**
             * ABRUPT CONCEPT DRIFT 40-70 (%4)
             */
            if (i>40 && i<70 && i % 4 == 0) {
                introduceAbruptConceptDrift();
            }

            /**
             * NO CONCEPT DRIFT 70-90
             */

            /**
             * GRADUAL CONCEPT DRIFT 90-100
             */
            if (i>90 && i<numIter) {
                introduceSmoothConceptDrift();
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
            sampler.setSeed(i);

            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize).toDataOnMemory();


            if(i>0) {
                printCounts();
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

            iter++;

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

            /* Learn maximum likelihood to get the real means*/
            ml.updateModel(batch);

            /**
             * Outputs: predLL, lambda, mean, population size
             */
            printOutput();

        }
    }

    public static BayesianNetwork createNetwork() throws Exception{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newMultinomialVariable("B",2);
        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varB);
        BayesianNetwork bayesianNetwork = new BayesianNetwork(dag);

        ((Multinomial)bayesianNetwork.getConditionalDistribution(varB)).setProbabilities(new double[]{0.4,0.6});

        Normal normal0 = ((Normal_MultinomialParents)bayesianNetwork.getConditionalDistribution(varA)).getNormal(0);
        Normal normal1 = ((Normal_MultinomialParents)bayesianNetwork.getConditionalDistribution(varA)).getNormal(1);

        normal0.setMean(-2.5);
        normal0.setVariance(0.75);

        normal1.setMean(2.5);
        normal1.setVariance(0.5);

        if(mixture)
            return bayesianNetwork;
        else
        return BayesianNetworkLoader.loadFromFile("./networks/simulated/Normal.bn");
    }


    public static void main(String[] args) throws Exception{

        /**
         * Parameters to test: batchSize,
         *                     memory in populationIV,
         *                     delta in initLearning of DriftSVB
         *                     learningRate in StochasticVB & populationIV
         */

        network = createNetwork();

        System.out.println(network);


        for (int i = 0; i < batchSize.length; i++) {

            for (int j = 0; j < deltaValue.length; j++) {

                for (int k = 0; k < memoryPopulationVI.length; k++) {

                    for (int l = 0; l < learningRate.length; l++) {


                        network = createNetwork();

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
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                ".txt", "UTF-8");
                        writerLambda = new PrintWriter(path + "lamda_" + "_bs" + batchSize[i] + "_delta" +
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                ".txt", "UTF-8");
                        writerMean = new PrintWriter(path + "mean_" + "bs" + batchSize[i] + "_delta" +
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                ".txt", "UTF-8");
                        writerGamma = new PrintWriter(path + "gamma_" + "bs" + batchSize[i] + "_delta" +
                                deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                ".txt", "UTF-8");


                        /**
                         * Choose concept drift scenario
                         */
                        //scenarioNoConceptDrift(batchSize[i]);
                        //scenario1MixedConceptDrift(batchSize[i]);
                        scenario2MixedConceptDrift(batchSize[i]);

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
