package xyz.kail.demo.hbase.client.scan;

import lombok.Cleanup;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.metrics.ScanMetrics;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;
import xyz.kail.demo.hbase.tools.HBaseUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class RowKeyScanMain {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

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
        final ScanMetrics scanMetrics = scanner.getScanMetrics();
        System.out.println(scanMetrics.getMetricsMap());
        System.out.println();

        //
        Iterator<Result> iterator = scanner.iterator();
        for (; iterator.hasNext(); ) {
            Result result = iterator.next();
            List<Cell> cells = result.listCells();
            for (Cell cell : cells) {
                HBaseUtils.printCell(cell);
            }
            System.out.println();
        }

    }

}
