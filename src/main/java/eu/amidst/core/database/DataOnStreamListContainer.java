package eu.amidst.core.database;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/12/14.
 */
public class DataOnStreamListContainer implements DataOnStream {
    List<DataInstance> instanceList;
    Attributes attributes;

    public DataOnStreamListContainer(Attributes attributes1){
        this.instanceList=new ArrayList();
        this.attributes=attributes1;
    }

    public DataOnStreamListContainer(List<Attribute> attributes1){
        this.instanceList=new ArrayList();
        this.attributes=new Attributes(attributes1);
    }

    public void add(int id, DataInstance data){
        this.instanceList.add(id,data);
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public Stream<DataInstance> stream() {
        return this.instanceList.stream();
    }
}
