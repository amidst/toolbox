package eu.amidst.cajamareval;

import COM.hugin.HAPI.Domain;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToHugin;

/**
 * Created by dario on 16/1/17.
 */
public class ConverterAmidstBN2Hugin {

    private static BayesianNetwork amidstBN;
    private static Domain huginBN;

    public static void main(String[] args) throws Exception {

        String bnPath;
        if(args.length==1) {
            bnPath=args[0];
        }
        else {
            bnPath="/Users/dario/Downloads/cambioformatoredesahugin/NB_20131231_model.bn";
        }

        System.out.println("Loading AMIDST network ...");
        amidstBN = BayesianNetworkLoader.loadFromFile(bnPath);
        System.out.println("Bayesian network " + amidstBN.getName() + " successfully loaded\n");


        System.out.println("Converting the AMIDST network into Hugin format ...");
        huginBN = BNConverterToHugin.convertToHugin(amidstBN);


        String outFile = bnPath + ".net";
        huginBN.saveAsNet(outFile);
        System.out.println("\nHugin network saved in \"" + outFile + "\"" + ".");
    }
}
