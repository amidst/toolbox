package eu.amidst.cajamareval;

import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.latentvariablemodels.dynamicmodels.classifiers.DynamicNaiveBayesClassifier;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Created by dario on 6/6/16.
 */
public class DynamicNaiveBayesEval {

    public static void main(String[] args) throws Exception {

        String className = "Default";

        Locale.setDefault(Locale.GERMANY);
        String dataFolderPath;
        String folderOutput;

        int factorNetworkBuffers;

        if (args.length == 3) {
            dataFolderPath = args[0];
            folderOutput = args[1];
            factorNetworkBuffers = Integer.parseInt(args[2]);
        }
        else {
            System.out.println("Incorrect number of arguments, use: \"DynamicNaiveBayesEval dataFolder outputFolder factorNetworkBuffers\"");
//            System.exit(-10);

            dataFolderPath = "/Users/dario/Desktop/CAJAMAR_corta/";
//        String folderTest = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_test/";
            folderOutput = "/Users/dario/Desktop/CAJAMAR_corta/output3/";

            factorNetworkBuffers = 128;
        }

        System.out.println("Number of JVM processors: " + Runtime.getRuntime().availableProcessors());

        File dataFolder = new File(dataFolderPath);
        List<File> foldersAllDays = Arrays.stream(dataFolder.listFiles()).collect(Collectors.toList());

        System.out.println("Daily folders to analyze:");
        foldersAllDays = foldersAllDays.stream().filter(file-> file.getName().matches("^[0-9]{8}$")).collect(Collectors.toList());



        foldersAllDays.forEach(file -> System.out.println(file.getName()));

        int parallelism1 = Runtime.getRuntime().availableProcessors();

        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", factorNetworkBuffers*parallelism1);
        conf.setInteger("taskmanager.numberOfTaskSlots",parallelism1);

        //StreamExecutionEnvironment env = LocalStreamEnvironment.createLocalEnvironment(parallelism, conf);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.setParallelism(parallelism1);

        DynamicNaiveBayesClassifier dynamicNaiveBayesClassifier = null;

        boolean firstFile = true;
        int timeID = 0;
        boolean useNormalization = true;


        for (File currentDayFolder: foldersAllDays) {

            String fileTrain = currentDayFolder.getAbsolutePath() + "/train.arff";
            String fileTest = currentDayFolder.getAbsolutePath()  + "/test.arff";

            String output = folderOutput        + "DNB_" + currentDayFolder.getName() + "_predictions.csv";
            String output_Flink = folderOutput  + "DNB_" + currentDayFolder.getName() + "_predictionsFlink.csv";
            String modelOutput = folderOutput   + "DNB_" + currentDayFolder.getName() + "_model.txt";
            String networkOutput = folderOutput + "DNB_" + currentDayFolder.getName() + "_model.dbn";

            DataFlink<DynamicDataInstance> dataTrain = DataFlinkLoader.loadDynamicDataFromFolder(env, fileTrain, useNormalization);

            if(firstFile) {
                dynamicNaiveBayesClassifier = new DynamicNaiveBayesClassifier(dataTrain.getAttributes());
                dynamicNaiveBayesClassifier.setClassName(className);
                dynamicNaiveBayesClassifier.setConnectChildrenTemporally(true);

                System.out.println(dynamicNaiveBayesClassifier.getDynamicDAG());

                firstFile = false;
            }

            System.out.println("DAY " + timeID + " TRAINING...");
            dynamicNaiveBayesClassifier.updateModel(timeID,dataTrain);
            System.out.println("DAY " + timeID + " TRAINING FINISHED");

            DataFlink<DynamicDataInstance> dataTest = DataFlinkLoader.loadDynamicDataFromFolder(env, fileTest, useNormalization);

            System.out.println("DAY " + timeID + " TESTING...");
            DataSet<Tuple3<Long,Double,Integer>> predictions = dynamicNaiveBayesClassifier.predict(timeID,dataTest);
            System.out.println("DAY " + timeID + " TESTING FINISHED");
            //predictions.print();

//            List<DynamicDataInstance> result = predictions.collect();
//            result.sort((prediction1,prediction2) -> (prediction1.getSequenceID()>prediction2.getSequenceID() ? 1 : -1));
////            List<DynamicDataInstance> result =
//            result.stream().forEach(prediction -> System.out.println("SEQ_ID:" + prediction.getSequenceID() + "p(Def)=" + prediction.outputString()));


            predictions.writeAsCsv(output_Flink,FileSystem.WriteMode.OVERWRITE);
            env.execute();


            int parallelism = env.getParallelism();
            env.setParallelism(1);
            DataSet<Tuple3<Long, Double,Integer>> csvInput = env.readCsvFile(output_Flink).types(Long.class,Double.class,Integer.class);

            csvInput.sortPartition(0, Order.ASCENDING).writeAsCsv(output,FileSystem.WriteMode.OVERWRITE);
            env.execute();
            env.setParallelism(parallelism);
            DynamicNaiveBayesEval.deleteFolder(new File(output_Flink));


//            List<DynamicDataInstance> dataTestInstances = dataTest.getDataSet().collect();
//
//
//            File outputFile = new File(output);
//            PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
//            writer.println("SEQUENCE_ID,DEFAULT_PROB");
//
//            System.out.println("DAY " + timeID + " TESTING...");
//
//            String resultLine = "";
//            for (DynamicDataInstance dynamicDataInstance : dataTestInstances) {
//
//                double classValue = dynamicDataInstance.getValue(dynamicNaiveBayesClassifier.getClassVar());
//                dynamicDataInstance.setValue(dynamicNaiveBayesClassifier.getClassVar(), Utils.missingValue());
//
//                Multinomial classVarPosteriorDistribution = null;
//                try {
//                    classVarPosteriorDistribution = dynamicNaiveBayesClassifier.predict(dynamicDataInstance);
//                    resultLine = Long.toString(dynamicDataInstance.getSequenceID()) + "," + Double.toString(classVarPosteriorDistribution.getParameters()[1]);
//                    writer.println(resultLine);
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                    System.out.println(e.getMessage());
//                    System.out.println(dynamicDataInstance.getSequenceID());
//                    System.out.println(dynamicDataInstance.getTimeID());
//
//                    writer.close();
//                    System.exit(-5);
//                }
//
//                //System.out.println("Class value: " + classValue + ", predicted prob. of defaulting: " + classVarPosteriorDistribution.getParameters()[1] );
//            }
//
//
//
//            writer.close();



            File modelOutputFile = new File(modelOutput);
            PrintWriter modelWriter = new PrintWriter(modelOutputFile, "UTF-8");
            modelWriter.print(dynamicNaiveBayesClassifier.getModel().toString());
            modelWriter.close();


            DynamicBayesianNetworkWriter.save(dynamicNaiveBayesClassifier.getModel(),networkOutput);
            System.out.println(dynamicNaiveBayesClassifier.getModel().toString());
            timeID++;

        }
//        String fileDay0 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train0.arff";
//        String fileDay1 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_train/train1.arff";
//
//        String fileTest0 = "/Users/dario/Desktop/CAJAMAR_dynamic/ACTIVOS_test/test.arff";
//
//
//
//        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
//
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink0 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileDay0, false);
//        dynamicDataInstanceDataFlink0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//
//        dynamicDataInstanceDataFlink0.getAttributes().getTime_id();
//
//
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink1 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileDay1, false);
//
//
//        DynamicNaiveBayesClassifier dynamicNaiveBayesClassifier = new DynamicNaiveBayesClassifier(dynamicDataInstanceDataFlink0.getAttributes());
//
//        dynamicNaiveBayesClassifier.setClassName("Default");
//        dynamicNaiveBayesClassifier.setConnectChildrenTemporally(true);
//
//        System.out.println(dynamicNaiveBayesClassifier.getDynamicDAG());
//
//        dynamicNaiveBayesClassifier.updateModel(0,dynamicDataInstanceDataFlink0);
//
//        System.out.println("\n\nUPDATED WITH TIME 0\n\n");
//        dynamicNaiveBayesClassifier.updateModel(1,dynamicDataInstanceDataFlink1);
//
//        System.out.println("\n\nUPDATED WITH TIME 1\n\n");
//        System.out.println(dynamicNaiveBayesClassifier.getModel());
//
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlinkTest = DataFlinkLoader.loadDynamicDataFromFolder(env, fileTest0, false);
//
//
//        List<DynamicDataInstance> dataTest = dynamicDataInstanceDataFlinkTest.getDataSet().collect();
//
//        for(DynamicDataInstance dynamicDataInstance : dataTest) {
//            double classValue = dynamicDataInstance.getValue(dynamicNaiveBayesClassifier.getClassVar());
//            dynamicDataInstance.setValue(dynamicNaiveBayesClassifier.getClassVar(), Utils.missingValue());
//            Multinomial classVarPosteriorDistribution = dynamicNaiveBayesClassifier.predict(dynamicDataInstance);
//            System.out.println("Class value: " + classValue + ", predicted prob. of defaulting: " + classVarPosteriorDistribution.getParameters()[1] );
//        }



