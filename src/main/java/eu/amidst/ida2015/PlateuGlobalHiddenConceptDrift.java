package eu.amidst.ida2015;

import eu.amidst.core.inference.vmp.Node;
import eu.amidst.core.learning.PlateuStructure;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 10/03/15.
 */
public class PlateuGlobalHiddenConceptDrift extends PlateuStructure{

    Variable globalHiddenVariable;
    Node globalHiddenNode;
    boolean dynamic;
    public PlateuGlobalHiddenConceptDrift(Variable globalHiddenVariable_, boolean dynamic_){
        this.globalHiddenVariable = globalHiddenVariable_;
        this.dynamic = dynamic_;
    }
    public Variable getGlobalHiddenVariable() {
        return globalHiddenVariable;
    }

    public void setGlobalHiddenVariable(Variable globalHiddenVariable) {
        this.globalHiddenVariable = globalHiddenVariable;
    }

    public void replicateModel(){
        parametersNode = new ArrayList();
        plateuNodes = new ArrayList<>(nReplications);

        variablesToNode = new ArrayList<>();
        parametersToNode = new ConcurrentHashMap<>();
        parametersNode = ef_learningmodel.getDistributionList().stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    parametersToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        globalHiddenNode = new Node(ef_learningmodel.getDistribution(globalHiddenVariable));

        for (int i = 0; i < nReplications; i++) {

            Map<Variable, Node> map = new ConcurrentHashMap();
            List<Node> tmpNodes = ef_learningmodel.getDistributionList().stream()
                    .filter(dist -> !dist.getVariable().isParameterVariable())
                    .filter(dist -> !dist.getVariable().equals(globalHiddenVariable))
                    .map(dist -> {
                        Node node = new Node(dist);
                        map.put(dist.getVariable(), node);
                        return node;
                    })
                    .collect(Collectors.toList());

            map.put(globalHiddenVariable, globalHiddenNode);

            this.variablesToNode.add(map);
            plateuNodes.add(tmpNodes);
        }


        for (int i = 0; i < nReplications; i++) {
            for (Node node : plateuNodes.get(i)) {
                final int slice = i;
                node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVar(var, slice)).collect(Collectors.toList()));
                node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var, slice).getChildren().add(node));
            }
        }
        globalHiddenNode.setParents(globalHiddenNode.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVar(var, 0)).collect(Collectors.toList()));
        globalHiddenNode.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var, 0).getChildren().add(globalHiddenNode));

        if (dynamic)
            globalHiddenNode.getParents().stream().forEach(node -> node.setActive(false));

        List<Node> allNodes = new ArrayList();

        allNodes.addAll(this.parametersNode);

        allNodes.add(globalHiddenNode);

        for (int i = 0; i < nReplications; i++) {
            allNodes.addAll(this.plateuNodes.get(i));
        }

        this.vmp.setNodes(allNodes);
    }

}