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
import java.util.List;

/**
 * Created by dario on 22/11/16.
 */
public class MAPInferenceEval {

    public static void main(String[] args) throws Exception {
        String modelPath;
        String fileOutput;

        if (args.length == 2) {
            modelPath = args[0];
            fileOutput = args[1];
        }
        else {
            System.out.println("Incorrect number of arguments, use: \"MAPInferenceEval bnFilePath outputFile\"");

            modelPath = "/Users/dario/Desktop/dynamic_t0_bug/NB_20131231_model.bn";
            fileOutput = "/Users/dario/Desktop/dynamic_t0_bug/MAPoutput.txt";
        }

        BayesianNetwork model = BayesianNetworkLoader.loadFromFile(modelPath);
        List<Variable> mapVariables = Serialization.deepCopy(model.getVariables().getListOfVariables());
        mapVariables.remove(mapVariables.lastIndexOf(model.getVariables().getVariableByName("Default")));
        mapVariables.sort((var1, var2) -> var1.getName().compareTo(var2.getName()));

        MAPInference mapInference = new MAPInference();

        mapInference.setModel(model);
        mapInference.setParallelMode(true);

        int nCores=16;
        int nSamplesPerCore=20;

        mapInference.setSampleSize(nCores*nSamplesPerCore);
        mapInference.setNumberOfIterations(100);
        mapInference.setMAPVariables(mapVariables);


        File resultsFile = new File(fileOutput);
        PrintWriter resultsWriter = new PrintWriter(resultsFile, "UTF-8");

        // *******************  NON-DEFAULTERS
        Assignment evidence = new HashMapAssignment(1);
        evidence.setValue(model.getVariables().getVariableByName("Default"),0);

        mapInference.setEvidence(evidence);

        mapInference.runInference();
        Assignment MAPforNonDefaulters = mapInference.getEstimate();

        resultsWriter.println("MAP Configuration for Non-Defaulters:");
        resultsWriter.println(MAPforNonDefaulters.outputString());
        resultsWriter.println();

        // *******************  DEFAULTERS
        evidence = new HashMapAssignment(1);
        evidence.setValue(model.getVariables().getVariableByName("Default"),1);

        mapInference.setEvidence(evidence);
        mapInference.runInference();
        Assignment MAPforDefaulters = mapInference.getEstimate();

        resultsWriter.println("MAP Configuration for Defaulters:");
        resultsWriter.println(MAPforDefaulters.outputString());
        resultsWriter.println();


        resultsWriter.close();

    }


}
