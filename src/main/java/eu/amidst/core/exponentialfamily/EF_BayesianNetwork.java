package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Vector;

import java.util.ArrayList;
import java.util.List;

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

    }


    @Override
    public void updateNaturalFromMomentParameters() {

        CompoundVector globalMomentsParam = (CompoundVector)this.momentParameters;
        CompoundVector vectorNatural = this.createCompoundVector();

        this.distributionList.parallelStream().forEach(w -> {
            MomentParameters localMomentParam = w.createZeroedMomentParameters();
            localMomentParam.copy(globalMomentsParam.getVectorByPosition(w.getVariable().getVarID()));
            w.setMomentParameters(localMomentParam);
            vectorNatural.setVectorByPosition(w.getVariable().getVarID(),w.getNaturalParameters());
        });
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(DataInstance data) {
        CompoundVector vectorSS = this.createCompoundVector();

        this.distributionList.parallelStream().forEach(w -> {
            vectorSS.getVectorByPosition(w.getVariable().getVarID()).copy(w.getSufficientStatistics(data));
        });

        return vectorSS;
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return sizeSS;
    }

    @Override
    public double computeLogBaseMeasure(DataInstance dataInstance) {
        return this.distributionList.parallelStream().mapToDouble(w -> w.computeLogBaseMeasure(dataInstance)).sum();
    }

    @Override
    public double computeLogNormalizer() {
        return this.distributionList.parallelStream().mapToDouble(w -> w.computeLogNormalizer()).sum();
    }

    @Override
    public Vector createZeroedVector() {
        return createCompoundVector();
    }

    private CompoundVector createCompoundVector(){
        return new CompoundVector(this.distributionList);

    }
    static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

        int size;
        Vector[] baseVectors;

        public CompoundVector(List<EF_ConditionalDistribution> dists){

            baseVectors = new Vector[dists.size()];

            size=0;
            for (int i = 0; i < baseVectors.length; i++) {
                baseVectors[i]=dists.get(i).createZeroedVector();
                size+=baseVectors[i].size();
            }

        }

        public void setVectorByPosition(int position, Vector vec){
            baseVectors[position].copy(vec);
        }

        public Vector getVectorByPosition(int position){
            return this.baseVectors[position];
        }

        @Override
        public double get(int i) {
            int total = 0;
            for (int j = 0; j < this.baseVectors.length; j++) {
                if (i<(total+this.baseVectors[j].size())){
                    return this.baseVectors[j].get(i-total);
                }else{
                    total+=this.baseVectors[j].size();
                }
            }
            return Double.NaN;
        }

        @Override
        public void set(int i, double val) {
            int total = 0;
            for (int j = 0; j < this.baseVectors.length; j++) {
                if (i<(total+this.baseVectors[j].size())){
                    this.baseVectors[j].set(i - total, val);
                }else{
                    total+=this.baseVectors[j].size();
                }
            }
        }

        @Override
        public int size() {
            return size;
        }

        public void copy(CompoundVector vector) {
            if (vector.size()!=this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            for (int i = 0; i < this.baseVectors.length; i++) {
                this.baseVectors[i].copy(vector.getVectorByPosition(i));
            }
        }
    }
}