//        String fileDay0 = "datasets/simulated/cajaMarSynthetic/data0.arff";
//        String fileDay1 = "datasets/simulated/cajaMarSynthetic/data1.arff";
//        String fileDay2 = "datasets/simulated/cajaMarSynthetic/data2.arff";
//
//        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
//
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink0 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileDay0, false);
//        dynamicDataInstanceDataFlink0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink1 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileDay1, false);
//        DataFlink<DynamicDataInstance> dynamicDataInstanceDataFlink2 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileDay2, false);
//
//
//        DynamicNaiveBayesClassifier dynamicNaiveBayesClassifier = new DynamicNaiveBayesClassifier(dynamicDataInstanceDataFlink0.getAttributes());
//
//        dynamicNaiveBayesClassifier.setClassName("DEFAULTER");
//        dynamicNaiveBayesClassifier.setConnectChildrenTemporally(true);
//
//        System.out.println(dynamicNaiveBayesClassifier.getDynamicDAG());
//
//        dynamicNaiveBayesClassifier.updateModel(0,dynamicDataInstanceDataFlink0);
//        dynamicNaiveBayesClassifier.updateModel(1,dynamicDataInstanceDataFlink1);
//        dynamicNaiveBayesClassifier.updateModel(2,dynamicDataInstanceDataFlink2);
//
//        System.out.println(dynamicNaiveBayesClassifier.getModel());









