package net.fimfiction.tgtipmeogc.dextools;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Item;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.ProtoIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.Instruction35c;
import org.jf.dexlib.Code.Format.Instruction3rc;
import org.jf.dexlib.Util.AccessFlags;

public class HookTool {
	
	private DexFile mDexFile;
	
	private Map<HookMethod, MethodIdItem> mMethods;
	private Map<HookClass, TypeIdItem> mClasses;
	private Map<String, Set<String>> mSubclasses;
	private Map<HookClass, HashSet<EncodedMethod>> mClassMethods;
	
	public HookTool(DexFile dexFile) {
		mDexFile = dexFile;
		
	}
	
	private Map<HookMethod, MethodIdItem> getMethodMap() {
		if(mMethods != null) {
			return mMethods;
		}
		
		mMethods = new HashMap<HookMethod, MethodIdItem>();
		
		for(MethodIdItem item : mDexFile.MethodIdsSection.getItems()) {
			mMethods.put(new HookMethod(item), item);
		}
		
		return mMethods;
	}
	
	private void initClassMaps() {
		mClasses = new HashMap<HookClass, TypeIdItem>();
		mClassMethods = new HashMap<HookClass, HashSet<EncodedMethod>>();
		mSubclasses = new HashMap<String, Set<String>>();
		
		for(ClassDefItem item : mDexFile.ClassDefsSection.getItems()) {
			TypeIdItem classType = item.getClassType();
			HookClass classHook = new HookClass(classType);
			mClasses.put(classHook, classType);
			
			String superclassType = item.getSuperclass().getTypeDescriptor();
			Set<String> subclassSet = mSubclasses.get(superclassType);
			
			if(subclassSet == null) {
				subclassSet = new TreeSet<String>();
				mSubclasses.put(superclassType, subclassSet);
			}
			
			subclassSet.add(classType.getTypeDescriptor());
			
			ClassDataItem classData = item.getClassData();
			
			if(classData == null) {
				continue;
			}
			
			HashSet<EncodedMethod> classMethods = new HashSet<EncodedMethod>();
			
			for(EncodedMethod method : classData.getDirectMethods()) {
				classMethods.add(method);
			}
			
			for(EncodedMethod method : classData.getVirtualMethods()) {
				classMethods.add(method);
			}
			
			mClassMethods.put(classHook, classMethods);
			
		}
		
	}
	
	private Map<HookClass, TypeIdItem> getClassMap() {
		if(mClasses == null) {
			initClassMaps();
		}
		
		return mClasses;
	}
	
	private Map<String, Set<String>> getSubclassMap() {
		if(mClasses == null) {
			initClassMaps();
		}
		
		return mSubclasses;
	}
	
	private Map<HookClass, HashSet<EncodedMethod>> getClassMethodsMap() {
		if(mClassMethods == null) {
			initClassMaps();
		}
		
		return mClassMethods;
	}
	
	/**
	 * Inserts hooks into a single code item.
	 * 
	 * @param hooks
	 * @param classData
	 * @param code
	 */
	private void hookReferences(HookMap hooks, ClassDataItem classData, CodeItem code) {
		if(code == null) {
			return;
		}
		
		Instruction[] instructions = code.getInstructions();
		for(int index=0; index < instructions.length; index++) {
			Instruction i = instructions[index];
			
			if(i instanceof InstructionWithReference) {
				InstructionWithReference inst = (InstructionWithReference) i;
				
				HookItem oldTarget = HookItem.getInstance(inst.getReferencedItem());
				
				if(!hooks.containsKey(oldTarget)) {
					continue;
				}
				
				Item newTarget = hooks.get(oldTarget);
				
				inst.referencedItem = newTarget;
				
				if(newTarget instanceof MethodIdItem) {
					// Need to update the invoke type if it changed
					updateInvokeType(inst, (MethodIdItem) newTarget);
					
				}
			}
		}
	}
	
	private void updateInvokeType(Instruction inst, MethodIdItem newMethod) {
		int accessFlags = newMethod.getAccess();
		int staticMask = AccessFlags.STATIC.getValue();
		int privateMask = AccessFlags.PRIVATE.getValue();
		int finalMask = AccessFlags.FINAL.getValue();
		int constructorMask = AccessFlags.CONSTRUCTOR.getValue();
		int directMask = privateMask | finalMask | constructorMask;
		
		if((accessFlags & staticMask) != 0) {
			if(inst instanceof Instruction35c) {
				inst.opcode = Opcode.INVOKE_STATIC;
			}
			else if(inst instanceof Instruction3rc) {
				inst.opcode = Opcode.INVOKE_STATIC_RANGE;
			}
			
		} else if((accessFlags & directMask) != 0) {
			if(inst instanceof Instruction35c) {
				inst.opcode = Opcode.INVOKE_DIRECT;
			}
			else if(inst instanceof Instruction3rc) {
				inst.opcode = Opcode.INVOKE_DIRECT_RANGE;
			}

		} else {
			if(inst instanceof Instruction35c) {
				inst.opcode = Opcode.INVOKE_VIRTUAL;
			}
			else if(inst instanceof Instruction3rc) {
				inst.opcode = Opcode.INVOKE_VIRTUAL_RANGE;
			}
		}
	}
	
