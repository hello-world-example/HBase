package xyz.kail.demo.hbase.tools;


import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HBase 工具类
 *
 * @author kail
 */
@Slf4j
public final class HBaseTool {

    public static final class Connect {

        /**
         * 获取链接
         *
         * @param quorum Zookeeper 地址： 127.0.0.1:2181
         */
        public static Connection getConnection(String quorum) throws IOException {
            Configuration hbaseConf = HBaseConfiguration.create();
            hbaseConf.set("hbase.zookeeper.quorum", quorum);
            return ConnectionFactory.createConnection(hbaseConf);
        }

        public static void close(Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.warn("", e);
            }
        }
    }

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

        public static final String DEF_COLUMN = "C";

        public static final byte[] DEF_COLUMN_BYTE = Bytes.toBytes(DEF_COLUMN);

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
         */
        public static void create(Connection conn, TableName tableName) throws IOException {

            final ColumnFamilyDescriptor columns = ColumnFamilyDescriptorBuilder
                    .newBuilder(DEF_COLUMN_BYTE)
                    .build();

            create(conn, tableName, columns);
        }

        public static void create(Connection conn, TableName tableName, ColumnFamilyDescriptor... columns) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            if (exist(conn, tableName)) {
                return;
            }

            // 先创建 Namespace
            Namespace.create(conn, tableName.getNamespaceAsString());

            // 再创建表
            final TableDescriptor tableDescriptor = TableDescriptorBuilder.newBuilder(tableName)
                    .setColumnFamilies(Arrays.asList(columns))
                    .build();

            admin.createTable(tableDescriptor);
        }

        public static void delete(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            admin.disableTable(tableName);
            admin.deleteTable(tableName);

        }
    }

    public static final class RowKey {

        // ********************************************************************************
        // ******************************** reverse ******************************************
        // ********************************************************************************

        public static String reverse(long l) {
            return reverse(String.valueOf(l));
        }

        public static String reverse(long l, int length) {
            return reverse(String.format("%0" + length + "d", l));
        }

        /**
         * 字符串反转
         */
        public static String reverse(String str) {
            if (null == str || str.length() <= 1) {
                return str;
            }

            char[] array = str.toCharArray();
            int length = array.length;
            for (int i = 0; i < length / 2; i++) {
                char tmp = array[i];
                array[i] = array[length - 1 - i];
                array[length - 1 - i] = tmp;
            }
            return new String(array);
        }


        // ********************************************************************************
        // ******************************** hash ******************************************
        // ********************************************************************************

        /**
         * 计算 字符串 的 hash值，同 String.hashCode()
         */
        public static int toHash(String key) {
            int hash = 7;
            for (int i = 0; i < key.length(); i++) {
                hash = hash * 31 + key.charAt(i);
            }
            return hash;
        }

        /**
         * 计算 字符串 的 hash值(同 String.hashCode()), 并取模
         */
        public static int toHash(String key, int mold) {
            int i = toHash(key) % mold;
            return i < 0 ? -i : i;
        }

        /**
         * 计算 字符串 的 hash值, 并取模
         *
         * @param mold   模，num % 1000，结果小于 1000
         * @param length 如果取模后数字 字符串长度不足 length，前几位补0
         */
        public static String formatHash(String key, int mold, int length) {
            int i = toHash(key, mold);
            return String.format("%0" + length + "d", i);
        }

    }

}
