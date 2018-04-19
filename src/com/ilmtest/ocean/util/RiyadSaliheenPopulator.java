package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Chapter;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.SahihBukhariProcessor;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;
import com.ilmtest.sunnah.com.boundary.Dictionary;
import com.ilmtest.sunnah.com.boundary.SunnahDotComDictionary;
import com.ilmtest.sunnah.com.controller.ChapterMutator;
import com.ilmtest.sunnah.com.controller.ChapterProcessor;
import com.ilmtest.sunnah.com.controller.HtmlChapterParser;
import com.ilmtest.sunnah.com.controller.NarrationParser;

public class RiyadSaliheenPopulator
{
	private List<Entry> m_entries = new ArrayList<>();
	
	public void process() throws Exception
	{
		ChapterMutator cp = new ChapterMutator();
		cp.addRange(1717800, 1717810, 361, "باب كراهة الخروج من بلد وقع فيها الوباء فرارًا منه وكراهة القدوم عليه");
		cp.add(1717435, 340, "باب النهي عن رفع البصر إِلَى السماء في الصلاة");
		
		for (int i = 1; i <= 20; i++)
		{
			System.out.println(i);
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/riyadussaliheen/html/"+i+".html") );

			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/riyadussaliheen/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/riyadussaliheen/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);

			m_entries.addAll(entries);
		}
		
		System.out.println("Total: "+m_entries.size());
		
		Populator p = new Populator( new SahihBukhariProcessor() );
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/2348.db").getPath());
		p.process(c);
		c.close();

		Map<Integer, Entry> indexToMetadata = new HashMap<>();

		for ( Entry e: p.getProcessor().getEntries() ) {
			indexToMetadata.put(e.id, e);
		}

		for (Entry e: m_entries)
		{
			Entry metadata = indexToMetadata.get( e.getIndex() );

			if (metadata != null) {
				e.part = metadata.part;
				e.pageNumber = metadata.pageNumber;
			}
		}
		
		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/riyad.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, m_entries, 342, 8938);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}
	
	public static void main(String[] args) throws Exception
	{
		new RiyadSaliheenPopulator().process();
	}
}