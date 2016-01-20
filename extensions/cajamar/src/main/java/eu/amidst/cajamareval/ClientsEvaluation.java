package eu.amidst.cajamareval;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.OptionParser;
import eu.amidst.core.utils.Utils;
import eu.amidst.huginlink.inference.HuginInference;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by dario on 08/01/16.
 */
public class ClientsEvaluation {

    public static void main(String[] args) throws IOException, ClassNotFoundException  {

        String fileModel= args[0];
        String fileTest = args[1];
        String fileOutput = args[2];

        String classVariableName = "Default";

        DataStream<DataInstance> test = DataStreamLoader.openFromFile(fileTest);
        FileWriter fw = new FileWriter(fileOutput);

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile(fileModel);
        System.out.println(bn);

        HuginInference inference = new HuginInference();
        inference.setModel(bn);

        Attribute seq_id = test.getAttributes().getSeq_id();
        Attribute classAtt = test.getAttributes().getAttributeByName(classVariableName);
        double nNontest = 0;
        for (DataInstance dataInstance : test) {
            dataInstance.setValue(classAtt, Utils.missingValue());
            try {
                inference.setEvidence(dataInstance);
                inference.runInference();
                Multinomial dist = inference.getPosterior(bn.getVariables().getVariableByName(classVariableName));

                double pred = dist.getParameters()[1];

                fw.write(dataInstance.getValue(seq_id) + "\t" + pred + "\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                nNontest++;
            }
        }

        System.out.println("Non tested clients: " + nNontest);

        fw.close();
    }
}
