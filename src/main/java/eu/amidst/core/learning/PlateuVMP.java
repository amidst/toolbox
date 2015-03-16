package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 10/03/15.
 */
public class PlateuVMP {
    List<Node> parametersNode;
    List<List<Node>> plateuNodes;
    EF_LearningBayesianNetwork ef_learningmodel;
    int nRepetitions = 100;
    VMP vmp = new VMP();

    Map<Variable, Node> parametersToNode;

    List<Map<Variable, Node>> variablesToNode;


    public VMP getVMP() {
        return vmp;
    }

    public void resetQs() {
        this.vmp.resetQs();
    }

    public void setSeed(int seed) {
        this.vmp.setSeed(seed);
    }

    public EF_LearningBayesianNetwork getEFLearningBN() {
        return ef_learningmodel;
    }

    public void setNRepetitions(int nRepetitions_) {
        this.nRepetitions = nRepetitions_;
    }

    public void runInference() {
        this.vmp.runInference();
    }

    public double getLogProbabilityOfEvidence() {
        return this.vmp.getLogProbabilityOfEvidence();
    }

    public void setPlateuModel(EF_LearningBayesianNetwork model) {
        ef_learningmodel = model;
        parametersNode = new ArrayList();
        plateuNodes = new ArrayList<>(nRepetitions);

        variablesToNode = new ArrayList<>();
        parametersToNode = new ConcurrentHashMap<>();
        parametersNode = ef_learningmodel.getDistributionList()
                .stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    parametersToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < nRepetitions; i++) {

            Map<Variable, Node> map = new ConcurrentHashMap<>();
            List<Node> tmpNodes = ef_learningmodel.getDistributionList().stream()
                    .filter(dist -> !dist.getVariable().isParameterVariable())
                    .map(dist -> {
                        Node node = new Node(dist);
                        map.put(dist.getVariable(), node);
                        return node;
                    })
                    .collect(Collectors.toList());
            this.variablesToNode.add(map);
            plateuNodes.add(tmpNodes);
        }


        for (int i = 0; i < nRepetitions; i++) {
            for (Node node : plateuNodes.get(i)) {
                final int slice = i;
                node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVar(var, slice)).collect(Collectors.toList()));
                node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var, slice).getChildren().add(node));
            }
        }

        List<Node> allNodes = new ArrayList();

        allNodes.addAll(this.parametersNode);

        for (int i = 0; i < nRepetitions; i++) {
            allNodes.addAll(this.plateuNodes.get(i));
        }

        this.vmp.setNodes(allNodes);
    }

    public void setEvidence(List<DataInstance> data) {
        if (data.size()>nRepetitions)
            throw new IllegalArgumentException("The size of the data is bigger that the number of repetitions");

        for (int i = 0; i < nRepetitions && i<data.size(); i++) {
            final int slice = i;
            this.plateuNodes.get(i).forEach(node -> node.setAssignment(data.get(slice)));
        }

        for (int i = data.size(); i < nRepetitions; i++) {
            this.plateuNodes.get(i).forEach(node -> node.setAssignment(null));
        }
    }

    public Node getNodeOfVar(Variable variable, int slice) {
        if (variable.isParameterVariable())
            return this.parametersToNode.get(variable);
        else
            return this.variablesToNode.get(slice).get(variable);
    }

    public <E extends EF_UnivariateDistribution> E getEFParameterPosterior(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E)this.parametersToNode.get(var).getQDist();
    }
}