package xyz.kail.demo.hbase.client.scan;

import lombok.Cleanup;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTemplate;

import java.io.IOException;

/**
 *
 */
public class RowKeyScanMain {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseTemplate.Connect.getConnection(Rcore.QUORUM);

        Table table = connection.getTable(ScanInitDataMain.tableName);

        final Scan scan = new Scan()
                .withStartRow(Bytes.toBytes("11111,"))
                .withStopRow(Bytes.toBytes("11111,A"))
                // .addColumn(Bytes.toBytes("C"), Bytes.toBytes("ID"))
                // .addColumn(Bytes.toBytes("C"), Bytes.toBytes("AGE"))
                // .setLimit(2)
                // .setOneRowLimit() // == setLimit(1).setReadType(ReadType.PREAD)
                .setScanMetricsEnabled(true);

        @Cleanup final ResultScanner scanner = table.getScanner(scan);

        HBaseTemplate.Debug.printScanner(scanner);

    }

}
