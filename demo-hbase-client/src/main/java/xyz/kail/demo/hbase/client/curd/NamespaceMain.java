package xyz.kail.demo.hbase.client.curd;

import lombok.Cleanup;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;
import xyz.kail.demo.hbase.tools.HBaseUtils;
import xyz.kail.demo.hbase.tools.Slf4jSimpleTool;

import java.io.IOException;

public class NamespaceMain {

    static {
        Slf4jSimpleTool.init();
    }

    final static Logger log = LoggerFactory.getLogger(NamespaceMain.class);

    public static void main(String[] args) throws IOException {


        @Cleanup Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

        final TableName tableName = TableName.valueOf("test:T_USER");
        log.info("==={}", tableName);

        @Cleanup final Table table = connection.getTable(tableName);
        log.info("==={}", table);

        try {
            final TableDescriptor descriptor = table.getDescriptor();
            log.info("==={}", descriptor);
        } catch (TableNotFoundException ex) {
            log.error("==={}", ex.getMessage());
            HBaseTool.Table.create(connection, tableName);
        }

        final boolean tableExists = HBaseTool.Table.exist(connection, tableName);
        log.info("==={}", tableExists);
    }


}
