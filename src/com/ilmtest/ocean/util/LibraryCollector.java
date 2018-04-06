package com.ilmtest.ocean.util;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ilmtest.lib.io.DBUtils;

public class LibraryCollector
{
	private File[] m_result;
	
	public LibraryCollector()
	{
	}
	
	public void process(String folder)
	{
		m_result = new File(folder).listFiles( new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".db");
			}
		});
	}
	
	public void write(Connection connection, String masterDB) throws SQLException
	{
		DBUtils.execStatement(connection, "CREATE TABLE pages (id INTEGER PRIMARY KEY, collection INTEGER, page_number INTEGER, ar_body TEXT, ar_body_plain TEXT, part_number INTEGER, part_page INTEGER, UNIQUE(collection,page_number) ON CONFLICT IGNORE)");
		
		for (File f: m_result)
		{
			System.out.println(f.getName());
			String name = f.getName().split("\\.")[0].trim();
			
			if ( name.matches("\\d+$") )
			{
				int foreignId = Integer.parseInt(name);
				PreparedStatement lookup = connection.prepareStatement("SELECT id FROM collections WHERE foreign_collection_id=?");
				lookup.setInt(1, foreignId);
				ResultSet rs = lookup.executeQuery();

				if ( rs.next() )
				{
					DBUtils.attach(connection, f.getPath(), "flip");
					
					PreparedStatement insert = connection.prepareStatement("INSERT INTO pages (collection,page_number,ar_body,ar_body_plain,part_number,part_page) SELECT ?,page_number,arabic_vowelled,arabic_plain,part_number,part_page FROM flip.entries");
					insert.setInt(1, rs.getInt("id"));
					insert.execute();
					insert.close();
					
					rs.close();
					lookup.close();
					DBUtils.detach(connection, "flip");
				} else {
					System.err.print("Could not find collection for: "+foreignId);
				}
			} else {
				System.err.print("InvalidFileName: "+name);
			}
		}
		
		DBUtils.execStatement(connection, "CREATE INDEX IF NOT EXISTS pages_index ON pages(part_number,part_page)");
	}
}