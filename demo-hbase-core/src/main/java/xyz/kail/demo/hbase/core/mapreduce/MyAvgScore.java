package xyz.kail.demo.hbase.core.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.StringTokenizer;


public class MyAvgScore extends Configured implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(MyAvgScore.class);

    private static final String NEW_LINE = System.getProperty("line.separator");

    /**
     *
     */
    public static class MyMap extends Mapper<Object, Text, Text, IntWritable> {

        Connection hbaseConfig = null;

        {
            Configuration hbaseConf = HBaseConfiguration.create();
            try {
                hbaseConfig = ConnectionFactory.createConnection(hbaseConf);
            } catch (IOException e) {
                logger.error("HBase 链接初始化失败", e);

            }
        }


        private static IntWritable lineNum = new IntWritable(1);//初始化一个变量值

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {


            String stuInfo = value.toString();

            logger.info("MapSudentInfo:" + stuInfo);

            StringTokenizer tokenizerArticle = new StringTokenizer(stuInfo, NEW_LINE);

            while (tokenizerArticle.hasMoreTokens()) {

                StringTokenizer tokenizer = new StringTokenizer(tokenizerArticle.nextToken());

                String name = tokenizer.nextToken(); // 第一列
                String score = tokenizer.nextToken(); // 第二列

                Text stu = new Text(name);
                int intScore = Integer.parseInt(score);

                logger.info("MapStu:" + stu.toString() + " " + intScore);

                context.write(stu, new IntWritable(intScore));  //zs 90

                Table table = hbaseConfig.getTable(TableName.valueOf("stu"));

                Put p1 = new Put(Bytes.toBytes("name" + lineNum));
                p1.addColumn(Bytes.toBytes("name"), Bytes.toBytes("1"), Bytes.toBytes(name));
                table.put(p1);//put 'stu','name','name:1','zs'  


                Put p2 = new Put(Bytes.toBytes("score" + lineNum));
                p2.addColumn(Bytes.toBytes("score"), Bytes.toBytes("1"), Bytes.toBytes(score));
                table.put(p2);//put 'stu','score','score:1','90'  

                lineNum = new IntWritable(lineNum.get() + 1);//对变量值进行变值处理
            }
        }

    }


    public int run(String[] args) throws Exception {

        Job job = Job.getInstance(getConf(), "avgscore");
        job.setJarByClass(MyAvgScore.class);


        job.setMapperClass(MyMap.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);


        /*
         * 设置 输入出处路径
         */
        job.setInputFormatClass(TextInputFormat.class);
        FileInputFormat.addInputPath(job, new Path(args[0])); // 设置输入文件路径

        job.setOutputFormatClass(TextOutputFormat.class);
        FileOutputFormat.setOutputPath(job, new Path(args[1])); // 设置输出文件路径

        /*
         * 等待执行完成
         */
        boolean success = job.waitForCompletion(true);

        return success ? 0 : 1;

    }


    public static void main(String[] args) throws Exception {

        int ret = ToolRunner.run(new MyAvgScore(), args);
        System.exit(ret);

        TableOutputFormat<Object> objectTableOutputFormat = new TableOutputFormat<>();
    }


} 