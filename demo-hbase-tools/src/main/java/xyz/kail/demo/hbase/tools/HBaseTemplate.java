package xyz.kail.demo.hbase.tools;


import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.metrics.ScanMetrics;
import org.apache.hadoop.hbase.filter.MultiRowRangeFilter;
import org.apache.hadoop.hbase.snapshot.SnapshotDoesNotExistException;
import org.apache.hadoop.hbase.snapshot.SnapshotExistsException;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HBase 工具类
 *
 * @author kail
 * @see HConstants
 * @see ConnectionConfiguration
 */
@Slf4j
public final class HBaseTemplate {

    public static void main(String[] args) throws IOException {
        Connection connection = Connect.getConnection("asd");
        org.apache.hadoop.hbase.client.Table table = connection.getTable(TableName.valueOf(""));
        List<Put> batch = new ArrayList<>();

        Mutation put = new Put(Bytes.toBytes("r1"));
        put.setDurability(Durability.SYNC_WAL)
        Mutation delete = new Put(Bytes.toBytes("r1"));
        RowMutations rowMutations = RowMutations.of(Arrays.asList(delete, put));
        table.mutateRow(rowMutations);

        RowMutations mutations = new RowMutations(Bytes.toBytes("r1"));
        mutations.add(put);
        mutations.add(delete);
    }

    public static final class Connect {

        /**
         * 获取链接
         *
         * @param quorum Zookeeper 地址： 127.0.0.1:2181
         */
        public static Connection getConnection(String quorum) throws IOException {
            Configuration hbaseConf = HBaseConfiguration.create();
            hbaseConf.set(HConstants.ZOOKEEPER_QUORUM, quorum);
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

        public static Map<String, Map<String, String>> listDescriptor(Connection conn) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();

            return Stream.of(admin.listNamespaceDescriptors()).collect(Collectors.toMap(
                    NamespaceDescriptor::getName,
                    NamespaceDescriptor::getConfiguration));
        }

        /**
         * 获取所有 Namespace
         */
        public static Set<String> list(Connection conn) throws IOException {
            return listDescriptor(conn).keySet();
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

        public static void drop(Connection conn, String ns) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            admin.deleteNamespace(ns);
        }

        /**
         * 同时删除表
         */
        public static void dropAndTable(Connection conn, String ns) throws IOException {
            Table.dropAll(conn, ns);
            Namespace.drop(conn, ns);
        }
    }

    public static final class Table {

        public static final String DEF_FAMILY = "F";

        public static final byte[] DEF_FAMILY_BYTE = Bytes.toBytes(DEF_FAMILY);

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
                    .newBuilder(DEF_FAMILY_BYTE)
                    .build();

            create(conn, tableName, columns);
        }

        public static void create(Connection conn, TableName tableName,
                                  ColumnFamilyDescriptor... columns) throws IOException {
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

        public static void drop(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();

            Table.disable(conn, tableName);

            admin.deleteTable(tableName);
        }

        public static void dropAll(Connection conn, String ns) throws IOException {
            @Cleanup
            Admin admin = conn.getAdmin();
            TableName[] tableNames = admin.listTableNames(Pattern.compile(ns + ":.+"));
            for (TableName tableName : tableNames) {
                Table.disable(conn, tableName);
                Table.drop(conn, tableName);
            }
        }

        public static void disable(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            if (admin.isTableEnabled(tableName)) {
                admin.disableTable(tableName);
            }
        }

        public static void enable(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            if (admin.isTableDisabled(tableName)) {
                admin.enableTable(tableName);
            }
        }

        public static void rename(Connection conn, TableName oldTableName, TableName newTableName) throws IOException {

            final String snapshotName = oldTableName.getQualifierAsString() + "__snapshot__"
                    + System.currentTimeMillis();

            Table.disable(conn, oldTableName);
            Snapshots.snapshot(conn, oldTableName, snapshotName);
            Snapshots.cloneSilent(conn, snapshotName, newTableName);
            Snapshots.delete(conn, snapshotName);
            Table.drop(conn, oldTableName);
        }
    }

    public static final class Field {

        public static Put toPut(Cell cell) {
            return toPut(cell, CellUtil.cloneQualifier(cell));
        }

        public static Put toPut(Cell cell, byte[] qualifier) {
            Put put = new Put(CellUtil.cloneRow(cell));
            put.addColumn(CellUtil.cloneFamily(cell), qualifier, cell.getTimestamp(), CellUtil.cloneValue(cell));
            return put;
        }

        public static Put toPut(byte[] rowKey, Cell cell) {
            Put put = new Put(rowKey);
            put.addColumn(
                    CellUtil.cloneFamily(cell),
                    CellUtil.cloneQualifier(cell),
                    cell.getTimestamp(),
                    CellUtil.cloneValue(cell)
            );
            return put;
        }

        public static Put toPut(byte[] rowKey, Cell cell, byte[] newValue) {
            Put put = new Put(rowKey);
            put.addColumn(
                    CellUtil.cloneFamily(cell),
                    CellUtil.cloneQualifier(cell),
                    cell.getTimestamp(),
                    newValue
            );
            return put;
        }

        public static Delete toDelete(byte[] key) {
            return new Delete(key);
        }

        public static Delete toDeleteRow(Cell cell) {
            return toDelete(CellUtil.cloneRow(cell));
        }

