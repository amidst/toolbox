/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package moa.classifiers.bayes;

import eu.amidst.corestatic.inference.InferenceAlgorithm;
import eu.amidst.corestatic.inference.messagepassing.VMP;
import eu.amidst.moalink.arffWekaReader.DataRowWeka;
import eu.amidst.corestatic.datastream.*;
import eu.amidst.corestatic.datastream.filereaders.DataInstanceImpl;
import eu.amidst.corestatic.distribution.Multinomial;
import eu.amidst.corestatic.distribution.Normal;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.utils.Utils;
import eu.amidst.corestatic.variables.StateSpaceType;
import eu.amidst.corestatic.variables.Variables;
import eu.amidst.corestatic.variables.Variable;
import eu.amidst.corestatic.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.corestatic.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.ida2015.GaussianHiddenTransitionMethod;
import eu.amidst.ida2015.NaiveBayesGaussianHiddenConceptDrift;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.SemiSupervisedLearner;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.options.MultiChoiceOption;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 20/04/15.
 */
public class amidstModels extends AbstractClassifier implements SemiSupervisedLearner{

    private static final long serialVersionUID = 1L;

    double accPerSeq = 0;

    ArrayList<Prediction> predictions = new ArrayList<>();

    int nbatch = 0;
    int sizePerSeq = 0;

    /**
     * Parameters of the amidst model
     */

    String[] driftModes = new String[]{
            "GLOBAL",
            "LOCAL",
            "GLOBAL_LOCAL"};

    public MultiChoiceOption driftDetectorOption = new MultiChoiceOption(
            "driftDetector", 'd', "Drift detector type.", new String[]{
            "GLOBAL", "LOCAL", "GLOBAL_LOCAL"}, new String[]{
            "GLOBAL",
            "LOCAL",
            "GLOBAL_LOCAL"}, 0);

    public IntOption windowSizeOption = new IntOption("windowSize",
            'w', "Size of the window in which to apply variational Bayes",
            100);
    protected  int windowSize_ = 100;

    public FloatOption transitionVarianceOption = new FloatOption("transitionVariance",
            'v', "Transition variance for the global hidden variable.",
            0.1);
    protected double transitionVariance_ = 0.1;

    public FloatOption fadingOption = new FloatOption("fading",
            'f', "Fading.",
            1);
    protected double fading_ = 1;

    public IntOption asNBOption = new IntOption("asNB",
            'n', "If 1 then build plain NB (without hidden)",
            0);
    protected  int asNB_ = 0;

    public IntOption numberOfGlobalVarsOption = new IntOption("numberOfGlobalVars",
            'g', "Number of global variables",
            1);
    protected  int numberOfGlobalVars_ = 1;

/*    public FloatOption MARclassOption = new FloatOption("MARclass",
            'm', "MARclass.",
            1);
    protected double MARclass_ = 1;*/

/*    public MultiChoiceOption semiSupervisedLearnerOption = new MultiChoiceOption(
            "semiSupervisedLearning", 'd', "Perform semi-supervised learning.", new String[]{
            "TRUE", "FALSE"}, new String[]{
            "TRUE",
            "FALSE", 0);*/



    /**
     * Private fields
     */
    //private StreamingVariationalBayesVMP svb_ = null;

    NaiveBayesGaussianHiddenConceptDrift nb_ = null;

    private Variable classVar_ = null;

    private Attributes attributes_ = null;

    private Attribute TIME_ID = null;

    private Attribute SEQUENCE_ID = null;

    private BayesianNetwork learntBN_ = null;


    DataOnMemoryListContainer<DataInstance> batch_ = null;

    private int count_ = 0;

    private int currentTimeID = 0;

    private DataInstance firstInstanceForBatch = null;

    private boolean dynamicFlag = false;

    private double[] meanHiddenVars;
    /**
     * SETTERS AND GETTERS
     */

    public int getWindowSize_() {
        return windowSize_;
    }

    public void setWindowSize_(int windowSize_) {
        this.windowSize_ = windowSize_;
    }

    public double getTransitionVariance_() {
        return transitionVariance_;
    }

    public void setTransitionVariance_(double transitionVariance_) {
        this.transitionVariance_ = transitionVariance_;
    }

    public double getFading_() {
        return fading_;
    }

    public void setFading_(double fading_) {
        this.fading_ = fading_;
    }

    public int getAsNB_() {
        return asNB_;
    }

    public void setAsNB_(int asNB_) {
        this.asNB_ = asNB_;
    }

    public int getNumberOfGlobalVars_() {
        return numberOfGlobalVars_;
    }

    public void setNumberOfGlobalVars_(int numberOfGlobalVars_) {
        this.numberOfGlobalVars_ = numberOfGlobalVars_;
    }

    /*    public double getMARclass_() {
        return MARclass_;
    }

    public void setMARclass_(double MARclass_) {
        this.MARclass_ = MARclass_;
    }*/

