package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Chapter;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.SahihMuslimProcessor;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;
import com.ilmtest.sunnah.com.boundary.Dictionary;
import com.ilmtest.sunnah.com.boundary.SunnahDotComDictionary;
import com.ilmtest.sunnah.com.controller.ChapterMutator;
import com.ilmtest.sunnah.com.controller.ChapterProcessor;
import com.ilmtest.sunnah.com.controller.HtmlChapterParser;
import com.ilmtest.sunnah.com.controller.NarrationParser;
import com.ilmtest.util.text.TextUtils;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class MuwattaPopulator
{
	public void process() throws Exception
	{
		List<EntryTuple> m_entries = new ArrayList<>();
		ChapterMutator cp = new ChapterMutator();

		for (int i = 1; i <= 61; i++)
		{
			System.out.println(i);
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/malik/html/"+i+".html") );

			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/malik/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/malik/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);

			for (Entry e: entries)
			{
				if (e.arabic == null) {
					e.arabic = "?";
				}

				if (e.book.english == null || e.book.arabic == null) {
					e.book = np.getEntries().get(0).book;
				}

				EntryTuple tuple = new EntryTuple();
				tuple.sunnah = e;
				m_entries.add(tuple);
			}
		}

		System.out.println("Total: "+m_entries.size());

		System.out.println("Searching for matches in vowels...");
		NormalizedLevenshtein l = new NormalizedLevenshtein();
		Populator p = new Populator( new SahihMuslimProcessor() );
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/1699.db").getPath());
		p.process(c);
		c.close();

		LinkedList<Entry> ll = new LinkedList<>( p.getProcessor().getEntries() );

		for (EntryTuple e: m_entries)
		{
			String eBody = TextUtils.normalize(e.sunnah.arabic).replaceAll("[^\\p{InArabic}]+", "");

			for (int i = 0; i < ll.size(); i++)
			{
				Entry vowelled = ll.get(i);

				if ( PopulatorUtils.arabicMatch(eBody, vowelled.arabic, l) )
				{
					e.vowels = vowelled;
					ll.remove(i);
					break;
				}
			}
		}

		System.out.println("Oustanding..."+ll.size());

		List<Entry> allEntries = new ArrayList<>();
		for (EntryTuple et: m_entries)
		{
			Entry e = et.sunnah;

			if (et.vowels != null) {
				e.part = et.vowels.part;
				e.pageNumber = et.vowels.pageNumber;
			}

			allEntries.add(e);
		}
		
		Collections.sort(allEntries/*, new Comparator<Entry>()
		{
			@Override
			public int compare(Entry o1, Entry o2)
			{
				if (o1.arabicIndex == null && o2.arabicIndex != null) {
					return -1;
				} else if (o1.arabicIndex != null && o2.arabicIndex == null) {
					return 1;
				} else if (o1.arabicIndex == null && o2.arabicIndex == null) {
					return 0;
				}
				
				return Integer.parseInt(o1.arabicIndex)-Integer.parseInt(o2.arabicIndex);
			}
		}*/);

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/muwatta.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, allEntries, 398, 9325);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}

	public static void main(String[] args) throws Exception
	{
		new MuwattaPopulator().process();
	}
}