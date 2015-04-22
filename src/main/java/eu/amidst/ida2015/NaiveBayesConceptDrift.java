package eu.amidst.ida2015;

import eu.amidst.core.datastream.*;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.InferenceEngineForBN;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 21/4/15.
 */
public class NaiveBayesConceptDrift {

    public enum DriftDetector {GLOBAL, LOCAL, GLOBAL_LOCAL};

    DataStream<DataInstance> data;
    int windowsSize;
    double transitionVariance;
    int classIndex = -1;
    DriftDetector conceptDriftDetector;
    int seed = 0;
    StreamingVariationalBayesVMP svb;
    List<Variable> hiddenVars;


    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    public void setData(DataStream<DataInstance> data) {
        this.data = data;
    }

    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
    }

    public void setTransitionVariance(double transitionVariance) {
        this.transitionVariance = transitionVariance;
    }

    public void setConceptDriftDetector(DriftDetector conceptDriftDetector) {
        this.conceptDriftDetector = conceptDriftDetector;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public StreamingVariationalBayesVMP getSvb() {
        return svb;
    }

    private void buildGlobalDAG(){
        StaticVariables variables = new StaticVariables(data.getAttributes());
        String className = data.getAttributes().getList().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        Variable globalHidden  = variables.newGaussianVariable("GlobalHidden");
        hiddenVars.add(globalHidden);

        Variable classVariable = variables.getVariableByName(className);

        DAG dag = new DAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            dag.getParentSet(variable).addParent(globalHidden);
        }

        System.out.println(dag.toString());

        svb = new StreamingVariationalBayesVMP();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuLocalHiddenConceptDrift(hiddenVars, true));
        svb.setTransitionMethod(new LocalHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance));
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);
        svb.initLearning();
    }

    private void buildLocalDAG(){

        StaticVariables variables = new StaticVariables(data.getAttributes());
        String className = data.getAttributes().getList().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        for (Attribute att : data.getAttributes()){
            if (att.getName().equals(className))
                continue;
            hiddenVars.add(variables.newGaussianVariable("Local_"+att.getName()));
        }

        Variable classVariable = variables.getVariableByName(className);

        DAG dag = new DAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            dag.getParentSet(variable).addParent(variables.getVariableByName("Local_"+att.getName()));
        }

        System.out.println(dag.toString());

        svb = new StreamingVariationalBayesVMP();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuLocalHiddenConceptDrift(hiddenVars, true));
        svb.setTransitionMethod(new LocalHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance));
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);
        svb.initLearning();
    }

    private void buildGlobalLocalDAG(){

        StaticVariables variables = new StaticVariables(data.getAttributes());
        String className = data.getAttributes().getList().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        Variable globalHidden  = variables.newGaussianVariable("GlobalHidden");
        hiddenVars.add(globalHidden);

        for (Attribute att : data.getAttributes()){
            if (att.getName().equals(className))
                continue;
            hiddenVars.add(variables.newGaussianVariable("Local_"+att.getName()));
        }

        Variable classVariable = variables.getVariableByName(className);

        DAG dag = new DAG(variables);

        for (Attribute att : data.getAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            dag.getParentSet(variable).addParent(classVariable);
            dag.getParentSet(variable).addParent(variables.getVariableByName("Local_"+att.getName()));
            dag.getParentSet(variable).addParent(globalHidden);

        }

        System.out.println(dag.toString());

        svb = new StreamingVariationalBayesVMP();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuLocalHiddenConceptDrift(hiddenVars, true));
        svb.setTransitionMethod(new LocalHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance));
        svb.setWindowsSize(this.windowsSize);
        svb.setDAG(dag);
        svb.initLearning();
    }

    public void learnDAG() {
        if (classIndex == -1)
            classIndex = data.getAttributes().getNumberOfAttributes()-1;


        switch (this.conceptDriftDetector){
            case GLOBAL:
                this.buildGlobalDAG();
                break;
            case LOCAL:
                this.buildLocalDAG();
                break;
            case GLOBAL_LOCAL:
                this.buildGlobalLocalDAG();
                break;
        }


        System.out.print("Sample");
        for (Variable hiddenVar : this.hiddenVars) {
            System.out.print("\t" + hiddenVar.getName());
        }
        System.out.println();
    }

    public void updateModel(DataOnMemory<DataInstance> batch){
        svb.updateModel(batch);
    }

    void learnModel() {
        if (classIndex == -1)
            classIndex = data.getAttributes().getNumberOfAttributes()-1;


        switch (this.conceptDriftDetector){
            case GLOBAL:
                this.buildGlobalDAG();
                break;
            case LOCAL:
                this.buildLocalDAG();
                break;
            case GLOBAL_LOCAL:
                this.buildGlobalLocalDAG();
                break;
        }


        System.out.print("Sample");
        for (Variable hiddenVar : this.hiddenVars) {
            System.out.print("\t" + hiddenVar.getName());
        }
        System.out.println();


        int count = windowsSize;
        double avACC = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowsSize)) {

            double accuracy = computeAccuracy(svb.getLearntBayesianNetwork(), batch);
            svb.updateModel(batch);

            System.out.print(count);

            for (Variable hiddenVar : this.hiddenVars) {
                Normal normal = svb.getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                System.out.print("\t" + normal.getMean());
            }
            System.out.print("\t" + accuracy);
            System.out.println();

            count += windowsSize;
            avACC+= accuracy;
        }

        System.out.println("Average Accuracy: " + avACC/(count/windowsSize));

    }

    public BayesianNetwork getLearntBayesianNetwork(){
        return svb.getLearntBayesianNetwork();
    }

    private double computeAccuracy(BayesianNetwork bn, DataOnMemory<DataInstance> data){

        Variable classVariable = bn.getStaticVariables().getVariableById(classIndex);
        double predictions = 0;
        InferenceEngineForBN.setModel(bn);
        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVariable);
            instance.setValue(classVariable, Utils.missingValue());
            InferenceEngineForBN.setEvidence(instance);
            InferenceEngineForBN.runInference();
            Multinomial posterior = InferenceEngineForBN.getPosterior(classVariable);
            if (Utils.maxIndex(posterior.getProbabilities())==realValue)
                predictions++;

            instance.setValue(classVariable, realValue);
        }

        return predictions/data.getNumberOfDataInstances();
    }



    public static void main(String[] args) {

        DataStream<DataInstance> data = DataStreamLoader.loadFromFile("./IDA2015/DriftSets/hyperplane9.arff");
        NaiveBayesConceptDrift nb = new NaiveBayesConceptDrift();
        nb.setClassIndex(-1);
        nb.setData(data);
        nb.setWindowsSize(100);
        nb.setTransitionVariance(0.1);
        nb.setConceptDriftDetector(DriftDetector.GLOBAL_LOCAL);

        nb.learnModel();

    }
}
