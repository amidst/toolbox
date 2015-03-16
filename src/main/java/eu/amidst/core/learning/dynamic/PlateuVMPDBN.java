package eu.amidst.core.learning.dynamic;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 10/03/15.
 */
public class PlateuVMPDBN {
    List<Node> parametersNodeTime0;
    List<Node> parametersNodeTimeT;

    List<Node> nodesTime0;
    List<List<Node>> plateuNodesTimeT;
    List<Node> cloneNodesTimeT;
    DynamicDAG dbnModel;

    EF_LearningBayesianNetwork ef_learningmodelTimeT;
    EF_LearningBayesianNetwork ef_learningmodelTime0;

    int nRepetitions = 100;
    VMP vmpTime0 = new VMP();
    VMP vmpTimeT = new VMP();
    Map<Variable, Node> cloneVariablesToNode;

    Map<Variable, Node> parametersToNodeTimeT;

    Map<Variable, Node> parametersToNodeTime0;

    Map<Variable, Node> variablesToNodeTime0;


    List<Map<Variable, Node>> variablesToNodeTimeT;


    public void resetQs() {
        this.vmpTime0.resetQs();
        this.vmpTimeT.resetQs();
    }

    public void setDBNModel(DynamicDAG dbnModel) {
        this.dbnModel = dbnModel;
    }

    public VMP getVMPTimeT() {
        return vmpTimeT;
    }

    public void setSeed(int seed) {
        this.vmpTime0.setSeed(seed);
        this.vmpTimeT.setSeed(seed);
    }

    public EF_LearningBayesianNetwork getEFLearningBNTimeT() {
        return ef_learningmodelTimeT;
    }

    public EF_LearningBayesianNetwork getEFLearningBNTime0() {
        return ef_learningmodelTime0;
    }

    public void setNRepetitions(int nRepetitions_) {
        this.nRepetitions = nRepetitions_;
    }

    public void runInferenceTimeT() {
        this.vmpTimeT.runInference();
        this.plateuNodesTimeT.get(this.nRepetitions-1).stream().filter(node -> !node.isObserved() && !node.getMainVariable().isParameterVariable()).forEach(node -> {
            Variable temporalClone = this.dbnModel.getDynamicVariables().getTemporalClone(node.getMainVariable());
            moveNodeQDist(this.getNodeOfVarTimeT(temporalClone,0), node);
        });
    }