	/**
	 * Insert hooks into the specified CodeItems.
	 * 
	 * @param hooks Description of what references to hook, and with what.
	 * @param codeItems Collection containing all CodeItem objects where hooks should be placed.
	 */
	public void hookReferences(HookMap hooks, Collection<Item> codeItems) {
		for(ClassDataItem classData : mDexFile.ClassDataSection.getItems()) {
			for(EncodedMethod method : classData.getDirectMethods()) {
				CodeItem code = method.codeItem;
			
				if(!codeItems.contains(code)) {
					continue;
				}
				
				hookReferences(hooks, classData, code);
			}
			
			for(EncodedMethod method : classData.getVirtualMethods()) {
				CodeItem code = method.codeItem;
			
				if(!codeItems.contains(code)) {
					continue;
				}
				
				hookReferences(hooks, classData, code);
			}
		}
	}
	
	/**
	 * Insert hooks into all code in the wrapped DexFile.
	 * 
	 * @param hooks Description of what references to hook, and with what.
	 */
	public void hookReferences(HookMap hooks) {
		for(ClassDataItem classData : mDexFile.ClassDataSection.getItems()) {
			for(EncodedMethod method : classData.getDirectMethods()) {
				CodeItem code = method.codeItem;
			
				hookReferences(hooks, classData, code);
			}
			
			for(EncodedMethod method : classData.getVirtualMethods()) {
				CodeItem code = method.codeItem;
			
				hookReferences(hooks, classData, code);
			}
		}
	}
	
	public void hookSuperclass(HookMap hooks, ClassDefItem classDef) {
		HookItem oldTarget = HookItem.getInstance(classDef.getSuperclass());
		
		if(oldTarget == null) {
			return;
		}
		
		if(!hooks.containsKey(oldTarget)) {
			return;
		}
		
		TypeIdItem newTarget = (TypeIdItem) hooks.get(oldTarget);
		
		assert(newTarget != null);
		
		if(newTarget == classDef.getClassType()) {
			return;
		}
		
		classDef.setSuperclass(newTarget);
		
		HookClass classType = new HookClass(classDef.getClassType());
		
		if(hooks.mIgnoreMethods.contains(classType)) {
			return;
		}
		
		if(hooks.mDead.isEmpty()) {
			return;
		}
		
		// get rid of virtual methods so the hooked class method is called instead
		
		String className = newTarget.getTypeDescriptor();
		
		List<EncodedMethod> newDirects = new LinkedList<EncodedMethod>();
		List<EncodedMethod> newVirtuals = new LinkedList<EncodedMethod>();
		
		ClassDataItem classData = classDef.getClassData();
		
		for(EncodedMethod encodedMethod : classData.getDirectMethods()) {
			newDirects.add(encodedMethod);
		}
		
		for(EncodedMethod encodedMethod : classData.getVirtualMethods()) {
			HookMethod method = new HookMethod(encodedMethod.method);
			method.mClasspath = className;
			
			if(!hooks.mDead.contains(method)) {
				newVirtuals.add(encodedMethod);
			}
		}
		
		classData.update(newDirects, newVirtuals);
		
	}
	
	public void hookSuperclasses(HookMap hooks) {
		for(ClassDefItem classDef : mDexFile.ClassDefsSection.getItems()) {
			hookSuperclass(hooks, classDef);
		}
	}
	
	public void hookSuperclasses(HookMap hooks, Collection<Item> classDefItems) {
		for(Item item : classDefItems) {
			if(item instanceof ClassDefItem) {
				hookSuperclass(hooks, (ClassDefItem) item);
			}
			
		}
	}
	
	public static class HookItem {
		public static HookItem getInstance(Item item) {
			if(item instanceof MethodIdItem) {
				return new HookMethod((MethodIdItem) item);
			}
			
			if(item instanceof TypeIdItem) {
				return new HookClass((TypeIdItem) item);
			}
			
			if(item instanceof ClassDefItem) {
				ClassDefItem classDef = (ClassDefItem) item;
				return new HookClass(classDef.getClassType());
			}
			
			return null;
		}
		
