package net.fimfiction.tgtipmeogc.apitools;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class ApiTools {
	
	private AndroidApi mApi;
	
	public ApiTools(String filename) throws Exception {
		DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder = domBuilderFactory.newDocumentBuilder();
		Document dom = domBuilder.parse(filename);
			
		mApi = new AndroidApi(dom);
	}

}
