package net.fimfiction.tgtipmeogc.manifesttools;

import java.io.IOException;

import pxb.android.axml.AxmlReader;

public class ManifestTools {
	
	private byte[] mManifest;
	private AndroidManifestParser mManifestInfo;
	
	public ManifestTools(byte[] manifest) throws IOException {
		mManifest = manifest;
		
		AxmlReader reader = new AxmlReader(manifest);
		mManifestInfo = new AndroidManifestParser();
		
		reader.accept(mManifestInfo);
	}
	
	public String getPackageName() {
		return mManifestInfo.mPackageName;
	}

}
