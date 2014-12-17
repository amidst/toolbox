package eu.amidst.core.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andresmasegosa on 11/12/14.
 */
public class DataOnDiskListContainer implements DataOnDisk {
    List<DataInstance> instanceList;
    Attributes attributes;

    public DataOnDiskListContainer(Attributes attributes1){
        this.instanceList=new ArrayList();
        this.attributes=attributes1;
    }

    public void add(int id, DataInstance data){
        this.instanceList.add(id,data);
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public void close() {

    }

    @Override
    public Iterator<DataInstance> iterator() {
        return this.instanceList.iterator();
    }

    @Override
    public void restart() {
    }
}
