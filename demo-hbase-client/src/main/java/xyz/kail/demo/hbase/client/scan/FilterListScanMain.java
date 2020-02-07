package xyz.kail.demo.hbase.client.scan;

import lombok.Cleanup;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.metrics.ScanMetrics;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PrefixFilter;
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
public class FilterListScanMain {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

        Table table = connection.getTable(ScanInitDataMain.tableName);

        // 多行键前缀查找
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("1111,")));
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("8888,")));
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("9999,")));
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("99999,")));
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("99999,")));
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("1234,")));
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("5678,")));

        for (int i = 5000; i < 6000; i++) {
            filterList.addFilter(new PrefixFilter(Bytes.toBytes(i + ",")));
        }

        final Scan scan = new Scan();
        scan.setFilter(filterList);
        // scan.readAllVersions();


        final long start = System.currentTimeMillis();

        System.out.println("start!");

        @Cleanup final ResultScanner scanner = table.getScanner(scan);


        Iterator<Result> iterator = scanner.iterator();
        for (; iterator.hasNext(); ) {
            Result result = iterator.next();
            List<Cell> cells = result.listCells();
            for (Cell cell : cells) {
                HBaseUtils.printCell(cell);
            }
            System.out.println();
        }

        System.out.println("start" + start);
        System.out.println("end  " + System.currentTimeMillis());
        System.out.println("end - start  " + (System.currentTimeMillis() - start));

    }

}
