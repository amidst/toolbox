package eu.amidst.icdm2016;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 27/04/16.
 */
public class NaiveBayesCDDetectorICDM2016 {

    private static NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;
    private static Variable unemploymentRateVar;
    private static boolean includeUR = false;
    private static boolean includeIndicators = false;

    //static String[] varNames = {"VAR01","VAR02","VAR03","VAR04","VAR07","VAR08"};

    static String[] varNames = {"VAR04"};

    static int windowSize = 50000;

    static String path = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/dataWeka";

    //static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
    //static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWekaUnemploymentRateShifted/dataWekaUnemploymentRateShifted";
    //static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataNoResidualsNoUR/dataNoResidualsNoUR";
    private static void printOutput(double [] meanHiddenVars, int currentMonth){
        for (int j = 0; j < meanHiddenVars.length; j++) {
            System.out.print(currentMonth + "\t" + meanHiddenVars[j]);
            meanHiddenVars[j] = 0;
        }

        if (unemploymentRateVar != null) {
            System.out.print("\t" +virtualDriftDetector.getSvb().getPlateuStructure().
                    getNodeOfNonReplicatedVar(unemploymentRateVar).getAssignment().getValue(unemploymentRateVar));
        }
        System.out.println();
    }


    public static void main(String[] args) {

        int NSETS = 84;

        int numVars = varNames.length;

        //We can open the data stream using the static class DataStreamLoader

        DataStream<DataInstance> dataMonth0 = DataStreamLoader.openFromFile(path+"00.arff");

        List<Attribute> attsSubSetList = new ArrayList<>();
        attsSubSetList.add(dataMonth0.getAttributes().getAttributeByName("DEFAULTING"));
        for (String varName : varNames) {
            attsSubSetList.add(dataMonth0.getAttributes().getAttributeByName(varName));
        }
        String unemploymentRateAttName = "UNEMPLOYMENT_RATE_ALMERIA";
        if(includeUR){
            attsSubSetList.add(dataMonth0.getAttributes().getAttributeByName(unemploymentRateAttName));
        }
        Attributes attsSubset = new Attributes(attsSubSetList);

        //We create a eu.amidst.eu.amidst.icdm2016.NaiveBayesVirtualConceptDriftDetector object
        virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();

        //We set class variable as the last attribute
        virtualDriftDetector.setClassIndex(-1);

        virtualDriftDetector.setSeed(1);

        //We set the data which is going to be used
        //virtualDriftDetector.setData(dataMonth0);
        virtualDriftDetector.setAttributes(attsSubset);

        //We fix the size of the window
        virtualDriftDetector.setWindowsSize(windowSize);

        virtualDriftDetector.setOutput(false);

        //We fix the number of global latent variables
        virtualDriftDetector.setNumberOfGlobalVars(1);

        virtualDriftDetector.setIncludeUR(includeUR);

        virtualDriftDetector.setIncludeIndicators(includeIndicators);

        //We should invoke this method before processing any data
        virtualDriftDetector.initLearning();

        //If UR is to be included
        //virtualDriftDetector.initLearningWithUR();

        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

        //Some prints
        System.out.print("Month");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        try {
            unemploymentRateVar = virtualDriftDetector.getSvb().getDAG().getVariables().getVariableByName(unemploymentRateAttName);
            System.out.print("\t UnempRate");
        } catch (UnsupportedOperationException e) {
        }


        System.out.println();

        double[] meanHiddenVars;



        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;
            DataStream<DataInstance> dataMonthi;
            if (currentMonth<10)
                dataMonthi= DataStreamLoader.openFromFile(path+"0"+currentMonth+".arff");
            else
                dataMonthi= DataStreamLoader.openFromFile(path+currentMonth+".arff");

            virtualDriftDetector.setTransitionVariance(0);

            meanHiddenVars = virtualDriftDetector.updateModel(dataMonthi);


            List<Variable> param = virtualDriftDetector.getSvb().getPlateuStructure().getNonReplicatedVariables();

            for (Variable variable : param) {
                if (!variable.isNormal() && !variable.isNormalParameter())
                    continue;

                Normal dist = virtualDriftDetector.getSvb().getParameterPosterior(variable);

                System.out.print(variable.getName()+"\t"+dist.getMean()+"\t"+dist.getVariance()+"\t");
            }

            System.out.println();

            virtualDriftDetector.setTransitionVariance(1);
            virtualDriftDetector.getSvb().applyTransition();

            //System.out.println(virtualDriftDetector.getLearntBayesianNetwork());

            //We print the output
            //printOutput(meanHiddenVars, currentMonth);

/*
            for (Variable paramVariable : virtualDriftDetector.getSvb().getPlateuStructure().getNonReplicatedVariables()) {

                if (!paramVariable.isNormalParameter())
                    continue;

                if (i>=1500 && paramVariable.getName().contains("_Beta_")) {
                    virtualDriftDetector.getSvb().getPlateuStructure().getNodeOfNonReplicatedVar(paramVariable).setActive(false);
                    EF_NormalParameter ef_normal = (EF_NormalParameter) virtualDriftDetector.getSvb().getPlateuStructure().getNodeOfNonReplicatedVar(paramVariable).getQDist();
                    ef_normal.setNaturalWithMeanPrecision(ef_normal.getMean(),Double.MAX_VALUE);
                    ef_normal.updateMomentFromNaturalParameters();
                } else if (i>=1 && paramVariable.getName().contains("_Beta0_")) {
                    virtualDriftDetector.getSvb().getPlateuStructure().getNodeOfNonReplicatedVar(paramVariable).setActive(false);
                    EF_NormalParameter ef_normal = (EF_NormalParameter) virtualDriftDetector.getSvb().getPlateuStructure().getNodeOfNonReplicatedVar(paramVariable).getQDist();
                    ef_normal.setNaturalWithMeanPrecision(ef_normal.getMean(),Double.MAX_VALUE);
                    ef_normal.updateMomentFromNaturalParameters();
                }

            }
            */
        }
    }
}
