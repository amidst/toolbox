package java8paper;

import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Vector;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Created by rcabanas on 03/10/16.
 */
public class MLexample {

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		int batchSize = 100;
		int seed = 1234;
		int nSamples = 500;
		int nDiscreteAtts = 4;
		int nContAtts = 0;

		//Load a data stream
		DataStream<DataInstance> data = DataSetGenerator.generate(seed, nSamples, nDiscreteAtts, nContAtts);

		//Generate a DAG with the naive bayes structure
		DAG dag = DAGGenerator.getNaiveBayesStructure(data.getAttributes(), "DiscreteVar0");
		System.out.println(dag);


		//Computes the vector of sufficient statistics
		Vector sumSS = MLE(data, dag);
		System.out.println(sumSS.output());

	}




	public static Vector MLE(DataStream<DataInstance> data, DAG dag) {
		Stream<DataOnMemory<DataInstance>> stream = data.parallelStreamOfBatches(100);


		EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(dag);

		Vector sumSS = stream
				.map(batch -> batch.stream()
						.map(efBayesianNetwork::getSufficientStatistics)
						.reduce(SufficientStatistics::sumVectorNonStateless)
						.get())
				.reduce(SufficientStatistics::sumVectorNonStateless).get();

		sumSS.divideBy(data.stream().count());
		return  sumSS;

	}
	

}
