package net.fimfiction.tgtipmeogc.dextools;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Item;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Section;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;


public class DexTools {
	private DexFile mDexFile;
	
	public DexTools(DexFile dexFile) {
		this.mDexFile = dexFile;
		
	}
	
	public Collection<Item> getItems() {
		Collection<Item> result = new LinkedList<Item>();
		
		for(ItemType type : ItemType.values()) {
			Section section = mDexFile.getSectionForType(type);
			
			if(section != null) {
				result.addAll(section.getItems());
			}
		}
		
		return result;
	}

	public List<MethodIdItem> getMethods() {
		List<MethodIdItem> result = new ArrayList<MethodIdItem>();
		
		for(CodeItem code : mDexFile.CodeItemsSection.getItems()) {
			for(Instruction i : code.getInstructions()) {
				if(i instanceof InstructionWithReference) {
					InstructionWithReference inst = (InstructionWithReference) i;
					Item item = inst.referencedItem;
					
					if(item instanceof MethodIdItem) {
						MethodIdItem method = (MethodIdItem) item;
						result.add(method);
						
					}
						
				}
			}
		}
		
		return result;
	}
	
	public byte[] toByteArray() {
		ByteArrayAnnotatedOutput dexStream = new ByteArrayAnnotatedOutput();

		mDexFile.place();
		mDexFile.writeTo(dexStream);
		
		byte[] bytes = dexStream.toByteArray();
		
		DexFile.calcSignature(bytes);
		DexFile.calcChecksum(bytes);
		
		return bytes;
		
	}
}
