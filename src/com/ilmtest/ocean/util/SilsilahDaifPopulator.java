package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;

import com.ilmtest.lib.io.DBUtils;
import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.SilsilahDaifProcessor;

public class SilsilahDaifPopulator
{
	public SilsilahDaifPopulator()
	{
	}

	public void process() throws Exception
	{
		Populator p = new Populator( new SilsilahDaifProcessor() );
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/311.db").getPath());
		p.process(c);
		c.close();

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/daif.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		c.setAutoCommit(false);

		List<String> entriesColumns = Arrays.asList("part_number INTEGER", "part_page INTEGER", "indexed_number INTEGER", "from_page INTEGER");
		DBUtils.createTable(c, "entries", entriesColumns);

		PreparedStatement insertEntry = DBUtils.createInsert(c, "entries", DBUtils.isolateColumnNames(entriesColumns));

		for (Entry e: p.getProcessor().getEntries())
		{
			int i = 0;
			insertEntry.setInt(++i, e.part.number);
			insertEntry.setInt(++i, e.part.page);
			insertEntry.setInt(++i, e.id);
			insertEntry.setInt(++i, e.pageNumber);
			insertEntry.execute();
		}

		c.commit();

		c.close();
	}
}
