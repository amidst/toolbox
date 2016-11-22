package eu.amidst.cajamareval;

/**
 * Created by dario on 18/11/16.
 */
public class IssueDynamicLearning {
    public static void main(String[] args) throws Exception {

//        String path = "datasets/issueDynamicLearning/datos.arff";
//        //int[] estadosDiscretas = new int[] {2,3,3,3,3};
//        DataStream<DynamicDataInstance> dataGaussians = DataSetGenerator.generate(2352,10000,7,10);
//        DataStreamWriter.writeDataToFile(dataGaussians, path);


        String dia = "20131231";
        String folder = "datasets/issueDynamicLearning/";
        String outputFolder = folder + "/outputs/";


        String [] argsEstaticos = new String[4];
        String [] argsDinamicos = new String[2];


        String fileTrain = folder + dia + "/train.arff";
        String fileTest =  folder + dia + "/test.arff";

        argsEstaticos[0] = fileTrain;
        argsEstaticos[1] = fileTest;
        argsEstaticos[2] = outputFolder;
        argsEstaticos[3] = dia;

        System.out.println("************************************************************\n************************************************************\nLEARNING STATIC MODEL");
        System.out.println("************************************************************\n************************************************************\n");
        NaiveBayesEval.main(argsEstaticos);

        argsDinamicos[0] = folder;
        argsDinamicos[1] = outputFolder;

        System.out.println("************************************************************\n************************************************************\nLEARNING DYNAMIC MODEL");
        System.out.println("************************************************************\n************************************************************\n");

        DynamicNaiveBayesEval.main(argsDinamicos);


    }
}
