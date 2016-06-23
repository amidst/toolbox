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

package eu.amidst.dataGeneration;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.utils.AmidstOptionsHandler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import org.apache.flink.api.common.functions.RichMapPartitionFunction;
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * This class generates CajaMar-like data and learns a dynamic NB to test the process was succesfull.
 * The two scripts to generate this data (IDA or SCAI-like if the includeSocioEconomicVars flag is activated)
 * can be found in the amidst Dropbox folder under the toolbox directory
 * (as it should not be made public)
 *
 * Example of call: (-Xmx8g)
 * -s 10 -numFiles 3 -RscriptsPath "./extensions/uai2016/io-experiments/dataGenerationForFlink/"
 * -outputFullPath "~/core/extensions/uai2016/io-experiments/dataGenerationForFlink/IDAlikeData"
 * -printINDEX -seed 0
 *
 * The option p.waitFor(); might not work on all systems, so data may have to be generated and tested on
 * different executions
 *
 * Created by ana@cs.aau.dk on 08/12/15.
 */
public class GenerateCajaMarData implements AmidstOptionsHandler {

    DynamicBayesianNetwork dbn = null;


    private int numSamplesPerFile = 100000;
    private int numFiles = 10;
    private String RscriptsPath = "./";
    private String outputFullPath = "./";
    private boolean includeSocioEconomicVars = false;
    private int batchSize = 1000;
    private boolean printINDEX = false;
    private int seed = 0;
    private boolean addConceptDrift = false;
    private boolean addRange = false;

    int nMonths = 15;

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

    public boolean isAddConceptDrift() {
        return addConceptDrift;
    }

    public void setAddConceptDrift(boolean addConceptDrift) {
        this.addConceptDrift = addConceptDrift;
    }

    public boolean isAddRange() {
        return addRange;
    }

    public void setAddRange(boolean addRange) {
        this.addRange = addRange;
    }

    private void generateIDADataFromRScript() throws Exception {
        /*
         * The 1st parameter is the number of files (per month)
         * The 2nd parameter is the length of the sequence, i.e., # of clients
         * The 3rd argument is the output path
         * The 4th argument is a boolean to indicate if the index should be printed for
         * the attributes
         */

        Process p = Runtime.getRuntime().exec("Rscript "+getRscriptsPath()+"/data_generator_IDA.R "+
                getNumFiles()+" "+getNumSamplesPerFile()+" "+ getOutputFullPath() + " " +
                String.valueOf(isPrintINDEX()).toUpperCase()+" "+getSeed()+ " " +
                String.valueOf(isAddConceptDrift()).toUpperCase()+ " " +
                String.valueOf(isAddRange()).toUpperCase());
        System.out.println("Rscript "+getRscriptsPath()+"/data_generator_IDA.R "+
                getNumFiles()+" "+getNumSamplesPerFile()+" "+ getOutputFullPath() + " " +
                String.valueOf(isPrintINDEX()).toUpperCase()+" "+getSeed()+ " " +
                String.valueOf(isAddConceptDrift()).toUpperCase()+ " " +
                String.valueOf(isAddRange()).toUpperCase());
        p.waitFor();


    }

    private void generateSCAIDataFromRScript() throws Exception{
        /*
         * The 1st parameter is the number of files (per month)
         * The 2nd parameter is the length of the sequence, i.e., # of clients
         * The 3rd argument is the output path
         * The 4th argument is a boolean to indicate if the index should be printed for
         * the attributes
         */

        Process p = Runtime.getRuntime().exec("Rscript "+getRscriptsPath()+"/data_generator_SCAI.R "+
                getNumFiles()+" "+getNumSamplesPerFile()+" "+ getOutputFullPath()  + " " +
                String.valueOf(isPrintINDEX()).toUpperCase()+" "+getSeed()+ " " +
                String.valueOf(isAddConceptDrift()).toUpperCase());
        p.waitFor();
    }
    public void generateData() throws Exception{
        if(this.isIncludeSocioEconomicVars())
            this.generateSCAIDataFromRScript();
        else
            this.generateIDADataFromRScript();
    }



