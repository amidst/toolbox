package eu.amidst.core.inference;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP_.Message;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import org.apache.commons.lang.time.StopWatch;
import scala.tools.cmd.gen.AnyVals;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
        double elbo = Double.NEGATIVE_INFINITY;
        while (!convergence) {
            //System.out.println(nodes.get(0).getQDist().getMomentParameters().get(0));
            //System.out.println(nodes.get(1).getQDist().getMomentParameters().get(1));

            boolean done = true;
            for (Node node : nodes) {
                Map<Variable, MomentParameters> momentParents = new HashMap<>();

                node.getParents().stream().forEach(p -> momentParents.put(p.getMainVariable(), p.getQMomentParameters()));

                momentParents.put(node.getMainVariable(), node.getQMomentParameters());

                Message<NaturalParameters> selfMessage = node.newSelfMessage(momentParents);

                for (Node children: node.getChildren()){
                    Map<Variable, MomentParameters> momentChildCoParents = new HashMap<>();
                    children.getParents().stream().forEach(p -> momentChildCoParents.put(p.getMainVariable(), p.getQMomentParameters()));
                    momentChildCoParents.put(children.getMainVariable(), children.getQMomentParameters());
                    selfMessage = Message.combine(children.newMessageToParent(node,momentChildCoParents), selfMessage);
                }
                node.updateCombinedMessage(selfMessage);

                done &= node.isDone();
            }

            if (done) {
                convergence = true;
            }

            //Compute lower-bound
            double newelbo = this.nodes.stream().mapToDouble(Node::computeELBO).sum();
            if (Math.abs(newelbo - elbo) < 0.00001) {
                convergence = true;
            }
            if (newelbo< elbo){
                throw new UnsupportedOperationException("The elbo is not monotonically increasing: " + elbo + ", "+ newelbo);
            }
            elbo = newelbo;
            System.out.println(elbo);

            //System.out.println(EF_DistributionBuilder.toDistribution((EF_Multinomial) nodes.get(0).getQDist()).toString());
            //System.out.println(EF_DistributionBuilder.toDistribution((EF_Multinomial) nodes.get(1).getQDist()).toString());
            //System.out.println(EF_DistributionBuilder.toDistribution((EF_Multinomial) nodes.get(2).getQDist()).toString());
        }
    }

    public void compileModelParallel() {
        if (assignment != null) {
            nodes.stream().forEach(node -> node.setAssignment(assignment));
        }

        boolean convergence = false;
        double elbo = Double.NEGATIVE_INFINITY;
        while (!convergence) {
            //System.out.println(nodes.get(0).getQDist().getMomentParameters().get(0));
            //System.out.println(nodes.get(1).getQDist().getMomentParameters().get(1));

            AtomicDouble newelbo = new AtomicDouble(0);
            //Send and combine messages
            Map<Variable, Optional<Message<NaturalParameters>>> group = nodes.stream()
                    .peek(node -> newelbo.addAndGet(node.computeELBO()))
                    .flatMap(node -> node.computeMessages())
                    .collect(
                            Collectors.groupingBy(Message::getVariable,
                                    Collectors.reducing(Message::combine))
                    );

            //Set Messages
            int numberOfNotDones = group.entrySet().stream()
                    .mapToInt(e -> {
                        Node node = nodes.get(e.getKey().getVarID());
                        node.updateCombinedMessage(e.getValue().get());
                        return (node.isDone())? 0:1;})
                    .sum();


            //Test whether all nodes are done.
            if (numberOfNotDones==0) {
                convergence = true;
            }

            //Compute lower-bound
            //double newelbo = this.nodes.stream().mapToDouble(Node::computeELBO).sum();
            if (Math.abs(newelbo.get() - elbo) < 0.00001) {
                convergence = true;
            }
            if (newelbo.get()< elbo){
                //throw new UnsupportedOperationException("The elbo is not monotonically increasing: " + elbo + ", "+ newelbo.get());
            }
            elbo = newelbo.get();
            System.out.println(elbo);

            System.out.println(EF_DistributionBuilder.toDistribution((EF_Multinomial) nodes.get(0).getQDist()).toString());
            System.out.println(EF_DistributionBuilder.toDistribution((EF_Multinomial) nodes.get(1).getQDist()).toString());
            System.out.println(EF_DistributionBuilder.toDistribution((EF_Multinomial) nodes.get(2).getQDist()).toString());
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
            node.getPDist().getConditioningVariables().stream().forEach(var -> nodes.get(var.getVarID()).getChildren().add(node));
        }

    }

    @Override
    public BayesianNetwork getModel() {
        return this.model;
    }


    @Override
    public void setEvidence(Assignment assignment_) {
        this.assignment = assignment_;
    }

    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
        return (E) EF_DistributionBuilder.toUnivariateDistribution(this.nodes.get(var.getVarID()).getQDist());
    }


    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/Munin1.bn");
        bn.randomInitialization(new Random(0));

        InferenceEngineForBN.setModel(bn);

        Stopwatch watch = Stopwatch.createStarted();
        InferenceEngineForBN.compileModel();
        System.out.println(watch.stop());


        System.out.println(InferenceEngineForBN.getPosterior(bn.getStaticVariables().getVariableById(0)).toString());

    }
}