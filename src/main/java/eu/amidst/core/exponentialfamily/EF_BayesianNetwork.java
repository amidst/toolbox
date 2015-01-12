package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.distribution.DistributionBuilder;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.StaticVariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            distributionList.set(ef_dist.getVariable().getVarID(), ef_dist);
            sizeSS+=ef_dist.sizeOfSufficientStatistics();
        }

        this.momentParameters = this.createZeroedMomentParameters();
        this.naturalParameters = this.createZeroedNaturalParameters();

    }

    public EF_BayesianNetwork(DAG  dag){
        distributionList = new ArrayList(dag.getStaticVariables().getNumberOfVars());

        sizeSS=0;
        for (ParentSet parentSet: dag.getParentSets()){
            ConditionalDistribution dist = DistributionBuilder.newDistribution(parentSet.getMainVar(), parentSet.getParents());
            EF_ConditionalDistribution ef_dist = EF_DistributionBuilder.toEFDistributionGeneral(dist);
            distributionList.add(ef_dist.getVariable().getVarID(), ef_dist);
            sizeSS+=ef_dist.sizeOfSufficientStatistics();
        }

    }

    public BayesianNetwork toBayesianNetwork(DAG dag){
        ConditionalDistribution[] dists = new ConditionalDistribution[dag.getStaticVariables().getNumberOfVars()];
        this.distributionList.stream().forEach(dist -> dists[dist.getVariable().getVarID()] = EF_DistributionBuilder.toDistributionGeneral(dist));
        return BayesianNetwork.newBayesianNetwork(dag, Arrays.asList(dists));
    }


    @Override
    public void updateNaturalFromMomentParameters() {

        CompoundVector globalMomentsParam = (CompoundVector)this.momentParameters;
        CompoundVector vectorNatural = this.createCompoundVector();

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
    public SufficientStatistics getSufficientStatistics(DataInstance data) {
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
    public double computeLogBaseMeasure(DataInstance dataInstance) {
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
        return new CompoundVector(this.distributionList, false);

    }

    private CompoundVector createEmtpyCompoundVector() {
        return new CompoundVector(this.distributionList, true);
    }

    static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

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

    static class IndexedVector {
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
}
