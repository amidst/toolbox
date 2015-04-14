package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.inference.vmp.Node;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 10/03/15.
 */
public class PlateuIIDReplication extends PlateuStructure{

    public void replicateModel(){
        parametersNode = new ArrayList();
        plateuNodes = new ArrayList<>(nRepetitions);

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

}