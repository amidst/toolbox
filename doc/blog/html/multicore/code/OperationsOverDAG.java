package java8paper;

import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rcabanas on 03/10/16.
 */
public class OperationsOverDAG {


	public static void main(String[] args) throws IOException, ClassNotFoundException {


		//Load a network
		BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/simulated/WasteIncinerator.bn");
		DAG dag = bn.getDAG();
		System.out.println(dag);


		//number of links in the DAG
		long nLinks = dag.getParentSets()
				.parallelStream()
				.mapToInt(set -> set.getParents().size())
				.sum();


		System.out.println(nLinks);

		//all the children of a variable
		Variable W = dag.getVariables().getVariableByName("W");
		List<Variable> children = dag.getParentSets()
				.parallelStream()
				.filter(set -> set.contains(W))
				.map(set -> set.getMainVar())
				.collect(Collectors.toList());


		children.forEach(ch -> System.out.println(ch.getName()));


	}






}
