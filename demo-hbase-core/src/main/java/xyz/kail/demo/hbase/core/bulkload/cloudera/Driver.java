package xyz.kail.demo.hbase.core.bulkload.cloudera;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import xyz.kail.demo.hbase.core.util.MapReduceUtil;

import java.util.Iterator;
import java.util.Map;

/**
 * HBase bulk import example<br>
 * Data preparation MapReduce job driver
 * <ol>
 * <li>args[0]: HDFS input path
 * <li>args[1]: HDFS output path
 * <li>args[2]: HBase table name
 * </ol>
 *
 *
 * /opt/websuite/hadoop-2.7.5/bin/hadoop jar /opt/websuite/hbase-1.2.6/lib/hbase-server-1.2.6.jar importtsv  -Dimporttsv.separator="," -Dimporttsv.bulk.output="/user/123/asd" -Dimporttsv.columns="HBASE_ROW_KEY,f:count" wordcount /word_count.csv
 */
public class Driver extends Configured implements Tool {

    public static void main(String[] args) throws Exception {

        args = MapReduceUtil.agrsIO(Driver.class, args);

        int run = ToolRunner.run(new Driver(), args);

        System.exit(run);

    }

    @Override
    public void setConf(Configuration conf) {
        super.setConf(conf);
        if (null != conf) {

            System.out.println();
            System.out.println();
            Iterator<Map.Entry<String, String>> iterator = conf.iterator();
            for (; iterator.hasNext(); ) {
                Map.Entry<String, String> entry = iterator.next();
                System.out.println(entry.getKey() + "=" + entry.getValue());
            }
            System.out.println();
            System.out.println();

            // conf.set("fs.defaultFS", "hdfs://localhost:9000"); // 默认 fs.defaultFS=file:///


        }
    }

    @Override
    public int run(String[] args) throws Exception {

        Configuration conf = this.getConf();


        Connection connection = ConnectionFactory.createConnection(conf);


        HTable wordCountTable = (HTable) connection.getTable(TableName.valueOf("wordcount"));


        Job job = Job.getInstance(conf, "HBase Bulk Import Example");
        job.setJarByClass(HBaseKVMapper.class);

        job.setMapperClass(HBaseKVMapper.class);

        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(KeyValue.class);


        // Auto configure partitioner and reducer
        job.setInputFormatClass(TextInputFormat.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
//        HFileOutputFormat2.configureIncrementalLoad(job, wordCountTable, wordCountTable.getRegionLocator());

        return job.waitForCompletion(true) ? 1 : 0;
    }
}