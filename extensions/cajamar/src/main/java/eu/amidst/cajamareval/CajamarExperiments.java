package eu.amidst.cajamareval;

/**
 * Created by dario on 18/11/16.
 */
public class CajamarExperiments {

    public static void main(String[] args) throws Exception {

//        String [] dias = new String[]{"20131231","20140101","20140102","20140103","20140104","20140105","20140106","20140107"};
        String [] dias = new String[]{"20131231"};
        String [] argsEstaticos = new String[4];
        String [] argsDinamicos = new String[2];

        String folder = "/Users/dario/Desktop/CAJAMAR_ultimos/";
        String outputFolder = "/Users/dario/Desktop/CAJAMAR_ultimos/output/";


//        for (int i = 0; i < dias.length; i++) {
//            String fileTrain = folder + dias[i] + "/train.arff";
//            String fileTest = folder + dias[i] + "/test.arff";
//
//            argsEstaticos[0] = fileTrain;
//            argsEstaticos[1] = fileTest;
//            argsEstaticos[2] = outputFolder;
//            argsEstaticos[3] = dias[i];
//
//            NaiveBayesEval.main(argsEstaticos);
//            NBIndicatorsEval.main(argsEstaticos);
//        }

        argsDinamicos[0] = folder;
        argsDinamicos[1] = outputFolder;

        DynamicNaiveBayesEval.main(argsDinamicos);
    }

}
