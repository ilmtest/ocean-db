package com.ilmtest.ocean.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.ocean.lib.io.DBUtils;

/**
 * Removes all the tashkeel from a specific column in a database table.
 * @author rhaq
 *
 */
public class SourcePageLinker
{
	private Map<Integer,Integer> m_idToPage = new HashMap<>();
	
	public SourcePageLinker()
	{
	}
	
	
	/**
	 * Collect and process.
	 * @param c
	 * @throws SQLException 
	 */
	public void process(Connection master, int collection) throws SQLException
	{
		final PreparedStatement pageNumberLookup = DBUtils.createQuery(master, "flip.entries", "arabic_vowelled LIKE ?", null, "page_number");
		
		final PreparedStatement ps = DBUtils.createQuery(master, "entries", "from_page ISNULL AND collection="+collection, null, "ar_body", "id");
		final ResultSet rs = ps.executeQuery();

		while ( rs.next() )
		{
			String body = rs.getString("ar_body");
			
			if ( body.contains("\n") ) {
				System.out.println("HasNewLine: "+rs.getInt("id"));
			} else if ( body.contains("]") ) {
				String rest = body.substring( body.lastIndexOf("]")+1 ).trim();
				
				if ( rest.length() < 10 ) { // it's near the end
					rest = body.substring( 0, body.lastIndexOf("[") ).trim();
				}

				linkPage(pageNumberLookup, rs, rest);
			} else {
				String rest = body.substring( body.length()/2 );
				boolean result = linkPage(pageNumberLookup, rs, rest);
				
				if (!result) {
					rest = body.substring( 0, body.length()/2 );
					linkPage(pageNumberLookup, rs, rest);
				}
			}
		}
		
		ps.close();
		rs.close();
	}
	
	
	private boolean linkPage(PreparedStatement pageNumberLookup, ResultSet rows, String body) throws SQLException
	{
		pageNumberLookup.setString(1, "%"+body+"%");
		ResultSet pages = pageNumberLookup.executeQuery();
		boolean result = false;
		
		System.out.println("\nHasSeparator: "+rows.getInt("id"));
		
		if ( pages.next() ) {
			System.out.println("PageNumber: "+pages.getInt("page_number"));
			
			int id = rows.getInt("id");
			int pageNumber = pages.getInt("page_number");
			
			if ( !pages.next() ) { // can only be one
				m_idToPage.put(id, pageNumber);
				result = true;
			} else {
				System.out.println("TOO_MANY_RESULTS");	
			}
		} else {
			System.out.println("NO_PAGE_NUMBER_FOUND");
		}
		
		pages.close();
		return result;
	}
	
	
	/**
	 * Write output
	 * @param c
	 * @throws SQLException 
	 */
	public void write(Connection master) throws SQLException
	{
		master.setAutoCommit(false);
		PreparedStatement ps = master.prepareStatement("UPDATE entries SET from_page=? WHERE id=?");
		
		for (int id: m_idToPage.keySet())
		{
			int i = 0;
			ps.setInt(++i, m_idToPage.get(id));
			ps.setInt(++i, id);
			ps.execute();
		}
		
		master.commit();
		ps.close();
	}
}
