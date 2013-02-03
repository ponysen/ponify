package net.fimfiction.tgtipmeogc.main;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;

import net.fimfiction.tgtipmeogc.apitools.ApiTools;
import net.fimfiction.tgtipmeogc.apktools.ApkTools;
import net.fimfiction.tgtipmeogc.dextools.DexTools;
import net.fimfiction.tgtipmeogc.dextools.HookTool;
import net.fimfiction.tgtipmeogc.dextools.MergeTool;
import net.fimfiction.tgtipmeogc.manifesttools.ManifestTools;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.Item;


public class Example {
	
	public static void main(String[] args) throws Exception {
		DexFile app = new DexFile(args[0]);
		DexFile lib = new DexFile(args[1]);
		String signedOut = args[2];
		String unsignedOut = signedOut + ".unsigned";
		String apiIn = args[3];
		
		Collection<Item> appItems = new DexTools(app).getItems();
		
		MergeTool appMerge = new MergeTool(app);
		appMerge.merge(lib);
		appMerge.place();
		
		HookTool appHooks = new HookTool(app);
		HookTool.HookMap hookMap = appHooks.new HookMap();

		
		//hookMap.addSuperclassHook(
		//		new HookTool.HookClass("Landroid/app/Activity;"),
		//		new HookTool.HookClass("Lnet/fimfiction/tgtipmeogc/mods/ExampleActivityMod;"));
	
		//hookMap.addClassTreeHook(
		//		new HookTool.HookClass("Landroid/view/View;"),
		//		new HookTool.HookClass("Lnet/fimfiction/tgtipmeogc/mods/ExampleViewMod;"));
	
		hookMap.place(appItems);
		
		DexTools dex = new DexTools(app);
		
		byte[] moddedDex = dex.toByteArray();
		
		FileInputStream appInput = new FileInputStream(args[0]);
		FileOutputStream appOutput = new FileOutputStream(unsignedOut);
		
		ApkTools apk = new ApkTools(appInput, appOutput);
		apk.replace("classes.dex", moddedDex);
		apk.place();
		
		byte[] manifestBytes = apk.getEntry("AndroidManifest.xml");
		ManifestTools manifest = new ManifestTools(manifestBytes);
		
		ApkTools.signApk(unsignedOut, signedOut);
		
		ApiTools api = new ApiTools(apiIn);
		
		
		
	}
	



	
	
}
