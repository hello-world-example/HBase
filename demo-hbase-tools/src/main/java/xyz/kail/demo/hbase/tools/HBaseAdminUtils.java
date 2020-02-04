package xyz.kail.demo.hbase.tools;


import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HBase 工具类
 *
 * @author kail
 */
@Slf4j
public final class HBaseAdminUtils {

    public static final class Namespace {
        /**
         * 获取所有 Namespace
         */
        public static Set<String> list(Connection conn) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            final NamespaceDescriptor[] nss = admin.listNamespaceDescriptors();
            return Stream.of(nss).map(NamespaceDescriptor::getName).collect(Collectors.toSet());
        }

        /**
         * 判断 Namespace 是否存在
         */
        public static boolean exist(Connection conn, String ns) throws IOException {
            return Namespace.list(conn).contains(ns);
        }

        /**
         * 创建 Namespace
         */
        public static void create(Connection conn, String ns) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            if (exist(conn, ns)) {
                return;
            }

            final NamespaceDescriptor namespaceDescriptor = NamespaceDescriptor.create(ns).build();
            admin.createNamespace(namespaceDescriptor);
        }
    }

    public static final class Table {
        /**
         * 获取所有 Table
         */
        public static Set<String> list(Connection conn, String ns) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            final TableName[] tableNames = admin.listTableNamesByNamespace(ns);
            return Stream.of(tableNames).map(TableName::getNameWithNamespaceInclAsString).collect(Collectors.toSet());
        }

        /**
         * 判断 Table 是否存在
         */
        public static boolean exist(Connection conn, String tableName) throws IOException {
            return exist(conn, TableName.valueOf(tableName));
        }

        public static boolean exist(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            return admin.tableExists(tableName);
        }

        public static void create(Connection conn, String tableName) throws IOException {
            create(conn, TableName.valueOf(tableName));
        }

        /**
         * 默认 Table
         * - 只有一列 F
         * - 版本保存最大
         */
        public static void create(Connection conn, TableName tableName) throws IOException {

            final ColumnFamilyDescriptor columns = ColumnFamilyDescriptorBuilder
                    .newBuilder(Bytes.toBytes("F"))
                    .setMaxVersions(Integer.MAX_VALUE)
                    .build();

            create(conn, tableName, columns);
        }

        public static void create(Connection conn, TableName tableName, ColumnFamilyDescriptor... columns) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();

            // 先创建 Namespace
            Namespace.create(conn, tableName.getNamespaceAsString());

            // 再创建表
            final TableDescriptor tableDescriptor = TableDescriptorBuilder.newBuilder(tableName)
                    .setColumnFamilies(Arrays.asList(columns))
                    .build();

            admin.createTable(tableDescriptor);
        }
    }

}
