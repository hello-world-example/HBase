package xyz.kail.demo.hbase.client.curd;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * create '_kail_test_remark', {NAME => 'remark', VERSIONS => 100000}
 *
 * @author kail
 */
public class PutMain {

    public static void main(String[] args) throws IOException {

        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

        TableName kailTestTableName = TableName.valueOf("wordcount");
        Table table = connection.getTable(kailTestTableName);


        List<Put> puts = new ArrayList<>();

        // 这三条数据会相互覆盖，尽管启动了多版本，但是因为执行时间过快，会相互覆盖
        puts.add(new Put(Bytes.toBytes("123_rowkey")).addColumn(Bytes.toBytes("remark"), Bytes.toBytes("100"), "aaa".getBytes()));
        puts.add(new Put(Bytes.toBytes("123_rowkey")).addColumn(Bytes.toBytes("remark"), Bytes.toBytes("100"), "bbb".getBytes()));
        puts.add(new Put(Bytes.toBytes("123_rowkey")).addColumn(Bytes.toBytes("remark"), Bytes.toBytes("100"), "ccc".getBytes()));

        // 第二列
        Put putn = new Put(Bytes.toBytes("123_rowkey"));
        putn.addColumn(Bytes.toBytes("remark"), Bytes.toBytes("200"), Long.valueOf(Long.toString(1L) + System.nanoTime() % 1000), "aaa".getBytes());
        putn.addColumn(Bytes.toBytes("remark"), Bytes.toBytes("200"), Long.valueOf(Long.toString(2L) + System.nanoTime() % 1000), "bbb".getBytes());
        putn.addColumn(Bytes.toBytes("remark"), Bytes.toBytes("200"), Long.valueOf(Long.toString(3L) + System.nanoTime() % 1000), "ccc".getBytes());
        puts.add(putn);

        table.put(puts);

        HBaseTool.Connect.close(connection);

    }

}
