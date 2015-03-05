package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class BayesianVMPLearning implements BayesianLearningAlgorithmForBN {

    EF_BayesianNetwork ef_extendedBN;
    VMP vmp = new VMP();

    DAG dag;
    Map<Variable,Node> variablesToNode;
    List<Node> nodes;
    DataStream<DataInstance> data;

    @Override
    public void runLearning() {

        int cont=0;
        for(DataInstance dataInstance: data){
            System.out.println("Sample: "+cont++);
            this.vmp.setEvidence(dataInstance);
            this.vmp.runInference();
            for (EF_ConditionalDistribution dist: this.ef_extendedBN.getDistributionList()){
                if (dist.getVariable().isParameterVariable()){
                    ((EF_BaseDistribution_MultinomialParents)dist).setBaseEFDistribution(0, this.vmp.getEFPosterior(dist.getVariable()).deepCopy());
                }
            }
        }
    }


    @Override
    public void setDAG(DAG dag) {

        this.dag = dag;
        ParameterVariables parametersVariables = new ParameterVariables(dag.getStaticVariables());

        List<EF_ConditionalDistribution> ef_condDistList = dag.getStaticVariables().getListOfVariables().stream().
                map(var -> var.getDistributionType().newEFConditionalDistribution(dag.getParentSet(var).getParents()).
                        toExtendedLearningDistribution(parametersVariables))
                .flatMap(listOfDist -> listOfDist.stream())
                .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID()).collect(Collectors.toList());

        this.ef_extendedBN = new EF_BayesianNetwork();
        ef_extendedBN.setDistributionList(ef_condDistList);
        vmp.setEFModel(ef_extendedBN);
    }

    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.data = data;
    }

    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        List<ConditionalDistribution> condDistList = new ArrayList<>();

        for (EF_ConditionalDistribution dist: this.ef_extendedBN.getDistributionList()) {
            if (dist.getVariable().isParameterVariable())
                continue;

            EF_ConditionalLearningDistribution distLearning = (EF_ConditionalLearningDistribution)dist;
            Map<Variable, Vector> expectedParameters = new HashMap<>();
            for(Variable var: distLearning.getParameterParentVariables()){
                EF_UnivariateDistribution uni =  ((EF_BaseDistribution_MultinomialParents)this.ef_extendedBN.getDistribution(var)).getBaseEFUnivariateDistribution(0);
                expectedParameters.put(var, uni.getExpectedParameters());
            }
            condDistList.add(distLearning.toConditionalDistribution(expectedParameters));
        }


        condDistList = condDistList.stream().sorted((a, b) -> a.getVariable().getVarID() - b.getVariable().getVarID()).collect(Collectors.toList());


        return BayesianNetwork.newBayesianNetwork(dag,condDistList);
    }

}