    @Override
    public String getPurposeString() {
        return "Amidst concept-drift classifier: performs concept-drift detection with a BN model plus one or several hidden variables.";
    }

    @Override
    public void resetLearningImpl() {

    }

    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);

        nb_ = new NaiveBayesGaussianHiddenConceptDrift();

        convertAttributes();
        batch_ = new DataOnMemoryListContainer(attributes_);

        nb_.setData(batch_);
        nb_.setSeed(randomSeed);//Note that the default value is 1
        nb_.setClassIndex(-1);
        setWindowSize_(windowSizeOption.getValue());
        nb_.setWindowsSize(windowSize_);
        setTransitionVariance_(transitionVarianceOption.getValue());
        nb_.setTransitionVariance(transitionVariance_);
        nb_.setConceptDriftDetector(NaiveBayesGaussianHiddenConceptDrift.DriftDetector.valueOf(this.driftModes[driftDetectorOption.getChosenIndex()]));
        setFading_(fadingOption.getValue());
        nb_.setFading(fading_);
        setAsNB_(asNBOption.getValue());
        if(asNB_ == 1) nb_.setGlobalHidden(false);
        else nb_.setGlobalHidden(true);
        setNumberOfGlobalVars_(numberOfGlobalVarsOption.getValue());
        nb_.setNumberOfGlobalVars(numberOfGlobalVars_);

        nb_.initLearning();

        List<Attribute> attributesExtendedList = new ArrayList<>(attributes_.getList());
        if(TIME_ID != null) {
            attributesExtendedList.add(TIME_ID);
            attributesExtendedList.add(SEQUENCE_ID);
            dynamicFlag = true;
            meanHiddenVars = new double[this.nb_.getHiddenVars().size()];
        }
        attributes_ = new Attributes(attributesExtendedList);
        batch_ = new DataOnMemoryListContainer(attributes_);
        //System.out.println(nb_.getLearntBayesianNetwork().getDAG().toString());

    }

    private void convertAttributes(){
        weka.core.Attribute attrWeka;
        Enumeration attributesWeka = modelContext.enumerateAttributes();
        List<Attribute> attrList = new ArrayList<>();
        /* Predictive attributes */
        while (attributesWeka.hasMoreElements()) {
            attrWeka = (weka.core.Attribute) attributesWeka.nextElement();
            convertAttribute(attrWeka,attrList);
        }
        /* Class attribute */
        convertAttribute(modelContext.classAttribute(), attrList);
        attributes_ = new Attributes(attrList);
        Variables variables = new Variables(attributes_);
        String className = modelContext.classAttribute().name();
        classVar_ = variables.getVariableByName(className);

    }

    private void convertAttribute(weka.core.Attribute attrWeka, List<Attribute> attrList){
        StateSpaceType stateSpaceTypeAtt;
        if(attrWeka.isNominal()){
            String[] vals = new String[attrWeka.numValues()];
            for (int i=0; i<attrWeka.numValues(); i++) {
                vals[i] = attrWeka.value(i);
            }
            stateSpaceTypeAtt = new FiniteStateSpace(attrWeka.numValues());
        }else{
            stateSpaceTypeAtt = new RealStateSpace();
        }
        Attribute att = new Attribute(attrWeka.index(),attrWeka.name(), stateSpaceTypeAtt);
        if(att.getName().equalsIgnoreCase("TIME_ID"))
            TIME_ID = att;
        else if(att.getName().equalsIgnoreCase("SEQUENCE_ID"))
            SEQUENCE_ID = att;
        else
            attrList.add(att);
    }

    @Override
    public void trainOnInstance(Instance inst) {
        boolean isTraining = (inst.weight() > 0.0);
        if (this instanceof SemiSupervisedLearner == false &&
                inst.classIsMissing() == true){
            isTraining = false;
        }
        if (isTraining) {
            this.trainingWeightSeenByModel += inst.weight();
            trainOnInstanceImpl(inst);
        }
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (dynamicFlag)
            trainOnInstanceImplDynamic(inst);
        else
            trainOnInstanceImplStatic(inst);
    }

    public void trainOnInstanceImplStatic(Instance inst) {

        if(firstInstanceForBatch != null) {
            batch_.add(firstInstanceForBatch);
            count_++;
            firstInstanceForBatch = null;
        }

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(inst));
        if(count_ < windowSize_){
                batch_.add(dataInstance);
                count_++;
        }else{
            count_ = 0;
            firstInstanceForBatch = dataInstance;

            double batchAccuracy=nb_.computeAccuracy(nb_.getLearntBayesianNetwork(), batch_);
            nbatch+=windowSize_;
            nb_.updateModel(batch_);
            batch_ = new DataOnMemoryListContainer(attributes_);
            learntBN_ = nb_.getLearntBayesianNetwork();
            //System.out.println(learntBN_.toString());

            System.out.print(nbatch);

            for (Variable hiddenVar : nb_.getHiddenVars()) {
                Normal normal = nb_.getSvb().getPlateuStructure().getEFVariablePosterior(hiddenVar, 0).toUnivariateDistribution();
                System.out.print("\t" + normal.getMean());
            }
            System.out.print("\t" + batchAccuracy);

            double[] param = learntBN_.getParameters();
            for (int i = 0; i < param.length; i++) {
                System.out.print("\t"+param[i]);
            }

            System.out.println();
        }
    }

    public void trainOnInstanceImplDynamic(Instance inst) {

        if(firstInstanceForBatch != null) {
            batch_.add(firstInstanceForBatch);
            count_++;
            firstInstanceForBatch = null;
        }

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(inst));
        if(count_ < windowSize_ && (int)dataInstance.getValue(TIME_ID) == currentTimeID) {
                batch_.add(dataInstance);
                count_++;
        }else{
            boolean isNewSeq = false;
            count_ = 0;
            GaussianHiddenTransitionMethod transitionMethod = nb_.getSvb().getTransitionMethod();
            if((int)dataInstance.getValue(TIME_ID) == currentTimeID) {
                transitionMethod.setTransitionVariance(0.0);
            }else{
                transitionMethod.setTransitionVariance(this.getTransitionVariance_());
                isNewSeq = true;
            }
            firstInstanceForBatch = dataInstance;
            currentTimeID = (int)dataInstance.getValue(TIME_ID);


            double batchAccuracy = computeAccuracyAndRecordPrediction(nb_.getLearntBayesianNetwork(),batch_);

            accPerSeq += batchAccuracy*batch_.getNumberOfDataInstances();


            nbatch+=windowSize_;
            sizePerSeq += batch_.getNumberOfDataInstances();

            nb_.updateModel(batch_);
            batch_ = new DataOnMemoryListContainer(attributes_);
            learntBN_ = nb_.getLearntBayesianNetwork();
            //System.out.println(learntBN_.toString());

            for (int i = 0; i < nb_.getHiddenVars().size(); i++) {
                Normal normal = nb_.getSvb().getPlateuStructure().getEFVariablePosterior(nb_.getHiddenVars().get(i), 0).toUnivariateDistribution();
                meanHiddenVars[i]+=normal.getMean();
            }

            if(isNewSeq) {
                System.out.print(sizePerSeq);

                ThresholdCurve thresholdCurve = new ThresholdCurve();
                Instances tcurve = thresholdCurve.getCurve(predictions);

                for (int i = 0; i < nb_.getHiddenVars().size(); i++) {
                    System.out.print("\t" + meanHiddenVars[i]);
                    meanHiddenVars[i]=0;
                }
                System.out.print("\t" + accPerSeq/sizePerSeq +"\t" + ThresholdCurve.getPRCArea(tcurve) +"\t" + ThresholdCurve.getROCArea(tcurve));

                double[] param = learntBN_.getParameters();
                for (int i = 0; i < param.length; i++) {
                    System.out.print("\t"+param[i]);
                }

                System.out.println();

                predictions = new ArrayList<>();
                accPerSeq = 0.0;
                sizePerSeq = 0;
            }
        }
    }


    public double computeAccuracyAndRecordPrediction(BayesianNetwork bn, DataOnMemory<DataInstance> data){


        double correctPredictions = 0;
        Variable classVariable = bn.getStaticVariables().getVariableById(nb_.getClassIndex());

        VMP vmp = new VMP();
        vmp.setModel(bn);
        for (DataInstance instance : data) {
            double realValue = instance.getValue(classVariable);
            instance.setValue(classVariable, Utils.missingValue());
            vmp.setEvidence(instance);
            vmp.runInference();
            Multinomial posterior = vmp.getPosterior(classVariable);

            if (Utils.maxIndex(posterior.getProbabilities())==realValue)
                correctPredictions++;
            Prediction prediction = new NominalPrediction(realValue, posterior.getProbabilities());
            predictions.add(prediction);

            instance.setValue(classVariable, realValue);
        }

        return correctPredictions/data.getNumberOfDataInstances();

    }

    @Override
    public double[] getVotesForInstance(Instance inst) {

        if(learntBN_ == null) {
            double[] votes = new double[classVar_.getNumberOfStates()];
            for (int i = 0; i < votes.length; i++) {
                votes[i] = 1.0/votes.length;
            }
            return votes;
        }

        InferenceAlgorithm vmp = new VMP();
        vmp.setModel(learntBN_);

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(inst));

        double realValue = dataInstance.getValue(classVar_);
        dataInstance.setValue(classVar_, Utils.missingValue());
        vmp.setEvidence(dataInstance);
        vmp.runInference();
        Multinomial posterior = vmp.getPosterior(classVar_);
        dataInstance.setValue(classVar_, realValue);

        return posterior.getProbabilities();
    }


    @Override
    public void getModelDescription(StringBuilder out, int indent) {

    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    public static void main(String[] args){
    }

}
