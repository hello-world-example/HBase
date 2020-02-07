package xyz.kail.demo.hbase.client.admin;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;

import java.io.IOException;

/**
 * TODO 注释
 *
 * @author Kail
 */
public class ReadHBaseMetaTableMain {

    private static Scan newScan(String start, String end) {
        Scan scan = new Scan();
        scan.setStartRow(Bytes.toBytes(start));
        scan.setStopRow(Bytes.toBytes(end));

        //指定需要的family或column ，如果没有调用任何addFamily或Column，会返回所有的columns；
        // scan.addFamily();
        // scan.addColumn();
        //
        // scan.setMaxVersions(); //指定最大的版本个数。如果不带任何参数调用setMaxVersions，表示取所有的版本。如果不掉用setMaxVersions，只会取到最新的版本.
        // scan.setTimeRange(); //指定最大的时间戳和最小的时间戳，只有在此范围内的cell才能被获取.
        // scan.setTimeStamp(); //指定时间戳
        // scan.setFilter(); //指定Filter来过滤掉不需要的信息
        // scan.setStartRow(); //指定开始的行。如果不调用，则从表头开始；
        // scan.setStopRow(); //指定结束的行（不含此行）；
        // scan.setBatch(); //指定最多返回的Cell数目。用于防止一行中有过多的数据，导致OutofMemory错误。

        return scan;
    }


    public static void main(String[] args) throws IOException {

        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);

        TableName tableName = TableName.valueOf("hbase:meta");
        Table table = connection.getTable(tableName);

        /**
         * 表描述
         */
        HTableDescriptor tableDescriptor = table.getTableDescriptor();
        HBaseTool.Debug.printHTableDescriptor(tableDescriptor);


        ResultScanner scanner = table.getScanner(newScan("test_boss_operate_log_v1", "test_boss_operate_log_v1,"));

        for (Result scanResult : scanner) {

            System.out.println(Bytes.toString(scanResult.getRow()));

            Cell[] cells = scanResult.rawCells();
            for (Cell cell : cells) {
                System.out.println(CellUtil.getCellKeyAsString(cell));

//                System.out.println(String.format("row:%s, family:%s, qualifier:%s, qualifiervalue:%s, timestamp:%s.",
//                        Bytes.toString(cell.getRow()),
//                        Bytes.toString(cell.getFamily()),
//                        Bytes.toString(kv.getQualifier()),
//                        Bytes.toString(kv.getValue()),
//                        kv.getTimestamp()));
            }
        }


        HBaseTool.Connect.close(scanner);
        HBaseTool.Connect.close(connection);

    }

}
