package eu.amidst.cajamareval;

import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.huginlink.converters.DBNConverterToHugin;

/**
 * Created by dario on 16/1/17.
 */
public class ConverterAmidstDBN2Hugin {

    private static DynamicBayesianNetwork amidstDBN;
    private static COM.hugin.HAPI.Class huginDBN;

    public static void main(String[] args) throws Exception {

        String dbnPath;
        if(args.length==1) {
            dbnPath=args[0];
        }
        else {
            dbnPath="/Users/dario/Downloads/cambioformatoredesahugin/DNB_20131231_model.dbn";
        }

        System.out.println("Loading AMIDST network ...");
        amidstDBN = DynamicBayesianNetworkLoader.loadFromFile(dbnPath);
        System.out.println("Bayesian network " + amidstDBN.getName() + " successfully loaded\n");


        System.out.println("Converting the AMIDST network into Hugin format ...");
        huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);


        String outFile = dbnPath + ".net";
        huginDBN.saveAsNet(outFile);
        System.out.println("\nHugin network saved in \"" + outFile + "\"" + ".");
    }
}
