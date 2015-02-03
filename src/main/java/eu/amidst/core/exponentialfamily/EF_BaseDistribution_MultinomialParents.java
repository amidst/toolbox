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

//TODO: Condiser the log-base-measure when defining the base distribution.

package eu.amidst.core.exponentialfamily;

import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        if (size != distributions1.size())
            throw new IllegalArgumentException("Size of base distributions list does not match with the number of parents configurations");

        this.var = distributions1.get(0).getVariable();
        this.multinomialParents = multinomialParents1;
        this.distributions = distributions1;


        this.parents = new ArrayList();
        for (Variable v : this.multinomialParents)
            this.parents.add(v);

        E dist = distributions.get(0);

        if (dist.getClass().getName().equals("eu.amidst.core.exponentialfamily.EF_ConditionalDistribution")) {
            EF_ConditionalDistribution distCond = (EF_ConditionalDistribution) dist;
            for (Variable v : distCond.getConditioningVariables())
                this.parents.add(v);
        }

        CompoundVector vectorNatural = this.createCompoundVector();

        for (int i = 0; i < numberOfConfigurations(); i++) {
            vectorNatural.setBaseConf(i, -this.getEF_BaseDistribution(i).computeLogNormalizer());
            vectorNatural.setVectorByPosition(i, this.getEF_BaseDistribution(i).getNaturalParameters());
        }

        this.naturalParameters = vectorNatural;
        this.momentParameters = null;

        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);

    }

    public void setEF_BaseDistribution(int indexMultinomial, E baseDist) {
        this.distributions.set(indexMultinomial, baseDist);
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

        CompoundVector vector = this.createCompoundVector();

        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, instance);

        vector.setBaseConf(position, 1.0);

        SufficientStatistics sufficientStatisticsBase = this.getEF_BaseDistribution(position).getSufficientStatistics(instance);

        vector.setVectorByPosition(position, sufficientStatisticsBase);

        return vector;

    }

    public int numberOfConfigurations() {
        return this.distributions.size();
    }

    public int sizeOfBaseSufficientStatistics() {
        return this.getEF_BaseDistribution(0).sizeOfSufficientStatistics();
    }

    public int sizeOfSufficientStatistics() {
        return numberOfConfigurations() + numberOfConfigurations() * sizeOfBaseSufficientStatistics();
    }

    public void updateNaturalFromMomentParameters() {

        CompoundVector globalMomentsParam = (CompoundVector) this.momentParameters;

        for (int i = 0; i < numberOfConfigurations(); i++) {
            MomentParameters moment = (MomentParameters) globalMomentsParam.getVectorByPosition(i);
            moment.divideBy(globalMomentsParam.getBaseConf(i));
            this.getEF_BaseDistribution(i).setMomentParameters(moment);
        }

        CompoundVector vectorNatural = this.createCompoundVector();


        for (int i = 0; i < numberOfConfigurations(); i++) {
            vectorNatural.setBaseConf(i, -this.getEF_BaseDistribution(i).computeLogNormalizer());
            vectorNatural.setVectorByPosition(i, this.getEF_BaseDistribution(i).getNaturalParameters());
        }

        this.naturalParameters = vectorNatural;

        return;
    }

    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    public double computeLogBaseMeasure(DataInstance dataInstance) {
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
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        return 0;
    }

    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        return null;
    }

    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        return null;
    }

    @Override
    public EF_UnivariateDistribution getEmptyEFUnivariateDistribution() {
        return null;
    }

    @Override
    public EF_UnivariateDistribution getEFUnivariateDistribution(Assignment assignment) {
        return null;
    }

    private CompoundVector createCompoundVector() {
        return new CompoundVector((EF_Distribution)this.getEF_BaseDistribution(0), this.numberOfConfigurations());
    }

    //TODO: Replace this CompoundVector by the compoundvector of indicator
    private static class CompoundVector implements SufficientStatistics, MomentParameters, NaturalParameters {

        int nConf;
        int baseSSLength;
        double[] baseConf;
        EF_Distribution baseDist;

        SparseVector baseVectors;

        public CompoundVector(EF_Distribution baseDist1, int nConf1) {
            nConf = nConf1;
            this.baseConf = new double[nConf];
            baseDist = baseDist1;
            baseVectors = new SparseVector(baseDist1::createZeroedVector,nConf);
            baseSSLength = baseDist.sizeOfSufficientStatistics();

        }

        public void setVectorByPosition(int position, Vector vec) {
            baseVectors.setVectorByPosition(position, vec);
        }

        public Vector getVectorByPosition(int position) {
            Vector vector =  this.baseVectors.getVectorByPosition(position);
            if (vector==null){
                return this.baseDist.createZeroedVector();
            }else {
                return vector;
            }
        }

        public double getBaseConf(int i) {
            return this.baseConf[i];
        }

        public void setBaseConf(int i, double val) {
            this.baseConf[i] = val;
        }

        @Override
        public double get(int i) {
            if (i < nConf) {
                return this.baseConf[i];
            } else {
                i -= nConf;
                return baseVectors.get(i);
            }
        }

        @Override
        public void set(int i, double val) {
            if (i < nConf) {
                baseConf[i] = val;
            } else {
                i -= nConf;
                baseVectors.set(i,val);
            }
        }

        @Override
        public int size() {
            return nConf + nConf * baseSSLength;
        }

        @Override
        public void sum(Vector vector) {
            this.sum((CompoundVector) vector);
        }

        public SparseVector getBaseVectors() {
            return baseVectors;
        }

        public void sum(CompoundVector vector) {
            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            for (int i = 0; i < baseConf.length; i++) {
                baseConf[i] += vector.getBaseConf(i);
            }

            this.baseVectors.sum(vector.getBaseVectors());
        }

        @Override
        public void copy(Vector vector) {
            this.copy((CompoundVector) vector);
        }

        public void copy(CompoundVector vector) {
            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            System.arraycopy(vector.baseConf, 0, this.baseConf, 0, this.nConf);
            this.baseVectors.copy(vector.getBaseVectors());
        }

        @Override
        public void divideBy(double val) {
            for (int i = 0; i < this.baseConf.length; i++) {
                this.baseConf[i] /= val;
            }
            this.baseVectors.divideBy(val);
        }

        @Override
        public double dotProduct(Vector vector) {
            return this.dotProduct((CompoundVector) vector);
        }

        public double dotProduct(CompoundVector vector) {
            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            double sum = 0;

            for (int i = 0; i < baseConf.length; i++) {
                sum += baseConf[i] * vector.getBaseConf(i);
            }

            sum += this.baseVectors.dotProduct(vector.getBaseVectors());

            return sum;
        }
    }

    private static class SparseVector implements Vector {

        VectorBuilder vectorBuilder;

        Map<Integer, Vector> vectorMap;

        int numVectors;

        int baseSize;

        int nonZeroEntries;

        public SparseVector(VectorBuilder vectorBuilder1, int numVectors1) {
            this.vectorBuilder = vectorBuilder1;
            Vector baseVector = this.vectorBuilder.createZeroedVector();
            this.baseSize = baseVector.size();
            this.numVectors = numVectors1;
            nonZeroEntries=0;
            vectorMap = new HashMap<Integer,Vector>();
        }

        public void setVectorByPosition(int position, Vector vec) {
            if (vectorMap.containsKey(position)){
                vectorMap.put(position,vec);
            }else{
                vectorMap.put(position,vec);
                this.nonZeroEntries++;
            }
        }

        public Vector getVectorByPosition(int position) {
            return this.vectorMap.get(position);
        }

        public int getNumberNonZeroEntries() {
            return nonZeroEntries;
        }

        @Override
        public double get(int i) {
            int baseIndex = Math.floorDiv(i, this.baseSize);
            if (vectorMap.containsKey(baseIndex))
                return vectorMap.get(baseIndex).get(i % baseSize);
            else
                return 0;
        }

        @Override
        public void set(int i, double val) {
            int baseIndex = Math.floorDiv(i, this.baseSize);
            if (vectorMap.containsKey(baseIndex)) {
                vectorMap.get(baseIndex).set(i % baseSize, val);
            } else {
                Vector baseVector = this.vectorBuilder.createZeroedVector();
                baseVector.set(i % baseSize, val);
                vectorMap.put(baseIndex, baseVector);
                nonZeroEntries++;
            }
        }

        @Override
        public int size() {
            return baseSize * numVectors;
        }

        @Override
        public void sum(Vector vector) {
            this.sum((SparseVector)vector);
        }

        public void sum(SparseVector vector) {
            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            vector.nonZeroEntries().forEach(entry -> {
                    Vector localVector = this.getVectorByPosition(entry.getKey());
                    Vector outerVector = entry.getValue();
                    if (localVector != null) {
                        localVector.sum(outerVector);
                    }else{
                        Vector newVector = this.vectorBuilder.createZeroedVector();
                        newVector.sum(outerVector);
                        this.setVectorByPosition(entry.getKey(),newVector);
                    }
                });
        }

        @Override
        public void copy(Vector vector) {
            this.copy((SparseVector)vector);
        }

        public void copy(SparseVector vector) {
            if (vector.size() != this.size())
                throw new IllegalArgumentException("Error in variable Vector. Method copy. The parameter vec has a different size. ");

            vectorMap = new HashMap<Integer,Vector>();

            vector.nonZeroEntries().forEach(entry -> {
                Vector newVector = this.vectorBuilder.createZeroedVector();
                newVector.copy(entry.getValue());
                this.setVectorByPosition(entry.getKey(),newVector);
            });
        }

        @Override
        public void divideBy(double val) {
            this.nonZeroEntries().forEach( entry -> entry.getValue().divideBy(val));
        }

        @Override
        public double dotProduct(Vector vec) {
            return dotProduct((SparseVector)vec);
        }

        public double dotProduct(SparseVector vec) {
            AtomicDouble sum= new AtomicDouble(0);

            if (this.getNumberNonZeroEntries()<vec.getNumberNonZeroEntries()){
                this.nonZeroEntries().forEach(entry ->{
                    Vector outerVector = vec.getVectorByPosition(entry.getKey());
                    if (outerVector!=null)
                        sum.addAndGet(entry.getValue().dotProduct(outerVector));
                });
            }else{
                vec.nonZeroEntries().forEach(entry ->{
                    Vector localVector = this.getVectorByPosition(entry.getKey());
                    if (localVector!=null)
                        sum.addAndGet(entry.getValue().dotProduct(localVector));
                });
            }
            return sum.doubleValue();
        }

        public Stream<Map.Entry<Integer,Vector>> nonZeroEntries(){
            return this.vectorMap.entrySet().stream();
        }
    }

    @FunctionalInterface
    private interface VectorBuilder {
            public Vector createZeroedVector();
    }
}
