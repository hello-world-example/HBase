package xyz.kail.demo.hbase.core;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.io.hfile.HFilePrettyPrinter;
import org.apache.hadoop.util.ToolRunner;

import java.util.Arrays;
import java.util.List;

/**
 * -s,--stats               Print statistics
 * -v,--verbose             Verbose output; emits file and meta data delimiters
 * -r,--region <arg>        Region to scan. Pass region name; e.g. 'hbase:meta,,1'
 * -w,--seekToRow <arg>     Seek to this row and print all the kvs for this row only
 */
public class Main {

    public static void main(String[] args) throws Exception {


        List<String> params = Arrays.asList(
                "-r", "dev_boss_remark_v1,,1521207497217.9983d7464bcf4488a0b89a51711048b5.",
                "-v"
        );

        args = params.toArray(new String[params.size()]);

        Configuration conf = HBaseConfiguration.create();
        conf.setFloat(HConstants.HFILE_BLOCK_CACHE_SIZE_KEY, 0);
        conf.set(HConstants.HBASE_DIR, "hdfs://s01.hadoop.ttp.wx:8020/hbase");


        int ret = ToolRunner.run(conf, new HFilePrettyPrinter(), args);
        System.exit(ret);

    }

}
