package eu.amidst.examples;

import COM.hugin.HAPI.ExceptionHugin;
import com.google.common.base.Stopwatch;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataOnStream;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.StaticDataOnMemoryFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.huginlink.ParallelTAN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.lang.Runtime;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Created by afa on 16/12/14.
 */
public class ParallelTANDemo {

    public static int nOfThreads;

    public static void demo1() throws ExceptionHugin, IOException {

        //It needs GBs, so avoid putting this file in a Dropbox folder!!!!
        String dataFile = new String("./datasets/Pigs.arff");


        BayesianNetwork bn = BayesianNetworkLoader.loadFromHugin("networks/Pigs.net");
        //int sampleSize = 100000;
        //BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        //sampler.setParallelMode(true);
        //sampler.sampleToAnARFFFile(dataFile, sampleSize);

        ArrayList<Integer> vSamplesOnMemory = new ArrayList(Arrays.asList(5000));
        ArrayList<Integer> vNumCores = new ArrayList(Arrays.asList(1, 2, 3, 4));

        for (Integer samplesOnMemory : vSamplesOnMemory) {
            for (Integer numCores : vNumCores) {
                System.out.println("Learning TAN: " + samplesOnMemory + " samples on memory, " + numCores + "core/s ...");
                DataBase data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));

                ParallelTAN tan = new ParallelTAN();
                tan.setNumCores(numCores);
                tan.setNumSamplesOnMemory(samplesOnMemory);
                tan.setNameRoot(bn.getStaticVariables().getListOfVariables().get(0).getName());
                tan.setNameTarget(bn.getStaticVariables().getListOfVariables().get(1).getName());
                Stopwatch watch = Stopwatch.createStarted();
                BayesianNetwork model = tan.learnBN(data);
                System.out.println(watch.stop());
            }
        }
    }


    public static void demo2(int numCores, int sampleSize, int samplesOnMemory, int numDiscVars, String dataFileImput) throws ExceptionHugin, IOException {



        String dataFile = "";
        int numContVars = 0;
        String nameRoot = "";
        String nameTarget = "";
        DataBase data;
        int nOfVars;

        if(dataFileImput.isEmpty()) {
            //It may need many GBs, so avoid putting this file in a Dropbox folder!!!
            dataFile = new String("./datasets/Data_#v"+numDiscVars+"_#s"+sampleSize+".arff");
            BayesianNetworkGenerator.setNumberOfContinuousVars(numContVars);
            BayesianNetworkGenerator.setNumberOfDiscreteVars(numDiscVars);
            BayesianNetworkGenerator.setNumberOfStates(2);
            BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(new Random(0));

            //BayesianNetwork bn = BayesianNetworkLoader.loadFromHugin("networks/Pigs.net");

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setParallelMode(true);
            sampler.sampleToAnARFFFile(dataFile, sampleSize);
            data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));
            nOfVars = numContVars + numDiscVars;
            System.out.println("Learning TAN: " + nOfVars +" variables, " + sampleSize +" samples on disk, "+ samplesOnMemory + " samples on memory, "+  numCores + " core(s) ...");
        }else{
            dataFile = dataFileImput;
            data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFileImput));
            numDiscVars = data.getAttributes().getNumberOfAttributes();
            nOfVars = numContVars + numDiscVars;
            System.out.println("Learning TAN: " + nOfVars +" variables, " + " samples on file " + dataFileImput + "," + samplesOnMemory + " samples on memory, "+  numCores + " core(s) ...");
        }





        nameRoot = data.getAttributes().getList().get(numDiscVars-1).getName();
        nameTarget = data.getAttributes().getList().get(0).getName();


        ParallelTAN tan = new ParallelTAN();
        tan.setParallelMode(false);
        tan.setNumSamplesOnMemory(samplesOnMemory);
        tan.setNameRoot(nameRoot);
        tan.setNameTarget(nameTarget);
        BayesianNetwork model = tan.learnBN(data);
        System.out.println();

        numCores=Runtime.getRuntime().availableProcessors();
        if(dataFileImput.isEmpty()) {
            System.out.println("Learning TAN: " + nOfVars + " variables, " + sampleSize + " samples on disk, " + samplesOnMemory + " samples on memory, " + numCores + " core(s) ...");
        }else{
            System.out.println("Learning TAN: " + nOfVars +" variables, " + " samples on file " + dataFileImput + "," + samplesOnMemory + " samples on memory, "+  numCores + " core(s) ...");
        }
        data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));

        tan = new ParallelTAN();
        tan.setParallelMode(true);
        tan.setNumSamplesOnMemory(samplesOnMemory);
        tan.setNameRoot(nameRoot);
        tan.setNameTarget(nameTarget);
        model = tan.learnBN(data);


    }



    /**
     * Write "help" to the provided OutputStream.
     */
    public static void printHelp(
            final Options options,
            final int printedRowWidth,
            final String header,
            final String footer,
            final int spacesBeforeOption,
            final int spacesBeforeOptionDescription,
            final boolean displayUsage,
            final OutputStream out)
    {
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
     * Construct and provide the Options.
     *
     * @return Options expected from command-line.
     */
    public static Options constructOptions()
    {
        final Options options = new Options();
        options.addOption("c", "cores", true, "Here you can set # of cores .");
        options.addOption("s", "samples", true, "Here you can set # of (out-of-core) samples .");
        options.addOption("m", "samplesOnMemory", true, "Here you can set # of (in memory) samples .");
        options.addOption("v", "variables", true, "Here you can set # of variables .");
        options.addOption("d", "dataPath", true, "Here you can specify the data path .");

        return options;
    }


    public static void main(String[] args) throws ExceptionHugin, IOException {

        nOfThreads = Runtime.getRuntime().availableProcessors();

        final String applicationName = "run.sh eu.amidst.examples.ParallelTANDemo";

        if (args.length < 1) {

            System.out.println("\n-- USAGE/HELP --\n");
            printHelp(
                    constructOptions(), 85, "Parallel TAN HELP", "End of PARALLEL TAN Help",
                    3, 5, true, System.out);

            System.out.println();
            System.out.println("Running using by default parameters:");
        }


        int cores = 1;
        int sampleSize = 10000;
        int samplesOnMemory = 1000;
        int numDiscVars = 2000;
        String dataFile = "";

        final CommandLineParser cmdLineGnuParser = new GnuParser();

        final Options gnuOptions = constructOptions();
        CommandLine commandLine;
        try
        {
            commandLine = cmdLineGnuParser.parse(gnuOptions, args);
            if ( commandLine.hasOption("c") )
            {
                cores = Integer.parseInt(commandLine.getOptionValue("c"));
            }
            if ( commandLine.hasOption("s") )
            {
                sampleSize = Integer.parseInt(commandLine.getOptionValue("s"));
            }
            if ( commandLine.hasOption("m") )
            {
                samplesOnMemory = Integer.parseInt(commandLine.getOptionValue("m"));
            }
            if ( commandLine.hasOption("v") )
            {
                numDiscVars = Integer.parseInt(commandLine.getOptionValue("v"));
            }
            if ( commandLine.hasOption("d") )
            {
                dataFile = commandLine.getOptionValue("d");
            }

        }
        catch (ParseException parseException)  // checked exception
        {
            System.err.println(
                    "Encountered exception while parsing using GnuParser:\n"
                            + parseException.getMessage() );
        }

        ParallelTANDemo.demo2(cores, sampleSize, samplesOnMemory, numDiscVars, dataFile);
    }

}