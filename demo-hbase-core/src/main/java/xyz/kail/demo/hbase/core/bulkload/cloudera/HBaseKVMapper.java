package xyz.kail.demo.hbase.core.bulkload.cloudera;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * HBase bulk import example
 * <p>
 * Parses Facebook and Twitter messages from CSV files and outputs <ImmutableBytesWritable, KeyValue>.
 * <p>
 * The ImmutableBytesWritable key is used by the TotalOrderPartitioner to map it into the correct HBase table region.
 * <p>
 * The KeyValue value holds the HBase mutation information (column family, column, and value)
 */
public class HBaseKVMapper extends Mapper<LongWritable, Text, ImmutableBytesWritable, KeyValue> {


    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String[] line = value.toString().split(",");

        ImmutableBytesWritable rowKey = new ImmutableBytesWritable(line[0].getBytes());

        KeyValue keyValue = new KeyValue(rowKey.get(), "f".getBytes(), "ad".getBytes(), line[1].getBytes());

        context.write(rowKey, keyValue);

    }
}