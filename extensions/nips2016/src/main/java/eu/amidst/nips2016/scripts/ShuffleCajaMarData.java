package eu.amidst.nips2016.scripts;

import eu.amidst.core.utils.Utils;

/**
 * Created by ana@cs.aau.dk on 09/05/16.
 */
public class ShuffleCajaMarData {
    public static void main(String[] args) {
        String inputPath="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWeka/dataWeka";
        String outputPath="/Users/ana/Documents/Amidst-MyFiles/CajaMar/dataWekaShuffled/dataWekaSuffled";

        for (int m = 0; m < 84; m++) {
            Utils.shuffleData(inputPath + m + ".arff",outputPath + m + ".arff");
        }
    }
}
