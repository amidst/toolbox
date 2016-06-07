package eu.amidst.icdm2016;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.distribution.Normal_MultinomialParents;
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

/**
 * Created by ana@cs.aau.dk on 23/05/16.
 */
public class TestGradientBoostingHypothesis {

    private static NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;
    private static ParallelMaximumLikelihood parallelMaximumLikelihood;

    static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
    static String outputPath="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWekaWithoutResiduals/dataWekaWithoutResiduals";

    static String varName ="VAR01";


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
        int NSETS = 20;
        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};
        int windowSize = 10000;
        double[] meanHiddenVars;


        for (int numIter = 0; numIter < 1; numIter++) {

            DataStream<DataInstance> dataMonth = DataStreamLoader.openFromFile(path+"0.arff");

            List<Attribute> attsSubSetList = new ArrayList<>();
            attsSubSetList.add(dataMonth.getAttributes().getAttributeByName("DEFAULTING"));
            attsSubSetList.add(dataMonth.getAttributes().getAttributeByName(varName));
            Attributes attsSubset = new Attributes(attsSubSetList);

            parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            initML(attsSubset);

            //We create a eu.amidst.eu.amidst.icdm2016.NaiveBayesVirtualConceptDriftDetector object
            virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();

            //We set class variable as the last attribute
            virtualDriftDetector.setClassIndex(-1);

            virtualDriftDetector.setSeed(1);

            //We set the data which is going to be used
            virtualDriftDetector.setAttributes(attsSubset);

            //We fix the size of the window
            virtualDriftDetector.setWindowsSize(windowSize);

            //We fix the number of global latent variables
            virtualDriftDetector.setNumberOfGlobalVars(1);

            //We should invoke this method before processing any data
            virtualDriftDetector.initLearning();


            //New dataset in which we remove the residuals
            DataOnMemoryListContainer<DataInstance> newData = new DataOnMemoryListContainer<>(attsSubset);

            String output = "\nrealMean_c0\tlearntMean_c0\trealMean_c1\tlearntMean_c1\tmeanH\n";

            for (int i = 0; i < NSETS; i++) {

                System.out.println();
                System.out.println();
                System.out.println("****************** ITERATION/MONTH "+numIter+"/"+i+ " ******************");

                int currentMonth = i;

                //if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                //    continue;

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

                Variables vars = virtualDriftDetector.getLearntBayesianNetwork().getDAG().getVariables();
                Variable globalHidden = vars.getVariableByName("GlobalHidden_0");
                Variable variable = virtualDriftDetector.
                        getLearntBayesianNetwork().getDAG().getVariables().getVariableByName(varName);
                Normal_MultinomialNormalParents distVAR = learntBN.getConditionalDistribution(variable);

                double globalHiddenMean = ((Normal) learntBN.getConditionalDistribution(globalHidden)).getMean();

                double b0_VAR_class0 = distVAR.getNormal_NormalParentsDistribution(0).getIntercept();
                double b1_VAR_class0 = distVAR.getNormal_NormalParentsDistribution(0).getCoeffForParent(globalHidden);
                double meanVAR_class0 = b0_VAR_class0 + b1_VAR_class0*globalHiddenMean;

                double b0_VAR_class1 = distVAR.getNormal_NormalParentsDistribution(1).getIntercept();
                double b1_VAR_class1 = distVAR.getNormal_NormalParentsDistribution(1).getCoeffForParent(globalHidden);
                double meanVAR_class1 = b0_VAR_class1 + b1_VAR_class1*globalHiddenMean;


                System.out.println("Learnt Mean for "+varName+"[0] = " + meanVAR_class0);
                System.out.println("Learnt Mean for "+varName+"[1] = " + meanVAR_class1);


                Normal_MultinomialParents distVARML = learntBN_ML.getConditionalDistribution(variable);
                output += distVARML.getNormal(0).getMean()+"\t";
                output += meanVAR_class0+"\t";
                output += distVARML.getNormal(1).getMean()+"\t";
                output += meanVAR_class1+"\t";
                output += globalHiddenMean+"\n";

                        //Remove residuals

                Variable classVar = vars.getVariableByName("DEFAULTING");
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

                                //if (instance.getValue(variable) != 0)
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
            System.out.println(output);
        }
    }
}
