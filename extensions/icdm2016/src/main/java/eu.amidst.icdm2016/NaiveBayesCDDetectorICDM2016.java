package eu.amidst.icdm2016;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 *
 * This is the class used to perform the experiments for ICDM2016 and for the journal version DSS.
 *
 * Created by ana@cs.aau.dk on 27/04/16.
 */
public class NaiveBayesCDDetectorICDM2016 {

    private static NaiveBayesVirtualConceptDriftDetector virtualDriftDetector;

    //Global Model including all variables
    //static String[] varNames = {"VAR01","VAR02","VAR03","VAR04","VAR07","VAR08"};

    //Local Model only including one variable
    static String[] varNames = {"VAR04"};

    static int windowSize = 50000;

    static String path = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/dataWeka";

    private static void printOutput(double [] meanHiddenVars, int currentMonth){
        for (int j = 0; j < meanHiddenVars.length; j++) {
            System.out.print(currentMonth + "\t" + meanHiddenVars[j]);
            meanHiddenVars[j] = 0;
        }

        System.out.println();
    }


    public static void main(String[] args) throws IOException {

        int NSETS = 84;

        //We can open the data stream using the static class DataStreamLoader

        DataStream<DataInstance> dataMonth0 = DataStreamLoader.openFromFile(path+"00.arff");

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

        //We set the seed that defines the random initialization for the Q's.
        virtualDriftDetector.setSeed(1);

        //We set the data which is going to be used
        virtualDriftDetector.setAttributes(attsSubset);

        //We fix the size of the window
        virtualDriftDetector.setWindowsSize(windowSize);

        virtualDriftDetector.setOutput(false);


        //Parameters
        //We fix the number of global latent variables
        virtualDriftDetector.setNumberOfGlobalVars(1);

        //We set the transition variance
        virtualDriftDetector.setTransitionVariance(10);

        //Set Prior for H0
        virtualDriftDetector.setPH0(0,1e100);

        //Set Prior for Alphas
        virtualDriftDetector.setPAlpha(0,1e100);

        //Set Prior for Betas
        virtualDriftDetector.setPBeta(0,1e100);


        //We should invoke this method before processing any data
        virtualDriftDetector.initLearning();


        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

        //Some prints
        System.out.print("Month");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }


        System.out.println();


        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;
            DataStream<DataInstance> dataMonthi;
            if (currentMonth<10)
                dataMonthi= DataStreamLoader.openFromFile(path+"0"+currentMonth+".arff");
            else
                dataMonthi= DataStreamLoader.openFromFile(path+currentMonth+".arff");

            virtualDriftDetector.updateModel(dataMonthi);

            List<Variable> param = virtualDriftDetector.getSvb().getPlateuStructure().getNonReplicatedVariables();

            for (Variable variable : param) {
                if (!variable.isNormal() && !variable.isNormalParameter())
                    continue;

                Normal dist = virtualDriftDetector.getSvb().getParameterPosterior(variable);

                System.out.print(variable.getName()+"\t"+dist.getMean()+"\t"+dist.getVariance()+"\t");
            }

            System.out.println();

            //System.out.println(virtualDriftDetector.getLearntBayesianNetwork());

            //RemoveGlobalHiddenResiduals.remove(i,virtualDriftDetector,dataMonthi);

        }
    }
}
