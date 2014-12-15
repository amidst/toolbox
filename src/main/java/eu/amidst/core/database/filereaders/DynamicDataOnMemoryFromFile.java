package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnMemoryFromFile implements DataOnMemory, DataOnDisk, DataOnStream, Iterator<DataInstance> {

    private DataFileReader reader;
    private Iterator<DataRow> dataRowIterator;
    private Attribute attSequenceID;
    private Attribute attTimeID;
    private NextDynamicDataInstance nextDynamicDataInstance;
    private DynamicDataInstance[] dataInstances;
    private int pointer = 0;


    public DynamicDataOnMemoryFromFile(DataFileReader reader1) {
        this.reader = reader1;
        dataRowIterator = this.reader.iterator();


        List<DynamicDataInstance> dataInstancesList = new ArrayList<>();

        DataRow present;
        DataRow past = new DataRowMissing();

        int timeID = 0;
        int sequenceID = 0;

        if (dataRowIterator.hasNext()) {
            present = this.dataRowIterator.next();
        }
        else {
            throw new UnsupportedOperationException("There are insufficient instances to learn a model.");
        }

        try {
            attSequenceID = this.reader.getAttributes().getAttributeByName("SEQUENCE_ID");
            sequenceID = (int)present.getValue(attSequenceID);
        }catch (UnsupportedOperationException e){
            attSequenceID = null;
        }
        try {
            attTimeID = this.reader.getAttributes().getAttributeByName("TIME_ID");
            timeID = (int)present.getValue(attSequenceID);
        }catch (UnsupportedOperationException e){
            attTimeID = null;
        }

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present, sequenceID, timeID);

        while (dataRowIterator.hasNext()) {

            /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
            /* 1 = true,  false, i.e., TimeID is provided */
            /* 2 = false, true,  i.e., SequenceID is provided */
            /* 3 = true,  true,  i.e., SequenceID is provided*/
            int option = (attTimeID == null) ? 0 : 1 + 2 * ((attSequenceID == null) ? 0 : 1);

            switch (option) {

            /* Not sequenceID nor TimeID are provided*/
                case 0:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(dataRowIterator));

             /* Only TimeID is provided*/
                case 1:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoSeq(dataRowIterator, attTimeID));

             /* Only SequenceID is provided*/
                case 2:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoTimeID(dataRowIterator, attSequenceID));

             /* SequenceID and TimeID are provided*/
                case 3:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance(dataRowIterator, attSequenceID, attTimeID));
            }
            throw new IllegalArgumentException();
        }
        reader.restart();
        this.dataRowIterator=reader.iterator();

        dataInstances = new DynamicDataInstance[dataInstancesList.size()];
        int counter = 0;
        for (DynamicDataInstance inst : dataInstancesList) {
            dataInstances[counter] = inst;
            counter++;
        }

    }

    @Override
    public int getNumberOfDataInstances() {
        return dataInstances.length;
    }

    @Override
    public DataInstance getDataInstance(int i) {
        return dataInstances[i];
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public DataInstance next() {
        if (pointer >= getNumberOfDataInstances()) {
            throw new UnsupportedOperationException("Make sure to call hasNext() to know when the sequence " +
                    "has finished (restart() moves the reader pointer to the beginning");
        }
        return dataInstances[pointer++];
    }

    @Override
    public boolean hasNext() {
        return pointer < getNumberOfDataInstances();
    }

    @Override
    public void restart() {
        pointer = 0;
    }

    @Override
    public Iterator<DataInstance> iterator() {
        return this;
    }
}