		public boolean equals(Object object) {
			// force this to be overridden
			throw new UnsupportedOperationException();
		}
		
		public int hashCode() {
			// force this to be overridden
			throw new UnsupportedOperationException();
		}
		
	}
	
	public static class HookMethod extends HookItem {
		public static final HookMethod NOP = null;
		
		private String mClasspath;
		private String mName;
		private String mArgs;
		private String mReturn;
		
		public HookMethod(String classpath, String name, String[] args, String returnType) {
			mClasspath = classpath;
			mName = name;
			
			if(args != null && args.length > 0) {
				StringBuilder argsBuilder = new StringBuilder(args[0]);
				
				for(int i=1; i < args.length; i++) {
					argsBuilder.append(args[i]);
				}
				
				mArgs = argsBuilder.toString();
			}	
			
			mReturn = returnType;
		}
		
		public HookMethod(MethodIdItem item) {
			mClasspath = item.getContainingClass().getTypeDescriptor();
			mName = item.getMethodName().getStringValue();
			
			TypeListItem parameters = item.getPrototype().getParameters();
			if(parameters != null) {
				mArgs = parameters.getTypeListString("");
			}

			mReturn = item.getPrototype().getReturnType().getTypeDescriptor();
			
		}
		
		public HookMethod clone() {
			HookMethod result = new HookMethod(mClasspath, mName, null, mReturn);
			result.mArgs = mArgs;
			return result;
		}
		
		public int hashCode() {
			return mClasspath.hashCode() + 
					mName.hashCode() + 
					mReturn.hashCode() + 
					(mArgs == null ? 0 : mArgs.hashCode());
		}
		
		public boolean equals(Object object) {
			
			if(object == null) {
				return false;
			}

			if(!(object instanceof HookMethod)) {
				return false;
			}

			HookMethod other = (HookMethod) object;
			
			if(!mClasspath.equals(other.mClasspath)) {
				return false;
			}

			if(!mName.equals(other.mName)) {
				return false;
			}
			
			
			if(!mReturn.equals(other.mReturn)) {
				return false;
			}

			return (mArgs == other.mArgs) || mArgs.equals(other.mArgs);
			
		}
		
		public String toString() {
			return mClasspath + "." +
					mName + "(" +
					mArgs + ")" +
					mReturn;
		}
		
	}
	
	public static class HookClass extends HookItem {
		String mClasspath;
		
		public HookClass(TypeIdItem item) {
			mClasspath = item.getTypeDescriptor();
		}
		
		public HookClass(String classpath) {
			mClasspath = classpath;
		}
		
		public boolean equals(Object object) {
			if(!(object instanceof HookClass)) {
				return false;
			}
			
			HookClass other = (HookClass) object;
			
			return mClasspath.equals(other.mClasspath);
		}
		
		public int hashCode() {
			return mClasspath.hashCode();
		}
	}
	
	/**
	 * An object to describe how item references should be hooked.
	 *
	 */
	public class HookMap {
		private Map<HookItem, Item> mHooks;
		private Set<HookItem> mDead;
		private Set<HookItem> mIgnoreMethods;
		private boolean mClassHooksExist;
		private boolean mMethodHooksExist;
		
		public HookMap() {
			mHooks = new HashMap<HookItem, Item>();
			mDead = new HashSet<HookItem>();
			mIgnoreMethods = new HashSet<HookItem>();
			mClassHooksExist = false;
			mMethodHooksExist = false;
		}
		
		public void place() {
			HookTool.this.hookReferences(this);
			
			if(mClassHooksExist) {
				HookTool.this.hookSuperclasses(this);
			}
		}
		
		public void place(Collection<Item> items) {
			HookTool.this.hookReferences(this, items);
			
			if(mClassHooksExist) {
				HookTool.this.hookSuperclasses(this);
			}
		}
		
		/**
		 * Hooks exactly the specified method and only that method
		 * @param from
		 * @param to
		 */
		public void addSpecificMethodHook(HookMethod from, MethodIdItem to) {
			mMethodHooksExist = true;
			mHooks.put(from, to);
		}
		
