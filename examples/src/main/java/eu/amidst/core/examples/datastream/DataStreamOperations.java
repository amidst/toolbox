package eu.amidst.core.examples.datastream;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.DataSetGenerator;

import java.util.function.Function;

/**
 * Created by rcabanas on 24/10/16.
 */
public class DataStreamOperations {
	public static void main(String[] args) {

		//Generate the data stream using the class DataSetGenerator
		DataStream<DataInstance> data = DataSetGenerator.generate(1,10,5,5);

		//Filter example: print only instances such that DiscreteVar0 = 1.0
		data.filter(d -> d.getValue(data.getAttributes().getAttributeByName("DiscreteVar0")) == 1)
				.forEach(d -> System.out.println(d));

		//Map example: new DataStream in which each instance has been multiplyed by 10
		data.map(d -> {
			Attribute gaussianVar0 = d.getAttributes().getAttributeByName("GaussianVar0");
			d.setValue(gaussianVar0, d.getValue(gaussianVar0)*10);
			return d;
		}).forEach(d -> System.out.println(d));

	}
}
