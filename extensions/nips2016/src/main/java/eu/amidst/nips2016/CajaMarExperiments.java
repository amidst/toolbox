package eu.amidst.nips2016;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.bayesian.DriftSVB;
import eu.amidst.core.learning.parametric.bayesian.PopulationVI;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.StochasticVI;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 04/05/16.
 */
public class CajaMarExperiments {

    public static boolean classVarInModel = true;
    public static boolean linkHidden = true;

    public static int[] batchSize = {1000};
    public static int[] memoryPopulationVI = {1000};
    public static double[] learningRate = {0.8};
    public static double[] deltaValue = {100};

    public static SVB svb;
    public static DriftSVB driftSVB;
    public static PopulationVI populationVI;
    public static StochasticVI stochasticVI;
    public static ParallelMaximumLikelihood ml;
    public static ParallelMaximumLikelihood mlPerBatch;

    public static PrintWriter writerPredLL;
    public static PrintWriter writerLambda ;
    public static PrintWriter writerMean;
    public static PrintWriter writerGamma;

    public static DAG dag;

    public static int numIter = 84;

    public static int iter = 0;

    public static int maxIterVI = 100;
    public static double thresholdVI = 0.001;

    public static void initSVBLearners(int batchSize, double deltaValue) {
        driftSVB = new DriftSVB();
        driftSVB.setWindowsSize(batchSize);
        driftSVB.setSeed(0);
        driftSVB.setDelta(deltaValue);
        driftSVB.setOutput(true);
        VMP vmp = driftSVB.getPlateuStructure().getVMP();
        //vmp.setOutput(true);
        vmp.setTestELBO(true);
        vmp.setMaxIter(maxIterVI);
        vmp.setThreshold(thresholdVI);
        driftSVB.setDAG(dag);
        driftSVB.initLearning();

        svb = new SVB();
        svb.setWindowsSize(batchSize);
        svb.setSeed(0);
        svb.setOutput(true);
        vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(true);
        vmp.setTestELBO(true);
        vmp.setMaxIter(maxIterVI);
        vmp.setThreshold(thresholdVI);
        svb.setDAG(dag);
        svb.initLearning();

    }

    public static void initVILearners(int batchSize, int memoryPopulationVI, double learningRate){

        populationVI = new PopulationVI();
        populationVI.setMemorySize(memoryPopulationVI);
        populationVI.setBatchSize(batchSize);
        populationVI.setSeed(0);
        populationVI.setOutput(true);
        populationVI.setLearningFactor(learningRate);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(true);
        vmp.setTestELBO(true);
        vmp.setMaxIter(maxIterVI);
        vmp.setThreshold(thresholdVI);
        populationVI.setDAG(dag);
        populationVI.initLearning();

        stochasticVI = new StochasticVI();
        stochasticVI.setDataSetSize(numIter*batchSize);
        stochasticVI.setBatchSize(batchSize);
        stochasticVI.setSeed(0);
        stochasticVI.setOutput(true);
        stochasticVI.setLearningFactor(learningRate);
        vmp = svb.getPlateuStructure().getVMP();
        //vmp.setOutput(true);
        vmp.setTestELBO(true);
        vmp.setMaxIter(maxIterVI);
        vmp.setThreshold(thresholdVI);
        stochasticVI.setDAG(dag);
        stochasticVI.initLearning();

        maximumLikelihoodInit();
    }


    private static void maximumLikelihoodInit(){
        ml = new ParallelMaximumLikelihood();
        ml.setParallelMode(true);
        ml.setDAG(dag);
        ml.initLearning();

        mlPerBatch = new ParallelMaximumLikelihood();
        mlPerBatch.setParallelMode(true);
        mlPerBatch.setDAG(dag);
        mlPerBatch.initLearning();
    }

