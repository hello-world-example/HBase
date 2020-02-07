package xyz.kail.demo.hbase.client.filter;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.exceptions.DeserializationException;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;
import xyz.kail.demo.hbase.tools.HBaseUtils;

/**
 * TODO 注释
 *
 * @author kail
 */
public class FilterMain {

    public static void main(String[] args) throws IOException, DeserializationException {
        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

//        Table table = connection.getTable(TableName.valueOf("hbase:meta"));
        Table table = connection.getTable(TableName.valueOf("dev_boss_remark_v1"));

        FilterBase prefixFilter = new PrefixFilter(Bytes.toBytes("dev"));
        KeyOnlyFilter keyOnlyFilter = new KeyOnlyFilter();
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filterList.addFilter(new PrefixFilter(Bytes.toBytes("dev")));
        filterList.addFilter(new KeyOnlyFilter());

        new SingleColumnValueFilter(null, null, CompareFilter.CompareOp.EQUAL, new BinaryComparator(Bytes.toBytes("")));


        Scan scan = new Scan();
//        scan.setStartRow(Bytes.toBytes("00001282"));
//        scan.setStopRow(Bytes.padTail(Bytes.toBytes("00001282"), 0));
//        scan.setStopRow(Bytes.add(Bytes.toBytes("00001282"), new byte[0]));

//        scan.addColumn(Bytes.toBytes("info"), Bytes.toBytes("regioninfo"));
//        scan.addColumn(Bytes.toBytes("info"), Bytes.toBytes("server"));
//        scan.setFilter(new SingleColumnValueFilter(Bytes.toBytes("info"),Bytes.toBytes("server"), CompareFilter.CompareOp.EQUAL,Bytes.toBytes("s03.hadoop.ttp.wx:60020")));
//        scan.setFilter(new PageFilter(20));
//        scan.setFilter(new FirstKeyOnlyFilter());
//        Pair<byte[], byte[]> pair = Pair.newPair(Bytes.toBytes("0000????"), new byte[]{0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00});
        Pair<byte[], byte[]> pair = Pair.newPair(Bytes.toBytes("0000????"), new byte[]{0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01});
        scan.setFilter(new FuzzyRowFilter(Arrays.asList(pair)));



        ResultScanner scanner = table.getScanner(scan);
        Iterator<Result> iterator = scanner.iterator();
        for (; iterator.hasNext(); ) {
            Result result = iterator.next();
            List<Cell> cells = result.listCells();
            for (Cell cell : cells) {
                HBaseUtils.printCell(cell);
            }
        }

        HBaseTool.Connect.close(connection);
    }


}
