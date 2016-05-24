package eu.amidst.icdm2016;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 23/05/16.
 */
public class TestGradientBoostingHypothesis {

    private static NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;
    private static ParallelMaximumLikelihood parallelMaximumLikelihood;

    static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
    static String outputPath="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWekaWithoutResiduals/dataWekaWithoutResiduals";

    private static void printOutput(double [] meanHiddenVars, int currentMonth){
        for (int j = 0; j < meanHiddenVars.length; j++) {
            System.out.print(currentMonth + "\t" + meanHiddenVars[j] + "\t");
            meanHiddenVars[j] = 0;
        }
        System.out.println();
    }

    public static void initML(Attributes atts){
        parallelMaximumLikelihood.setParallelMode(true);
        Variables vars = new Variables(atts);
        DAG dag = new DAG(vars);
        Variable classVar = vars.getVariableByName("DEFAULTING");
        for (Variable var : vars) {
            if(var != classVar){
                dag.getParentSet(var).addParent(classVar);
            }
        }

        parallelMaximumLikelihood.setDAG(dag);
        parallelMaximumLikelihood.initLearning();

    }

    public static void main(String[] args) throws IOException{
        int NSETS = 4;
        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};
        int windowSize = 10000;
        double[] meanHiddenVars;


        for (int numIter = 0; numIter < 4; numIter++) {

            DataStream<DataInstance> dataMonth = DataStreamLoader.openFromFile(path+"0.arff");

            List<Attribute> attsSubSetList = new ArrayList<>();
            attsSubSetList.add(dataMonth.getAttributes().getAttributeByName("DEFAULTING"));
            attsSubSetList.add(dataMonth.getAttributes().getAttributeByName("VAR01"));
            Attributes attsSubset = new Attributes(attsSubSetList);

            parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            initML(attsSubset);

            //We create a eu.amidst.eu.amidst.icdm2016.NaiveBayesVirtualConceptDriftDetector object
            virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();

            //We set class variable as the last attribute
            virtualDriftDetector.setClassIndex(-1);

            virtualDriftDetector.setSeed(1);

            //We set the data which is going to be used
            virtualDriftDetector.setAttributes(dataMonth.getAttributes());

            //We fix the size of the window
            virtualDriftDetector.setWindowsSize(windowSize);

            //We fix the number of global latent variables
            virtualDriftDetector.setNumberOfGlobalVars(1);

            //We should invoke this method before processing any data
            virtualDriftDetector.initLearning();


            //New dataset in which we remove the residuals
            DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<>(attsSubset);

            for (int i = 0; i < NSETS; i++) {

                int currentMonth = i;

                if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                    continue;

                dataMonth = DataStreamLoader.openFromFile(path + currentMonth + ".arff");

                virtualDriftDetector.setTransitionVariance(0);

                meanHiddenVars = virtualDriftDetector.updateModel(dataMonth);

                for (DataOnMemory<DataInstance> batch : dataMonth.iterableOverBatches(100)){
                    parallelMaximumLikelihood.updateModel(batch);
                }

                virtualDriftDetector.setTransitionVariance(0.1);
                virtualDriftDetector.getSvb().applyTransition();

                //We print the output
                BayesianNetwork learntBN_ML = parallelMaximumLikelihood.getLearntBayesianNetwork();
                System.out.println("-------- MAXIMUM LIKELIHOOD --------");
                System.out.println(learntBN_ML);
                BayesianNetwork learntBN = virtualDriftDetector.getLearntBayesianNetwork();
                System.out.println("-------- VIRTUAL CONCEPT DRIFT DETECTOR --------");
                System.out.println(learntBN);
                printOutput(meanHiddenVars, currentMonth);

                System.out.println("Learnt Mean for VAR01 = ");


                //Remove residuals
                Variables vars = virtualDriftDetector.getLearntBayesianNetwork().getDAG().getVariables();

                Variable classVar = vars.getVariableByName("DEFAULTING");
                Variable globalHidden = vars.getVariableByName("GlobalHidden_0");
                dataMonth.restart();
                dataMonth = dataMonth.map(instance -> {
                    vars.getListOfVariables().stream()
                            .filter(var -> !var.equals(classVar))
                            .filter(var -> !var.equals(globalHidden))
                            .forEach(var -> {
                                int classVal = (int) instance.getValue(classVar);

                                Normal_MultinomialNormalParents dist = learntBN.getConditionalDistribution(var);
                                double b0 = dist.getNormal_NormalParentsDistribution(classVal).getIntercept();
                                double b1 = dist.getNormal_NormalParentsDistribution(classVal).getCoeffForParent(globalHidden);
                                double globalHiddenMean = ((Normal) learntBN.getConditionalDistribution(globalHidden)).getMean();

                                if (instance.getValue(var) != 0)
                                    instance.setValue(var, instance.getValue(var) - b0 - b1 * globalHiddenMean);
                            });
                    return instance;
                });

                dataMonth.restart();

                for (DataInstance dataInstance : dataMonth) {
                    newData.add(dataInstance);
                }

                //Print new dataset
                DataStreamWriter.writeDataToFile(newData, outputPath + currentMonth + ".arff");

                newData = new DataOnMemoryListContainer<>(attsSubset);
            }
            path = outputPath;
        }
    }
}
