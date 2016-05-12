package hBaseExample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;

public class HBaseExample {

	/* API can be found here:
		https://hbase.apache.org/apidocs/
	*/

	private static Configuration conf;
	private static Connection connection;
	private static Admin admin;

	// A buffer to tempory store for put request, used to speed up the process of putting records into hbase
	private static List<Put> putList;
	private static int listCapacity = 1000000;

	private static String[] tableName = { "s103062512:eng", "s103062512:math" };
	private static String[] tableColFamilies = { "grade" };

	public static void createTable(String tableName, String[] colFamilies) throws Exception {
		// Instantiating table descriptor class
		TableName hTableName = TableName.valueOf(tableName);
		if (admin.tableExists(hTableName)) {
			System.out.println(tableName + " : Table already exists!");
		} else {
			HTableDescriptor tableDescriptor = new HTableDescriptor(hTableName);
			for (String cf : colFamilies) {
        tableDescriptor.addFamily(new HColumnDescriptor(cf));
			}

			System.out.println("Creating table: " + tableName + "...");
			admin.createTable(tableDescriptor);
			System.out.println("Table created");
		}
	}

	public static void removeTable(String tableName) throws Exception {
		TableName hTableName = TableName.valueOf(tableName);
		if (!admin.tableExists(hTableName)) {
			System.out.println(tableName + ": Table does not exist!");
		} else {
			System.out.println("Deleting table: " + tableName + "...");
		  admin.deleteTable(hTableName);
			System.out.println("Table deleted");
		}
	}

	public static void addRecordToPutList(String rowKey, String colFamily,
			String qualifier, String value) throws Exception {
		Put put = new Put(rowKey.getBytes());
		put.addColumn(colFamily.getBytes(), qualifier.getBytes(), value.getBytes());
		putList.add(put);
	}

	public static void addRecordToHBase(String tableName) throws Exception {
		Table table = connection.getTable(TableName.valueOf(tableName));
		table.put(putList);
		putList.clear();
	}

	public static void deleteRecord(String tableName, String rowKey) throws Exception {
		Table table = connection.getTable(TableName.valueOf(tableName));
		// TODO use Delete to wrap key information and use Table api to delete it.
	}

	public static void main(String[] args){

		try {
			// Instantiating hbase connection
			conf = HBaseConfiguration.create();
			connection = ConnectionFactory.createConnection(conf);
			admin = connection.getAdmin();
			for (int curTableID = 0; curTableID < args.length; ++curTableID) {
				System.out.println("fetching " + args[0] + "...");
				// remove the old table on hbase
				removeTable(tableName[curTableID]);
				// create a new table on hbase
				createTable(tableName[curTableID], tableColFamilies);
				// Read Content from local file
				File file = new File(args[curTableID]);
				BufferedReader br = new BufferedReader(new FileReader(file));
				int linecount = 0;
				String line = null;
        String[] comps = args[curTableID].split("/");
        String tmpCol = comps[comps.length - 1].replaceAll(".txt", "");
				putList = new ArrayList<Put>(listCapacity);
				while (null != (line = br.readLine())) {
					if (0 == linecount % 100000) System.out.println(linecount + " lines added to hbase.");
          System.out.println(line);

          String[] pair = line.split("\\s");
          addRecordToPutList(pair[0], "grade", "name", pair[0]);
          addRecordToPutList(pair[0], "grade", tmpCol, pair[0]);

					// if capacity of our putList buffer is reached, dump them into HBase
					if (putList.size() == listCapacity) {
						addRecordToHBase(tableName[curTableID]);
					}
					++linecount;
				}
				System.out.println(linecount + " lines added to hbase.");
				// dump remaining contents into HBase
				addRecordToHBase(tableName[curTableID]);
				br.close();
			}
			// Finalize and close connection to Hbase
			admin.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
