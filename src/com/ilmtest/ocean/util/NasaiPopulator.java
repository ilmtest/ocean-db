package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Book;
import com.ilmtest.searchengine.model.Chapter;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.NasaiNoVowels;
import com.ilmtest.shamela.processors.NasaiVowelled;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;
import com.ilmtest.sunnah.com.boundary.Dictionary;
import com.ilmtest.sunnah.com.boundary.SunnahDotComDictionary;
import com.ilmtest.sunnah.com.controller.ChapterMutator;
import com.ilmtest.sunnah.com.controller.ChapterProcessor;
import com.ilmtest.sunnah.com.controller.HtmlChapterParser;
import com.ilmtest.sunnah.com.controller.NarrationParser;
import com.ilmtest.util.text.TextUtils;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class NasaiPopulator
{
	private List<EntryTuple> m_entries = new ArrayList<>();

	public NasaiPopulator()
	{
	}

	public void process() throws Exception
	{
		NormalizedLevenshtein l = new NormalizedLevenshtein();
		ChapterMutator cp = new ChapterMutator();

		for (int i = 0; i <= 51; i++)
		{
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/nasai/html/"+i+".html") );

			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/nasai/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/nasai/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);

			for (Entry e: entries)
			{
				if (e.arabic == null) {
					System.out.println(e);
				} else if (e.english == null) {
					System.out.println("BodyMissing: "+e);
				} else {
					EntryTuple et = new EntryTuple();
					et.sunnah = e;
					m_entries.add(et);
				}
			}
		}

		System.out.println("Total: "+m_entries.size());

		// unvowelled ones, we just want the grades
		System.out.println("Searching for matches in no vowels...");
		Populator p = new Populator( new NasaiNoVowels() );
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/nasai_no_vowels.db").getPath());
		p.process(c);
		c.close();

		LinkedList<Entry> ll = new LinkedList<>( p.getProcessor().getEntries() );

		for (EntryTuple e: m_entries)
		{
			String eBody = TextUtils.normalize(e.sunnah.arabic).replaceAll("[^\\p{InArabic}]+", "");

			for (int i = 0; i < ll.size(); i++)
			{
				Entry noVowel = ll.get(i);

				if ( PopulatorUtils.arabicMatch(eBody, noVowel.arabic, l) )
				{
					e.noVowels = noVowel;
					ll.remove(i);
					break;
				}
			}
		}

		System.out.println("Oustanding..."+ll.size());
		System.out.println("Trying index matches...");

		for (int i = 0; i < ll.size(); i++)
		{
			Entry noVowel = ll.get(i);

			for (EntryTuple e: m_entries)
			{
				if (e.noVowels == null && (e.sunnah.englishIndex != null) && e.sunnah.englishIndex.equals( String.valueOf(noVowel.id) ) ) {
					e.noVowels = noVowel;
					ll.remove(i);
					break;
				}
			}
		}

		System.out.println("Oustanding (Part 2)..."+ll.size());

		// vowelled ones, we just want the grades
		System.out.println("Searching for matches in vowels...");
		p = new Populator( new NasaiVowelled() );
		c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/nasai_vowels.db").getPath());
		p.process(c);
		c.close();

		LinkedList<Entry> ll2 = new LinkedList<>( p.getProcessor().getEntries() );

		for (EntryTuple e: m_entries)
		{
			String eBody = TextUtils.normalize(e.sunnah.arabic).replaceAll("[^\\p{InArabic}]+", "");

			for (int i = 0; i < ll2.size(); i++)
			{
				Entry vowelled = ll2.get(i);

				if ( PopulatorUtils.arabicMatch(eBody, vowelled.arabic, l) )
				{
					e.vowels = vowelled;
					ll2.remove(i);
					break;	
				}
			}
		}

		System.out.println("Oustanding..."+ll2.size());
		System.out.println("Trying index matches...");

		for (int i = 0; i < ll2.size(); i++)
		{
			Entry vowelled = ll2.get(i);

			for (EntryTuple e: m_entries)
			{
				if (e.vowels == null && (e.sunnah.englishIndex != null) && e.sunnah.englishIndex.equals( String.valueOf(vowelled.id) ) ) {
					e.vowels = vowelled;
					ll2.remove(i);
					break;
				}
			}
		}

		System.out.println("Oustanding (Part 2)..."+ll2.size());

		List<Entry> allEntries = new ArrayList<>();
		Map<Integer, Book> bookIdToTitle = new HashMap<>();
		for (EntryTuple et: m_entries)
		{
			Entry e = et.sunnah;
			e.arabicGrade = "";
			e.englishGrade = null;
			e.arabicIndex = e.englishIndex;

			if (e.book != null && e.book.english != null && e.book.arabic != null) {
				e.book.english = e.book.english.replaceAll("[\\t\\n\\r]+"," ");
				bookIdToTitle.put(e.book.number, e.book);
			} else if (e.book != null && (e.book.english == null || e.book.arabic == null)) {
				e.book = bookIdToTitle.get(e.book.number);
			}

			if (et.noVowels != null && et.noVowels.arabicGrade != null) {
				e.arabicGrade += "الألباني: "+et.noVowels.arabicGrade+"\n";
			} else if (et.vowels != null && et.vowels.arabicGrade != null) {
				e.arabicGrade += "الألباني: "+et.vowels.arabicGrade+"\n";
			}

			e.arabicGrade = !e.arabicGrade.isEmpty() ? e.arabicGrade.trim() : null;

			if (et.vowels != null) {
				e.part = et.vowels.part;
				e.arabicIndex = String.valueOf(et.vowels.id);
				e.pageNumber = et.vowels.pageNumber;
			} else if (et.noVowels != null) {
				e.arabicIndex = String.valueOf(et.noVowels.id);
			}

			allEntries.add(e);
		}

		Collections.sort(allEntries);

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/nasai.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, allEntries, 399, 9996);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}

	public static void main(String[] args) throws Exception
	{
		new NasaiPopulator().process();
	}
}