		/**
		 * Hooks the specified method and all call-through methods
		 * @param from
		 * @param to
		 */
		public void addMethodHook(HookMethod from, MethodIdItem to) {
			mMethodHooksExist = true;
			
			Queue<HookMethod> affectedMethods = new LinkedList<HookMethod>();
			affectedMethods.add(from);
			
			do {
				HookMethod affected = affectedMethods.remove();
				mHooks.put(affected, to);
				
				Set<String> allSubclasses = getSubclassMap().get(affected.mClasspath);
				if(allSubclasses == null) {
					continue;
				}
				
				for(String subclass : allSubclasses) {
					HookMethod possible = affected.clone();
					possible.mClasspath = subclass;
					
					MethodIdItem possibleMethodInfo = getMethodMap().get(possible);
					if(possibleMethodInfo == null || possibleMethodInfo.isFallthrough()) {
						affectedMethods.add(possible);
					}
				}
				
			} while(!affectedMethods.isEmpty());
		}
		
		
		public void addMethodHook(MethodIdItem from, HookMethod to) {
			addMethodHook(new HookMethod(from), to);
		}
		
		public void addMethodHook(HookMethod from, HookMethod to) {
			if(to == HookMethod.NOP) {
				mMethodHooksExist = true;
				mHooks.put(from, null);
				return;
			}
			
			MethodIdItem newMethod = getMethodMap().get(to);
			
			if(newMethod == null) {
				throw new NullPointerException("unknown method replacement: " + to);
			}
			
			addMethodHook(from, newMethod);
		}
		
		public void addSuperclassHook(HookClass from, HookClass to) {
			if(to == null) {
				throw new NullPointerException("replacement class cannot be null");
			}
			
			TypeIdItem newClass = getClassMap().get(to);
			
			if(newClass == null) {
				throw new NullPointerException("unknown class replacement");
			}
			
			mClassHooksExist = true;
			mHooks.put(from, newClass);
			
			// hook all direct methods
			
			HashSet<EncodedMethod> newClassMethods = getClassMethodsMap().get(to);
			
			if(newClassMethods != null) {
				for(EncodedMethod newDirect : newClassMethods) {
					if(!newDirect.isDirect()) {
						continue;
					}
					
					ProtoIdItem newPrototype = newDirect.method.methodPrototype;
					
					TypeListItem parameters = newPrototype.getParameters();
					String[] parameterList = null;
					
					if(parameters != null) {
						parameterList = parameters.toStringArray();
					}
					
					String methodName = newDirect.method.getMethodName().getStringValue();
					String returnType = newPrototype.getReturnType().getTypeDescriptor();
					
					HookMethod oldConstructor = new HookMethod(
							from.mClasspath, methodName, parameterList, returnType);

					addSpecificMethodHook(oldConstructor, newDirect.method);
					
				}
			}
		}
		
		public void addSubclassHook(HookClass from, HookClass to) {
			addSuperclassHook(from, to);
			
			HashSet<EncodedMethod> newClassMethods = getClassMethodsMap().get(to);
			
			if(newClassMethods != null) {
				for(EncodedMethod newVirtual : newClassMethods) {
					if(newVirtual.isDirect()) {
						continue;
					}
					
					HookMethod deadMethod = new HookMethod(newVirtual.method);
					mDead.add(deadMethod);
					
				}
			}
			
		}
		
		public void addClassTreeHook(HookClass origin, HookClass to) {
			addSuperclassHook(origin, to);
			
			Set<HookClass> superclassHookSet = new HashSet<HookClass>();
			superclassHookSet.add(origin);
			
			for(ClassDefItem classDef : mDexFile.ClassDefsSection.getItems()) {
				HookClass superclass = new HookClass(classDef.getSuperclass());
				
				if(superclassHookSet.contains(superclass)) {
					TypeIdItem classType = classDef.getClassType();
					HookClass newHook = new HookClass(classType);
					superclassHookSet.add(newHook);
					mHooks.put(newHook, classType);
				}
				
			}
			
			HookClass[] superclassHooks = superclassHookSet.toArray(new HookClass[0]);

			HashSet<EncodedMethod> newClassMethods = getClassMethodsMap().get(to);
			
			if(newClassMethods != null) {
				for(EncodedMethod newVirtual : newClassMethods) {
					if(newVirtual.isDirect()) {
						continue;
					}
					
					for(HookClass superclass : superclassHooks) {
						HookMethod deadMethod = new HookMethod(newVirtual.method);
						deadMethod.mClasspath = superclass.mClasspath;
						mDead.add(deadMethod);
					}
					
				}
			}
			
		}
		
		public void ignoreMethods(HookItem from) {
			mIgnoreMethods.add(from);
		}
		
		public Item get(HookItem key) {
			return mHooks.get(key);
		}
		
		public boolean containsKey(HookItem key) {
			if(key == null) {
				return false;
			}
			
			return mHooks.containsKey(key);
		}
		
	}


}
