package xyz.kail.demo.hbase.client.admin;

import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import xyz.kail.demo.hbase.client.Rcore;
import xyz.kail.demo.hbase.tools.HBaseTool;

import java.io.IOException;

/**
 * Created by kail on 2018/3/5.
 */
public class ReadClusterStatusMain {

    public static void main(String[] args) throws IOException {
        Connection connection = HBaseTool.Connect.getConnection(Rcore.QUORUM);
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


        HBaseTool.Connect.close(connection);

    }

}