    public void runInferenceTime0() {
        this.vmpTime0.runInference();
        this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved() && !node.getMainVariable().isParameterVariable()).forEach(node -> {
            Variable temporalClone = this.dbnModel.getDynamicVariables().getTemporalClone(node.getMainVariable());
            moveNodeQDist(this.getNodeOfVarTimeT(temporalClone,0), node);
        });
    }

    private static void moveNodeQDist(Node toTemporalCloneNode, Node fromNode){
        EF_UnivariateDistribution uni = fromNode.getQDist().deepCopy();
        ((EF_BaseDistribution_MultinomialParents)toTemporalCloneNode.getPDist()).setBaseEFDistribution(0,uni);
        toTemporalCloneNode.setQDist(uni);
    }

    public double getLogProbabilityOfEvidenceTimeT() {
        return this.vmpTimeT.getLogProbabilityOfEvidence();
    }

    public double getLogProbabilityOfEvidenceTime0() {
        return this.vmpTime0.getLogProbabilityOfEvidence();
    }

    public void setPlateuModelTime0(EF_LearningBayesianNetwork modelTime0) {
        ef_learningmodelTime0 = modelTime0;
        parametersNodeTime0 = new ArrayList();

        parametersToNodeTime0 = new ConcurrentHashMap<>();
        parametersNodeTime0 = ef_learningmodelTime0.getDistributionList()
                .stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    parametersToNodeTime0.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());
        this.variablesToNodeTime0 =  new ConcurrentHashMap<>();

        this.nodesTime0 = ef_learningmodelTime0.getDistributionList().stream()
                .filter(dist -> !dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    variablesToNodeTime0.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        for (Node node : nodesTime0) {
            node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVarTime0(var)).collect(Collectors.toList()));
            node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVarTime0(var).getChildren().add(node));
        }


        List<Node> allNodesTime0 = new ArrayList();
        allNodesTime0.addAll(this.parametersNodeTime0);
        allNodesTime0.addAll(nodesTime0);
        this.vmpTime0.setNodes(allNodesTime0);

    }

    public void setPlateuModelTimeT(EF_LearningBayesianNetwork modelTimeT) {

        ef_learningmodelTimeT = modelTimeT;
        parametersNodeTimeT = new ArrayList();
        plateuNodesTimeT = new ArrayList(nRepetitions);
        cloneNodesTimeT = new ArrayList();
        variablesToNodeTimeT = new ArrayList();
        parametersToNodeTimeT = new ConcurrentHashMap<>();


        parametersNodeTimeT = ef_learningmodelTimeT.getDistributionList()
                .stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    parametersToNodeTimeT.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());



        cloneVariablesToNode = new ConcurrentHashMap<>();
        cloneNodesTimeT = ef_learningmodelTimeT.getDistributionList()
                .stream()
                .filter(dist -> !dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Variable temporalClone = this.dbnModel.getDynamicVariables().getTemporalClone(dist.getVariable());
                    EF_UnivariateDistribution uni = temporalClone.getDistributionType().newUnivariateDistribution().toEFUnivariateDistribution();

                    EF_ConditionalDistribution pDist = new EF_BaseDistribution_MultinomialParents(new ArrayList<Variable>(),
                            Arrays.asList(uni));

                    Node node = new Node(pDist);
                    node.setActive(false);
                    cloneVariablesToNode.put(temporalClone, node);
                    return node;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < nRepetitions; i++) {

            Map<Variable, Node> map = new ConcurrentHashMap<>();
            List<Node> tmpNodes = ef_learningmodelTimeT.getDistributionList().stream()
                    .filter(dist -> !dist.getVariable().isParameterVariable())
                    .map(dist -> {
                        ModifiedConditionalDistribution newDist = new ModifiedConditionalDistribution(dist, this.dbnModel.getDynamicVariables());
                        Node node = new Node(newDist);
                        map.put(dist.getVariable(), node);
                        return node;
                    })
                    .collect(Collectors.toList());
            this.variablesToNodeTimeT.add(map);
            plateuNodesTimeT.add(tmpNodes);
        }


        for (int i = 0; i < nRepetitions; i++) {
            for (Node node : plateuNodesTimeT.get(i)) {
                final int slice = i;
                node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVarTimeT(var, slice)).collect(Collectors.toList()));
                node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVarTimeT(var, slice).getChildren().add(node));
            }
        }

        List<Node> allNodesTimeT = new ArrayList();

        allNodesTimeT.addAll(this.parametersNodeTimeT);

        for (int i = 0; i < nRepetitions; i++) {
            allNodesTimeT.addAll(this.plateuNodesTimeT.get(i));
        }

        this.vmpTimeT.setNodes(allNodesTimeT);
    }

    public void setEvidenceTime0(DynamicDataInstance data) {
        this.vmpTime0.setEvidence(data);
    }

    public void setEvidenceTimeT(List<DynamicDataInstance> data) {
        if (data.size()>nRepetitions)
            throw new IllegalArgumentException("The size of the data is bigger that the number of repetitions");

        this.cloneNodesTimeT.forEach( node -> node.setAssignment(data.get(0)));

        for (int i = 0; i < nRepetitions && i<data.size(); i++) {
            final int slice = i;
            this.plateuNodesTimeT.get(i).forEach(node -> node.setAssignment(data.get(slice)));
        }

        for (int i = data.size(); i < nRepetitions; i++) {
            this.plateuNodesTimeT.get(i).forEach(node -> node.setAssignment(null));
        }
    }

    public Node getNodeOfVarTime0(Variable variable) {
        if (variable.isParameterVariable()) {
            return this.parametersToNodeTime0.get(variable);
        }else{
            return this.variablesToNodeTime0.get(variable);
        }
    }

    public Node getNodeOfVarTimeT(Variable variable, int slice) {
        if (variable.isParameterVariable()) {
            return this.parametersToNodeTimeT.get(variable);
        } else if (!variable.isTemporalClone()){
            return this.variablesToNodeTimeT.get(slice).get(variable);
        }else if (variable.isTemporalClone() && slice>0){
            return this.variablesToNodeTimeT.get(slice - 1).get(this.dbnModel.getDynamicVariables().getVariableFromTemporalClone(variable));
        }else if (variable.isTemporalClone() && slice==0){
            return this.cloneVariablesToNode.get(variable);
        }else{
            throw new IllegalArgumentException();
        }
    }

    public <E extends EF_UnivariateDistribution> E getEFParameterPosteriorTimeT(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E)this.parametersToNodeTimeT.get(var).getQDist();
    }

    public <E extends EF_UnivariateDistribution> E getEFParameterPosteriorTime0(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E)this.parametersToNodeTime0.get(var).getQDist();
    }


    private static class ModifiedConditionalDistribution extends EF_ConditionalLearningDistribution{

        EF_ConditionalLearningDistribution dist;


        public ModifiedConditionalDistribution(EF_ConditionalLearningDistribution dist_, DynamicVariables dynamicVariables){

            this.dist=dist_;

            parents = new ArrayList<>();

            for (Variable var: dist_.getConditioningVariables()){
                if (!var.isTemporalClone()) {
                    parents.add(var);
                }else{
                    parents.add(dynamicVariables.getVariableFromTemporalClone(var));
                }
            }
        }

        @Override
        public List<Variable> getParameterParentVariables() {
            return dist.getParameterParentVariables();
        }


        @Override
        public Variable getVariable() {
            return dist.getVariable();
        }

        @Override
        public NaturalParameters getNaturalParameters() {
            return dist.getNaturalParameters();
        }


        @Override
        public MomentParameters getMomentParameters() {
            return dist.getMomentParameters();
        }

        @Override
        public void setNaturalParameters(NaturalParameters parameters) {
            dist.setNaturalParameters(parameters);
        }


        @Override
        public void setMomentParameters(SufficientStatistics parameters) {
            dist.setMomentParameters(parameters);
        }

        @Override
        public void setMomentParameters(MomentParameters parameters) {
            dist.setMomentParameters(parameters);
        }

        @Override
        public List<Variable> getConditioningVariables() {
            return this.parents;
        }


        @Override
        public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedParameters) {
            return dist.toConditionalDistribution(expectedParameters);
        }

        @Override
        public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
            return dist.getExpectedLogNormalizer(parent,momentChildCoParents);
        }

        @Override
        public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
            return dist.getExpectedLogNormalizer(momentParents);
        }

        @Override
        public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
            return dist.getExpectedNaturalFromParents(momentParents);
        }

        @Override
        public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
            return dist.getExpectedNaturalToParent(parent, momentChildCoParents);
        }

        @Override
        public void updateNaturalFromMomentParameters() {
            dist.updateNaturalFromMomentParameters();
        }

        @Override
        public void updateMomentFromNaturalParameters() {
            dist.updateMomentFromNaturalParameters();
        }

        @Override
        public SufficientStatistics getSufficientStatistics(Assignment data) {
            return dist.getSufficientStatistics(data);
        }

        @Override
        public int sizeOfSufficientStatistics() {
            return dist.sizeOfSufficientStatistics();
        }

        @Override
        public double computeLogBaseMeasure(Assignment dataInstance) {
            return dist.computeLogBaseMeasure(dataInstance);
        }

        @Override
        public double computeLogNormalizer() {
            return dist.computeLogNormalizer();
        }

        @Override
        public Vector createZeroedVector() {
            return dist.createZeroedVector();
        }
    }

}