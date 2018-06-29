package com.ilmtest.ocean.util;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.ilmtest.lib.io.IOUtils;

public class BulughDiskReader
{

	public BulughDiskReader()
	{
	}
	
	public static Collection<File> listFileTree(File dir) {
	    List<File> fileTree = new ArrayList<File>();
	    if(dir==null||dir.listFiles()==null){
	        return fileTree;
	    }
	    for (File entry : dir.listFiles()) {
	        if (entry.isFile()) fileTree.add(entry);
	        else fileTree.addAll(listFileTree(entry));
	    }
	    return fileTree;
	}

	public static void main(String[] args) throws Exception
	{
		ArrayList<String> narrations = new ArrayList<>();
		
		Collection<File> result = listFileTree( new File("/Users/rhaq/workspace/resources/disk/english/bulugh/02. Prayer") );
		Arrays.sort( result.toArray() );
		
		for (File f: result)
		{
			String content = IOUtils.readFileUtf8(f);
			narrations.add(content);
		}
		
		IOUtils.writeFile("/Users/rhaq/workspace/resources/bulugh.txt", StringUtils.join(narrations, "\n\n") );
	}
}
