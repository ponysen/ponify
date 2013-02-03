package net.fimfiction.tgtipmeogc.manifesttools;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.AxmlVisitor.NodeVisitor;
import pxb.android.axml.AxmlWriter;



class AndroidManifestParser extends AxmlVisitor {
	private final static String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
	
	String mPackageName;
	
	public AndroidManifestParser(AxmlWriter writer) {
		super(writer);
	}
	
	public AndroidManifestParser() {

	}

	@Override
	public NodeVisitor first(String ns, String name) {
		NodeVisitor manifestNode = super.first(ns, name);
		return new ManifestParser(manifestNode);
	}
	
	private class ManifestParser extends NodeVisitor {
		public ManifestParser(NodeVisitor first) {
			super(first);
		}

		public void attr(String ns, String name, int resourceId, int type, Object obj) {
			super.attr(ns, name, resourceId, type, obj);
			
			if(ns == null && name.equals("package")) {
				mPackageName = obj.toString();
			}
			
		}
		
	}
}