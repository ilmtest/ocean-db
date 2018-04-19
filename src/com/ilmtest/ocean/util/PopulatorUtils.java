package com.ilmtest.ocean.util;

import com.ilmtest.util.text.TextUtils;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class PopulatorUtils
{
	public static boolean arabicMatch(String main, String other, NormalizedLevenshtein l)
	{
		String otherBody = TextUtils.normalize(other).replaceAll("[^\\p{InArabic}]+", "");
		double distance = l.distance(main, otherBody);

		return (distance < 0.25) || main.contains(otherBody) || otherBody.contains(main);
	}
}
