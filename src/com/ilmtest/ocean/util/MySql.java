package com.ilmtest.ocean.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySql
{

	public MySql()
	{
	}

	public static void main(String[] args) throws SQLException
	{
		String url = "jdbc:mysql://al-jadwal.com:3306/aljadwal_maktabah?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
		String username = "aljadwal_bookwrm";
		String password = "&}lO+G7hsNlA";

		System.out.println("Connecting database...");

		try (Connection connection = DriverManager.getConnection(url, username, password)) {
		    System.out.println("Database connected!");
		    connection.close();
		} catch (SQLException e) {
		    throw new IllegalStateException("Cannot connect the database!", e);
		}

	}

}
