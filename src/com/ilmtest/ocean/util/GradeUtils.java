package com.ilmtest.ocean.util;

import com.ilmtest.searchengine.model.Entry;

public class GradeUtils
{
	public static final boolean translateGrade(Entry e)
	{
		String arabicGrade = e.arabicGrade.trim();
		
		if (arabicGrade.startsWith("صحيح موقوفا")) {
			e.englishGrade = "Ṣaḥīḥ Mawqūf";
		} else if ( arabicGrade.startsWith("صحيح") || arabicGrade.endsWith(") صحيح") ) {
			e.englishGrade = "Ṣaḥīḥ (Authentic)";
		} else if ( arabicGrade.startsWith("حسن صحيح") ) {
			e.englishGrade = "Hasan Ṣaḥīḥ";
		} else if ( arabicGrade.startsWith("حسن") || arabicGrade.endsWith(") حسن") ) {
			e.englishGrade = "Hasan (Good)";
		} else if (arabicGrade.startsWith("حسن")) {
			e.englishGrade = "Hasan (Good)";
		} else if ( arabicGrade.startsWith("ضعيف") ) {
			e.englishGrade = "Da'īf (Weak)";
		} else if ( arabicGrade.startsWith("منكر") ) {
			e.englishGrade = "Munkar";
		} else if ( arabicGrade.startsWith("مقطوع") ) {
			e.englishGrade = "Maqtu'";
		} else if ( arabicGrade.startsWith("شاذ") ) {
			e.englishGrade = "Shādh (Irregular)";
		} else if ( arabicGrade.startsWith("موقوف") ) {
			e.englishGrade = "Mawqūf";
		}else {
			return false;
		}
		
		return true;
	}
}