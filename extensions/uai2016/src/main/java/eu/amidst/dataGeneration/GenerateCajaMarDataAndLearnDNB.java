package eu.amidst.dataGeneration;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.utils.AmidstOptionsHandler;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.cajamar.CajaMarLearn;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * This class generates CajaMar-like data and learns a dynamic NB to test the process was succesfull.
 * The two scripts to generate this data (IDA or SCAI-like if the includeSocioEconomicVars flag is activated)
 * can be found in the amidst Dropbox folder under the toolbox directory
 * (as it should not be made public)
 *
 * Example of call: (-Xmx8g)
 * -s 10 -numFiles 3 -RscriptsPath "./extensions/uai2016/doc-experiments/dataGenerationForFlink/"
 * -outputFullPath "~/core/extensions/uai2016/doc-experiments/dataGenerationForFlink/IDAlikeData"
 * -printINDEX -seed 0
 *
 *
 * Created by ana@cs.aau.dk on 08/12/15.
 */
public class GenerateCajaMarDataAndLearnDNB implements AmidstOptionsHandler {

    DynamicBayesianNetwork dbn = null;


    private int numSamplesPerFile = 100000;
    private int numFiles = 10;
    private String RscriptsPath = "./";
    private String outputFullPath = "./";
    private boolean includeSocioEconomicVars = false;
    private int batchSize = 1000;
    private boolean printINDEX = true;
    private int seed = 0;

    public int getNumSamplesPerFile() {
        return numSamplesPerFile;
    }

    public void setNumSamplesPerFile(int numSamplesPerFile) {
        this.numSamplesPerFile = numSamplesPerFile;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    public String getRscriptsPath() {
        return RscriptsPath;
    }

    public void setRscriptsPath(String rscriptsPath) {
        RscriptsPath = rscriptsPath;
    }

    public String getOutputFullPath() {
        return outputFullPath;
    }

    public void setOutputFullPath(String outputFullPath) {
        this.outputFullPath = outputFullPath.replaceFirst("^~",System.getProperty("user.home"));;
    }

    public boolean isIncludeSocioEconomicVars() {
        return includeSocioEconomicVars;
    }

    public void setIncludeSocioEconomicVars(boolean includeSocioEconomicVars) {
        this.includeSocioEconomicVars = includeSocioEconomicVars;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isPrintINDEX() {
        return printINDEX;
    }

    public void setPrintINDEX(boolean printINDEX) {
        this.printINDEX = printINDEX;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public void generateIDADataFromRScript() throws Exception {
        /*
         * The 1st parameter is the number of files (per month)
         * The 2nd parameter is the length of the sequence, i.e., # of clients
         * The 3rd argument is the output path
         * The 4th argument is a boolean to indicate if the index should be printed for
         * the attributes
         */

        Process p = Runtime.getRuntime().exec("Rscript "+getRscriptsPath()+"/data_generator_IDA.R "+
                getNumFiles()+" "+getNumSamplesPerFile()+" "+ getOutputFullPath() + " " +
                String.valueOf(isPrintINDEX()).toUpperCase()+" "+getSeed());
        p.waitFor();


    }

    public void generateSCAIDataFromRScript() throws Exception{
        /*
         * The 1st parameter is the number of files (per month)
         * The 2nd parameter is the length of the sequence, i.e., # of clients
         * The 3rd argument is the output path
         * The 4th argument is a boolean to indicate if the index should be printed for
         * the attributes
         */

        Process p = Runtime.getRuntime().exec("Rscript "+getRscriptsPath()+"/data_generator_SCAI.R "+
                getNumFiles()+" "+getNumSamplesPerFile()+" "+ getOutputFullPath()  + " " +
                String.valueOf(isPrintINDEX()).toUpperCase()+" "+getSeed());
        p.waitFor();
    }
    public void generateStaticData() throws Exception{
        if(this.isIncludeSocioEconomicVars())
            this.generateSCAIDataFromRScript();
        else
            this.generateIDADataFromRScript();
    }

    /**
     * This method returns a DynamicDAG object with naive Bayes structure for the given attributes.
     * @param attributes
     * @return
     */
    public static DynamicDAG getNaiveBayesStructure(Attributes attributes){

        //We create a Variables object from the attributes of the data stream
        DynamicVariables dynamicVariables = new DynamicVariables(attributes);

        //We define the predicitive class variable
        Variable classVar = dynamicVariables.getVariableByName("DEFAULT");

        //Then, we create a DAG object with the defined model header
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        //We set the links of the DAG.
        dag.getParentSetsTimeT().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> {
                            w.addParent(classVar);
                            //Connect children in consecutive time steps
                            //w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                        }
                );

        //Connect the class variable in consecutive time steps
        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

        return dag;
    }

    public void learnDynamicNB() throws Exception{

        /*
         * We learn a dynamicNB with flink from CajaMar static data
         */
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        System.out.println(getOutputFullPath() + "/MONTH1.arff");
        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicData(env,
                    getOutputFullPath() + "/MONTH1.arff");
        DynamicDAG dynamicDAG = getNaiveBayesStructure(data0.getAttributes());
        dbn = new DynamicBayesianNetwork(dynamicDAG);

        CajaMarLearn learn = new CajaMarLearn();
        learn.setMaximumGlobalIterations(10);
        learn.setBatchSize(getBatchSize());
        learn.setDAG(dynamicDAG);
        learn.setOutput(true);
        learn.initLearning();

        System.out.println("--------------- MONTH " + 1 + " --------------------------");
        learn.updateModelWithNewTimeSlice(0, data0);


        for (int i = 2; i < 85; i++) {
            System.out.println("--------------- MONTH " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicData(env,
                    getOutputFullPath()+"/MONTH" + i + ".arff");
            learn.updateModelWithNewTimeSlice(i, dataNew);
        }
        dbn = learn.getLearntDynamicBayesianNetwork();
        System.out.println(dbn);

    }




    public static void main(String[] args) throws Exception{

        GenerateCajaMarDataAndLearnDNB generateData = new GenerateCajaMarDataAndLearnDNB();

        generateData.setOptions(args);

        generateData.generateStaticData();
        generateData.learnDynamicNB();
    }

    @Override
    public String listOptions() {
        return  this.classNameID() +",\\"+
                "-s, 100000, Number of samples per file\\" +
                "-numFiles, 10, Number of files\\" +
                "-RscriptsPath, ./, Path for the R script(s)\\" +
                "-outputFullPath, ./, Path for the output files\\" +
                "-includeSocioEconomicVars, false, If set, then socioeconoic vars are included, i.e., " +
                "data like the SCAI paper is generated.\\"+
                "-batchSize, 1000, batchSize for learning the dbn.\\"+
                "-printINDEX, true, print index in attribute arff header\\"+
                "-seed, 0, set seed for data sample generation in Rscript";
    }

    @Override
    public String listOptionsRecursively() {
        return this.listOptions();
    }

    @Override
    public void loadOptions() {
        this.setNumSamplesPerFile(this.getIntOption("-s"));
        this.setNumFiles(this.getIntOption("-numFiles"));
        this.setRscriptsPath(this.getOption("-RscriptsPath"));
        this.setOutputFullPath(this.getOption("-outputFullPath"));
        this.setIncludeSocioEconomicVars(getBooleanOption("-includeSocioEconomicVars"));
        this.setBatchSize(this.getIntOption("-batchSize"));
        this.setPrintINDEX(this.getBooleanOption("-printINDEX"));
        this.setSeed(this.getIntOption("-seed"));
    }
}
