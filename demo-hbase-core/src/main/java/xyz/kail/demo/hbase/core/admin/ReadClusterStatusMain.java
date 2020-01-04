package xyz.kail.demo.hbase.core.admin;

import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;

import xyz.kail.demo.hbase.core.Rcore;
import xyz.kail.demo.hbase.core.util.HBaseUtils;

/**
 * Created by kail on 2018/3/5.
 */
public class ReadClusterStatusMain {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseUtils.getConnection(Rcore.QUORUM);
        Admin admin = connection.getAdmin();

        ClusterStatus clusterStatus = admin.getClusterStatus();
        System.out.println(clusterStatus);
        System.out.println();

        System.out.println(clusterStatus.getClusterId());
        System.out.println(clusterStatus.getHBaseVersion());
        System.out.println(clusterStatus.getMaster().toShortString());
        System.out.println(clusterStatus.getAverageLoad());
        System.out.println(clusterStatus.getRegionsCount());
        System.out.println();

        int masterInfoPort = admin.getMasterInfoPort();
        System.out.println(masterInfoPort);


        HBaseUtils.close(connection);

    }

}
