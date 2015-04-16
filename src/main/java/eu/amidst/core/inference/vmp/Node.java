package eu.amidst.core.inference.vmp;

import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public class Node {

    List<Node> parents;

    List<Node> children;

    Assignment assignment;

    EF_UnivariateDistribution QDist;

    EF_ConditionalDistribution PDist;

    boolean observed=false;

    SufficientStatistics sufficientStatistics;

    boolean isDone = false;

    boolean active = true;

    Variable mainVar;

    int seed = 0;

    boolean parallelActivated = true;

    Map<Variable, Node> variableToParentsNodeMap;

    Map<Node, Variable> nodeParentsToVariableMap;

    String name;

    public Node(EF_ConditionalDistribution PDist) {
         this(PDist, PDist.getVariable().getName());
    }

    public Node(EF_ConditionalDistribution PDist, String name_) {
        this.PDist = PDist;
        this.mainVar = this.PDist.getVariable();
        this.QDist= this.mainVar.getDistributionType().newEFUnivariateDistribution();
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.observed=false;
        sufficientStatistics=null;
        this.name = name_;
    }

    public String getName() {
        return name;
    }

    public boolean isParallelActivated() {
        return parallelActivated;
    }

    public void setParallelActivated(boolean parallelActivated) {
        this.parallelActivated = parallelActivated;
    }

    public void resetQDist(Random random){
        this.QDist= this.mainVar.getDistributionType().newEFUnivariateDistribution().randomInitialization(random);
    }

    public void setPDist(EF_ConditionalDistribution PDist) {
        this.PDist = PDist;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public List<Node> getParents() {
        return parents;
    }

    public void setParents(List<Node> parents) {
        this.parents = parents;
        variableToParentsNodeMap = new ConcurrentHashMap();
        nodeParentsToVariableMap = new ConcurrentHashMap();

        for( Node node: parents){
            variableToParentsNodeMap.put(node.getMainVariable(), node);
            nodeParentsToVariableMap.put(node, node.getMainVariable());
        }

    }

    public Assignment getAssignment() {
        return assignment;
    }

    public boolean isObserved() {
        return observed;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
        if (this.assignment==null || Utils.isMissingValue(this.assignment.getValue(this.getMainVariable()))){
            this.observed=false;
            sufficientStatistics=null;
            //if (this.isActive()) resetQDist();
        }else {
            this.observed=true;
            sufficientStatistics = this.QDist.getSufficientStatistics(assignment);
        }
    }

    public EF_UnivariateDistribution getQDist() {
        return (isObserved())? null: QDist;
    }

    public void setQDist(EF_UnivariateDistribution QDist) {
        this.QDist = QDist;
    }

    public MomentParameters getQMomentParameters(){
        return (isObserved())? (MomentParameters) this.sufficientStatistics: QDist.getMomentParameters();
    }

    public EF_ConditionalDistribution getPDist() {
        return PDist;
    }

    public Variable getMainVariable(){
        return this.mainVar;
    }

    public Stream<Message<NaturalParameters>> computeMessagesParallelVMP(){


        Map<Variable, MomentParameters> momentParents = this.getMomentParents();

        List<Message<NaturalParameters>> messages = this.parents.stream()
                                                                .filter(parent -> parent.isActive())
                                                                .filter(parent -> !parent.isObserved())
                                                                .filter(parent -> parent.isParallelActivated())
                                                                .map(parent -> this.newMessageToParent(parent, momentParents))
                                                                .collect(Collectors.toList());

        if (isActive() && isParallelActivated() && !isObserved()) {
            messages.add(this.newSelfMessage(momentParents));
        }

        return messages.stream();
    }

    public Message<NaturalParameters> newMessageToParent(Node parent, Map<Variable, MomentParameters> momentChildCoParents){
        Message<NaturalParameters> message = new Message(parent);
        message.setVector(this.PDist.getExpectedNaturalToParent(parent.getMainVariable(), momentChildCoParents));
        message.setDone(this.messageDoneToParent(parent.getMainVariable()));

        return message;
    }

    public Message<NaturalParameters> newSelfMessage(Map<Variable, MomentParameters> momentParents) {
        Message<NaturalParameters> message = new Message(this);
        message.setVector(this.PDist.getExpectedNaturalFromParents(momentParents));
        message.setDone(this.messageDoneFromParents());

        return message;
    }


    public Message<NaturalParameters> newMessageToParent(Node parent){

        Map<Variable, MomentParameters> momentChildCoParents = this.getMomentParents();

        Message<NaturalParameters> message = new Message<>(parent);
        message.setVector(this.PDist.getExpectedNaturalToParent(this.nodeParentToVariable(parent), momentChildCoParents));
        message.setDone(this.messageDoneToParent(parent.getMainVariable()));

        return message;
    }

    public Message<NaturalParameters> newSelfMessage() {

        Map<Variable, MomentParameters> momentParents = this.getMomentParents();

        Message<NaturalParameters> message = new Message(this);
        message.setVector(this.PDist.getExpectedNaturalFromParents(momentParents));
        message.setDone(this.messageDoneFromParents());

        return message;
    }

    Map<Variable, MomentParameters> getMomentParents(){
        Map<Variable, MomentParameters> momentParents = new ConcurrentHashMap<>();

        //this.getParents().stream().forEach(parent -> momentParents.put(parent.getMainVariable(), parent.getQMomentParameters()));

        this.getPDist().getConditioningVariables().stream().forEach(var -> momentParents.put(var,this.variableToNodeParent(var).getQMomentParameters()));

        momentParents.put(this.getMainVariable(), this.getQMomentParameters());

        return momentParents;
    }

    public Variable nodeParentToVariable(Node parent){
        return this.nodeParentsToVariableMap.get(parent);
    }

    public Node variableToNodeParent(Variable var){
        return this.variableToParentsNodeMap.get(var);
    }

    public void setVariableToNodeParent(Variable var, Node parent){
        this.variableToParentsNodeMap.put(var, parent);
        this.nodeParentsToVariableMap.put(parent, var);
    }

    private boolean messageDoneToParent(Variable parent){

        if (!this.isObserved())
            return false;

        for (Node node : this.getParents()){
            if (node.isActive() && node.getMainVariable().getVarID()!=parent.getVarID() && !node.isObserved())
                return false;
        }

        return true;
    }

    private boolean messageDoneFromParents(){

        for (Node node : this.getParents()){
            if (node.isActive() && !node.isObserved())
                return false;
        }

        return true;
    }

    public void updateCombinedMessage(Message<NaturalParameters> message){
        this.QDist.setNaturalParameters(message.getVector());
        this.isDone = message.isDone();
    }

    public boolean isDone(){
        return isDone || this.observed;
    }

    public double computeELBO(){
        Map<Variable, MomentParameters> momentParents = this.getMomentParents();

        double elbo=0;
        NaturalParameters expectedNatural = this.PDist.getExpectedNaturalFromParents(momentParents);

        if (!isObserved()) {
            expectedNatural.substract(this.QDist.getNaturalParameters());
            elbo += expectedNatural.dotProduct(this.QDist.getMomentParameters());
            elbo -= this.PDist.getExpectedLogNormalizer(momentParents);
            elbo += this.QDist.computeLogNormalizer();
        }else {
            elbo += expectedNatural.dotProduct(this.sufficientStatistics);
            elbo -= this.PDist.getExpectedLogNormalizer(momentParents);
            elbo += this.PDist.computeLogBaseMeasure(this.assignment);
        }

        if (elbo>0 && !this.isObserved() && Math.abs(expectedNatural.sum())<0.01) {
            elbo=0;
        }

        if ((elbo>1.1 && !this.isObserved()) || Double.isNaN(elbo)) {
            this.PDist.getExpectedLogNormalizer(momentParents);
            throw new IllegalStateException("NUMERICAL ERROR!!!!!!!!: " + this.getMainVariable().getName() + ", " +  elbo + ", " + expectedNatural.sum());
        }



        return  elbo;
    }

}
