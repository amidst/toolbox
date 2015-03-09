package eu.amidst.core.inference;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP_.Message;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public class VMP implements InferenceAlgorithmForBN {

    BayesianNetwork model;
    EF_BayesianNetwork ef_model;
    Assignment assignment = new HashMapAssignment(0);
    List<Node> nodes;
    Map<Variable,Node> variablesToNode;
    boolean parallelMode = false;
    int seed = 0;
    double probOfEvidence = Double.NaN;


    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    public int getSeed() {
        return seed;
    }

    @Override
    public void setSeed(int seed) {
        this.seed = seed;
    }

    @Override
    public void runInference() {
        if (parallelMode)
            this.compileModelParallel();
        else
            this.compileModelSerial();
    }

    public void compileModelSerial() {
        nodes.stream().forEach(node -> node.setAssignment(assignment));

        boolean convergence = false;
        double elbo = Double.NEGATIVE_INFINITY;
        int niter = 0;
        while (!convergence && (niter++)<100) {

            boolean done = true;
            for (Node node : nodes) {
                if (!node.isActive() || node.isObserved())
                    continue;

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
            if (Math.abs(newelbo - elbo) < 0.001) {
                convergence = true;
            }
            if ((!convergence && newelbo< elbo) || Double.isNaN(elbo)){
                throw new UnsupportedOperationException("The elbo is not monotonically increasing: " + elbo + ", "+ newelbo);
            }
            elbo = newelbo;
            //System.out.println(elbo);

        }
        probOfEvidence = elbo;
        System.out.println("N Iter: "+niter +", elbo:"+elbo);
    }

    public void compileModelParallel() {
        nodes.stream().forEach(node -> node.setAssignment(assignment));

        nodes.stream().filter(Node::isActive).forEach( node -> node.setParallelActivated(false));

        Random rand = new Random(this.getSeed());
        boolean convergence = false;
        double elbo = Double.NEGATIVE_INFINITY;
        int niter = 0;
        while (!convergence && (niter++)<100) {
            AtomicDouble newelbo = new AtomicDouble(0);
            int numberOfNotDones = 0;

            //nodesTimeT.stream().forEach( node -> node.setActive(node.getMainVariable().getVarID()%2==0));
            nodes.stream().filter(Node::isActive).forEach(node -> node.setParallelActivated(rand.nextBoolean()));
            //nodes.stream().forEach( node -> node.setActive(rand.nextInt()%100==0));

            //Send and combine messages
            Map<Variable, Optional<Message<NaturalParameters>>> group = nodes.parallelStream()
                    //.peek(node -> newelbo.addAndGet(node.computeELBO()))
                    .flatMap(node -> node.computeMessagesParallelVMP())
                    .collect(
                            Collectors.groupingBy(Message::getVariable,ConcurrentHashMap::new,
                                    Collectors.reducing(Message::combine))
                    );

            //Set Messages
            numberOfNotDones += group.entrySet().parallelStream()
                    .mapToInt(e -> {
                        Node node = this.getNodeOfVar(e.getKey());
                        node.updateCombinedMessage(e.getValue().get());
                        return (node.isDone()) ? 0 : 1;
                    })
                    .sum();

            nodes.stream().filter(Node::isActive).forEach(node -> node.setParallelActivated(!node.isParallelActivated()));

            //Send and combine messages
            group = nodes.parallelStream()
                    .peek(node -> newelbo.addAndGet(node.computeELBO()))
                    .flatMap(node -> node.computeMessagesParallelVMP())
                    .collect(
                            Collectors.groupingBy(Message::getVariable,ConcurrentHashMap::new,
                                    Collectors.reducing(Message::combine))
                    );

            //Set Messages
            numberOfNotDones += group.entrySet().parallelStream()
                    .mapToInt(e -> {
                        Node node = this.getNodeOfVar(e.getKey());
                        node.updateCombinedMessage(e.getValue().get());
                        return (node.isDone()) ? 0 : 1;
                    })
                    .sum();


            //Test whether all nodesTimeT are done.
            if (numberOfNotDones == 0) {
                convergence = true;
            }

            //Compute lower-bound
            //newelbo.set(this.nodes.parallelStream().mapToDouble(Node::computeELBO).sum());
            if (Math.abs(newelbo.get() - elbo) < 0.0001) {
                convergence = true;
            }
            if ((!convergence && newelbo.get()< elbo) || Double.isNaN(elbo)){
                //throw new UnsupportedOperationException("The elbo is NaN or is not monotonically increasing: " + elbo + ", "+ newelbo.get());
            }
            elbo = newelbo.get();
            //System.out.println(elbo);
        }
        probOfEvidence = elbo;
        System.out.println("N Iter: "+niter +", elbo:"+elbo);
    }

    @Override
    public void setModel(BayesianNetwork model_) {
        model = model_;
        this.setEFModel(new EF_BayesianNetwork(this.model));
    }

    public void setEFModel(EF_BayesianNetwork model){
        ef_model = model;

        variablesToNode = new ConcurrentHashMap<>();
        nodes = ef_model.getDistributionList()
                .stream()
                .map(dist -> {
                    Node node = new Node(dist);
                    node.setSeed(this.getSeed());
                    variablesToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        for (Node node : nodes){
            node.setParents(node.getPDist().getConditioningVariables().stream().map(this::getNodeOfVar).collect(Collectors.toList()));
            node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var).getChildren().add(node));
        }
    }

    public EF_BayesianNetwork getEFModel() {
        return ef_model;
    }

    public Node getNodeOfVar(Variable variable){
        return this.variablesToNode.get(variable);
    }
    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
        variablesToNode = new ConcurrentHashMap<>();

        nodes.stream().forEach( node -> variablesToNode.put(node.getMainVariable(),node));

        for (Node node : nodes){
            node.setParents(
                    node.getPDist()
                    .getConditioningVariables()
                    .stream()
                    .map(this::getNodeOfVar)
                    .collect(Collectors.toList())
            );

            node.getPDist().getConditioningVariables().stream()
                    .forEach(var -> this.getNodeOfVar(var).getChildren().add(node));
        }
    }

    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }


    @Override
    public void setEvidence(Assignment assignment_) {
        this.assignment = assignment_;
    }

    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
        return this.getNodeOfVar(var).getQDist().toUnivariateDistribution();
    }

    @Override
    public double getLogProbabilityOfEvidence() {
        return this.probOfEvidence;
    }

    public <E extends EF_UnivariateDistribution> E getEFPosterior(Variable var) {
        return (E)this.getNodeOfVar(var).getQDist();
    }

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/Munin4.bn");
        System.out.println(bn.getNumberOfVars());
        System.out.println(bn.getConditionalDistributions().stream().mapToInt(p->p.getNumberOfFreeParameters()).max().getAsInt());

        VMP vmp = new VMP();
        //vmp.setSeed(10);
        vmp.setParallelMode(true);
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);

        double avg  = 0;
        for (int i = 0; i < 20; i++)
        {
            InferenceEngineForBN.setModel(bn);

            Stopwatch watch = Stopwatch.createStarted();
            InferenceEngineForBN.runInference();
            System.out.println(watch.stop());
            avg += watch.elapsed(TimeUnit.MILLISECONDS);
        }
        System.out.println(avg/20);
        System.out.println(InferenceEngineForBN.getPosterior(bn.getStaticVariables().getVariableById(0)).toString());

    }
}