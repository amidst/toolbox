package moa.classifiers.bayes;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataInstanceImpl;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.moalink.converterFromMoaToAmidst.Converter;
import eu.amidst.moalink.converterFromMoaToAmidst.DataRowWeka;
import moa.classifiers.AbstractClassifier;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.options.FlagOption;
import moa.options.IntOption;
import weka.core.Instance;

import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 18/06/15.
 */
public class AmidstClassifier extends AbstractClassifier {

    public IntOption batchSizeOption = new IntOption("batchSize",
            'w', "Size of the batch in which to perform learning (significant if hidden variables)",
            100);

    public int getBatchSize_() {
        return batchSize_;
    }

    public void setBatchSize_(int batchSize_) {
        this.batchSize_ = batchSize_;
    }

    public IntOption nOfGaussianHiddenVarsOption = new IntOption("nOfGaussHiddenVars",
            'g', "Number of Gaussian hidden super-parent variables",
            0);

    public int getnOfGaussianHiddenVars_() {
        return nOfGaussianHiddenVars_;
    }

    public void setnOfGaussianHiddenVars_(int nOfGaussianHiddenVars_) {
        this.nOfGaussianHiddenVars_ = nOfGaussianHiddenVars_;
    }

    public IntOption nOfStatesMultHiddenVarOption = new IntOption("nOfStatesMultHiddenVar",
            'm', "Number of states for the multinomial hidden super-parent variable",
            0);

    public int getnOfStatesMultHiddenVar_() {
        return nOfStatesMultHiddenVar_;
    }

    public void setnOfStatesMultHiddenVar_(int nOfStatesMultHiddenVar_) {
        this.nOfStatesMultHiddenVar_ = nOfStatesMultHiddenVar_;
    }

    public FlagOption parallelModeOption = new FlagOption("parallelMode", 'p',
            "Learn parameters in parallel mode when possible (e.g. ML)");

    public boolean isParallelMode_() {
        return parallelMode_;
    }

    public void setParallelMode_(boolean parallelMode_) {
        this.parallelMode_ = parallelMode_;
    }

    protected  int batchSize_ = 100;

    protected  int nOfGaussianHiddenVars_ = 0;

    protected  int nOfStatesMultHiddenVar_ = 0;

    protected boolean parallelMode_ = false;


    private DAG dag = null;
    private Variable classVar_;
    private DataOnMemoryListContainer<DataInstance> batch_;
    private ParameterLearningAlgorithm parameterLearningAlgorithm_;
    private BayesianNetwork bnModel_;
    InferenceAlgorithm predictions_;
    Attributes attributes_;


    @Override
    public void resetLearningImpl() {

    }

    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);

        setBatchSize_(batchSizeOption.getValue());
        setnOfGaussianHiddenVars_(nOfGaussianHiddenVarsOption.getValue());
        setnOfStatesMultHiddenVar_(nOfStatesMultHiddenVarOption.getValue());
        setParallelMode_(parallelModeOption.isSet());

        attributes_ = Converter.convertAttributes(this.modelContext);
        Variables modelHeader = new Variables(attributes_);
        classVar_ = modelHeader.getVariableByName(modelContext.classAttribute().name());

        batch_ = new DataOnMemoryListContainer(attributes_);
        predictions_ = new VMP();
        predictions_.setSeed(this.randomSeed);

        /* Create hidden variables*/

        if(getnOfGaussianHiddenVars_() > 0)
            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1).forEach(i -> modelHeader.newGaussianVariable("HiddenG_" + i));
        if(getnOfStatesMultHiddenVar_() > 0)
            modelHeader.newMultionomialVariable("HiddenM", getnOfStatesMultHiddenVar_());

        dag = new DAG(modelHeader);

        /* Set DAG structure */
        /* 1.- Add classVar as parent of all hidden variables*/
        if(getnOfGaussianHiddenVars_() > 0)
            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1).parallel()
                    .forEach(hv -> dag.getParentSet(modelHeader.getVariableByName("HiddenG_" + hv)).addParent(classVar_));
        if(getnOfStatesMultHiddenVar_() > 0)
            dag.getParentSet(modelHeader.getVariableByName("HiddenM")).addParent(classVar_);

        /* 2.- Add classVar and all hidden variables as parents of all predictive attributes */
        /*     Note however that Gaussian hidden variables are only parents of Gaussian predictive attributes*/
        if (isParallelMode_()) {
            dag.getParentSets().parallelStream()
                    .filter(w -> w.getMainVar().getVarID() != classVar_.getVarID())
                    .filter(w -> w.getMainVar().isObservable())
                    .forEach(w -> {
                        w.addParent(classVar_);
                        if (getnOfStatesMultHiddenVar_() != 0)
                            w.addParent(modelHeader.getVariableByName("HiddenM"));
                        if (w.getMainVar().isNormal() && getnOfGaussianHiddenVars_() != 0)
                            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1).parallel()
                                    .forEach(hv -> w.addParent(modelHeader.getVariableByName("HiddenG_" + hv)));
                    });

        }
        else {
            dag.getParentSets().stream()
                    .filter(w -> w.getMainVar().getVarID() != classVar_.getVarID())
                    .filter(w -> w.getMainVar().isObservable())
                    .forEach(w -> {
                        w.addParent(classVar_);
                        if (getnOfStatesMultHiddenVar_() != 0)
                            w.addParent(modelHeader.getVariableByName("HiddenM"));
                        if (w.getMainVar().isNormal() && getnOfGaussianHiddenVars_() != 0)
                            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1)
                                    .forEach(hv -> w.addParent(modelHeader.getVariableByName("HiddenG_" + hv)));});
        }

        System.out.println(dag.toString());

        /*
        if(getnOfStatesMultHiddenVar_() == 0 && getnOfGaussianHiddenVars_() == 0){   //ML can be used when Lapalace is introduced
            parameterLearningAlgorithm_ = new MaximumLikelihood();
        }else
            parameterLearningAlgorithm_ = new StreamingVariationalBayesVMP();
            */
        parameterLearningAlgorithm_ = new StreamingVariationalBayesVMP();
    }

    @Override
    public void trainOnInstanceImpl(Instance instance) {
        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(instance, this.attributes_));
        if(batch_.getNumberOfDataInstances() < getBatchSize_()-1) {  //store
            batch_.add(dataInstance);
        }else{                                                  //store & learn
            batch_.add(dataInstance);
            if(bnModel_==null) {
                //parameterLearningAlgorithm_.setParallelMode(isParallelMode_());
                parameterLearningAlgorithm_.setDAG(dag);
                parameterLearningAlgorithm_.initLearning();
                parameterLearningAlgorithm_.updateModel(batch_);
            }else{
                parameterLearningAlgorithm_.updateModel(batch_);
            }
            bnModel_ = parameterLearningAlgorithm_.getLearntBayesianNetwork();
            predictions_.setModel(bnModel_);
            batch_ = new DataOnMemoryListContainer(attributes_);
        }
    }

    @Override
    public double[] getVotesForInstance(Instance instance) {

        if(bnModel_ == null) {
            return new double[0];
        }

        DataInstance dataInstance = new DataInstanceImpl(new DataRowWeka(instance, this.attributes_));
        double realValue = dataInstance.getValue(classVar_);
        dataInstance.setValue(classVar_, Utils.missingValue());
        this.predictions_.setEvidence(dataInstance);
        this.predictions_.runInference();
        Multinomial multinomial = this.predictions_.getPosterior(classVar_);
        dataInstance.setValue(classVar_, realValue);
        return multinomial.getProbabilities();
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }
}
