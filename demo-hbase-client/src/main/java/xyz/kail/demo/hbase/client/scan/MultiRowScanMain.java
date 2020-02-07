package xyz.kail.demo.hbase.client.scan;

import lombok.Cleanup;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.MultiRowRangeFilter;
import org.apache.hadoop.hbase.filter.MultiRowRangeFilter.RowRange;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 速度 1260， 是 FilterList 的 10倍以上
 */
public class MultiRowScanMain {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

        Table table = connection.getTable(ScanInitDataMain.tableName);

        List<RowRange> rows = new ArrayList<>();
        rows.add(new RowRange("1111,", true, "1111,A", false));
        rows.add(new RowRange("8888,", true, "8888,A", false));
        rows.add(new RowRange("9999,", true, "9999,A", false));
        rows.add(new RowRange("99999,", true, "99999,A", false));
        rows.add(new RowRange("1234,", true, "1234,A", false));
        rows.add(new RowRange("5678,", true, "5678,A", false));

        for (int i = 5000; i < 6000; i++) {
            rows.add(new RowRange(i + ",", true, i + ",A", false));
        }

        final Scan scan = new Scan();
        scan.setFilter(new MultiRowRangeFilter(rows));
        // scan.readAllVersions();


        final long start = System.currentTimeMillis();

        System.out.println("start!");

        @Cleanup final ResultScanner scanner = table.getScanner(scan);

        HBaseTool.Debug.printScanner(scanner);

        System.out.println("start" + start);
        System.out.println("end  " + System.currentTimeMillis());
        System.out.println("end - start  " + (System.currentTimeMillis() - start));

        //

    }

}