    /**
     * This method returns a DynamicDAG object with naive Bayes structure for the given attributes.
     * @param attributes object of the class Attributes
     * @return object with the dynamic DAG
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

    public void assignRanges() throws Exception{
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        /*
         * Calculate max and min ranges for all attributes
         */
        System.out.println("--------------- Calculate max and min ranges for all attributes ---------------");
        MinMaxValues globalResult = null;
        for (int i = 1; i <= nMonths; i++) {
            DataFlink<DynamicDataInstance> data = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    getOutputFullPath()+"/MONTH" + i + ".arff" ,false);
            MinMaxValues monthResult = data.getDataSet()
                    .mapPartition(new GetRange(data.getAttributes().getNumberOfAttributes()))
                    .reduce(new ReduceMinMax())
                    .collect().get(0);
            if(i==1)
                globalResult = monthResult;
            else
                globalResult = combineMonthlyResults(globalResult,monthResult);
        }

        /*
         * Write header with ranges
         */
        System.out.println("--------------- Write header with ranges ---------------");
        for (int i = 1; i <= nMonths; i++) {
            System.out.println("++++++++++++"+i);
            DataFlink<DynamicDataInstance> data = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    getOutputFullPath()+"/MONTH" + i + ".arff" ,false);
            for (Attribute att: data.getAttributes()){
                if(!att.getName().equalsIgnoreCase("DEFAULT")) {
                    ((RealStateSpace) att.getStateSpaceType()).setMaxInterval(globalResult.max[att.getIndex()]);
                    ((RealStateSpace) att.getStateSpaceType()).setMinInterval(globalResult.min[att.getIndex()]);
                }
            }

            DataFlinkWriter.writeHeader(env,data, getOutputFullPath()+"/MONTH" + i + ".arff", true);
        }


    }

    private MinMaxValues combineMonthlyResults(MinMaxValues globalResult, MinMaxValues monthResult){
        for (int i = 0; i < globalResult.min.length; i++) {
            if(monthResult.max[i]>globalResult.max[i])
                globalResult.max[i] = monthResult.max[i];
            if(monthResult.min[i]<globalResult.min[i])
                globalResult.min[i] = monthResult.min[i];
        }
        return globalResult;
    }

    static class GetRange extends RichMapPartitionFunction<DynamicDataInstance, MinMaxValues> {

        int numAtts;

        public GetRange(int numAtts_){
            numAtts = numAtts_;
        }
        @Override
        public void mapPartition(Iterable<DynamicDataInstance> values, Collector<MinMaxValues> out){

            double[] min = new double[numAtts];
            double[] max = new double[numAtts];

            for (int i = 0; i < numAtts; i++) {
                min[i] = Double.MAX_VALUE;
                max[i] = Double.MIN_VALUE;
            }

            for(DataInstance instance: values){
                instance.getAttributes().forEach(att -> {
                    if(instance.getValue(att)>max[att.getIndex()])
                        max[att.getIndex()] = instance.getValue(att);
                    if(instance.getValue(att)<min[att.getIndex()])
                        min[att.getIndex()] = instance.getValue(att);
                });
            }

            out.collect(new MinMaxValues(min,max));

        }
    }

    static class ReduceMinMax extends RichReduceFunction<MinMaxValues> {

        @Override
        public MinMaxValues reduce(MinMaxValues set1, MinMaxValues set2) throws Exception {

            double[] min = new double[set1.max.length];
            double[] max = new double[set1.max.length];
            for (int i = 0; i < min.length; i++) {
                if(set1.max[i]>set2.max[i])
                    max[i] = set1.max[i];
                else
                    max[i] = set2.max[i];
                if(set1.min[i]<set2.min[i])
                    min[i] = set1.min[i];
                else
                    min[i] = set2.min[i];

            }
            return new MinMaxValues(min, max);
        }
    }

    public static class MinMaxValues {
        double[] max;
        double[] min;

        public MinMaxValues(double[] min_, double[] max_){
            max = max_;
            min = min_;
        }
    }

    public void learnDynamicNB() throws Exception{

        /*
         * We learn a dynamicNB with flink from CajaMar static data
         */
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        System.out.println(getOutputFullPath() + "/MONTH1.arff");

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,
                getOutputFullPath() + "/MONTH1.arff", true);
        DynamicDAG dynamicDAG = getNaiveBayesStructure(data0.getAttributes());
        dbn = new DynamicBayesianNetwork(dynamicDAG);

        DynamicParallelVB learn = new DynamicParallelVB();
        learn.setMaximumGlobalIterations(10);
        learn.setBatchSize(getBatchSize());
        learn.setDAG(dynamicDAG);
        learn.setOutput(false);
        learn.initLearning();

        System.out.println("--------------- MONTH " + 1 + " --------------------------");
        learn.updateModelWithNewTimeSlice(0, data0);


        for (int i = 2; i <= nMonths; i++) {
            System.out.println("--------------- MONTH " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    getOutputFullPath()+"/MONTH" + i + ".arff" ,true);
            learn.updateModelWithNewTimeSlice(i, dataNew);
        }
        dbn = learn.getLearntDynamicBayesianNetwork();
        System.out.println(dbn);

    }




    public static void main(String[] args) throws Exception{

        GenerateCajaMarData generateData = new GenerateCajaMarData();

        generateData.setOptions(args);

        //generateData.generateData();

        generateData.assignRanges();

        //generateData.learnDynamicNB();
    }

    @Override
    public String listOptions() {
        return  this.classNameID() +",\\"+
                "-s, 100000, Number of samples per file\\" +
                "-numFiles, 10, Number of files\\" +
                "-RscriptsPath, ./, Path for the R script(s)\\" +
                "-outputFullPath, ./, Path for the output files\\" +
                "-includeSocioEconomicVars, false, If set then socioeconomic vars are included and " +
                "data like the SCAI paper is generated.\\"+
                "-windowsSize, 1000, windowsSize for learning the dbn.\\"+
                "-printINDEX, false, print index in attribute arff header\\"+
                "-seed, 0, set seed for data sample generation in Rscript\\"+
                "-addConceptDrift, false, If set add concept drift at months 35 and 60\\"+
                "-addRange, false, If set add range to attributes";
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
        this.setBatchSize(this.getIntOption("-windowsSize"));
        this.setPrintINDEX(this.getBooleanOption("-printINDEX"));
        this.setSeed(this.getIntOption("-seed"));
        this.setAddConceptDrift(getBooleanOption("-addConceptDrift"));
        this.setAddRange(getBooleanOption("-addRange"));
    }
}
