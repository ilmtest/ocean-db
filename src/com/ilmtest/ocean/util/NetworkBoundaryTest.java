package com.ilmtest.ocean.util;

import java.sql.SQLException;

import com.ilmtest.lib.io.NetworkBoundary;

public class NetworkBoundaryTest
{

	public NetworkBoundaryTest()
	{
	}
	
	public static void main(String[] args) throws SQLException
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 151; i <= 447; i++) {
			sb.append(i+" ");
		}
		
		System.out.println(sb);
		
		if (true) {
			return;
		}
		
		for (int i = 3; i <= 447; i++)
		{
			NetworkBoundary.getHTML("http://al-jadwal.com/api/port.php?collection="+i);
		}
		
		if (true) {
			return;
		}
		
		
		for (int i = 2; i <= 436; i++)
		{
			String mySqlTotal = NetworkBoundary.getHTML("http://al-jadwal.com/api/get_total_pages.php?collectionId="+i, false);
			String sqliteTotal = NetworkBoundary.getHTML("https://ilmtest.net/api/get_total_pages.php?collectionId="+i, false);
			
			if ( !mySqlTotal.equals(sqliteTotal) ) {
				System.err.println(i+"; "+mySqlTotal+" vs. "+sqliteTotal);
			}
		}
	}

}