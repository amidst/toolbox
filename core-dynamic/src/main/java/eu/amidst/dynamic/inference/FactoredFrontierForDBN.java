package eu.amidst.dynamic.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.learning.dynamic.DynamicNaiveBayesClassifier;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 10/11/15.
 */
public class FactoredFrontierForDBN  implements InferenceAlgorithmForDBN {

    private InferenceAlgorithm infAlgTime0;
    private InferenceAlgorithm infAlgTimeT;

    private DynamicBayesianNetwork model;

    // If it is Importance Sampling, data should not be kept on memory

    private DynamicAssignment assignment = new HashMapDynamicAssignment(0);

    private long timeID;
    private long sequenceID;

    public FactoredFrontierForDBN(InferenceAlgorithm inferenceAlgorithm){
        infAlgTime0 = inferenceAlgorithm;
        infAlgTimeT = Serialization.deepCopy(inferenceAlgorithm);
        timeID = -1;
        this.setSeed(0);
    }

    public void setSeed(int seed) {
        infAlgTime0.setSeed(seed);
        infAlgTimeT.setSeed(seed);
    }

    /*
     * Return all non-observed and temporally connected variables for Time T
     */
    private List<Variable> getTargetVarsTimeT(){
        return this.model.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(var -> !var.isInterfaceVariable())
                .filter(var -> Utils.isMissingValue(this.assignment.getValue(var)))
                .filter(var -> this.model.getDynamicDAG().getParentSetTimeT(var).contains(var.getInterfaceVariable()))
                .collect(Collectors.toList());
    }

    /*
     * Return all non-observed variables for Time 0
     */
    private List<Variable> getTargetVarsTime0(){
        return this.model.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(var -> Utils.isMissingValue(this.assignment.getValue(var)))
                .collect(Collectors.toList());
    }

