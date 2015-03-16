package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class EF_LearningBayesianNetwork extends EF_Distribution {

    List<EF_ConditionalLearningDistribution> distributionList;
    ParameterVariables parametersVariables;

    public EF_LearningBayesianNetwork(DAG dag){

        parametersVariables = new ParameterVariables(dag.getStaticVariables());

        distributionList =
                dag.getStaticVariables()
                        .getListOfVariables()
                        .stream()
                        .map(var -> var.getDistributionType().newEFConditionalDistribution(dag.getParentSet(var).getParents()).toExtendedLearningDistribution(parametersVariables))
                        .flatMap(listOfDist -> listOfDist.stream())
                        .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID())
                        .collect(Collectors.toList());

        this.naturalParameters = null;
        this.momentParameters = null;
    }

    public EF_LearningBayesianNetwork(List<EF_ConditionalDistribution> distributions, StaticVariables staticVariables){

        parametersVariables = new ParameterVariables(staticVariables);

        distributionList =
                distributions
                        .stream()
                        .map(dist -> dist.toExtendedLearningDistribution(parametersVariables))
                        .flatMap(listOfDist -> listOfDist.stream())
                        .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID())
                        .collect(Collectors.toList());

        this.naturalParameters = null;
        this.momentParameters = null;
    }

    public EF_LearningBayesianNetwork(List<EF_ConditionalDistribution> distributions, DynamicVariables dynamicVariables){

        parametersVariables = new ParameterVariables(dynamicVariables);

        distributionList =
                        distributions
                        .stream()
                        .map(dist -> dist.toExtendedLearningDistribution(parametersVariables))
                        .flatMap(listOfDist -> listOfDist.stream())
                        .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID())
                        .collect(Collectors.toList());

        this.naturalParameters = null;
        this.momentParameters = null;
    }

    public List<ConditionalDistribution> toConditionalDistribution(){
        List<ConditionalDistribution> condDistList = new ArrayList<>();

        for (EF_ConditionalDistribution dist: distributionList) {
            if (dist.getVariable().isParameterVariable())
                continue;

            EF_ConditionalLearningDistribution distLearning = (EF_ConditionalLearningDistribution)dist;
            Map<Variable, Vector> expectedParameters = new HashMap<>();
            for(Variable var: distLearning.getParameterParentVariables()){
                EF_UnivariateDistribution uni =  ((EF_BaseDistribution_MultinomialParents)distributionList.get(var.getVarID())).getBaseEFUnivariateDistribution(0);
                expectedParameters.put(var, uni.getExpectedParameters());
            }
            condDistList.add(distLearning.toConditionalDistribution(expectedParameters));
        }


        condDistList = condDistList.stream().sorted((a, b) -> a.getVariable().getVarID() - b.getVariable().getVarID()).collect(Collectors.toList());

        return condDistList;
    }

    public EF_BayesianNetwork toEFBayesianNetwork(){
        EF_BayesianNetwork ef_bn = new EF_BayesianNetwork();
        List<EF_ConditionalDistribution> distributions = new ArrayList<>();
        distributions.addAll(this.distributionList);
        ef_bn.setDistributionList(distributions);

        return ef_bn;
    }

    public List<EF_ConditionalLearningDistribution> getDistributionList() {
        return distributionList;
    }


    public EF_ConditionalLearningDistribution getDistribution(Variable var) {
        return distributionList.get(var.getVarID());
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public int sizeOfSufficientStatistics() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("Method not implemented yet!");
    }

}
