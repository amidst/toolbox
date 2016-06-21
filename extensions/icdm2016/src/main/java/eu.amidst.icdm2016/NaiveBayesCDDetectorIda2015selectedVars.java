package eu.amidst.icdm2016;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.DataStreamLoader;
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
 * Created by ana@cs.aau.dk on 06/06/16.
 */
public class NaiveBayesCDDetectorIda2015selectedVars {
    private static NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;
    private static ParallelMaximumLikelihood parallelMaximumLikelihood;

    //static String path="/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaWithoutResiduals/dataWekaWithoutResiduals_2_";

    static String path="/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/dataWeka";

    //static String path="/Users/andresmasegosa/Documents/tmp/R3_";

    //static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
    static String[] varNames = {"VAR01","VAR02","VAR03","VAR04","VAR07","VAR08"};

    //static String[] varNames = {"VAR04"};


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

    public static void main(String[] args) throws IOException {
        int NSETS = 84;
        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};
        int windowSize = 50000;
        double[] meanHiddenVars;

        int numVars = varNames.length;

        DataStream<DataInstance> dataMonth = DataStreamLoader.openFromFile(path+"0.arff");

        List<Attribute> attsSubSetList = new ArrayList<>();
        attsSubSetList.add(dataMonth.getAttributes().getAttributeByName("DEFAULTING"));
        for (String varName : varNames) {
            attsSubSetList.add(dataMonth.getAttributes().getAttributeByName(varName));
        }
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


        String output = "\n";
        for (String varName : varNames) {
            output += varName+"realMean_c0\t"+varName+"learntMean_c0\t"+"b0\tb1\t"+
                      varName+"realMean_c1\t"+varName+"learntMean_c1\t"+"b0\tb1\t";
        }
        output+="meanH\n";


        for (int i = 0; i < NSETS; i++) {

            System.out.println();
            System.out.println();
            System.out.println("****************** MONTH "+i+ " ******************");

            int currentMonth = i;

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            dataMonth = DataStreamLoader.openFromFile(path + currentMonth + ".arff");

            virtualDriftDetector.setTransitionVariance(0);

            meanHiddenVars = virtualDriftDetector.updateModel(dataMonth);

            parallelMaximumLikelihood.initLearning();

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

            //printOutput(meanHiddenVars, currentMonth);

            Variables vars = virtualDriftDetector.getLearntBayesianNetwork().getDAG().getVariables();
            Variable globalHidden = vars.getVariableByName("GlobalHidden_0");
            List<Variable> variables = new ArrayList<>();
            List<Normal_MultinomialNormalParents> distVARs = new ArrayList<>();
            for (String varName : varNames) {
                Variable variable = virtualDriftDetector.
                        getLearntBayesianNetwork().getDAG().getVariables().getVariableByName(varName);
                variables.add(variable);
                distVARs.add(learntBN.getConditionalDistribution(variable));
            }

            double globalHiddenMean = ((Normal) learntBN.getConditionalDistribution(globalHidden)).getMean();

            double[][] b0_VAR = new double[numVars][2];
            double[][] b1_VAR = new double[numVars][2];
            double[][] meanVAR = new double[numVars][2];

            for (int v = 0; v < varNames.length; v++) {
                for (int c = 0; c < 2; c++) {
                    b0_VAR[v][c] = distVARs.get(v).getNormal_NormalParentsDistribution(c).getIntercept();
                    b1_VAR[v][c] = distVARs.get(v).getNormal_NormalParentsDistribution(c).getCoeffForParent(globalHidden);
                    meanVAR[v][c] = b0_VAR[v][c] + b1_VAR[v][c]*globalHiddenMean;
                }
            }

            for (int v = 0; v < varNames.length; v++) {
                Normal_MultinomialParents distVARML = learntBN_ML.getConditionalDistribution(
                        vars.getVariableByName(varNames[v]));
                for (int c = 0; c < 2; c++) {
                    System.out.println("Real Mean for " + varNames[v] + "["+c+"] = " + distVARML.getNormal(c).getMean());
                    System.out.println("Learnt Mean for " + varNames[v] + "["+c+"] = " + meanVAR[v][c]);
                    output += distVARML.getNormal(c).getMean() + "\t";
                    output += meanVAR[v][c] + "\t";
                    output += b0_VAR[v][c] + "\t";
                    output += b1_VAR[v][c] + "\t";
                }
            }
            output += globalHiddenMean + "\t"+ virtualDriftDetector.getElbo() + "\n";

            //RemoveGlobalHiddenResiduals.remove(i,virtualDriftDetector,dataMonth);

            virtualDriftDetector.getSvb().getLogMarginalProbability();
        }
        System.out.println(output);

    }
}
