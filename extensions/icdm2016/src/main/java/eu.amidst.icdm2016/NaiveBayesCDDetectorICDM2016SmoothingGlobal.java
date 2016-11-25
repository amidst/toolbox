package eu.amidst.icdm2016;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 27/04/16.
 */
public class NaiveBayesCDDetectorICDM2016SmoothingGlobal {

    private static NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;
    static String[] varNames = {"VAR01","VAR02","VAR03","VAR04","VAR07","VAR08"};
    static int windowSize = 50000;

    static String path="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
    private static void printOutput(double [] meanHiddenVars, int currentMonth){
        for (int j = 0; j < meanHiddenVars.length; j++) {
            System.out.print(currentMonth + "\t" + meanHiddenVars[j]);
            meanHiddenVars[j] = 0;
        }
        System.out.println();
    }


    public static void main(String[] args) {

        int NSETS = 84;


        //We can open the data stream using the static class DataStreamLoader

        DataStream<DataInstance> dataMonth0 = DataStreamLoader.openFromFile(path+"0.arff");

        List<Attribute> attsSubSetList = new ArrayList<>();
        attsSubSetList.add(dataMonth0.getAttributes().getAttributeByName("DEFAULTING"));
        for (String varName : varNames) {
            attsSubSetList.add(dataMonth0.getAttributes().getAttributeByName(varName));
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

        //We should invoke this method before processing any data
        virtualDriftDetector.initLearning();


        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

        //Some prints
        System.out.print("Month");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        System.out.println();

        double[] meanHiddenVars;



        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            DataStream<DataInstance> dataMonthi = DataStreamLoader.openFromFile(path+currentMonth+".arff");

            virtualDriftDetector.setTransitionVariance(0);

            meanHiddenVars = virtualDriftDetector.updateModel(dataMonthi);

            virtualDriftDetector.setTransitionVariance(0.1);
            virtualDriftDetector.getSvb().applyTransition();


            //We print the output
            //printOutput(meanHiddenVars, currentMonth);


        }

        System.out.println("");


        virtualDriftDetector.getSvb()
                .getPlateuStructure()
                .getNonReplictedNodes()
                .filter(node -> node.getName().contains("Beta") || node.getName().contains("Gamma"))
                .forEach(node -> node.setActive(false));


        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            DataStream<DataInstance> dataMonthi = DataStreamLoader.openFromFile(path+currentMonth+".arff");

            virtualDriftDetector.setTransitionVariance(0);

            meanHiddenVars = virtualDriftDetector.updateModel(dataMonthi);

            virtualDriftDetector.setTransitionVariance(0.1);
            virtualDriftDetector.getSvb().applyTransition();

            //System.out.println(virtualDriftDetector.getLearntBayesianNetwork());

            //We print the output
            printOutput(meanHiddenVars, currentMonth);


        }

    }
}
