/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */
package eu.amidst.huginlink.examples.demos;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.huginlink.learning.ParallelTAN;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class includes demos for the parallel learning of a TAN model.
 */
public class ParallelTANDemo {

    static int numCores = Runtime.getRuntime().availableProcessors();
    static int sampleSize = 10000;
    static int samplesOnMemory = 1000;
    static int numDiscVars = 2000;
    static String dataFileInput = "";
    static boolean onServer = false;
    static int batchSize = 1000;
    static int numStates = 2;

    public static void demoPigs() throws IOException, ClassNotFoundException {

        //It needs GBs, so avoid putting this file in a Dropbox folder!!
        //String dataFile = new String("/Users/afa/Pigs.arff");

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/dataWeka/Pigs.bn");

        int sampleSize = 10000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        ArrayList<Integer> vSamplesOnMemory = new ArrayList(Arrays.asList(5000));
        ArrayList<Integer> vNumCores = new ArrayList(Arrays.asList(1, 2, 3, 4));

        for (Integer samplesOnMemory : vSamplesOnMemory) {
            for (Integer numCores : vNumCores) {
                System.out.println("Learning TAN: " + samplesOnMemory + " samples on memory, " + numCores + " core/s ...");
                DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);

                ParallelTAN tan = new ParallelTAN();
                tan.setNumCores(numCores);
                tan.setNumSamplesOnMemory(samplesOnMemory);
                tan.setNameRoot(bn.getVariables().getListOfVariables().get(0).getName());
                tan.setNameTarget(bn.getVariables().getListOfVariables().get(1).getName());
                Stopwatch watch = Stopwatch.createStarted();
                BayesianNetwork model = tan.learn(data);
                System.out.println(watch.stop());
            }
        }
    }


    public static void demoLive() throws IOException {

        String dataFile = "";
        int numContVars = 0;
        String nameRoot = "";
        String nameTarget = "";
        DataStream data;
        int nOfVars;

        //It may need many GBs, so avoid putting this file in a Dropbox folder!!
        dataFile = new String("./datasets/simulated/Data_#v" + numDiscVars + "_#s" + sampleSize + ".arff");
        BayesianNetworkGenerator.setNumberOfGaussianVars(numContVars);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(numDiscVars, 2);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(sampleSize);
        DataStreamWriter.writeDataToFile(dataStream, dataFile);

        data = DataStreamLoader.open(dataFile);
        nOfVars = numContVars + numDiscVars;
        System.out.println("Learning TAN: " + nOfVars + " variables, " + sampleSize + " samples on disk, " + samplesOnMemory + " samples on memory, 1 core(s) ...");


        nameRoot = data.getAttributes().getFullListOfAttributes().get(numDiscVars - 1).getName();
        nameTarget = data.getAttributes().getFullListOfAttributes().get(0).getName();


        ParallelTAN tan = new ParallelTAN();
        tan.setParallelMode(false);
        tan.setNumSamplesOnMemory(samplesOnMemory);
        tan.setNameRoot(nameRoot);
        tan.setNameTarget(nameTarget);
        BayesianNetwork model = tan.learn(data);
        System.out.println();

        System.out.println("Learning TAN: " + nOfVars + " variables, " + sampleSize + " samples on disk, " + samplesOnMemory + " samples on memory, " + numCores + " core(s) ...");

        data = DataStreamLoader.open(dataFile);

        tan = new ParallelTAN();
        tan.setParallelMode(true);
        tan.setNumSamplesOnMemory(samplesOnMemory);
        tan.setNameRoot(nameRoot);
        tan.setNameTarget(nameTarget);
        tan.setNumCores(numCores);
        tan.setBatchSize(batchSize);
        model = tan.learn(data);
    }

    public static void demoLuxembourg() throws IOException {
        String dataFile = "";
        int numContVars = 0;
        String nameRoot = "";
        String nameTarget = "";
        DataStream data;
        int nOfVars;

        // Generate some fake data and write to file
        dataFile = new String("./datasets/simulated/Data_#v" + numDiscVars + "_#s" + sampleSize + ".arff");
        BayesianNetworkGenerator.setNumberOfGaussianVars(numContVars);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(numDiscVars, 2);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(sampleSize);
        DataStreamWriter.writeDataToFile(dataStream, dataFile);

        data = DataStreamLoader.open(dataFile);
        nOfVars = numContVars + numDiscVars;

        // Get information about the model: Root and Target
        nameRoot = data.getAttributes().getFullListOfAttributes().get(numDiscVars - 1).getName();
        nameTarget = data.getAttributes().getFullListOfAttributes().get(0).getName();

        // Setup the TAN object
        ParallelTAN tan = new ParallelTAN();
        tan.setParallelMode(numCores > 1);
        tan.setNumCores(numCores);
        tan.setNumSamplesOnMemory(samplesOnMemory);
        tan.setNameRoot(nameRoot);
        tan.setNameTarget(nameTarget);
        tan.setBatchSize(batchSize);

        System.out.println("\nLearning TAN (" + nOfVars + " variables) using " + tan.getNumCores() + " core/s.");
        System.out.println("Structure learning (Hugin) uses " + tan.getNumSamplesOnMemory() + " samples.");
        System.out.println("Parameter learning (toolbox) uses " +
                sampleSize + " samples (batch size " + tan.getBatchSize() + ").");

        /// Learn the BayesianNetwork
        System.out.println("Run-times:");
        BayesianNetwork model = tan.learn(data);
        System.out.println();
    }

    public static void demoOnServer() throws IOException {
        String dataFile = "";
        int numContVars = 0;
        String nameRoot = "";
        String nameTarget = "";
        DataStream data;
        int nOfVars;

        //Sample from NB network with the specified features.
        if (dataFileInput.isEmpty()) {
            //It may need many GBs, so avoid putting this file in a Dropbox folder!!!
            dataFile = new String("./datasets/Data_#v" + numDiscVars + "_#s" + sampleSize + ".arff");
            BayesianNetworkGenerator.setNumberOfGaussianVars(numContVars);
            BayesianNetworkGenerator.setNumberOfMultinomialVars(numDiscVars, numStates);
            BayesianNetworkGenerator.setSeed(0);
            BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            DataStream<DataInstance> dataStream = sampler.sampleToDataStream(sampleSize);
            ARFFDataWriter.writeToARFFFile(dataStream, dataFile);

            data = DataStreamLoader.open(dataFile);
            nOfVars = numContVars + numDiscVars;
            System.out.println("Learning TAN: " + nOfVars + " variables, " + numStates + " states/var, " + sampleSize + " samples on disk, " + samplesOnMemory + " samples on memory, " + numCores + " core(s) ...");
        } else {
            data = DataStreamLoader.open(dataFile);
            numDiscVars = data.getAttributes().getNumberOfAttributes();
            nOfVars = numContVars + numDiscVars;
            System.out.println("Learning TAN: " + nOfVars + " variables, " + " samples on file " + dataFileInput + "," + samplesOnMemory + " samples on memory, " + numCores + " core(s) ...");
        }

        nameRoot = data.getAttributes().getFullListOfAttributes().get(numDiscVars - 1).getName();
        nameTarget = data.getAttributes().getFullListOfAttributes().get(0).getName();

        // Serial mode
        if (numCores == 1) {
            ParallelTAN tan = new ParallelTAN();
            tan.setParallelMode(false);
            tan.setNumSamplesOnMemory(samplesOnMemory);
            tan.setNameRoot(nameRoot);
            tan.setNameTarget(nameTarget);
            BayesianNetwork model = tan.learn(data);
        } else {

            //Parallel mode (by default, and also by default all available cores are used)
            ParallelTAN tan = new ParallelTAN();
            tan.setParallelMode(true);
            tan.setNumSamplesOnMemory(samplesOnMemory);
            tan.setNameRoot(nameRoot);
            tan.setNameTarget(nameTarget);
            tan.setNumCores(numCores);
            tan.setBatchSize(batchSize);

            BayesianNetwork model = tan.learn(data);
        }
    }

    public static void useGnuParser(final String[] commandLineArguments) {

        final CommandLineParser cmdLineGnuParser = new GnuParser();

        final Options gnuOptions = constructOptions();
        CommandLine commandLine;
        try {
            commandLine = cmdLineGnuParser.parse(gnuOptions, commandLineArguments);
            if (commandLine.hasOption("c")) {
                numCores = Integer.parseInt(commandLine.getOptionValue("c"));
                System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(numCores));
            }
            if (commandLine.hasOption("s")) {
                sampleSize = Integer.parseInt(commandLine.getOptionValue("s"));
            }
            if (commandLine.hasOption("m")) {
                samplesOnMemory = Integer.parseInt(commandLine.getOptionValue("m"));
            }
            if (commandLine.hasOption("v")) {
                numDiscVars = Integer.parseInt(commandLine.getOptionValue("v"));
            }
            if (commandLine.hasOption("d")) {
                dataFileInput = commandLine.getOptionValue("d");
            }
            if (commandLine.hasOption("b")) {
                batchSize = Integer.parseInt(commandLine.getOptionValue("b"));
            }
            if (commandLine.hasOption("onServer")) {
                onServer = true;
            }
            if (commandLine.hasOption("r")) {
                numStates = Integer.parseInt(commandLine.getOptionValue("r"));
            }

        } catch (ParseException parseException)  // checked exception
        {
            System.err.println(
                    "Encountered exception while parsing using GnuParser:\n"
                            + parseException.getMessage());
        }
    }


    /**
     * Writes "help" to the provided OutputStream.
     * @param options an Options object
     * @param printedRowWidth an integer
     * @param header a String
     * @param footer a String
     * @param spacesBeforeOption an integer
     * @param spacesBeforeOptionDescription an integer
     * @param displayUsage a boolean
     * @param out an OutputStream object
     */
    public static void printHelp(
            final Options options,
            final int printedRowWidth,
            final String header,
            final String footer,
            final int spacesBeforeOption,
            final int spacesBeforeOptionDescription,
            final boolean displayUsage,
            final OutputStream out) {
        final String commandLineSyntax = "run.sh eu.amidst.examples.ParallelTANDemo";
        final PrintWriter writer = new PrintWriter(out);
        final HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(
                writer,
                printedRowWidth,
                commandLineSyntax,
                header,
                options,
                spacesBeforeOption,
                spacesBeforeOptionDescription,
                footer,
                displayUsage);
        writer.flush();
    }


    /**
     * Constructs and provides the Options.
     * @return Options expected from command-line.
     */
    public static Options constructOptions() {
        final Options options = new Options();
        options.addOption("c", "numCores", true, "Here you can set # of cores for hugin.");
        options.addOption("s", "samples", true, "Here you can set # of (out-of-core) samples for parameter learning (amidst).");
        options.addOption("m", "samplesOnMemory", true, "Here you can set # of (in memory) samples for structural learning (hugin).");
        options.addOption("v", "variables", true, "Here you can set # of variables .");
        options.addOption("d", "dataPath", true, "Here you can specify the data path .");
        options.addOption("b", "windowsSize", true, "Here you can specify the batch size for learning.");
        options.addOption("onServer", "onServer", false, "write onServer to run onServer method (with more options).");
        options.addOption("r", "numStates", true, "Here you can set # of states.");

        return options;
    }


    //TODO: Sub-options should be considered in the future
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ParallelTANDemo.demoPigs();
    }

   /*     final String applicationName = "run.sh eu.amidst.examples.ParallelTANDemo";

        if (args.length < 1) {

            System.out.println("\n-- USAGE/HELP --\n");
            printHelp(
                    constructOptions(), 85, "Parallel TAN HELP", "End of PARALLEL TAN Help",
                    3, 5, true, System.out);

            System.out.println();
            System.out.println("Running using by default parameters:");
        }

        useGnuParser(args);

        if(onServer)
            ParallelTANDemo.demoOnServer();
        else
            ParallelTANDemo.demoLuxembourg();
    }
*/
}