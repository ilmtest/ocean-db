package com.ilmtest.ocean.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import com.ocean.lib.io.DBUtils;
import com.ocean.lib.text.TextUtils;

/**
 * Removes all the tashkeel from a specific column in a database table.
 * @author rhaq
 *
 */
public class ColumnNormalizer
{
	private String table;
	private String idColumn;
	private String where;
	private Map<Integer,String> m_entries;

	public ColumnNormalizer(String table, String idColumn, String where)
	{
		this.table = table;
		this.idColumn = idColumn;
		this.where = where;
		this.m_entries = new TreeMap<>();
	}
	
	
	/**
	 * Collect and process.
	 * @param c
	 * @throws SQLException 
	 */
	public void process(Connection c, String sourceColumn) throws SQLException
	{
		final PreparedStatement ps = DBUtils.createQuery(c, table, where, idColumn, idColumn, sourceColumn);
		final ResultSet rs = ps.executeQuery();

		while ( rs.next() )
		{
			int id = rs.getInt(idColumn);
			String arabic = TextUtils.normalize( rs.getString(sourceColumn) );

			m_entries.put(id, arabic);
		}
		
		ps.close();
		rs.close();
	}
	
	
	/**
	 * Write output
	 * @param c
	 * @throws SQLException 
	 */
	public void write(Connection c, String targetColumn) throws SQLException
	{
		c.setAutoCommit(false);
		PreparedStatement ps = c.prepareStatement("UPDATE "+table+" SET "+targetColumn+"=? WHERE "+idColumn+"=?");
		
		for (int id: m_entries.keySet())
		{
			int i = 0;
			ps.setString(++i, m_entries.get(id));
			ps.setInt(++i, id);
			ps.execute();
		}
		
		c.commit();
		ps.close();
	}
}
