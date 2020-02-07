package xyz.kail.demo.hbase.client.scan;

import lombok.Cleanup;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScanInitDataMain {

    public static final TableName tableName = TableName.valueOf("scan_init_data_main");

    /**
     * 创建表
     */
    private static void createTable(Connection connection) throws IOException {

        if (HBaseTool.Table.exist(connection, tableName)) {
            return;
        }

        final ColumnFamilyDescriptor family = ColumnFamilyDescriptorBuilder
                .newBuilder(Bytes.toBytes("F"))
                .setMaxVersions(10)
                .build();

        HBaseTool.Table.create(connection, tableName, family);
    }

    private static void putData(Connection connection) throws IOException, InterruptedException {

        @Cleanup final Table table = connection.getTable(tableName);

        List<Put> putList = new ArrayList<>();

        for (long i = 1000; i < 10_0000; i++) {
            Put put = new Put(Bytes.toBytes(HBaseTool.RowKey.reverse(i) + "," + (Long.MAX_VALUE - System.currentTimeMillis())));
            put.addColumn(Bytes.toBytes("F"), Bytes.toBytes("a"), 1L, Bytes.toBytes(i));
            put.addColumn(Bytes.toBytes("F"), Bytes.toBytes("a"), 2L, Bytes.toBytes(i));
            putList.add(put);

            TimeUnit.MILLISECONDS.sleep(1);

            System.out.println("add " + i);
        }

        System.out.println("putList ok!");

        final long start = System.currentTimeMillis();
        System.out.println("start " + start);
        table.put(putList);

        final long end = System.currentTimeMillis();
        System.out.println("end " + end);
        System.out.println("end - start " + (end - start));

    }


    public static void main(String[] args) throws IOException, InterruptedException {

        @Cleanup final Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

        // 创建表
        createTable(connection);

        // 创建数据
        putData(connection);


    }

}
