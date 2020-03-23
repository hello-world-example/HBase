package xyz.kail.demo.hbase1.client;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Table;
import xyz.kail.demo.hbase.tools.HBaseTemplate;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseTemplate.Connect.getConnection("");
        HTableInterface table = (HTableInterface)connection.getTable(TableName.valueOf(""));
        table.setAutoFlush(false);
        table.setWriteBufferSize(123);
        table.flushCommits();
    }

}