        public static Delete toDeleteQualifier(Cell cell) {
            Delete delete = toDeleteRow(cell);
            delete.addColumns(CellUtil.cloneFamily(cell), CellUtil.cloneQualifier(cell));
            return delete;
        }

        public static Delete toDeleteTimestamp(Cell cell) {
            Delete delete = toDeleteRow(cell);
            delete.addColumns(CellUtil.cloneFamily(cell), CellUtil.cloneQualifier(cell), cell.getTimestamp());
            return delete;
        }
    }

    /**
     * 0.95 开始引入 Snapshot
     * <p>
     * Snapshot 可以在线做，也可以离线做
     * <p>
     * Snapshot 的实现不涉及到 table 实际数据的拷贝，仅仅拷贝一些元数据
     * 比如组成 table 的 region info，表的 descriptor，还有表对应的HFile的文件的引用。
     */
    public static final class Snapshots {

        public static Map<String, String> list(Connection conn) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            return admin.listSnapshots().stream()
                    .collect(Collectors.toMap(
                            SnapshotDescription::getName,
                            SnapshotDescription::getTableNameAsString));
        }

        public static boolean exist(Connection conn, String snapshot) throws IOException {
            return list(conn).containsKey(snapshot);
        }

        public static void snapshot(Connection conn, TableName tableName, String snapshot) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            try {
                admin.snapshot(snapshot, tableName);
            } catch (SnapshotExistsException ex) {
                log.warn("{}", ex.getMessage());
            }
        }

        public static void clone(Connection conn, String snapshot, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            admin.cloneSnapshot(snapshot, tableName);
        }

        public static void cloneSilent(Connection conn, String snapshot, TableName tableName) throws IOException {
            try {
                clone(conn, snapshot, tableName);
            } catch (TableExistsException ex) {
                log.warn("{}", ex.getMessage());
            }

        }

        public static void delete(Connection conn, String snapshot) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            try {
                admin.deleteSnapshot(snapshot);
            } catch (SnapshotDoesNotExistException ex) {
                log.warn("{}", ex.getMessage());
            }
        }
    }

    public static final class RowKey {

        // region reverse

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

        // endregion

        // region hash

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

        // endregion

        public static boolean isValid(byte[] startRow, byte[] stopRow) {
            return isValid(startRow, stopRow, false);
        }

        /**
         * @param startRow
         * @param stopRow
         * @param stopRowInclusive
         * @return
         * @see MultiRowRangeFilter.RowRange#isValid
         */
        public static boolean isValid(byte[] startRow, byte[] stopRow, boolean stopRowInclusive) {
            return Bytes.equals(startRow, HConstants.EMPTY_BYTE_ARRAY)
                    || Bytes.equals(stopRow, HConstants.EMPTY_BYTE_ARRAY)
                    || Bytes.compareTo(startRow, stopRow) < 0
                    || (Bytes.compareTo(startRow, stopRow) == 0 && stopRowInclusive);
        }

    }

    public static final class Tool {

        public static void flush(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            admin.flush(tableName);
        }

        public static void minorCompact(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            admin.compact(tableName);
        }

        public static void majorCompact(Connection conn, TableName tableName) throws IOException {
            @Cleanup final Admin admin = conn.getAdmin();
            admin.majorCompact(tableName);
        }

    }

    public static final class Debug {

        public static void printHTableDescriptor(HTableDescriptor hTableDescriptor) {
            TableName tableName = hTableDescriptor.getTableName();
            System.out.println("tableName:" + tableName);

            Map<String, String> configuration = hTableDescriptor.getConfiguration();
            Set<Map.Entry<String, String>> entries = configuration.entrySet();
            System.out.println("    configuration:");
            for (Map.Entry<String, String> entry : entries) {
                System.out.println("        " + entry.getKey() + ":" + entry.getValue());
            }

            System.out.println("    ColumnFamilies:");
            HColumnDescriptor[] columnFamilies = hTableDescriptor.getColumnFamilies();
            for (HColumnDescriptor columnDescriptor : columnFamilies) {
                System.out.println("        " + columnDescriptor);
            }
            System.out.println();
            System.out.println();
        }

        public static void printCell(Cell cell) {
            System.out.print(Bytes.toStringBinary(CellUtil.cloneRow(cell)));
            System.out.print(":");
            System.out.print(Bytes.toStringBinary(CellUtil.cloneFamily(cell)));
            System.out.print(":");
            System.out.print(Bytes.toStringBinary(CellUtil.cloneQualifier(cell)));
            System.out.print(":");
            System.out.print(cell.getTimestamp() + "("
                    + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(cell.getTimestamp())) + ")");
            System.out.print(" => ");
            System.out.println(Bytes.toString(CellUtil.cloneValue(cell)));
        }

        public static void printScanner(ResultScanner scanner) {
            final ScanMetrics scanMetrics = scanner.getScanMetrics();
            if (null != scanMetrics) {
                System.out.println(scanMetrics.getMetricsMap());
                System.out.println();
            }

            //
            Iterator<Result> iterator = scanner.iterator();
            for (; iterator.hasNext(); ) {
                Result result = iterator.next();
                List<Cell> cells = result.listCells();
                for (Cell cell : cells) {
                    printCell(cell);
                }
                System.out.println();
            }
        }
    }

}
