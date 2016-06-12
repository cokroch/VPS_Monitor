package com.seassoon.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DataSource {
	
	private String type;
	
	public DataSource(){
		super();
	}
	public DataSource(String type){
		this.type=type;
	}
	


	@SuppressWarnings("serial")
	private static Map<String, String[]> dbInfoMap = new HashMap<String, String[]>() {
		{
			put("db_vps_host", new String[]{"jdbc:mysql://***:3306/db_vps_host?characterEncoding=utf-8","***","***"});
		}
	};
	

	private static final String DRIVER = "com.mysql.jdbc.Driver";

	private Connection connnection = null;

	private PreparedStatement preparedStatement = null;

	private Statement statement = null;

	private CallableStatement callableStatement = null;

	private ResultSet resultSet = null;

	static {
		try {
			Class.forName(DRIVER);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public Connection getConnection() {

		try {
			if(connnection == null || connnection.isClosed()==true ){
				String[] dbInfo =dbInfoMap.get(type);
				connnection = DriverManager.getConnection(dbInfo[0], dbInfo[1],	dbInfo[2]);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connnection;
	}

	public int executeUpdate(String sql, Object[] params) {
		int affectedLine = 0;
		try {
			connnection = this.getConnection();
			preparedStatement = connnection.prepareStatement(sql);
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					preparedStatement.setObject(i + 1, params[i]);
				}
			}
			affectedLine = preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeAll();
		}
		return affectedLine;
	}

	public int executeUpdate(String sql) {
		int affectedLine = 0;
		try {
			connnection = this.getConnection();
			statement = connnection.createStatement();
			affectedLine = statement.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeAll();
		}
		return affectedLine;
	}

	private ResultSet executeQueryRS(String sql, Object[] params) {
		try {
			connnection = this.getConnection();
			preparedStatement = connnection.prepareStatement(sql);
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					preparedStatement.setObject(i + 1, params[i]);
				}
			}
			resultSet = preparedStatement.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return resultSet;
	}

	public Object executeQuerySingle(String sql, Object[] params) {
		Object object = null;
		try {
			connnection = this.getConnection();
			preparedStatement = connnection.prepareStatement(sql);
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					preparedStatement.setObject(i + 1, params[i]);
				}
			}
			resultSet = preparedStatement.executeQuery();
			if (resultSet.next()) {
				object = resultSet.getObject(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeAll();
		}

		return object;
	}

	public List<Object> excuteQuery(String sql, Object[] params) {
		ResultSet rs = executeQueryRS(sql, params);
		ResultSetMetaData rsmd = null;
		int columnCount = 0;
		try {
			rsmd = rs.getMetaData();
			columnCount = rsmd.getColumnCount();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		List<Object> list = new ArrayList<Object>();
		try {
			while (rs.next()) {
				Map<String, Object> map = new HashMap<String, Object>();
				for (int i = 1; i <= columnCount; i++) {
					map.put(rsmd.getColumnLabel(i), rs.getObject(i));
				}
				list.add(map);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeAll();
		}
		return list;
	}

	

	public void closeAll() {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		if (preparedStatement != null) {
			try {
				preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (callableStatement != null) {
			try {
				callableStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (connnection != null) {
			try {
				connnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		DataSource ds = new DataSource("db_vps_host");
//		ds.executeUpdate("update vps_host set host = '111.111.111.111'");
		String afterChangedIp = "333.333.333.333";
		String lastTimeIp = "111.111.111.111";
		ds.executeUpdate("update vps_host set host = '"+ afterChangedIp +"' where host = '"+ lastTimeIp +"'");
	}

}
