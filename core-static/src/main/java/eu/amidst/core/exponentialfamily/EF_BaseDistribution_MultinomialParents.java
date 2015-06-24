/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

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
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.*;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EF_BaseDistribution_MultinomialParents<E extends EF_Distribution> extends EF_ConditionalDistribution {


    private final List<E> distributions;
    private final List<Variable> multinomialParents;

    private boolean isBaseConditionalDistribution;

    public List<Variable> getMultinomialParents() {
        return multinomialParents;
    }

    public EF_BaseDistribution_MultinomialParents(List<Variable> multinomialParents1, List<E> distributions1) {
        this(multinomialParents1,distributions1,true);
    }

    public EF_BaseDistribution_MultinomialParents(List<Variable> multinomialParents1, List<E> distributions1, boolean initializeMomentNaturalParameters) {

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

        if (distributions.get(0) instanceof EF_ConditionalDistribution) {
            this.isBaseConditionalDistribution = true;
            for (int i = 0; i < size; i++) {
                for (Variable v : this.getBaseEFConditionalDistribution(i).getConditioningVariables()) {
                    if (!this.parents.contains(v))
                        this.parents.add(v);
                }
            }
        } else {
            this.isBaseConditionalDistribution = false;
        }

        this.naturalParameters = null;
        this.momentParameters = null;

        if (initializeMomentNaturalParameters) {
            CompoundVector vectorNatural = this.createCompoundVector();

            for (int i = 0; i < numberOfConfigurations(); i++) {
                vectorNatural.setBaseConf(i, -this.getBaseEFDistribution(i).computeLogNormalizer());
                vectorNatural.setVectorByPosition(i, this.getBaseEFDistribution(i).getNaturalParameters());
            }

            this.naturalParameters = vectorNatural;
        }

        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);

    }

    public boolean isBaseConditionalDistribution() {
        return isBaseConditionalDistribution;
    }

    public EF_ConditionalDistribution getBaseEFConditionalDistribution(int multinomialIndex) {
        return (EF_ConditionalDistribution)this.getBaseEFDistribution(multinomialIndex);
    }

    public EF_UnivariateDistribution getBaseEFUnivariateDistribution(int multinomialIndex) {
        return (EF_UnivariateDistribution)this.getBaseEFDistribution(multinomialIndex);
    }

    public void setBaseEFDistribution(int indexMultinomial, E baseDist) {
        this.distributions.set(indexMultinomial, baseDist);
    }

    public E getBaseEFDistribution(int indexMultinomial) {

        return distributions.get(indexMultinomial);
    }

    public E getBaseEFDistribution(DataInstance dataInstance) {
        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, dataInstance);
        return getBaseEFDistribution(position);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment instance) {

        CompoundVector vector = this.createCompoundVector();

        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, instance);

        vector.setBaseConf(position, 1.0);

        SufficientStatistics sufficientStatisticsBase = this.getBaseEFDistribution(position).getSufficientStatistics(instance);

        vector.setVectorByPosition(position, sufficientStatisticsBase);

        return vector;

    }

    public int numberOfConfigurations() {
        return this.distributions.size();
    }

    public int sizeOfBaseSufficientStatistics() {
        return this.getBaseEFDistribution(0).sizeOfSufficientStatistics();
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return numberOfConfigurations() + numberOfConfigurations() * sizeOfBaseSufficientStatistics();
    }

    @Override
    public void updateNaturalFromMomentParameters() {

        CompoundVector globalMomentsParam = (CompoundVector) this.momentParameters;

        for (int i = 0; i < numberOfConfigurations(); i++) {
            MomentParameters moment = (MomentParameters) globalMomentsParam.getVectorByPosition(i);
            moment.divideBy(globalMomentsParam.getBaseConf(i));
            this.getBaseEFDistribution(i).setMomentParameters(moment);
        }

        CompoundVector vectorNatural = this.createCompoundVector();


        for (int i = 0; i < numberOfConfigurations(); i++) {
            vectorNatural.setBaseConf(i, -this.getBaseEFDistribution(i).computeLogNormalizer());
            vectorNatural.setVectorByPosition(i, this.getBaseEFDistribution(i).getNaturalParameters());
        }

        this.naturalParameters = vectorNatural;

        return;
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, dataInstance);
        return this.getBaseEFDistribution(position).computeLogBaseMeasure(dataInstance);
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
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        return 0;
    }

    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {

        int nConf = MultinomialIndex.getNumberOfPossibleAssignments(this.multinomialParents);

        double expectedLogNormalizer = 0;

        for (int i = 0; i < nConf; i++) {
            double[] assignment = MultinomialIndex.getVariableArrayAssignmentFromIndex(this.multinomialParents, i);
            double momentValue = 1;
            for (int j = 0; j < assignment.length; j++) {
                momentValue*=momentParents.get(this.multinomialParents.get(j)).get((int)assignment[j]);
            }
            if (momentValue==0)
                continue;

            double partialLogNormalizer = 0;

            if (this.isBaseConditionalDistribution) {
                partialLogNormalizer = this.getBaseEFConditionalDistribution(i).getExpectedLogNormalizer(momentParents);
            }else{
                partialLogNormalizer = this.getBaseEFUnivariateDistribution(i).computeLogNormalizer();
            }

            expectedLogNormalizer+=momentValue*partialLogNormalizer;
        }

        return expectedLogNormalizer;
    }

    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {

        int nConf = MultinomialIndex.getNumberOfPossibleAssignments(this.multinomialParents);

        NaturalParameters expectedNaturalFromParents = null;

        for (int i = 0; i < nConf; i++) {
            double[] assignment = MultinomialIndex.getVariableArrayAssignmentFromIndex(this.multinomialParents, i);
            double momentValue = 1;
            for (int j = 0; j < assignment.length; j++) {
                momentValue*=momentParents.get(this.multinomialParents.get(j)).get((int)assignment[j]);
            }
            NaturalParameters paritalExpectedNatural = null;

            if (this.isBaseConditionalDistribution) {
                paritalExpectedNatural = this.getBaseEFConditionalDistribution(i).getExpectedNaturalFromParents(momentParents);
            }else{
                paritalExpectedNatural = this.getBaseEFUnivariateDistribution(i).createZeroedNaturalParameters();
                paritalExpectedNatural.copy(this.getBaseEFUnivariateDistribution(i).getNaturalParameters());
            }

            paritalExpectedNatural.multiplyBy(momentValue);
            if (expectedNaturalFromParents==null){
                expectedNaturalFromParents=paritalExpectedNatural;
            }else {
                expectedNaturalFromParents.sum(paritalExpectedNatural);
            }
        }

        return expectedNaturalFromParents;
    }

    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        NaturalParameters expectedNaturalToParents = null;

        int indexOfMultinomialParent = this.multinomialParents.indexOf(parent);

        if (indexOfMultinomialParent==-1 && parent.isMultinomial())
            throw new IllegalArgumentException("Parent Variable is multinomial and not included in the list of multinomial parents: "+parent.getName());


        if (indexOfMultinomialParent!=-1) {
            expectedNaturalToParents =  new EF_Multinomial(parent).createZeroedNaturalParameters();//  new ArrayVector(parent.getNumberOfStates());

            int nConf = MultinomialIndex.getNumberOfPossibleAssignments(this.multinomialParents);

            for (int state = 0; state<parent.getNumberOfStates(); state++) {
                double partialSum = 0;
                for (int i = 0; i < nConf; i++) {
                    double[] assignment = MultinomialIndex.getVariableArrayAssignmentFromIndex(this.multinomialParents, i);

                    if (assignment[indexOfMultinomialParent]!=state)
                        continue;

                    double momentValue = 1;
                    for (int j = 0; j < assignment.length; j++) {
                        if (j==indexOfMultinomialParent)
                            continue;
                        momentValue *= momentChildCoParents.get(this.multinomialParents.get(j)).get((int)assignment[j]);
                    }
                    NaturalParameters paritalExpectedNatural = null;

                    double localSum = 0;
                    if (this.isBaseConditionalDistribution) {
                        paritalExpectedNatural = this.getBaseEFConditionalDistribution(i).getExpectedNaturalFromParents(momentChildCoParents);
                        localSum += paritalExpectedNatural.dotProduct(momentChildCoParents.get(this.getVariable()));
                        localSum -= this.getBaseEFConditionalDistribution(i).getExpectedLogNormalizer(momentChildCoParents);
                    } else {
                        paritalExpectedNatural = this.getBaseEFUnivariateDistribution(i).createZeroedNaturalParameters();
                        paritalExpectedNatural.copy(this.getBaseEFUnivariateDistribution(i).getNaturalParameters());
                        localSum += paritalExpectedNatural.dotProduct(momentChildCoParents.get(this.getVariable()));
                        localSum -= this.getBaseEFUnivariateDistribution(i).computeLogNormalizer();
                    }

                    localSum*=momentValue;

                    partialSum+=localSum;
                }

                expectedNaturalToParents.set(state,partialSum);
            }

        }else{

            if (!this.isBaseConditionalDistribution())
                throw new IllegalArgumentException("Parent Variable is no multinomial and based distribution has no parents");



            //int indexOfNonMultinomialParent = this..getBaseEFConditionalDistribution(0).getConditioningVariables().indexOf(parent);
            //if (indexOfMultinomialParent==-1)

            if (!this.parents.contains(parent))
                throw new IllegalArgumentException("Parent Variable is no multinomial and is not included in the list of parents of the base distribution");



            int nConf = MultinomialIndex.getNumberOfPossibleAssignments(this.multinomialParents);

            for (int i = 0; i < nConf; i++) {
                //TODO This might be inefficient for the use of sequential search in a list
                if (!this.getBaseEFConditionalDistribution(i).getConditioningVariables().contains(parent))
                    continue;

                double[] assignment = MultinomialIndex.getVariableArrayAssignmentFromIndex(this.multinomialParents, i);
                double momentValue = 1;
                for (int j = 0; j < assignment.length; j++) {
                        momentValue *= momentChildCoParents.get(this.multinomialParents.get(j)).get((int)assignment[j]);
                }
                NaturalParameters paritalExpectedNatural = null;


                if (this.isBaseConditionalDistribution) {
                    paritalExpectedNatural = this.getBaseEFConditionalDistribution(i).getExpectedNaturalToParent(parent, momentChildCoParents);
                } else {
                    paritalExpectedNatural = this.getBaseEFUnivariateDistribution(i).createZeroedNaturalParameters();
                    paritalExpectedNatural.copy(this.getBaseEFUnivariateDistribution(i).getNaturalParameters());
                }

                paritalExpectedNatural.multiplyBy(momentValue);
                if (expectedNaturalToParents == null) {
                    expectedNaturalToParents = paritalExpectedNatural;
                } else {
                    expectedNaturalToParents.sum(paritalExpectedNatural);
                }
            }


        }
        return expectedNaturalToParents;
    }

    @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables parameters) {

        if (this.getConditioningVariables().size()==0){
            return this.getBaseEFDistribution(0).toExtendedLearningDistribution(parameters);
        }else {
            List<EF_ConditionalDistribution> totalDists = this.distributions.stream()
                    .flatMap(dist -> dist.toExtendedLearningDistribution(parameters).stream())
                    .collect(Collectors.toList());

            List<EF_ConditionalDistribution> dist_NoParameter = totalDists.stream()
                    .filter(dist -> !dist.getVariable().isParameterVariable())
                    .collect(Collectors.toList());

            List<EF_ConditionalDistribution> dist_Parameter = totalDists.stream()
                    .filter(dist -> dist.getVariable().isParameterVariable())
                    .collect(Collectors.toList());

            EF_BaseDistribution_MultinomialParents<EF_ConditionalDistribution> base =
                    new EF_BaseDistribution_MultinomialParents<>(this.multinomialParents, dist_NoParameter, false);

            dist_Parameter.add(base);
            return dist_Parameter;
        }
    }

    @Override
    public <E extends ConditionalDistribution> E toConditionalDistribution() {
        BaseDistribution_MultinomialParents<Distribution> base = new BaseDistribution_MultinomialParents(this.var,this.getConditioningVariables());
        if (this.isBaseConditionalDistribution()) {
            for (int i = 0; i < this.numberOfConfigurations(); i++) {
                base.setBaseDistribution(i, this.getBaseEFConditionalDistribution(i).toConditionalDistribution());
            }
        }else{
            for (int i = 0; i < this.numberOfConfigurations(); i++) {
                base.setBaseDistribution(i, this.getBaseEFUnivariateDistribution(i).toUnivariateDistribution());
            }
        }

        return (E)DistributionTypeEnum.FromBaseDistributionToConditionalDistribution(base);
    }

    private CompoundVector createCompoundVector() {
        return new CompoundVector((EF_Distribution)this.getBaseEFDistribution(0), this.numberOfConfigurations());
    }

    @Override
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables) {

        List<Distribution> distributionList = new ArrayList();
        for (EF_Distribution dist: this.distributions){
            EF_ConditionalDistribution learningDistribution = (EF_ConditionalDistribution)dist;
            ConditionalDistribution conditionalDistribution = learningDistribution.toConditionalDistribution(expectedValueParameterVariables);
            if (conditionalDistribution instanceof BaseDistribution_MultinomialParents){
                BaseDistribution_MultinomialParents base = (BaseDistribution_MultinomialParents)conditionalDistribution;
                distributionList.add(base.getBaseDistribution(0));
            }else {
                distributionList.add(conditionalDistribution);
            }
        }

        return DistributionTypeEnum.FromBaseDistributionToConditionalDistribution(new BaseDistribution_MultinomialParents(this.multinomialParents,distributionList));
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
            vectorMap = new ConcurrentHashMap<Integer,Vector>();
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

            vectorMap = new ConcurrentHashMap<Integer,Vector>();

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
