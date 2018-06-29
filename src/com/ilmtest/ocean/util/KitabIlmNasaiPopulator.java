package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.KitabIlmNasai;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;

public class KitabIlmNasaiPopulator
{
	public KitabIlmNasaiPopulator()
	{
	}

	public void process() throws Exception
	{
		KitabIlmNasai processor = new KitabIlmNasai();
		Populator p = new Populator(processor);
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/7777.db").getPath());
		p.process(c);
		c.close();

		System.out.println("Outstanding: "+processor.getEntries().size());
		
		for (Entry e: processor.getEntries())
		{
			if (e.arabicGrade != null)
			{
				GradeUtils.translateGrade(e);				
				e.arabicGrade = "الألباني: "+e.arabicGrade;
			}
		}

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/kitab_ilm.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, processor.getEntries(), 329, 15360);

		c.close();
	}


	public static void main(String[] args) throws Exception
	{
		new KitabIlmNasaiPopulator().process();
	}
}