    public static void printOutput() throws Exception{

        BayesianNetwork bnML = ml.getLearntBayesianNetwork();
        BayesianNetwork bnSVB = svb.getLearntBayesianNetwork();
        BayesianNetwork bnDriftSVB = driftSVB.getLearntBayesianNetwork();
        BayesianNetwork bnStochasticVI = stochasticVI.getLearntBayesianNetwork();
        BayesianNetwork bnPopulationVI = populationVI.getLearntBayesianNetwork();
        BayesianNetwork bnMLPerBatch = mlPerBatch.getLearntBayesianNetwork();


        double[] meanML=new double[2], meanSVB=new double[2], meanDriftSVB=new double[2], meanStochasticVI=new double[2],
                meanPopulationVI=new double[2], realMean=new double[2];

        if(!classVarInModel) {
            meanML[0] = ((Normal) bnML.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getMean();
            meanSVB[0] = ((Normal) bnSVB.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getMean();
            meanDriftSVB[0] = ((Normal) bnDriftSVB.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getMean();
            meanStochasticVI[0] = ((Normal) bnStochasticVI.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getMean();
            meanPopulationVI[0] = ((Normal) bnPopulationVI.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getMean();
            realMean[0] = ((Normal)bnMLPerBatch.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getMean();
        }else{
            for (int i = 0; i < 2; i++) {
                meanML[i] = ((Normal_MultinomialParents) bnML.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getNormal(i).getMean();
                meanSVB[i] = ((Normal_MultinomialParents) bnSVB.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getNormal(i).getMean();
                meanDriftSVB[i] = ((Normal_MultinomialParents) bnDriftSVB.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getNormal(i).getMean();
                meanStochasticVI[i] = ((Normal_MultinomialParents) bnStochasticVI.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getNormal(i).getMean();
                meanPopulationVI[i] = ((Normal_MultinomialParents) bnPopulationVI.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getNormal(i).getMean();
                realMean[i] = ((Normal_MultinomialParents)bnMLPerBatch.getConditionalDistribution(dag.getVariables().getVariableByName("VAR01"))).getNormal(i).getMean();
            }
        }

        String means = "";
        for (int i = 0; i < 2; i++) {
            if(i!=0)
                means += "\t";
            means += realMean[i]+"\t"+meanML[i]+"\t"+meanSVB[i]+"\t"+meanDriftSVB[i]+"\t"+meanStochasticVI[i]+"\t"+meanPopulationVI[i];
        }


        writerMean.println(means);


        writerLambda.println(driftSVB.getLambdaValue());

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

    public static DAG createDAG(Attributes attributes, int nlocals){
        // Create a Variables object from the attributes of the input data stream.
        Variables variables = new Variables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableByName("DEFAULTING");

        // Define the local hidden variables.
        List<Variable> localHiddenVars = new ArrayList<>();
        for (int i = 0; i < nlocals; i++) {
            localHiddenVars.add(variables.newGaussianVariable("Hidden_"+i));
        }


        // Create an empty DAG object with the defined variables.
        DAG dag = new DAG(variables);

        // Link the class as parent of all attributes
        if(classVarInModel) {
            dag.getParentSets()
                    .stream()
                    .filter(w -> w.getMainVar() != classVar)
                    .filter(w -> !w.getMainVar().getName().startsWith("Hidden"))
                    .forEach(w -> w.addParent(classVar));
        }

        // Link the local hidden as parent of all predictive attributes
        for (Variable localHiddenVar : localHiddenVars) {
            dag.getParentSets()
                    .stream()
                    .filter(w -> w.getMainVar() != classVar)
                    .filter(w -> !w.getMainVar().getName().startsWith("Hidden"))
                    .forEach(w -> w.addParent(localHiddenVar));
        }


        // Connect local hidden variables with each other
        if(linkHidden) {
            for (int i = 0; i < localHiddenVars.size() - 1; i++) {
                for (int j = i + 1; j < localHiddenVars.size(); j++) {
                    dag.getParentSet(localHiddenVars.get(i)).addParent(localHiddenVars.get(j));
                }

            }
        }




        // Show the new dynamic DAG structure
        System.out.println(dag.toString());

        return dag;
    }

    public static void main(String[] args) throws Exception{

        //int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

        String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
        String outputPath = "extensions/nips2016/doc-Experiments/preliminaryExperiments/";

        for (int i = 0; i < batchSize.length; i++) {

            for (int j = 0; j < deltaValue.length; j++) {

                for (int k = 0; k < memoryPopulationVI.length; k++) {

                    for (int l = 0; l < learningRate.length; l++) {


                        for (int m = 0; i < numIter; m++) {

                            int currentMonth = m;

                            //if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                            //    continue;

                            DataStream<DataInstance> dataMonthi = DataStreamLoader.openFromFile(path + currentMonth + ".arff");

                            dag = createDAG(dataMonthi.getAttributes(), 5);

                            /**
                             * Define Learning VI techniques
                             */
                            initSVBLearners(batchSize[i], deltaValue[j]);
                            initVILearners(batchSize[i], memoryPopulationVI[k], learningRate[l]);

                            /**
                             * Update with all different learning techniques
                             */
                            int batchCount = 0;
                            for (DataOnMemory<DataInstance> batch : dataMonthi.iterableOverBatches(batchSize[i])) {

                                System.out.println("--------------------------------- MONTH "+currentMonth+"/"+batchCount+ " --------------------------");
                                driftSVB.updateModelWithConceptDrift(batch);
                                svb.updateModel(batch);
                                populationVI.updateModel(batch);
                                stochasticVI.updateModel(batch);

                                /* Learn maximum likelihood to get the real means*/
                                ml.updateModel(batch);
                                mlPerBatch.initLearning();
                                mlPerBatch.updateModel(batch);

                                if(i>0) {
                                    printPredLL(batch);
                                    printCounts();
                                }
                                batchCount++;
                            }

                            //We print the output
                            /**
                             * Output files for predLL, lambda, mean, population size
                             */
                            writerPredLL = new PrintWriter(outputPath + "CajaMar_predLL_" + "bs" + batchSize[i] + "_delta" +
                                    deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                    ".txt", "UTF-8");
                            writerLambda = new PrintWriter(outputPath + "CajaMar_lamda_" + "_bs" + batchSize[i] + "_delta" +
                                    deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                    ".txt", "UTF-8");
                            writerMean = new PrintWriter(outputPath + "CajaMar_mean_" + "bs" + batchSize[i] + "_delta" +
                                    deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                    ".txt", "UTF-8");
                            writerGamma = new PrintWriter(outputPath + "CajaMar_gamma_" + "bs" + batchSize[i] + "_delta" +
                                    deltaValue[j]+ "_mem" + memoryPopulationVI[k] + "_lr" + learningRate[l] +
                                    ".txt", "UTF-8");

                            /**
                             * Outputs: lambda, mean, population size
                             */
                            printOutput();

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
        }
    }
}
