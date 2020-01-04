package xyz.kail.demo.hbase.core.admin;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by kail on 2018/3/5.
 */
public class HBaseConfigurationMain {

    public static void main(String[] args) {
        //创建HBase的配置对象
        Configuration hbaseConf = HBaseConfiguration.create();
        hbaseConf.set("hbase.zookeeper.quorum", "s02.hadoop.ttp.wx,s04.hadoop.ttp.wx,s03.hadoop.ttp.wx");

        Iterator<Map.Entry<String, String>> iterator = hbaseConf.iterator();
        for (; iterator.hasNext(); ) {
            Map.Entry<String, String> next = iterator.next();
            System.out.println(next.getKey() + "=" + next.getValue());
        }


    }

}
