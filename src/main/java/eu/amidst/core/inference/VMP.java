package eu.amidst.core.inference;

import eu.amidst.core.distribution.DistributionBuilder;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_DistributionBuilder;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.inference.VMP_.Message;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public class VMP implements InferenceAlgorithmForBN {

    BayesianNetwork model;
    EF_BayesianNetwork ef_model;
    Assignment assignment = new HashMapAssignment(0);
    List<Node> nodes;

    @Override
    public void compileModel() {
        if (assignment != null) {
            nodes.stream().forEach(node -> node.setAssignment(assignment));
        }

        boolean convergence = false;
        double elbo = 0;
        while (!convergence) {
            //Send and combine messages
            Map<Variable, Optional<Message<NaturalParameters>>> group = nodes.stream()
                    .flatMap(node -> node.computeMessages())
                    .collect(
                            Collectors.groupingBy(Message::getVariable,
                                    Collectors.reducing(Message::combine))
                    );

            //Set Messages
            group.entrySet().stream().forEach(e ->
                    nodes.get(e.getKey().getVarID()).updateCombinedMessage(e.getValue().get()));


            //Test whether all nodes are done.
            boolean notAllDone = nodes.stream().filter(e -> !e.isDone()).findFirst().isPresent();
            if (!notAllDone) {
                convergence = true;
                break;
            }

            //Compute lower-bound
            double newelbo = this.nodes.stream().mapToDouble(Node::computeELBO).sum();
            if (Math.abs(newelbo - elbo) < 0.001) {
                convergence = true;
            }
            elbo = newelbo;
        }
    }

    @Override
    public void setModel(BayesianNetwork model_) {
        model = model_;
        ef_model = new EF_BayesianNetwork(this.model);

        nodes = ef_model.getDistributionList()
                .stream()
                .map(dist -> new Node(dist))
                .collect(Collectors.toList());


        for (Node node : nodes){
            node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> nodes.get(var.getVarID())).collect(Collectors.toList()));
        }

    }

    @Override
    public void setEvidence(Assignment assignment_) {
        this.assignment = assignment_;
    }

    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
        return (E) EF_DistributionBuilder.toUnivariateDistribution(this.nodes.get(var.getVarID()).getQDist());
    }


    public static void main(String[] arguments) {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A",2);
        Variable varB = variables.addHiddenMultionomialVariable("B",2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        System.out.println(bn.toString());

        HashMapAssignment assignment = new HashMapAssignment(1);

        InferenceEngineForBN.setInferenceAlgorithmForBN(new VMP());
        InferenceEngineForBN.setModel(bn);
        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Multinomial postB = InferenceEngineForBN.getPosterior(varB);

        System.out.println("P(B) = " + postB.toString());

    }
}