//        DataStream<DynamicDataInstance> dataInstanceDataStream0 = DynamicDataStreamLoader.loadFromFile(fileDay0);
//        dataInstanceDataStream0.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//
//        System.out.println();
//
//        DataStream<DynamicDataInstance> dataInstanceDataStream1 = DynamicDataStreamLoader.loadFromFile(fileDay1);
//        dataInstanceDataStream1.getAttributes().forEach(attribute -> System.out.println(attribute.getName()));
//
//        System.out.println();
//
//        DynamicNaiveBayesClassifier dynamicNaiveBayesClassifier = new DynamicNaiveBayesClassifier(dataInstanceDataStream0.getAttributes());
//
//        dynamicNaiveBayesClassifier.setClassName("Default");
//        dynamicNaiveBayesClassifier.setConnectChildrenTemporally(true);
//
//        dynamicNaiveBayesClassifier.updateModel(dataInstanceDataStream0);
//        System.out.println(dynamicNaiveBayesClassifier.getModel());
//
//        System.out.println("Test data with # instances: " + dataInstanceDataStream1.stream().count());
//
//
//        Variables variables = new Variables(dataInstanceDataStream1.getAttributes());
//
//        dataInstanceDataStream1 = DynamicDataStreamLoader.loadFromFile(fileDay1);
//
//        for(DynamicDataInstance dynamicDataInstance : dataInstanceDataStream1) {
//            System.out.println(dynamicDataInstance);
//        }
//        List<DynamicDataInstance> dataTrain = dataInstanceDataStream0.stream().collect(Collectors.toList());
//        List<DynamicDataInstance> dataTest = dataInstanceDataStream1.stream().collect(Collectors.toList());




//        System.out.println("TRAIN DATA INSTANCES:");
//        for (int i = 0; i < 10; i++) {
//            DynamicDataInstance dynamicDataInstance = dataTrain.get(i);
//            System.out.println("SEQUENCE_ID = " + dynamicDataInstance.getSequenceID() + ", TIME_ID = " + dynamicDataInstance.getTimeID() + ", " + dynamicDataInstance.outputString(variables.getListOfVariables()));
//
//        }
//
//        System.out.println("TEST DATA INSTANCES:");
//        for (int i = 0; i < 10; i++) {
//            DynamicDataInstance dynamicDataInstance = dataTest.get(i);
//            System.out.println(dynamicDataInstance.outputString());
//            //System.out.println("SEQUENCE_ID = " + dynamicDataInstance.getSequenceID() + ", TIME_ID = " + dynamicDataInstance.getTimeID() + ", " + dynamicDataInstance.outputString(variables.getListOfVariables()));
//
//        }

//        for(DynamicDataInstance dynamicDataInstance : dataTest) {
//
//            dynamicDataInstance.setValue(dynamicNaiveBayesClassifier.getClassVar(), Utils.missingValue());
//            Multinomial classVarPosteriorDistribution = dynamicNaiveBayesClassifier.predict(dynamicDataInstance);
//            System.out.println(classVarPosteriorDistribution.toString());
//            dynamicNaiveBayesClassifier.resetModel();
//        }
    }


    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
