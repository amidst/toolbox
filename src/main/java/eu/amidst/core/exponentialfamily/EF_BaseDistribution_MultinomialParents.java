/**
 * ****************** ISSUE LIST ******************************
 *
 *
 *
 * 1. getConditioningVariables change to getParentsVariables()
 *
 *
 *
 * **********************************************************
 */


package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class EF_BaseDistribution_MultinomialParents<E extends EF_Distribution> extends EF_ConditionalDistribution {


    private List<E> distributions;
    private List<Variable> multinomialParents;
    private List<Variable> nonMultinomialParents;

    public EF_BaseDistribution_MultinomialParents(Variable var, List<Variable> parents) {

        this.var = var;
        this.parents = parents;

        for (Variable v : parents) {
            if (v.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                this.multinomialParents.add(var);
            } else {
                this.nonMultinomialParents.add(var);
            }
        }

        // Computes the size of the array of probabilities as the number of possible assignments for the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(this.multinomialParents);

        // Initialize the distribution uniformly for each configuration of the parents.
        this.distributions = new ArrayList<E>(size);
        for (int i = 0; i < size; i++) {
            this.distributions.add(createNewBaseDistribution(var, nonMultinomialParents));
        }
        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);
    }

    public abstract E createNewBaseDistribution(Variable var, List<Variable> nonMultinomialParents);

    public void setEF_BaseDistribution(int indexMultinomial, E baseDist) {
        this.distributions.set(indexMultinomial,baseDist);
    }

    public E getEF_BaseDistribution(int indexMultinomial) {

        return distributions.get(indexMultinomial);
    }

    public E getEF_BaseDistribution(DataInstance dataInstance) {
        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, dataInstance);
        return distributions.get(position);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(DataInstance instance) {

        SufficientStatistics sufficientStatisticsTotal = new SufficientStatistics(this.sizeOfSufficientStatistics());

        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, instance);

        SufficientStatistics sufficientStatisticsBase = this.distributions.get(position).getSufficientStatistics(instance);

        int sizeSSBase=sufficientStatisticsBase.size()+1;

        sufficientStatisticsTotal.set(position*sizeSSBase, 1);

        for (int i=0; i<sufficientStatisticsBase.size(); i++){
            sufficientStatisticsTotal.set(position*sizeSSBase + 1 + i,sufficientStatisticsBase.get(i));
        }

        return sufficientStatisticsTotal;
    }

    public int sizeOfSufficientStatistics(){
        return this.distributions.size()*(this.distributions.get(0).sizeOfSufficientStatistics()+1);
    }



    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters = parameters;

        int sizeSSBase = this.distributions.get(0).sizeOfSufficientStatistics()+1;
        for (int i=0; i<this.distributions.size(); i++){
            for (int j=0; j<this.distributions.get(0).sizeOfSufficientStatistics(); j++){
                this.distributions.get(i).getNaturalParameters().set(j,parameters.get(sizeSSBase*i + 1 + j));
            }
        }
        this.updateMomentFromNaturalParameters();
    }


    public void setMomentParameters(MomentParameters parameters) {
        this.momentParameters = parameters;

        int sizeBase = this.distributions.get(0).sizeOfSufficientStatistics();
        for (int i=0; i<this.distributions.size(); i++){
            for (int j=0; j<sizeBase; j++){
                this.distributions.get(i).getMomentParameters().set(j,parameters.get(sizeBase*i+j));
            }
        }
        this.updateNaturalFromMomentParameters();
    }

    public void updateNaturalFromMomentParameters(){
        return;
    }

    public void updateMomentFromNaturalParameters(){
        return;
    }

    public double computeLogBaseMeasure(DataInstance dataInstance){
        int position = MultinomialIndex.getIndexFromDataInstance(this.multinomialParents, dataInstance);
        return this.distributions.get(position).computeLogBaseMeasure(dataInstance);
    }

    public double computeLogNormalizer(NaturalParameters parameters){
        return 0;
    }
}
