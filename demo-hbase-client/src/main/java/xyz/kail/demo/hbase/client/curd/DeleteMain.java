package xyz.kail.demo.hbase.client.curd;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;

import java.io.IOException;

/**
 * TODO 注释
 *
 * @author kail
 */
public class DeleteMain {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

        Table table = connection.getTable(TableName.valueOf("test"));

        Delete delete = new Delete(Bytes.toBytes("k1"));
        delete.addColumn(Bytes.toBytes("f"), Bytes.toBytes("c"), 1520865214884L);
        delete.addColumns(Bytes.toBytes("f"), Bytes.toBytes("c"), 1520865214884L);
        table.delete(delete);


        HBaseTool.Connect.close(connection);

    }

}
