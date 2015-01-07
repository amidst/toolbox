/**
 * ****************** ISSUE LIST ******************************
 *
 *
 *
 * 1. getConditioningVariables change to getParentsVariables()
 *
 * 2. Rewrite the naturalmparameters and momementsparametes interfaces to allow sparse implementations and translage
 * that to the implemementaiton of setNatural and setMoments
 *
 *
 *
 * **********************************************************
 */


package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EF_BaseDistribution_MultinomialParents<E extends EF_Distribution> extends EF_ConditionalDistribution {


    private final List<E> distributions;
    private final List<Variable> multinomialParents;

    public List<Variable> getMultinomialParents() {
        return multinomialParents;
    }

    public EF_BaseDistribution_MultinomialParents(List<Variable> multinomialParents1, List<E> distributions1) {

        //if (multinomialParents1.size()==0) throw new IllegalArgumentException("Size of multinomial parents is zero");
        if (distributions1.size() == 0) throw new IllegalArgumentException("Size of base distributions is zero");

        int size = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents1);
        if (size!= distributions1.size()) throw new IllegalArgumentException("Size of base distributions list does not match with the number of parents configurations");

        this.var = distributions1.get(0).getVariable();
        this.multinomialParents = multinomialParents1;
        this.distributions = distributions1;


        this.parents = new ArrayList();
        for (Variable v : this.multinomialParents)
            this.parents.add(v);

        E dist = distributions.get(0);

        if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_ConditionalDistribution")){
            EF_ConditionalDistribution distCond = (EF_ConditionalDistribution)dist;
            for (Variable v : distCond.getConditioningVariables())
                this.parents.add(v);
        }



        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);

    }

    public void setEF_BaseDistribution(int indexMultinomial, E baseDist) {
        this.distributions.set(indexMultinomial,baseDist);
    }

    public E getEF_BaseDistribution(int indexMultinomial) {

        return distributions.get(indexMultinomial);
    }

    public E getEF_BaseDistribution(DataInstance dataInstance) {
        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, dataInstance);
        return getEF_BaseDistribution(position);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(DataInstance instance) {

        CompoundVector<E> vector = this.createCompoundVector();

        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, instance);

        vector.setBaseConf(position,1.0);

        SufficientStatistics sufficientStatisticsBase = this.getEF_BaseDistribution(position).getSufficientStatistics(instance);

        vector.setVectorByPosition(position, sufficientStatisticsBase);

        return vector;

    }

    public int numberOfConfigurations(){
        return this.distributions.size();
    }

    public int sizeOfBaseSufficientStatistics(){
        return this.getEF_BaseDistribution(0).sizeOfSufficientStatistics();
    }

    public int sizeOfSufficientStatistics(){
        return numberOfConfigurations() + numberOfConfigurations()*sizeOfBaseSufficientStatistics();
    }

    public void updateNaturalFromMomentParameters(){

        CompoundVector<E> globalMomentsParam = (CompoundVector<E>)this.momentParameters;

        for (int i = 0; i < numberOfConfigurations(); i++) {
            MomentParameters moment = (MomentParameters)globalMomentsParam.getVectorByPosition(i);
            moment.divideBy(globalMomentsParam.getBaseConf(i));
            this.getEF_BaseDistribution(i).setMomentParameters(momentParameters);
        }

        CompoundVector<E> vectorNatural = this.createCompoundVector();


        for (int i = 0; i < numberOfConfigurations(); i++) {
            vectorNatural.setBaseConf(i, -this.getEF_BaseDistribution(i).computeLogNormalizer());
            vectorNatural.setVectorByPosition(i, this.getEF_BaseDistribution(i).getNaturalParameters());
        }

        this.naturalParameters=vectorNatural;

        return;
    }

    public void updateMomentFromNaturalParameters(){
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    public double computeLogBaseMeasure(DataInstance dataInstance){
        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, dataInstance);
        return this.getEF_BaseDistribution(position).computeLogBaseMeasure(dataInstance);
    }

    @Override
    public double computeLogNormalizer() {
        return 0;
    }


    @Override
    public Vector createZeroedVector() {
        return this.createCompoundVector();
    }

    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        return null;
    }

    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        return null;
    }

    private CompoundVector<E> createCompoundVector(){
        return new CompoundVector<>(this.getEF_BaseDistribution(0),this.numberOfConfigurations());
    }

    static class CompoundVector<E extends EF_Distribution> implements SufficientStatistics, MomentParameters, NaturalParameters {

        int nConf;
        int baseSSLength;
        double[] baseConf;
        E baseDist;

        Vector[] baseVectors;

        public CompoundVector(E baseDist1, int nConf1){
            nConf = nConf1;
            this.baseConf = new double[nConf];
            baseDist = baseDist1;
            baseVectors = new Vector[nConf];
            baseSSLength = baseDist.sizeOfSufficientStatistics();

            for (int i = 0; i < nConf; i++) {
                baseVectors[i]=baseDist.createZeroedVector();
            }

        }

        public void setVectorByPosition(int position, Vector vec){
            baseVectors[position]=vec;
        }

        public Vector getVectorByPosition(int position){
            return this.baseVectors[position];
        }

        public double getBaseConf(int i){
            return this.baseConf[i];
        }

        public void setBaseConf(int i, double val){
            this.baseConf[i]=val;
        }

        @Override
        public double get(int i) {
            if (i<nConf) {
                return this.baseConf[i];
            }else{
                i -= nConf;
                return baseVectors[Math.floorDiv(i,this.baseSSLength)].get(i%baseSSLength);
            }
        }

        @Override
        public void set(int i, double val) {
            if (i < nConf){
                baseConf[i] = val;
            }else{
                i -= nConf;
                baseVectors[Math.floorDiv(i,this.baseSSLength)].set(i%baseSSLength,val);
            }
        }

        @Override
        public int size() {
            return nConf + nConf*baseSSLength;
        }

        public void copy(CompoundVector<E> vector) {
            if (vector.size()!=this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            System.arraycopy(vector.baseConf,0,this.baseConf,0,this.nConf);
            for (int i = 0; i < this.nConf; i++) {
                this.baseVectors[i].copy(vector.getVectorByPosition(i));
            }
        }

        @Override
        public void divideBy(double val) {
            for (int i = 0; i < this.baseConf.length; i++) {
                this.baseConf[i]/=val;
                this.baseVectors[i].divideBy(val);
            }
        }
    }
}
