package com.ilmtest.ocean.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ilmtest.lib.io.IOUtils;
import com.ilmtest.searchengine.model.Book;
import com.ilmtest.searchengine.model.Chapter;
import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.shamela.controller.Populator;
import com.ilmtest.shamela.processors.AdabAlMufrad;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;
import com.ilmtest.sunnah.com.boundary.Dictionary;
import com.ilmtest.sunnah.com.boundary.SunnahDotComDictionary;
import com.ilmtest.sunnah.com.controller.ChapterMutator;
import com.ilmtest.sunnah.com.controller.ChapterProcessor;
import com.ilmtest.sunnah.com.controller.HtmlChapterParser;
import com.ilmtest.sunnah.com.controller.NarrationParser;
import com.ilmtest.util.text.TextUtils;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class AdabMufradPopulator
{
	private List<EntryTuple> m_entries = new ArrayList<>();
	private Map<String,String> m_grades = new HashMap<>();

	public AdabMufradPopulator()
	{
		m_grades.put("صـحـيـح", "صحيح");
		m_grades.put("ضـعـيـف", "ضعيف");
	}

	private void cleanGrade(Entry e)
	{
		if ( m_grades.containsKey(e.arabicGrade) ) {
			e.arabicGrade = m_grades.get(e.arabicGrade);
		}
	}

	public void process() throws Exception
	{
		ChapterMutator cp = new ChapterMutator();

		for (int i = 1; i <= 57; i++)
		{
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/adab/html/"+i+".html") );

			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/adab/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/adab/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);
			System.out.println(i);

			for (Entry e: entries)
			{
				if (e.arabic == null) {
					e.arabic = "?";
				}

				if (e.arabicGrade != null && e.englishGrade != null) {
					cleanGrade(e);
				} else if (e.arabicGrade == null && e.englishGrade == null) {
					System.out.println("BOTH GRADES NULL: "+e);
				} else if (e.arabicGrade == null) {
					System.out.println("ArabicMissing: "+e.englishGrade);
				} else if (e.englishGrade == null) {
					GradeUtils.translateGrade(e);
				}

				EntryTuple tuple = new EntryTuple();
				tuple.sunnah = e;
				m_entries.add(tuple);
			}
		}

		System.out.println("Total: "+m_entries.size());

		// --- first deal with the vowels, this has the right book numbers
		NormalizedLevenshtein l = new NormalizedLevenshtein();
		AdabAlMufrad processor = new AdabAlMufrad();
		Populator p = new Populator(processor);
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/12991.db").getPath());
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

		System.out.println("Outstanding..."+ll.size());

		System.out.println("Trying index matches...");

		for (int i = 0; i < ll.size(); i++)
		{
			Entry vowelled = ll.get(i);

			for (EntryTuple e: m_entries)
			{
				if (e.vowels == null
						&& (e.sunnah.arabicIndex != null)
						&& e.sunnah.arabicIndex.equals( String.valueOf(vowelled.id) )
						&& !processor.isDuplicateIndex(vowelled.id) ) {
					e.vowels = vowelled;
					ll.remove(i);
					break;
				}
			}
		}

		System.out.println("Outstanding..."+ll.size());

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

			signGrade(e);

			if (et.vowels != null) {
				e.part = et.vowels.part;
				e.pageNumber = et.vowels.pageNumber;
			}

			allEntries.add(e);

			if ( e.arabicIndex.equals("490") || e.arabicIndex.equals("603") )
			{
				Iterator<Entry> i = ll.iterator();
				while ( i.hasNext() )
				{
					Entry left = i.next();

					if ( (left.id > 490 && left.id < 538) || (left.id > 603 && left.id < 746) ) {
						translateGrade(left);
						signGrade(left);
						left.arabicIndex = String.valueOf(left.id);

						allEntries.add(left);
						i.remove();
					}
				}
			}
		}

		for (Entry e: allEntries)
		{
			if ( e.book.equals( new Book() ) ) {
				e.book = null;
			} else if ( (e.book != null && e.book.arabic != null) && (e.book.arabic.equals("ok") || e.book.arabic.equals("blank")) ) {
				e.book.arabic = null;
			}
		}

		Collections.sort(allEntries, new Comparator<Entry>()
		{
			@Override
			public int compare(Entry e1, Entry e2)
			{
				if (e1.pageNumber > 0 && e2.pageNumber > 0) {
					return e1.pageNumber-e2.pageNumber;
				}

				return e1.getIndex()-e2.getIndex();
			}
		});

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/adab.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, allEntries, 402, 14724);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}

	private void signGrade(Entry e)
	{
		if (e.arabicGrade != null)
		{
			if ( e.arabicGrade.trim().isEmpty() ) {
				e.arabicGrade = null;
			} else {
				e.arabicGrade = "الألباني: "+e.arabicGrade;
			}
		}

		if (e.englishGrade != null)
		{
			if ( e.englishGrade.trim().isEmpty() ) {
				e.englishGrade = null;
			} else {
				e.englishGrade = "Al-Albānī: "+e.englishGrade;
			}
		}
	}

	private void translateGrade(Entry left)
	{
		String grade = left.arabicGrade;

		if ( grade != null && grade.trim().isEmpty() ) {
			left.arabicGrade = null;
		} else if (grade != null) {
			GradeUtils.translateGrade(left);
		}
	}


	public static void main(String[] args) throws Exception
	{
		new AdabMufradPopulator().process();
	}
}