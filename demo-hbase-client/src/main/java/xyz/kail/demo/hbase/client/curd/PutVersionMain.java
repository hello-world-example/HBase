package xyz.kail.demo.hbase.client.curd;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTemplate;

import java.io.IOException;

/**
 * create '_kail_test_remark', {NAME => 'remark', VERSIONS => 100000}
 *
 * @author Kail
 */
public class PutVersionMain {

    public static void main(String[] args) throws IOException {

        Connection connection = HBaseTemplate.Connect.getConnection(Rcore.QUORUM);

        TableName kailTestTableName = TableName.valueOf("_kail_test_remark");
        Table table = connection.getTable(kailTestTableName);

//        Delete delete = new Delete(Bytes.toBytes("123_rowkey"));
//        delete.addColumn(Bytes.toBytes("remark"), Bytes.toBytes("200"), 1612L);
//        table.delete(delete);

        Put putn = new Put(Bytes.toBytes("123_rowkey"));
        putn.addColumn(Bytes.toBytes("remark"), Bytes.toBytes("200"), 1600L, "1600L".getBytes());
        putn.addColumn(Bytes.toBytes("remark"), Bytes.toBytes("200"), 1611L, "1611L".getBytes());
        putn.addColumn(Bytes.toBytes("remark"), Bytes.toBytes("200"), 1622L, "1622L".getBytes());
        table.put(putn);


        HBaseTemplate.Connect.close(connection);

    }

}
