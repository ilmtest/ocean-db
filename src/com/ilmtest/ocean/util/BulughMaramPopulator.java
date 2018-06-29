package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Book;
import com.ilmtest.searchengine.model.Chapter;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.BulughMaram;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;
import com.ilmtest.sunnah.com.boundary.Dictionary;
import com.ilmtest.sunnah.com.boundary.SunnahDotComDictionary;
import com.ilmtest.sunnah.com.controller.ChapterMutator;
import com.ilmtest.sunnah.com.controller.ChapterProcessor;
import com.ilmtest.sunnah.com.controller.HtmlChapterParser;
import com.ilmtest.sunnah.com.controller.NarrationParser;
import com.ilmtest.util.text.TextUtils;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class BulughMaramPopulator
{
	private List<EntryTuple> m_entries = new ArrayList<>();

	public BulughMaramPopulator()
	{
	}

	public void process() throws Exception
	{
		BulughMaram processor = new BulughMaram();
		Populator p = new Populator(processor);
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/9111.db").getPath());
		p.process(c);
		c.close();

		ChapterMutator cp = new ChapterMutator();

		for (int i = 1; i <= 16; i++)
		{
			System.out.println(i);
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/bulugh/html/"+i+".html") );

			Map<Integer, Book> bookIdToTitle = new HashMap<>();
			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/bulugh/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/bulugh/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);

			for (Entry e: entries)
			{
				if (e.arabic == null) {
					e.arabic = "?";
				}
				
				if (e.book != null && e.book.english != null && e.book.arabic != null) {
					bookIdToTitle.put(e.book.number, e.book);
				} else if (e.book != null && (e.book.english == null || e.book.arabic == null)) {
					e.book = bookIdToTitle.get(e.book.number);
				}

				EntryTuple tuple = new EntryTuple();
				tuple.sunnah = e;
				m_entries.add(tuple);
			}

			if (i == 2 || i == 7 || i == 8)
			{
				Iterator<Entry> iterator = processor.getEntries().iterator();
				while ( iterator.hasNext() )
				{
					Entry left = iterator.next();

					if (left.book.number == i)
					{
						EntryTuple tuple = new EntryTuple();
						tuple.vowels = left;
						m_entries.add(tuple);

						iterator.remove();
					}
				}
			}
		}

		System.out.println("Total: "+m_entries.size());
		System.out.println("Outstanding: "+processor.getEntries().size());

		// --- first deal with the vowels, this has the right book numbers
		NormalizedLevenshtein l = new NormalizedLevenshtein();

		for (EntryTuple e: m_entries)
		{
			if (e.sunnah != null && e.vowels == null)
			{
				String eBody = TextUtils.normalize(e.sunnah.arabic).replaceAll("[^\\p{InArabic}]+", "");
				String eCommentary = e.sunnah.commentary != null ? TextUtils.normalize(e.sunnah.commentary).replaceAll("[^\\p{InArabic}]+", "") : null;

				Iterator<Entry> iterator = processor.getEntries().iterator();
				while ( iterator.hasNext() )
				{
					Entry vowelled = iterator.next();

					if ( PopulatorUtils.arabicMatch(eBody, vowelled.arabic, l) || ( (eCommentary != null) && (vowelled.commentary != null) && PopulatorUtils.arabicMatch(eCommentary, vowelled.commentary, l) ) )
					{
						e.vowels = vowelled;
						iterator.remove();
						break;
					}
				}
			}
		}

		System.out.println("Outstanding..."+processor.getEntries().size());

		List<Entry> allEntries = new ArrayList<>();
		for (EntryTuple et: m_entries)
		{
			Entry e = et.sunnah;
			Entry vowels = et.vowels;

			if (e == null)
			{
				e = vowels;
				vowels = null;
				e.arabicIndex = String.valueOf(e.id);
			}

			if (vowels != null)
			{
				e.part = vowels.part;
				e.pageNumber = vowels.pageNumber;

				if (e.commentary == null || e.commentary.isEmpty()) {
					e.commentary = vowels.commentary;
				}

				if (e.chapter == null) {
					e.chapter = vowels.chapter;
				}

				if (e.book == null) {
					e.book = vowels.book;
				}
			}

			e.arabicGrade = e.commentary;

			if (e.arabicGrade != null && e.arabicGrade.isEmpty()) {
				e.arabicGrade = null;
			}

			e.commentary = null;

			allEntries.add(e);
		}

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/bulugh.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, allEntries, 428, 15360);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}


	public static void main(String[] args) throws Exception
	{
		new BulughMaramPopulator().process();
	}
}