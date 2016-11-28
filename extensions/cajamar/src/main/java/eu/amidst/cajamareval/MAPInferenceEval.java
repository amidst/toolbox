package eu.amidst.cajamareval;

import eu.amidst.core.inference.MAPInference;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dario on 22/11/16.
 */
public class MAPInferenceEval {

    public static void main(String[] args) throws Exception {
        String modelPath;
        String fileOutput;
        String attributesFile;

        if (args.length == 3) {
            modelPath = args[0];
            fileOutput = args[1];
            attributesFile = args[2];
        }
        else {
            System.out.println("Incorrect number of arguments, use: \"MAPInferenceEval bnFilePath outputFile\"");

            //modelPath = "/Users/dario/Desktop/dynamic_t0_bug/NB_20131231_model.bn";
            //fileOutput = "/Users/dario/Desktop/dynamic_t0_bug/MAPoutput.txt";

            //modelPath = "/Users/dario/Desktop/CAJAMAR_ultimos/20131231/TAN__model.bn";
            //fileOutput = "/Users/dario/Desktop/CAJAMAR_ultimos/20131231/TAN__MAP_output.txt";

            //modelPath =  "/Users/dario/Desktop/TAN_20131231_model.bn";
            //fileOutput = "/Users/dario/Desktop/TAN_20131231_MAP_output.txt";

            modelPath =  "/Users/dario/Desktop/PC__model.bn";
            fileOutput = "/Users/dario/Desktop/PC__model_MAP_output.txt";
            attributesFile = "/Users/dario/Desktop/datosPrueba.arff/attributes.txt";
        }




        BayesianNetwork model = BayesianNetworkLoader.loadFromFile(modelPath);

        System.out.println(model.toString());

        List<Variable> mapVariables = Serialization.deepCopy(model.getVariables().getListOfVariables());
        mapVariables.remove(mapVariables.lastIndexOf(model.getVariables().getVariableByName("Default")));
        mapVariables.sort((var1, var2) -> var1.getName().compareTo(var2.getName()));

        MAPInference mapInference = new MAPInference();

        mapInference.setModel(model);
        mapInference.setParallelMode(true);

        int nCores=4;
        int nSamplesPerCore=50;

        mapInference.setSampleSize(nCores*nSamplesPerCore);
        mapInference.setNumberOfIterations(300);
        mapInference.setMAPVariables(mapVariables);


        File resultsFile = new File(fileOutput);
        PrintWriter resultsWriter = new PrintWriter(resultsFile, "UTF-8");


        Variable classVar = model.getVariables().getVariableByName("Default");






        // *******************  NON-DEFAULTERS
        Assignment evidence = new HashMapAssignment(1);
        evidence.setValue(classVar,0);

        mapInference.setEvidence(evidence);

        mapInference.runInference();
        Assignment MAPforNonDefaulters = mapInference.getEstimate();

//        resultsWriter.println("MAP Configuration for Non-Defaulters:");
//        resultsWriter.println(MAPforNonDefaulters.outputString(model.getVariables().getListOfVariables()));
//        resultsWriter.println();


        // *******************  DEFAULTERS
        evidence = new HashMapAssignment(1);
        evidence.setValue(classVar,1);

        mapInference.setEvidence(evidence);
        mapInference.runInference();
        Assignment MAPforDefaulters = mapInference.getEstimate();

//        resultsWriter.println("MAP Configuration for Defaulters:");
//        resultsWriter.println(MAPforDefaulters.outputString(model.getVariables().getListOfVariables()));
//        resultsWriter.println();

        List<Variable> nonClassVariables = Serialization.deepCopy(model.getVariables().getListOfVariables());
        nonClassVariables.remove(classVar);



        List<String> attLines = Files.lines(Paths.get(attributesFile))
                .map(String::trim)
                .filter(w -> !w.isEmpty())
                .filter(w -> !w.startsWith("%"))
                .filter(line -> line.startsWith("@attribute"))
                .filter(line -> !line.startsWith("@attribute SEQUENCE_ID"))
                .filter(line -> !line.startsWith("@attribute TIME_ID"))
                .filter(line -> !line.startsWith("@attribute Default"))
                .collect(Collectors.toList());

        if(nonClassVariables.size() != attLines.size()) {
            System.out.println("ERROR: Amount of Variable objects does not match amount of Attribute lines");
        }

        for (int i = 0; i < nonClassVariables.size(); i++) {
            Variable variable = nonClassVariables.get(i);
            String [] varStates = getAttributeStateNames(attLines.get(i));

//            System.out.println(variable.getName() + " with states " + Arrays.toString(varStates));

            String outputLine = variable.getName() + "," + varStates[(int)MAPforNonDefaulters.getValue(variable)] + "," + varStates[(int)MAPforDefaulters.getValue(variable)];
            resultsWriter.println(outputLine);
        }
        //attLines.forEach(attLine -> System.out.println(Arrays.toString(getAttributeStateNames(attLine))));




        resultsWriter.close();

    }


    public static String[] getAttributeStateNames(String attributesFileLine) {
        String[] states = new String[0];

        String[] parts = attributesFileLine.split("\\s+|\t+");
//        System.out.println(parts.length);

        if (!parts[0].trim().startsWith("@attribute"))
            throw new IllegalArgumentException("Attribute attributesFileLine does not start with @attribute");

        String name = parts[1].trim();

        if (name.equals("SEQUENCE_ID") || name.equals("TIME_ID")) {
            return states;
        }

        if (parts[3].equals("real")) {
            return states;
        }

        else {
            parts[3]=attributesFileLine.substring(attributesFileLine.indexOf("{")).replaceAll("\t", "");
            states = parts[3].substring(1,parts[3].length()-1).split(",");

            List<String> statesNames = Arrays.stream(states).map(String::trim).collect(Collectors.toList());
            return statesNames.toArray(states);
        }

    }

}
