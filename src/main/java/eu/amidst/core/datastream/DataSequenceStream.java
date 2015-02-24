package eu.amidst.core.datastream;

import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 19/02/15.
 */
public final class DataSequenceStream {

    public static Stream<DataSequence> streamOfDataSequences(DataStream<DynamicDataInstance> dataStream){
        return DataSequenceSpliterator.toDataSequenceStream(dataStream);
    }

    public static Stream<DataSequence> parallelStreamOfDataSequences(DataStream<DynamicDataInstance> dataStream){
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(DataSequenceStream.streamOfDataSequences(dataStream), 1);
    }

}
