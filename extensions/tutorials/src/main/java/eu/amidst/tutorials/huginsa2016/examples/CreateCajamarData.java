package eu.amidst.tutorials.huginsa2016.examples;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import org.apache.commons.lang.math.IntRange;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by rcabanas on 20/05/16.
 */
public class CreateCajamarData {

    public static void main(String[] args) throws Exception{

        int nContinuousAttributes=4;
        String path = "datasets/simulated/";

        int nSamples=1000;



        List<Attribute> list = new ArrayList<Attribute>();
        String names[] = {"SEQUENCE_ID", "TIME_ID", "VAR3","VAR5","VAR11","VAR15","VAR17","VAR18","Default","VAR7","VAR8","VAR10","VAR13"};

        List<StateSpaceType> listSpaces = new ArrayList<StateSpaceType>();
        listSpaces.add(new RealStateSpace());
        listSpaces.add(new RealStateSpace());
        listSpaces.add(new FiniteStateSpace(Arrays.asList(new String[]{"-1","1","2"})));
        listSpaces.add(new FiniteStateSpace(Arrays.asList(new String[]{"-1","0","1","2"})));
        listSpaces.add(new FiniteStateSpace(Arrays.asList(new String[]{"-1","0","1","2"})));
        listSpaces.add(new FiniteStateSpace(Arrays.asList(new String[]{"-1","0","1","2"})));
        listSpaces.add(new FiniteStateSpace(Arrays.asList(new String[]{"-1","0","1","2"})));
        listSpaces.add(new FiniteStateSpace(Arrays.asList(new String[]{"s00","s01","s02","s03","s04"})));
        listSpaces.add(new FiniteStateSpace(Arrays.asList(new String[]{"s00","s01"})));
        listSpaces.add(new RealStateSpace());
        listSpaces.add(new RealStateSpace());
        listSpaces.add(new RealStateSpace());
        listSpaces.add(new RealStateSpace());


        int nOfDisc = (int) listSpaces.stream().filter(s -> s instanceof FiniteStateSpace).count();
        int nDiscreteStates[] = listSpaces.stream()
                 .filter(s -> s instanceof FiniteStateSpace)
                 .mapToInt(s -> ((FiniteStateSpace)s).getNumberOfStates()).toArray();





        DynamicBayesianNetworkGenerator.setSeed(1234);
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(nContinuousAttributes);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(nDiscreteStates.length);
        DynamicBayesianNetworkGenerator.setNumberOfStates(0);
        int nTotal = nDiscreteStates.length+nContinuousAttributes;
        int nLinksMin = nTotal-1;
        int nLinksMax = nTotal*(nTotal-1)/2;
        DynamicBayesianNetworkGenerator.setNumberOfLinks((int)(0.9*nLinksMin + 0.1*nLinksMax));

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicBayesianNetwork(nDiscreteStates);

        Variable var = dbn.getDynamicVariables().getVariableById(0);

        dbn.getDynamicVariables().getListOfDynamicVariables().forEach(v -> System.out.println(v.getName()));


        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(1234);
        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(nSamples/50,50);






        IntStream.range(0, data.getAttributes().getNumberOfAttributes())
                .forEach(i -> {
                    Attribute a = data.getAttributes().getFullListOfAttributes().get(i);
                    StateSpaceType s = listSpaces.get(i);
                    Attribute a2 = new Attribute(a.getIndex(), names[i],s);
                    list.add(a2);
                });




        Attributes att2 = new Attributes(list);


        att2.forEach(a -> {System.out.println(a.isSpecialAttribute()+"---"+a.getName());});

        List<DynamicDataInstance> listData = data.stream().collect(Collectors.toList());



        DataStream<DynamicDataInstance> data2 =
                new DataOnMemoryListContainer<DynamicDataInstance>(att2,listData);

        DataStreamWriter.writeDataToFile(data2, path+"cajamar.arff");

             final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
             DataFlink<DataInstance> data2Flink = DataFlinkLoader.loadDataFromFile(env, path + "cajamar.arff", false);


            DataFlinkWriter.writeDataToARFFFolder(data2Flink, path+"cajamarDistributed.arff");




    }

}
