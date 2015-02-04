package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.distribution.DistributionBuilder;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class EF_BayesianNetwork extends EF_Distribution {

    List<EF_ConditionalDistribution> distributionList;
    int sizeSS;

    public EF_BayesianNetwork(BayesianNetwork network){
        distributionList = new ArrayList(network.getNumberOfVars());

        sizeSS=0;
        for (ConditionalDistribution dist: network.getDistributions()){
            EF_ConditionalDistribution ef_dist = EF_DistributionBuilder.toEFDistributionGeneral(dist);
            distributionList.add(ef_dist.getVariable().getVarID(), ef_dist);
            sizeSS+=ef_dist.sizeOfSufficientStatistics();
        }

        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        for (EF_ConditionalDistribution dist : distributionList){
            vectorNatural.setVectorByPosition(dist.getVariable().getVarID(), dist.getNaturalParameters());
        }
        this.naturalParameters = vectorNatural;
        this.momentParameters = null;
    }

    public EF_BayesianNetwork(DAG  dag){
        this(dag.getParentSets());
    }


    public EF_BayesianNetwork(List<ParentSet>  parentSets){
        distributionList = new ArrayList(parentSets.size());

        sizeSS=0;
        for (ParentSet parentSet: parentSets){
            ConditionalDistribution dist = DistributionBuilder.newDistribution(parentSet.getMainVar(), parentSet.getParents());
            dist.randomInitialization(new Random(0));
            EF_ConditionalDistribution ef_dist = EF_DistributionBuilder.toEFDistributionGeneral(dist);
            distributionList.add(ef_dist.getVariable().getVarID(), ef_dist);
            sizeSS+=ef_dist.sizeOfSufficientStatistics();
        }
        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        for (EF_ConditionalDistribution dist : distributionList){
            vectorNatural.setVectorByPosition(dist.getVariable().getVarID(), dist.getNaturalParameters());
        }
        this.naturalParameters = vectorNatural;
        this.momentParameters = null;
    }

    public BayesianNetwork toBayesianNetwork(DAG dag){
        return BayesianNetwork.newBayesianNetwork(dag, toConditionalDistribution(this.distributionList));
    }

    public static List<ConditionalDistribution> toConditionalDistribution(List<EF_ConditionalDistribution> ef_dists){
        ConditionalDistribution[] dists = new ConditionalDistribution[ef_dists.size()];
        ef_dists.stream().forEach(dist -> dists[dist.getVariable().getVarID()] = EF_DistributionBuilder.toDistributionGeneral(dist));
        return Arrays.asList(dists);
    }

    public List<EF_ConditionalDistribution> getDistributionList() {
        return distributionList;
    }

    public EF_ConditionalDistribution getDistribution(Variable var) {
        return distributionList.get(var.getVarID());
    }

    @Override
    public void updateNaturalFromMomentParameters() {

        CompoundVector globalMomentsParam = (CompoundVector)this.momentParameters;
        CompoundVector vectorNatural = this.createEmtpyCompoundVector();

        this.distributionList.stream().forEach(w -> {
            MomentParameters localMomentParam = (MomentParameters) globalMomentsParam.getVectorByPosition(w.getVariable().getVarID());
            w.setMomentParameters(localMomentParam);
            vectorNatural.setVectorByPosition(w.getVariable().getVarID(),w.getNaturalParameters());
        });

        this.naturalParameters=vectorNatural;
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        CompoundVector vectorSS = this.createEmtpyCompoundVector();//.createCompoundVector();

        this.distributionList.stream().forEach(w -> {
            vectorSS.setVectorByPosition(w.getVariable().getVarID(), w.getSufficientStatistics(data));
        });

        return vectorSS;
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return sizeSS;
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.distributionList.stream().mapToDouble(w -> w.computeLogBaseMeasure(dataInstance)).sum();
    }

    @Override
    public double computeLogNormalizer() {
        return this.distributionList.stream().mapToDouble(w -> w.computeLogNormalizer()).sum();
    }

    @Override
    public Vector createZeroedVector() {
        return createCompoundVector();
    }

    private CompoundVector createCompoundVector(){
        return new CompoundVector(this.distributionList.stream().map(w-> w.createZeroedVector()).collect(Collectors.toList()));
    }

    private CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(this.distributionList.size(), this.sizeOfSufficientStatistics());
    }

    public boolean equal_efBN(EF_BayesianNetwork ef_bayesianNetwork, double threshold){

        for (EF_ConditionalDistribution this_dist: this.getDistributionList()){
            EF_ConditionalDistribution ef_dist = ef_bayesianNetwork.getDistribution(this_dist.getVariable());
            if(!this_dist.getClass().getName().equals(ef_dist.getClass().getName()))
                return false;
            List<Variable> this_Vars = this_dist.getConditioningVariables();
            List<Variable> ef_Vars = ef_dist.getConditioningVariables();
            if(this_Vars.size()!=ef_Vars.size())
                return false;
            for(Variable var: this_Vars){
                if(!ef_Vars.contains(var))
                    return false;
            }
        }

        return this.getNaturalParameters().equalsVector(ef_bayesianNetwork.getNaturalParameters(), threshold);

    }

    /*
    private static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

        int size;
        List<IndexedVector> baseVectors;

        public CompoundVector(List<EF_ConditionalDistribution> dists, boolean empty) {

            baseVectors = new ArrayList(dists.size());

            size = 0;
            for (int i = 0; i < dists.size(); i++) {
                if (empty)
                    baseVectors.add(i, new IndexedVector(i, null));
                else
                    baseVectors.add(i, new IndexedVector(i, dists.get(i).createZeroedVector()));

                size += dists.get(i).sizeOfSufficientStatistics();
            }

        }

        public void setVectorByPosition(int position, Vector vec) {
            baseVectors.get(position).setVector(vec);
        }

        public Vector getVectorByPosition(int position) {
            return this.baseVectors.get(position).getVector();
        }

        @Override
        public double get(int i) {
            int total = 0;
            for (int j = 0; j < this.baseVectors.size(); j++) {
                if (i < (total + this.baseVectors.get(j).getVector().size())) {
                    return this.baseVectors.get(j).getVector().get(i - total);
                } else {
                    total += this.baseVectors.get(j).getVector().size();
                }
            }
            return Double.NaN;
        }

        @Override
        public void set(int i, double val) {
            int total = 0;
            for (int j = 0; j < this.baseVectors.size(); j++) {
                if (i < (total + this.baseVectors.get(j).getVector().size())) {
                    this.baseVectors.get(j).getVector().set(i - total, val);
                } else {
                    total += this.baseVectors.get(j).getVector().size();
                }
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void sum(Vector vector) {
            this.sum((CompoundVector)vector);
        }

        @Override
        public void copy(Vector vector) {
            this.copy((CompoundVector)vector);
        }

        @Override
        public void divideBy(double val) {
            this.baseVectors.stream().forEach(w -> w.getVector().divideBy(val));
        }

        @Override
        public double dotProduct(Vector vec) {
            return this.dotProduct((CompoundVector)vec);
        }

        public double dotProduct(CompoundVector vec) {
            return this.baseVectors.stream().mapToDouble(w -> w.getVector().dotProduct(vec.getVectorByPosition(w.getIndex()))).sum();
        }

        public void copy(CompoundVector vector) {
            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            this.baseVectors.stream().forEach(w -> w.getVector().copy(vector.getVectorByPosition(w.getIndex())));

        }

        public void sum(CompoundVector vector) {
            this.baseVectors.stream().forEach(w -> w.getVector().sum(vector.getVectorByPosition(w.getIndex())));
        }

    }

    private static class IndexedVector {
        Vector vector;
        int index;

        IndexedVector(int index1, Vector vec1){
            this.vector=vec1;
            this.index = index1;
        }

        public Vector getVector() {
            return vector;
        }

        public int getIndex() {
            return index;
        }

        public void setVector(Vector vector) {
            this.vector = vector;
        }
    }

    */
}
