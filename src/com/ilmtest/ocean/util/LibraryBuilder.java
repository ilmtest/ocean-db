package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

import com.ilmtest.lib.io.DBUtils;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.FlipBookPopulator;
import com.ilmtest.shamela.controller.LibraryPopulator;
import com.ilmtest.shamela.controller.ShamelaConverter;
import com.ilmtest.util.text.TextUtils;

public class LibraryBuilder
{
	private static final int SITE_SHAMELA_NEW_REP = 11;
	private static final int SITE_SHAMELA = 1;

	public static void main(String[] args) throws Exception
	{
		File lib = new File("/Users/rhaq/workspace/resources/library.db");

		if ( lib.exists() ) {
			lib.delete();
		}

		Connection library = DriverManager.getConnection("jdbc:sqlite:"+lib.getPath());
		DBUtils.execStatement(library, "CREATE TABLE pages (id INTEGER PRIMARY KEY, collection INTEGER, page_number INTEGER, ar_body TEXT, ar_body_plain TEXT, part_number INTEGER, part_page INTEGER, UNIQUE(collection,page_number) ON CONFLICT IGNORE)");
		DBUtils.execStatement(library, "CREATE VIRTUAL TABLE fts_idx USING fts5(ar_body_plain, content='pages', content_rowid='id')");
		DBUtils.execStatement(library, "CREATE TRIGGER tbl_ai AFTER INSERT ON pages BEGIN INSERT INTO fts_idx(rowid, ar_body_plain) VALUES (new.id, new.ar_body_plain); END");
		DBUtils.execStatement(library, "CREATE TRIGGER tbl_ad AFTER DELETE ON pages BEGIN INSERT INTO fts_idx(fts_idx, rowid, ar_body_plain) VALUES('delete', old.id, old.ar_body_plain); END");
		DBUtils.execStatement(library, "CREATE TRIGGER tbl_au AFTER UPDATE ON pages BEGIN INSERT INTO fts_idx(fts_idx, rowid, ar_body_plain) VALUES('delete', old.id, old.ar_body_plain);"
				+ "INSERT INTO fts_idx(rowid, ar_body_plain) VALUES (new.id, new.ar_body_plain); END");
		PreparedStatement insert = DBUtils.createInsert(library, "pages", Arrays.asList("collection", "page_number", "ar_body", "ar_body_plain", "part_number", "part_page"));

		library.setAutoCommit(false);

		File f = new File("/Users/rhaq/workspace/resources/master.db");
		Connection master = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		final PreparedStatement ps = master.prepareStatement("SELECT id,foreign_collection_id,foreign_site FROM collections WHERE foreign_site="+SITE_SHAMELA+" OR foreign_site="+SITE_SHAMELA_NEW_REP+" AND id >= 474 AND id <= 475 ORDER BY id");
		ResultSet rs = ps.executeQuery();

		while ( rs.next() )
		{
			int collectionId = rs.getInt("id");
			int bookID = rs.getInt("foreign_collection_id");
			int site = rs.getInt("foreign_site");
			System.out.println("Processing: "+collectionId);

			String folder = site == SITE_SHAMELA ? "raw" : "shamela";

			f = new File("/Users/rhaq/workspace/resources/"+folder+"/"+bookID+".db");

			if ( !f.exists() ) {
				System.err.println("RawNotFound: "+bookID+"; collectionId: "+collectionId);
				continue;
			}

			LibraryPopulator lp = site == SITE_SHAMELA ? new FlipBookPopulator() : new ShamelaConverter();

			{
				Connection connection = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
				lp.process(connection);
				connection.close();	
			}

			System.out.println("Writing to library: "+collectionId);

			for (final Entry n: lp.getEntries())
			{
				int i = 0;

				insert.setInt(++i, collectionId);
				insert.setInt(++i, n.pageNumber);
				insert.setString(++i, n.arabic.trim());
				insert.setString(++i, TextUtils.normalize(n.arabic.trim(), true));
				DBUtils.setNullInt(++i, n.part.number, insert);
				DBUtils.setNullInt(++i, n.part.page, insert);

				insert.execute();
			}

			library.commit();
		}

		rs.close();
		ps.close();
		master.close();
		library.close();
	}
}