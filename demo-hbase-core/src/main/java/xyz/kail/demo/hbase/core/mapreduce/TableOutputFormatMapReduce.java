package xyz.kail.demo.hbase.core.mapreduce;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapred.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by kail on 2018/3/10.
 */
public class TableOutputFormatMapReduce {

    public class HBaseMapper extends MapReduceBase implements Mapper<LongWritable, Text, LongWritable, Text> {
        @Override
        public void map(LongWritable key, Text values, OutputCollector<LongWritable, Text> output, Reporter reporter) throws IOException {
            output.collect(key, values);
        }
    }


    public class HBaseReducer extends MapReduceBase implements Reducer<LongWritable, Text, ImmutableBytesWritable, Put> {
        @Override
        public void reduce(LongWritable key, Iterator<Text> values,
                           OutputCollector<ImmutableBytesWritable, Put> output, Reporter reporter)
                throws IOException {
            String value = "";
            ImmutableBytesWritable immutableBytesWritable = new ImmutableBytesWritable();
            Text text = new Text();
            while (values.hasNext()) {
                value = values.next().toString();
                if (value != null && !"".equals(value)) {
                    Put put = createPut(value.toString());
                    if (put != null)
                        output.collect(immutableBytesWritable, put);
                }
            }
        }

        // str格式为row:family:qualifier:value 简单模拟下而已
        private Put createPut(String str) {
            String[] strs = str.split(":");
            if (strs.length < 4)
                return null;
            String row = strs[0];
            String family = strs[1];
            String qualifier = strs[2];
            String value = strs[3];
            Put put = new Put(Bytes.toBytes(row));
            put.add(Bytes.toBytes(family), Bytes.toBytes(qualifier), 1L, Bytes.toBytes(value));
            return put;
        }
    }

    public static void main(String[] args) {
        JobConf conf = new JobConf(TableOutputFormatMapReduce.class);
        conf.setMapperClass(HBaseMapper.class);
        conf.setReducerClass(HBaseReducer.class);

        conf.setMapOutputKeyClass(LongWritable.class);
        conf.setMapOutputValueClass(Text.class);

        conf.setOutputKeyClass(ImmutableBytesWritable.class);
        conf.setOutputValueClass(Put.class);

        conf.setOutputFormat(TableOutputFormat.class);

        FileInputFormat.setInputPaths(conf, "/home/yinjie/input");
        FileOutputFormat.setOutputPath(conf, new Path("/home/yinjie/output"));

        conf.set(TableOutputFormat.OUTPUT_TABLE, "t1");
        conf.set("hbase.zookeeper.quorum", "localhost");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        try {
            JobClient.runJob(conf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
