package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class BayesianVMPLearning implements BayesianLearningAlgorithmForBN {

    VMP vmp = new VMP();

    Map<Variable,Node> variablesToNode;
    List<Node> nodes;
    DataStream<DataInstance> data;

    @Override
    public void runLearning() {

        for(DataInstance dataInstance: data){
            this.vmp.setEvidence(dataInstance);
            this.vmp.runInference();
            for (Node node: this.vmp.getNodes()){
                if (!node.getMainVariable().isParameterVariable())
                    continue;
                ((EF_BaseDistribution_MultinomialParents)node.getPDist()).setBaseEFDistribution(0,node.getQDist().deepCopy());
            }
        }
    }


    @Override
    public void setDAG(DAG dag) {

        ParameterVariables parametersVariables = new ParameterVariables(dag.getStaticVariables());

        List<EF_ConditionalDistribution> ef_condDistList = dag.getStaticVariables().getListOfVariables().stream().
                map(var -> var.getDistributionType().newEFConditionalDistribution(dag.getParentSet(var).getParents()).
                        toExtendedLearningDistribution(parametersVariables))
                .flatMap(listOfDist -> listOfDist.stream())
                .sorted((a,b) -> a.getVariable().getVarID() - b.getVariable().getVarID()).collect(Collectors.toList());

        EF_BayesianNetwork ef_bayesianNetwork = new EF_BayesianNetwork();
        ef_bayesianNetwork.setDistributionList(ef_condDistList);
        vmp.setEFModel(ef_bayesianNetwork);
    }

    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.data = data;
    }

}
