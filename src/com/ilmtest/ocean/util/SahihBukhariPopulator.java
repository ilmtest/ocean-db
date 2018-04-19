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

public class SahihBukhariPopulator
{
	private List<Entry> m_entries = new ArrayList<>();

	public SahihBukhariPopulator()
	{
	}

	public void process() throws Exception
	{
		ChapterMutator cp = new ChapterMutator();
		cp.addRange(100540, 100560, 41);
		cp.addRange(101160, 101170, 41);
		cp.add(110080, 3, "بَابُ طُولِ السُّجُودِ فِي قِيَامِ اللَّيْلِ");
		cp.add(110090, 4, "بَابُ تَرْكِ القِيَامِ لِلْمَرِيضِ");
		cp.addRange(113180, 113190, "باب فضل صدقة الشحيح الصحيح");
		cp.add(116730, 148, "باب النُّزُولِ بِذِي طُوًى قَبْلَ أَنْ يَدْخُلَ مَكَّةَ، وَالنُّزُولِ بِالْبَطْحَاءِ الَّتِي بِذِي الْحُلَيْفَةِ إِذَا رَجَعَ مِنْ مَكَّةَ");
		cp.add(117240, "بَابُ قَوْلِ اللَّهِ تَعَالَى: {أَوْ صَدَقَةٍ} وَهْيَ إِطْعَامُ سِتَّةِ مَسَاكِينَ");
		cp.add(122290, "بَابُ إِذَا وَكَّلَ رَجُلاً، فَتَرَكَ الْوَكِيلُ شَيْئًا، فَأَجَازَهُ الْمُوَكِّلُ، فَهُوَ جَائِزٌ، وَإِنْ أَقْرَضَهُ إِلَى أَجَلٍ مُسَمًّى جَازَ");
		cp.add(123890, "بَابُ إِذَا اخْتَلَفُوا فِي الطَّرِيقِ الْمِيتَاءِ- وَهْيَ الرَّحْبَةُ تَكُونُ بَيْنَ الطَّرِيقِ- ثُمَّ يُرِيدُ أَهْلُهَا الْبُنْيَانَ، فَتُرِكَ مِنْهَا الطَّرِيقُ سَبْعَةَ أَذْرُعٍ");
		cp.add(125981, "بَابُ الْيَمِينُ عَلَى الْمُدَّعَى عَلَيْهِ، فِي الأَمْوَالِ وَالْحُدُودِ");
		cp.add(126320, "بَابُ قَوْلُ النَّبِيِّ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ لِلْحَسَنِ بْنِ عَلِيٍّ رَضِيَ اللَّهُ عَنْهُمَا: «ابْنِي هَذَا سَيِّدٌ، وَلَعَلَّ اللَّهَ أَنْ يُصْلِحَ بِهِ بَيْنَ فِئَتَيْنِ عَظِيمَتَيْنِ»");
		cp.add(126940, "بَابُ قَوْلِ اللَّهِ تَعَالَى: {وَيَسْأَلُونَكَ عَنِ الْيَتَامَى قُلْ إِصْلاَحٌ لَهُمْ خَيْرٌ وَإِنْ تُخَالِطُوهُمْ فَإِخْوَانُكُمْ وَاللَّهُ يَعْلَمُ الْمُفْسِدَ مِنَ الْمُصْلِحِ وَلَوْ شَاءَ اللَّهُ لأَعْنَتَكُمْ إِنَّ اللَّهَ عَزِيزٌ حَكِيمٌ}");
		cp.add(168260, "بَابُ عَمَلٌ صَالِحٌ قَبْلَ الْقِتَالِ");
		cp.add("بَابُ مَا أَقْطَعَ النَّبِيُّ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ مِنَ الْبَحْرَيْنِ، وَمَا وَعَدَ مِنْ مَالِ الْبَحْرَيْنِ وَالْجِزْيَةِ، وَلِمَنْ يُقْسَمُ الْفَيْءُ وَالْجِزْيَةُ؟", 128790, 128801);
		cp.addRange(171070, 171160, "بَابُ خَلْقِ آدَمَ صَلَوَاتُ اللَّهِ عَلَيْهِ وَذُرِّيَّتِهِ");
		cp.addRange(171240, 171260, "بَابُ قَوْلِ اللَّهِ تَعَالَى: {وَإِلَى عَادٍ أَخَاهُمْ هُودًا قَالَ يَا قَوْمِ اعْبُدُوا اللَّهَ}، وَقَوْلِهِ: {إِذْ أَنْذَرَ قَوْمَهُ بِالأَحْقَافِ} إِلَى قَوْلِهِ تَعَالَى: {كَذَلِكَ نَجْزِي الْقَوْمَ الْمُجْرِمِينَ}");
		cp.add(171530, "بَابُ قَوْلُهُ عَزَّ وَجَلَّ: {وَنَبِّئْهُمْ عَنْ ضَيْفِ إِبْرَاهِيمَ إِذْ دَخَلُوا عَلَيْهِ الْآيَةَ}");
		cp.add(171740, "بَابُ: {وَاذْكُرْ فِي الْكِتَابِ مُوسَى إِنَّهُ كَانَ مُخْلِصًا وَكَانَ رَسُولاً نَبِيًّا وَنَادَيْنَاهُ مِنْ جَانِبِ الطُّورِ الأَيْمَنِ وَقَرَّبْنَاهُ نَجِيًّا} كَلَّمَهُ");
		cp.addRange(172490, 172730, 53, "باب حَدِيثُ الْغَارِ");
		cp.addRange(131260, 131280, "باب مَنَاقِبِ قُرَيْشٍ");
		
		for (int i = 1; i <= 97; i++)
		{
			List<Chapter> chapters = new HtmlChapterParser().parseChapters( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/bukhari/html/"+i+".html") );

			NarrationParser np = new NarrationParser();
			np.setMutator(cp);
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/bukhari/arabic/"+i+".json") );
			np.collect( new File("/Users/rhaq/workspace/resources/sunnah.com/collections/bukhari/english/"+i+".json") );

			List<Entry> entries = np.getEntries();
			ChapterProcessor chapterProcessor = new ChapterProcessor();
			chapterProcessor.process(entries, chapters);

			m_entries.addAll(entries);
		}

		System.out.println("Total: "+m_entries.size());

		Populator p = new Populator( new SahihBukhariProcessor() );
		Connection c = DriverManager.getConnection("jdbc:sqlite:"+new File("/Users/rhaq/workspace/resources/raw/1681.db").getPath());
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

		File f = IOUtils.getFreshFile("/Users/rhaq/workspace/resources/bukhari.db");
		c = DriverManager.getConnection("jdbc:sqlite:"+f.getPath());
		DatabaseBoundary db = new DatabaseBoundary();
		db.process(c, m_entries, 335, 0);

		Dictionary d = new Dictionary();
		SunnahDotComDictionary.apply(d);

		d.apply(c, "entries", "body");
		d.apply(c, "parts", "title");
		c.close();
	}
}