    @Override
    public void runInference() {

        if (this.timeID==-1 && assignment.getTimeID()>0) {
            this.infAlgTime0.setEvidence(null);
            this.infAlgTime0.runInference();
            this.timeID=0;
            this.getTargetVarsTime0().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTime0,this.infAlgTimeT,var));
        }

        if (assignment.getTimeID()==0) {
            this.infAlgTime0.setEvidence(this.assignment);
            this.infAlgTime0.runInference();
            this.timeID=0;
            this.getTargetVarsTime0().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTime0,this.infAlgTimeT,var));

        }else{
            //If there is a missing instance
            if ((this.assignment.getTimeID() - this.timeID)>1)
                this.moveWindow((int)(this.assignment.getTimeID() - this.timeID - 1));

            this.timeID=this.assignment.getTimeID();
            this.infAlgTimeT.setEvidence(this.assignment);
            this.infAlgTimeT.runInference();
            this.getTargetVarsTimeT().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTimeT,this.infAlgTimeT,var));
        }
    }

    private void moveNodeQDist(InferenceAlgorithm InfAlgfrom, InferenceAlgorithm InfAlgto, Variable var){

        //Recover original model and do the copy, then set again.
        Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(var);
        Variable staticTemporalClone = InfAlgto.getOriginalModel().getVariables().getVariableByName(temporalClone.getName());
        UnivariateDistribution posteriorDist = InfAlgfrom.getPosterior(var).deepCopy(staticTemporalClone);
        InfAlgto.getOriginalModel().setConditionalDistribution(staticTemporalClone,posteriorDist);
        InfAlgto.setModel(InfAlgto.getOriginalModel());
    }

    private void moveWindow(int nsteps){
        //The first step we need to manually move the evidence from master to clone variables.
        HashMapDynamicAssignment newassignment =null;

        if (this.assignment!=null) {
            newassignment=new HashMapDynamicAssignment(this.model.getNumberOfDynamicVars());
            for (Variable var : this.model.getDynamicVariables()) {
                newassignment.setValue(this.model.getDynamicVariables().getInterfaceVariable(var), this.assignment.getValue(var));
                newassignment.setValue(var, Utils.missingValue());
            }
        }

        for (int i = 0; i < nsteps; i++) {
            this.infAlgTimeT.setEvidence(newassignment);
            this.infAlgTimeT.runInference();
            this.getTargetVarsTimeT().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTimeT,this.infAlgTimeT,var));
            newassignment=null;
        }
    }

    @Override
    public void setModel(DynamicBayesianNetwork model_) {
        this.model = model_;
        this.infAlgTime0.setModel(this.model.toBayesianNetworkTime0());
        this.infAlgTimeT.setModel(this.model.toBayesianNetworkTimeT());
    }

    @Override
    public DynamicBayesianNetwork getOriginalModel() {
        return this.model;
    }

    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {
        if (this.sequenceID!= -1 && this.sequenceID != assignment_.getSequenceID())
            throw new IllegalArgumentException("The sequence ID does not match. If you want to change the sequence, invoke reset method");

        if (this.timeID>= assignment_.getTimeID())
            throw new IllegalArgumentException("The provided assignment is not posterior to the previous provided assignment.");

        this.assignment = assignment_;
    }

    @Override
    public void reset() {
        this.timeID = -1;
        this.sequenceID = -1;

        this.resetInfAlgorithms();
    }

    private void resetInfAlgorithms(){

        this.infAlgTime0.setModel(this.model.toBayesianNetworkTime0());
        this.infAlgTimeT.setModel(this.model.toBayesianNetworkTimeT());
    }

    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        return (getTimeIDOfPosterior()==0)? this.infAlgTime0.getPosterior(var): this.infAlgTimeT.getPosterior(var);
    }

    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {
        if (timeID==-1){
            this.infAlgTime0.setEvidence(null);
            this.infAlgTime0.runInference();
            this.getTargetVarsTimeT().stream()
                    .forEach(v -> moveNodeQDist(this.infAlgTimeT,this.infAlgTimeT,v));

            this.moveWindow(nTimesAhead-1);
            E resultQ = this.getFilteredPosterior(var);
            this.resetInfAlgorithms();

            return resultQ;
        }else {

            Map<Variable, UnivariateDistribution> map = new HashMap<>();

            //Create at copy of Qs
            this.getTargetVarsTimeT().stream()
                    .forEach(v -> map.put(v,this.infAlgTimeT.getPosterior(var)));

            this.moveWindow(nTimesAhead);
            E resultQ = this.getFilteredPosterior(var);

            //Come to the original state
            map.entrySet().forEach(e -> this.infAlgTimeT.getOriginalModel().setConditionalDistribution(e.getKey(),e.getValue()));
            this.infAlgTimeT.setModel(this.infAlgTimeT.getOriginalModel());

            return resultQ;
        }
    }

    @Override
    public long getTimeIDOfLastEvidence() {
        return this.assignment.getTimeID();
    }

    @Override
    public long getTimeIDOfPosterior() {
        return this.timeID;
    }

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {


        /************** BANK DATA **************/

        String file = "./datasets/bank_data_train_tiny.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);//We set -3 to account for time id and seq_id
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork bn = model.getDynamicBNModel();

        System.out.println(bn.toString());


        file = "./datasets/bank_data_predict.arff";
        DataStream<DynamicDataInstance> dataPredict = DynamicDataStreamLoader.loadFromFile(file);

        Variable targetVar = bn.getDynamicVariables().getVariableByName("DEFAULT");

        /************************************/

        UnivariateDistribution dist = null;
        UnivariateDistribution distAhead = null;
        //AtomicInteger countRightPred = new AtomicInteger();


        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new DynamicVMP());
        InferenceEngineForDBN.setModel(bn);

        System.out.println("VMP");
        for(DynamicDataInstance instance: dataPredict){

            if (instance.getTimeID()==0 && dist != null) {
                System.out.println("\nNew sequence #"+instance.getSequenceID());
                InferenceEngineForDBN.reset();
            }
            //double trueClass = instance.getValue(targetVar);
            instance.setValue(targetVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(targetVar);
            /* Get predicted class
            Double[] doubleArray = ArrayUtils.toObject(dist.getParameters());
            List<Double> doubleArr = Arrays.asList(doubleArray);
            IntStream.range(0, doubleArr.size())
                    .reduce((a,b)->doubleArr.get(a)<doubleArr.get(b)? b: a)
                    .ifPresent(ix->{
                        System.out.println("max index = "+ix); if(ix==trueClass) countRightPred.getAndIncrement();});
            */
            System.out.println(dist.toString());
            distAhead = InferenceEngineForDBN.getPredictivePosterior(targetVar,1);
            System.out.println(distAhead.toString());
        }
        //System.out.println("Right predictions for VMP = "+countRightPred.get());


        System.out.println("Importance Sampling");

        ImportanceSampling importanceSampling = new ImportanceSampling();
        importanceSampling.setKeepDataOnMemory(false);
        FactoredFrontierForDBN FFalgorithm = new FactoredFrontierForDBN(importanceSampling);
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);
        InferenceEngineForDBN.setModel(bn);
        dist=null;
        //countRightPred.set(0);

        for(DynamicDataInstance instance: dataPredict){

            if (instance.getTimeID()==0 && dist != null) {
                System.out.println("\nNew sequence #"+instance.getSequenceID());
                InferenceEngineForDBN.reset();
            }
            //double trueClass = instance.getValue(targetVar);
            instance.setValue(targetVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(targetVar);
            /*
            Double[] doubleArray = ArrayUtils.toObject(dist.getParameters());
            List<Double> doubleArr = Arrays.asList(doubleArray);
            IntStream.range(0, doubleArr.size())
                    .reduce((a,b)->doubleArr.get(a)<doubleArr.get(b)? b: a)
                    .ifPresent(ix->{
                        System.out.println("max index = "+ix); if(ix==trueClass) countRightPred.getAndIncrement();});
            */
            System.out.println(dist.toString());
            distAhead = InferenceEngineForDBN.getPredictivePosterior(targetVar,1);
            System.out.println(distAhead.toString());
        }
        //System.out.println("Right predictions for IS = "+countRightPred.get());


    }
}
