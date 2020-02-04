package xyz.kail.demo.hbase.client.curd;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseUtils;

import java.io.IOException;
import java.util.List;

/**
 * count "rel_boss_operate_log_v1" 12042
 * count "test_boss_operate_log_v1" 1178
 *
 * @author Kail
 */
public class GetMain {

    public static void main(String[] args) throws IOException {

        Connection connection = HBaseUtils.getConnection(Rcore.QUORUM);

        Table relBossOperateLogV1 = connection.getTable(TableName.valueOf("order_info"));

        Get get = new Get(Bytes.toBytes("3"));
        // 设置读取所有版本
        get.setMaxVersions();
        // 指定读取的列
        // get.addColumn(Bytes.toBytes("e"), Bytes.toBytes("c1"));
        get.addFamily(Bytes.toBytes("product"));
        get.setTimeStamp(1577439569573L);

        long start = System.currentTimeMillis();
        Result result = relBossOperateLogV1.get(get);
        System.out.println(System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        result = relBossOperateLogV1.get(get);
        System.out.println(System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        result = relBossOperateLogV1.get(get);
        System.out.println(System.currentTimeMillis() - start);

//        System.out.println(Bytes.toString(result.getRow()));

        List<Cell> cells = result.listCells();
        for (Cell cell : cells) {
            HBaseUtils.printCell(cell);
        }

        HBaseUtils.close(connection);
    }

}
