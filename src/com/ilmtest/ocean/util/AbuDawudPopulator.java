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
import com.ilmtest.shamela.processors.AbuDawudNoVowels;
import com.ilmtest.shamela.processors.AbuDawudVowels;
import com.ilmtest.sunnah.com.boundary.DatabaseBoundary;
import com.ilmtest.sunnah.com.boundary.Dictionary;
import com.ilmtest.sunnah.com.boundary.SunnahDotComDictionary;
import com.ilmtest.sunnah.com.controller.ChapterMutator;
import com.ilmtest.sunnah.com.controller.ChapterProcessor;
import com.ilmtest.sunnah.com.controller.HtmlChapterParser;
import com.ilmtest.sunnah.com.controller.NarrationParser;

public class AbuDawudPopulator
{
	private List<Entry> m_entries = new ArrayList<>();

	public AbuDawudPopulator()
	{
	}

	public void process() throws Exception
	{
		ChapterMutator cp = new ChapterMutator();
		cp.add(900510, 28, "Chapter: Washing The Siwak");
		cp.addRange(917040, 917230, 1, "بَابُ التَّعْرِيفِ بِاللُّقَطَةِ");

		for (int i = 1; i <= 43; i++)
		{
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/abudawud/html/"+i+".html") );

			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/abudawud/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/abudawud/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);
			System.out.println(i);

			m_entries.addAll(entries);
		}

		System.out.println("Total: "+m_entries.size());
		
		Map<Integer, Entry> indexToMetadata = new HashMap<>();
		
		// --- first deal with the vowels, this has the right book numbers
		Populator p = new Populator( new AbuDawudVowels() );
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/1726.db").getPath());
		p.process(c);
		c.close();
		
		for ( Entry e: p.getProcessor().getEntries() ) {
			indexToMetadata.put(e.id, e);
		}
		
		for (Entry e: m_entries)
		{
			Entry metadata = indexToMetadata.get( e.getIndex() );

			if (metadata != null) {
				e.part = metadata.part;
				e.pageNumber = metadata.pageNumber;
				
				if ( (e.arabicGrade == null) && (metadata.arabicGrade != null) ) {
					e.arabicGrade = metadata.arabicGrade;
				}
			}
		}
		// --- done with vowelled ones
		
		// unvowelled ones, we just want the grades
		indexToMetadata.clear();
		p = new Populator( new AbuDawudNoVowels() );
		c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/1755.db").getPath());
		p.process(c);
		c.close();

		for ( Entry e: p.getProcessor().getEntries() ) {
			indexToMetadata.put(e.id, e);
		}

		for (Entry e: m_entries)
		{
			Entry metadata = indexToMetadata.get( e.getIndex() );

			if (metadata != null)
			{
				if ( (metadata.arabicGrade != null) && !metadata.arabicGrade.equals("**") && ((e.arabicGrade == null) || ( metadata.arabicGrade.matches(".*\\d+.*") ) ) ) {
					e.arabicGrade = metadata.arabicGrade;
				}
			}
		}
		
		for (Entry e: m_entries)
		{
			if (e.arabic == null || e.english == null) {
				System.out.println("BodyMissing: "+e);
			}
			
			if (e.arabicIndex == null || e.englishIndex == null ) {
				System.out.println("IndexMissing: "+e);
			}
			
			if ( !e.arabicIndex.matches("\\d+$") ) {
				System.out.println("WrongIndex: "+e);
			}
			
			if (e.arabicGrade == null || e.arabicGrade.isEmpty()) {
				System.out.println(e.arabicIndex+" ArabicGradingMissing: "+e.arabicGrade);
			}
			
			if (e.englishGrade == null && e.arabicGrade != null)
			{
				if ( e.arabicGrade.startsWith("صحيح") || e.arabicGrade.endsWith(") صحيح") ) {
					e.englishGrade = "Sahih";
				} else if ( e.arabicGrade.startsWith("حسن صحيح") ) {
					e.englishGrade = "Hasan Sahih";
				} else if ( e.arabicGrade.startsWith("حسن") || e.arabicGrade.endsWith(") حسن") ) {
					e.englishGrade = "Hasan";
				} else if ( e.arabicGrade.startsWith("ضعيف") ) {
					e.englishGrade = "Da'if";
				} else if ( e.arabicGrade.startsWith("منكر") ) {
					e.englishGrade = "Munkar";
				} else if ( e.arabicGrade.startsWith("مقطوع") ) {
					e.englishGrade = "Maqtu'";
				} else if ( e.arabicGrade.startsWith("شاذ") ) {
					e.englishGrade = "Shaadh";
				} else {
					System.out.println(e.arabicIndex+" EnglishGradingMissing: "+e.arabicGrade);
				}
			}

			if (e.book == null || e.book.arabic == null || e.book.english == null ||
					e.chapter == null || e.chapter.arabicTitle == null || e.chapter.englishTitle == null) {
				//System.out.println(e);
			}
		}
		
		for (Entry e: m_entries)
		{
			if (e.arabicGrade != null) {
				e.arabicGrade = "الألباني: "+e.arabicGrade;
			}
			
			if (e.englishGrade != null) {
				e.englishGrade = "Al-Albānī: "+e.englishGrade;
			}
		}

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/abudawud.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, m_entries, 341);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}
}