package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Book;
import com.ilmtest.searchengine.model.Chapter;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.SahihMuslim;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;
import com.ilmtest.sunnah.com.boundary.Dictionary;
import com.ilmtest.sunnah.com.boundary.SunnahDotComDictionary;
import com.ilmtest.sunnah.com.controller.ChapterMutator;
import com.ilmtest.sunnah.com.controller.ChapterProcessor;
import com.ilmtest.sunnah.com.controller.HtmlChapterParser;
import com.ilmtest.sunnah.com.controller.NarrationParser;
import com.ilmtest.util.text.TextUtils;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class SahihMuslimPopulator
{
	public void process() throws Exception
	{
		ChapterMutator cp = new ChapterMutator();
		List<EntryTuple> m_entries = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\D+");
		
		for (int i = 0; i <= 56; i++)
		{
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/muslim/html/"+i+".html") );

			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/muslim/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/muslim/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);

			for (Entry e: entries)
			{
				EntryTuple tuple = new EntryTuple();
				tuple.sunnah = e;
				m_entries.add(tuple);
				
				if ( e.arabicIndex.matches("^\\d+ [a-z]+$") ) {
					e.arabicIndex = e.arabicIndex.split(" ")[0];
				} else if ( e.arabicIndex.matches("^Introduction \\d+$") ) {
					e.arabicIndex = e.arabicIndex.split(" ")[1];
				} else if ( !e.arabicIndex.matches("^\\d+$") ) {
					Matcher matcher = pattern.matcher(e.arabicIndex);
					matcher.find();
					e.arabicIndex = e.arabicIndex.substring(0, e.arabicIndex.indexOf(matcher.group()));
				}
			}
		}
		
		System.out.println("Total: "+m_entries.size());
		
		NormalizedLevenshtein l = new NormalizedLevenshtein();
		
		// vowelled ones, we just want the grades
		System.out.println("Searching for matches in vowels...");
		Populator p = new Populator( new SahihMuslim() );
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/1727.db").getPath());
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
		Map<Integer, Book> bookIdToTitle = new HashMap<>();
		for (EntryTuple et: m_entries)
		{
			Entry e = et.sunnah;

			if (e.book != null && e.book.english != null && e.book.arabic != null) {
				e.book.english = e.book.english.replaceAll("[\\t\\n\\r]+"," ");
				bookIdToTitle.put(e.book.number, e.book);
			} else if (e.book != null && (e.book.english == null || e.book.arabic == null)) {
				e.book = bookIdToTitle.get(e.book.number);
			}

			if (et.vowels != null) {
				e.part = et.vowels.part;
				e.pageNumber = et.vowels.pageNumber;
			}
			
			if ( et.sunnah.arabicIndex.startsWith("Sahih Muslim") ) {
				et.sunnah.arabicIndex = et.sunnah.arabicIndex.split(" ")[3].trim();
			}

			allEntries.add(e);
		}

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/muslim.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, allEntries, 336, 0);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}
	
	public static void main(String[] args) throws Exception
	{
		new SahihMuslimPopulator().process();
	}
}