package com.ilmtest.ocean.util;

import com.ilmtest.searchengine.model.Entry;
import com.ilmtest.util.text.TextUtils;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class EntryUtils
{
	public static boolean isSame(Entry e, Entry other)
	{
		NormalizedLevenshtein l = new NormalizedLevenshtein();

		String eBody = TextUtils.normalize(e.arabic).replaceAll("[^\\p{InArabic}]+", "");
		String otherBody = TextUtils.normalize(other.arabic).replaceAll("[^\\p{InArabic}]+", "");

		double distance = l.distance(eBody, otherBody);
		System.out.println(distance);

		return  (distance < 0.30) || eBody.contains(otherBody) || otherBody.contains(eBody);
	}
}