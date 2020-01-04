package xyz.kail.demo.hbase.cdh5.dbversion;

import lombok.Cleanup;
import org.apache.hadoop.hbase.client.Connection;
import xyz.kail.demo.hbase.core.util.HBaseUtils;

import java.io.IOException;

/**
 *  GP 官方文档： https://gpdb.docs.pivotal.io/5240/pxf/hbase_pxf.html
 *  GP 中文文档： https://docs.greenplum.cn/6-0/pxf/overview_pxf.html
 */
public class DbVersionMain {

    private static final String quorum = "s02.hadoop.ttp.wx:2181,s03.hadoop.ttp.wx:2181,s04.hadoop.ttp.wx:2181";

    public static void main(String[] args) throws IOException {

        @Cleanup final Connection connection = HBaseUtils.getConnection(quorum);


    }

}
