package xyz.kail.demo.hbase.tools;


import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * HBase 工具类
 *
 * @author kail
 */
@Slf4j
public class HBaseUtils {

    /**
     * 获取 HBase 链接
     */
    public static Connection getConnection(String quorum) throws IOException {
        Configuration hbaseConf = HBaseConfiguration.create();
        hbaseConf.set("hbase.zookeeper.quorum", quorum);
        return ConnectionFactory.createConnection(hbaseConf);
    }

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log.warn("", e);
        }
    }


    public static void printHTableDescriptor(HTableDescriptor hTableDescriptor) {
        TableName tableName = hTableDescriptor.getTableName();
        System.out.println("tableName:" + tableName);

        Map<String, String> configuration = hTableDescriptor.getConfiguration();
        Set<Map.Entry<String, String>> entries = configuration.entrySet();
        System.out.println("    configuration:");
        for (Map.Entry<String, String> entry : entries) {
            System.out.println("        " + entry.getKey() + ":" + entry.getValue());
        }


        System.out.println("    ColumnFamilies:");
        HColumnDescriptor[] columnFamilies = hTableDescriptor.getColumnFamilies();
        for (HColumnDescriptor columnDescriptor : columnFamilies) {
            System.out.println("        " + columnDescriptor);
        }
        System.out.println();
        System.out.println();
    }

    public static void printCell(Cell cell) {
        System.out.print(Bytes.toStringBinary(CellUtil.cloneRow(cell)));
        System.out.print(":");
        System.out.print(Bytes.toStringBinary(CellUtil.cloneFamily(cell)));
        System.out.print(":");
        System.out.print(Bytes.toStringBinary(CellUtil.cloneQualifier(cell)));
        System.out.print(":");
        System.out.print(cell.getTimestamp() + "(" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(cell.getTimestamp())) + ")");
        System.out.print(" => ");
        System.out.println(Bytes.toString(CellUtil.cloneValue(cell)));

        System.out.println();
        System.out.println();

    }

}
