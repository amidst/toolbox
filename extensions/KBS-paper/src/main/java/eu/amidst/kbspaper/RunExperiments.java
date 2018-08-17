package eu.amidst.kbspaper;

import COM.hugin.HAPI.*;
import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.latentvariablemodels.staticmodels.FactorAnalysis;
import eu.amidst.latentvariablemodels.staticmodels.GaussianMixture;
import eu.amidst.latentvariablemodels.staticmodels.MixtureOfFactorAnalysers;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Created by rcabanas on 14/08/2018.
 */




public class RunExperiments {

    public static void main(String[] args) throws IOException {


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String datestr = sdf.format(new Date());

        LogResults log = new LogResults(conf.logfolder+"log"+datestr);



        for (String bnname : conf.BNs) {


            for (long i = conf.s_init; i <= conf.s_stop; i = i + conf.s_step) {

                long t = 0;

                try {


                    t = hugin_learning(i, bnname);


                    System.gc();


                } catch (Error e) {
                    System.out.println(e.getMessage());
                    System.out.println("Hugin error at at " + i);
                    t = -1;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Hugin exception at at " + i);
                    t = -2;

                } finally {
                    log.record("hugin", bnname, i, conf.numvars, getFilesSize(i), t);
                    System.gc();
                    if (t < 0)
                        break;

                }


            }

            for (long i = conf.s_init; i <= conf.s_stop; i = i + conf.s_step) {

                long t = 0;

                try {

                    t = amidst_learning(i, bnname);


                } catch (Error e) {
                    System.out.println(e.getMessage());
                    System.out.println("AMIDST error at at " + i);
                    t = -1;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("AMIDST exception at at " + i);
                    t = -2;

                } finally {
                    log.record("amidst", bnname, i, conf.numvars, getFilesSize(i), t);
                    System.gc();
                    if (t < 0)
                        break;

                }

            }
        }



        log.getPrinter().close();


    }


    public static long amidst_learning(long s, String bnname) throws IOException {

        String file = conf.datafolder +"data_"+(s/1000)+"k.arff";

        System.out.println("amidst learning "+bnname+" from "+file);


        Stopwatch stopwatch = Stopwatch.createStarted();

        DataStream<DataInstance> dataStream = DataStreamLoader.open(file);


        eu.amidst.latentvariablemodels.staticmodels.Model m = null;


        if (bnname == "mog") {
            m = new GaussianMixture(dataStream.getAttributes())
                    .setDiagonal(true)
                    .setNumStatesHiddenVar(conf.pca_numStatesHiddenVar);

        } else if (bnname == "mfa") {

            m = new MixtureOfFactorAnalysers(dataStream.getAttributes())
                    .setNumberOfLatentVariables(conf.mfa_numLatent)
                    .setNumberOfStatesLatentDiscreteVar(conf.mfa_numStatesLatent);
        } else if (bnname == "fa") {

            m = new FactorAnalysis(dataStream.getAttributes())
                    .setNumberOfLatentVariables(conf.mfa_numLatent);
        }

        m.updateModel(dataStream);

        stopwatch.stop(); // optional

        long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println(millis+" ms.");
        return millis;


    }







    public static long hugin_learning(long s, String bnname) throws ExceptionHugin, IOException, ClassNotFoundException {

        // load the network structure

        //transform it

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile(conf.netfolder +bnname+".bn");
        Domain huginNetwork = BNConverterToHugin.convertToHugin(bn);


        // time start
        Stopwatch stopwatch = Stopwatch.createStarted();


        // load all the instances and transform (or use a hugin compatible type)

        String file = conf.datafolder + "data_"+(s/1000)+"k.arff";

        System.out.println("hugin learning "+bnname+" from "+file);

        DataStream<DataInstance> dataStream = DataStreamLoader.open(file);

        DataOnMemoryListContainer<DataInstance> dataOnMemory = new DataOnMemoryListContainer(dataStream.getAttributes());
        Stream<DataInstance> stream = dataStream.stream().map(a -> (DataInstance)a);
        List<DataInstance> list = stream.collect(Collectors.toList());
        dataOnMemory.addAll(list);


        // make the inference of the parameters

        // Set the number of cases
        int numCases = dataOnMemory.getNumberOfDataInstances();
        huginNetwork.setNumberOfCases(numCases);
        huginNetwork.setConcurrencyLevel(1);
        NodeList nodeList = huginNetwork.getNodes();

        // It is more efficient to loop the matrix of values in this way. 1st variables and 2nd cases
        for (int i = 0; i < nodeList.size(); i++) {
            Variable var = bn.getDAG().getVariables().getVariableById(i);
            Node n = (Node) nodeList.get(i);
            if (n.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0) {
                ((DiscreteChanceNode) n).getExperienceTable();
                for (int j = 0; j < numCases; j++) {
                    double state = dataOnMemory.getDataInstance(j).getValue(var);
                    if (!Utils.isMissingValue(state))
                        ((DiscreteChanceNode) n).setCaseState(j, (int)state);
                }
            } else {
                ((ContinuousChanceNode) n).getExperienceTable();
                for (int j = 0; j < numCases; j++) {
                    double value = dataOnMemory.getDataInstance(j).getValue(var);
                    if (!Utils.isMissingValue(value))
                        ((ContinuousChanceNode) n).setCaseValue(j, value);
                }
            }
        }

        huginNetwork.compile();

        //parameter learning
        huginNetwork.learnTables();

        // System.out.println((BNConverterToAMIDST.convertToAmidst(huginNetwork)));
        // time stop and save
        stopwatch.stop(); // optional

        long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        System.out.println(millis+" ms.");

        return millis;



    }


    private  static long getFilesSize(long s){
        File f = new File(conf.datafolder +"data_"+(s/1000)+"k.arff");
        return FileUtils.sizeOf(f);


    }



}
