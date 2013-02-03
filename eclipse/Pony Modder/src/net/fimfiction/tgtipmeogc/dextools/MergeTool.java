package net.fimfiction.tgtipmeogc.dextools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.jf.dexlib.AnnotationDirectoryItem;
import org.jf.dexlib.AnnotationDirectoryItem.FieldAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.FieldAnnotationIteratorDelegate;
import org.jf.dexlib.AnnotationDirectoryItem.MethodAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.MethodAnnotationIteratorDelegate;
import org.jf.dexlib.AnnotationDirectoryItem.ParameterAnnotation;
import org.jf.dexlib.AnnotationDirectoryItem.ParameterAnnotationIteratorDelegate;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.AnnotationSetRefList;
import org.jf.dexlib.AnnotationVisibility;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDataItem.EncodedField;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.CodeItem.EncodedCatchHandler;
import org.jf.dexlib.CodeItem.EncodedTypeAddrPair;
import org.jf.dexlib.CodeItem.TryItem;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.EncodedArrayItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.Item;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.ProtoIdItem;
import org.jf.dexlib.StringDataItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.TypeListItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.EncodedValue.AnnotationEncodedSubValue;

public class MergeTool {
	
	/** All items will be merged into this DexFile */
	private DexFile mDestination;
	
	private HashMap<Item, Item> mMerged;
	
	/**
	 * @param destination All items will be merged into this DexFile
	 */
	public MergeTool(DexFile destination) {
		mDestination = destination;
		mMerged = new HashMap<Item, Item>();
	}
	
	public void place() {
		updateInstructions(mMerged);
	}
	
	/**
	 * Copy all items from the specified DexFile into the working DexFile.
	 * @param sourceFile
	 * @return
	 */
	public void merge(DexFile sourceFile) {
		final HashMap<Item, Item> mergeMap = mMerged;
		
		for(StringDataItem item : sourceFile.StringDataSection.getItems()) {
			Item result = StringDataItem.internStringDataItem(mDestination, item.getStringValue());
			mergeMap.put(item, result);
		}

		for(StringIdItem item : sourceFile.StringIdsSection.getItems()) {
			Item result = StringIdItem.internStringIdItem(mDestination, item.getStringValue());
			mergeMap.put(item, result);
		}

		for(TypeIdItem item : sourceFile.TypeIdsSection.getItems()) {
			Item result = TypeIdItem.internTypeIdItem(mDestination, item.getTypeDescriptor());
			mergeMap.put(item, result);
		}

		for(TypeListItem item : sourceFile.TypeListsSection.getItems()) {
			List<TypeIdItem> typeList = new LinkedList<TypeIdItem>();
			for(TypeIdItem typeId : item.getTypes()) {
				typeList.add((TypeIdItem)mergeMap.get(typeId));
			}
			
			Item result = TypeListItem.internTypeListItem(mDestination, typeList);
			mergeMap.put(item, result);
		}

		for(ProtoIdItem item : sourceFile.ProtoIdsSection.getItems()) {
			TypeIdItem returnType = (TypeIdItem)mergeMap.get(item.getReturnType());
			TypeListItem parameters = (TypeListItem)mergeMap.get(item.getParameters());
			
			Item result = ProtoIdItem.internProtoIdItem(mDestination, returnType, parameters);
			mergeMap.put(item, result);
		}

		for(FieldIdItem item : sourceFile.FieldIdsSection.getItems()) {
			TypeIdItem classType = (TypeIdItem)mergeMap.get(item.getContainingClass());
			TypeIdItem fieldType = (TypeIdItem)mergeMap.get(item.getFieldType());
			StringIdItem fieldName = (StringIdItem)mergeMap.get(item.getFieldName());
			
			Item result = FieldIdItem.internFieldIdItem(mDestination, classType, fieldType, fieldName);
			mergeMap.put(item, result);
		}

		for(MethodIdItem item : sourceFile.MethodIdsSection.getItems()) {
			TypeIdItem classType = (TypeIdItem)mergeMap.get(item.getContainingClass());
			ProtoIdItem methodPrototype = (ProtoIdItem)mergeMap.get(item.getPrototype());
			StringIdItem methodName = (StringIdItem)mergeMap.get(item.getMethodName());
			
			Item result = MethodIdItem.internMethodIdItem(mDestination, classType, methodPrototype, methodName);
			mergeMap.put(item, result);
		}

		for(AnnotationItem item : sourceFile.AnnotationsSection.getItems()) {
			AnnotationVisibility visibility = item.getVisibility();
			AnnotationEncodedSubValue sourceAnnotation = item.getEncodedAnnotation();
			TypeIdItem annotationType = (TypeIdItem)mergeMap.get(sourceAnnotation.annotationType);
			ArrayList<StringIdItem> names = new ArrayList<StringIdItem>();
			for(StringIdItem name : sourceAnnotation.names) {
				names.add((StringIdItem)mergeMap.get(name));
			}
			
			AnnotationEncodedSubValue annotationValue =
				new AnnotationEncodedSubValue(annotationType, names.toArray(new StringIdItem[0]), sourceAnnotation.values);
			
			Item result = AnnotationItem.internAnnotationItem(mDestination, visibility, annotationValue);
			mergeMap.put(item, result);
		}

		for(AnnotationSetItem item : sourceFile.AnnotationSetsSection.getItems()) {
			List<AnnotationItem> annotations = new LinkedList<AnnotationItem>();
			for(AnnotationItem annotation : item.getAnnotations()) {
				annotations.add((AnnotationItem)mergeMap.get(annotation));
			}
			
			Item result = AnnotationSetItem.internAnnotationSetItem(mDestination, annotations);
			mergeMap.put(item, result);
		}

		for(AnnotationSetRefList item : sourceFile.AnnotationSetRefListsSection.getItems()) {
			List<AnnotationSetItem> annotationSets = new LinkedList<AnnotationSetItem>();
			for(AnnotationSetItem set : item.getAnnotationSets()) {
				annotationSets.add((AnnotationSetItem)mergeMap.get(set));
			}
			
			Item result = AnnotationSetRefList.internAnnotationSetRefList(mDestination, annotationSets);
			mergeMap.put(item, result);
		}

		for(AnnotationDirectoryItem item : sourceFile.AnnotationDirectoriesSection.getItems()) {
			AnnotationSetItem classAnnotations = (AnnotationSetItem)mergeMap.get(item.getClassAnnotations());
			final List<FieldAnnotation> fieldList = new LinkedList<FieldAnnotation>();
			item.iterateFieldAnnotations(new FieldAnnotationIteratorDelegate() {
				public void processFieldAnnotations(FieldIdItem field,
						AnnotationSetItem fieldAnnotations) {
					FieldAnnotation fieldAnnotation = new FieldAnnotation(
							(FieldIdItem)mergeMap.get(field), 
							(AnnotationSetItem)mergeMap.get(fieldAnnotations));
					fieldList.add(fieldAnnotation);
				}
			});
			
			final List<MethodAnnotation> methodList = new LinkedList<MethodAnnotation>();
			item.iterateMethodAnnotations(new MethodAnnotationIteratorDelegate() {
				public void processMethodAnnotations(MethodIdItem method,
						AnnotationSetItem methodAnnotations) {
					MethodAnnotation methodAnnotation = new MethodAnnotation(
							(MethodIdItem)mergeMap.get(method), 
							(AnnotationSetItem)mergeMap.get(methodAnnotations));
					methodList.add(methodAnnotation);
					
				}
			});
			
			final List<ParameterAnnotation> parameterList = new LinkedList<ParameterAnnotation>();
			item.iterateParameterAnnotations(new ParameterAnnotationIteratorDelegate() {
				public void processParameterAnnotations(MethodIdItem method,
						AnnotationSetRefList parameterAnnotations) {
					ParameterAnnotation parameterAnnotation = new ParameterAnnotation(
							(MethodIdItem)mergeMap.get(method), 
							(AnnotationSetRefList)mergeMap.get(parameterAnnotations));
					parameterList.add(parameterAnnotation);
					
				}
			});
			
			Item result = AnnotationDirectoryItem.internAnnotationDirectoryItem(mDestination, classAnnotations, fieldList, methodList, parameterList);
			mergeMap.put(item, result);
		}

		for(DebugInfoItem item : sourceFile.DebugInfoItemsSection.getItems()) {
			ArrayList<Item> referencedItems = new ArrayList<Item>();
			for(Item reference : item.getReferencedItems()) {
				referencedItems.add(mergeMap.get(reference));
			}
			
			ArrayList<StringIdItem> parameterNames = new ArrayList<StringIdItem>();
			for(StringIdItem name : item.getParameterNames()) {
				parameterNames.add((StringIdItem)mergeMap.get(name));
			}
			
			Item result = DebugInfoItem.internDebugInfoItem(mDestination, item.getLineStart(), parameterNames.toArray(new StringIdItem[0]), item.getEncodedDebugInfo(), referencedItems.toArray(new Item[0]));
			mergeMap.put(item, result);
		}

		for(CodeItem item : sourceFile.CodeItemsSection.getItems()) {
			DebugInfoItem debugInfo = (DebugInfoItem)mergeMap.get(item.getDebugInfo());
			List<Instruction> instructions = arrayAsList(item.getInstructions());
			List<TryItem> tries = new LinkedList<TryItem>();
			List<EncodedCatchHandler> encodedCatchHandlers = new LinkedList<EncodedCatchHandler>();
			for(TryItem sourceTry : item.getTries()) {
				ArrayList<EncodedTypeAddrPair> handlers = new ArrayList<EncodedTypeAddrPair>();
				for(EncodedTypeAddrPair handler : sourceTry.encodedCatchHandler.handlers) {
					TypeIdItem exceptionType = (TypeIdItem)mergeMap.get(handler.exceptionType);
					handlers.add(new EncodedTypeAddrPair(exceptionType, handler.getHandlerAddress()));
				}
				
				EncodedCatchHandler encodedCatchHandler = new EncodedCatchHandler(handlers.toArray(new EncodedTypeAddrPair[0]), sourceTry.encodedCatchHandler.getCatchAllHandlerAddress());
				encodedCatchHandlers.add(encodedCatchHandler);
				
				TryItem destTry = new TryItem(sourceTry.getStartCodeAddress(), sourceTry.getTryLength(), encodedCatchHandler);
				tries.add(destTry);
			}
			
			CodeItem result = CodeItem.internCodeItem(mDestination, item.getRegisterCount(), item.getInWords(), item.getOutWords(), debugInfo, instructions, tries, encodedCatchHandlers);
			mergeMap.put(item, result);
		}

		for(ClassDataItem item : sourceFile.ClassDataSection.getItems()) {
			List<EncodedField> staticFields = new LinkedList<EncodedField>();
			for(EncodedField sourceField : item.getStaticFields()) {
				FieldIdItem field = (FieldIdItem)mergeMap.get(sourceField.field);
				EncodedField destField = new EncodedField(field, sourceField.accessFlags);
				staticFields.add(destField);
			}
			
			List<EncodedField> instanceFields = new LinkedList<EncodedField>();
			for(EncodedField sourceField : item.getInstanceFields()) {
				FieldIdItem field = (FieldIdItem)mergeMap.get(sourceField.field);
				EncodedField destField = new EncodedField(field, sourceField.accessFlags);
				instanceFields.add(destField);
			}
			
			List<EncodedMethod> directMethods = new LinkedList<EncodedMethod>();
			for(EncodedMethod sourceMethod : item.getDirectMethods()) {
				MethodIdItem method = (MethodIdItem)mergeMap.get(sourceMethod.method);
				CodeItem codeItem = (CodeItem)mergeMap.get(sourceMethod.codeItem);
				
				EncodedMethod destMethod = new EncodedMethod(method, sourceMethod.accessFlags, codeItem);
				directMethods.add(destMethod);
			}
			
			List<EncodedMethod> virtualMethods = new LinkedList<EncodedMethod>();
			for(EncodedMethod sourceMethod : item.getVirtualMethods()) {
				MethodIdItem method = (MethodIdItem)mergeMap.get(sourceMethod.method);
				CodeItem codeItem = (CodeItem)mergeMap.get(sourceMethod.codeItem);
				
				EncodedMethod destMethod = new EncodedMethod(method, sourceMethod.accessFlags, codeItem);
				virtualMethods.add(destMethod);
			}
			
			Item result = ClassDataItem.internClassDataItem(mDestination, staticFields, instanceFields, directMethods, virtualMethods);
			mergeMap.put(item, result);
		}

		for(EncodedArrayItem item : sourceFile.EncodedArraysSection.getItems()) {
			Item result = EncodedArrayItem.internEncodedArrayItem(mDestination, item.getEncodedArray());
			mergeMap.put(item, result);
		}

		for(ClassDefItem item : sourceFile.ClassDefsSection.getItems()) {
			TypeIdItem classType = (TypeIdItem)mergeMap.get(item.getClassType());
			TypeIdItem superType = (TypeIdItem)mergeMap.get(item.getSuperclass());
			TypeListItem implementedInterfaces = (TypeListItem)mergeMap.get(item.getInterfaces());
			AnnotationDirectoryItem annotations = (AnnotationDirectoryItem)mergeMap.get(item.getAnnotations());
			ClassDataItem classData = (ClassDataItem)mergeMap.get(item.getClassData());
			
			Item result = ClassDefItem.internClassDefItem(mDestination, classType, item.getAccessFlags(), superType, implementedInterfaces, item.getSourceFile(), annotations, classData, item.getStaticFieldInitializers());
			mergeMap.put(item, result);
		}
		
	}
	
	/**
	 * Updates all old item references in all methods.
	 * @param mergeMap Map where all old references are keys and new references are the corresponding values.
	 */
	private void updateInstructions(HashMap<Item, Item> mergeMap) {
		for(CodeItem code : mDestination.CodeItemsSection.getItems()) {
			for(Instruction i : code.getInstructions()) {
				if(i instanceof InstructionWithReference) {
					InstructionWithReference inst = (InstructionWithReference) i;

					if(mergeMap.containsKey(inst.referencedItem))
						inst.referencedItem = mergeMap.get(inst.referencedItem);
					
				}
			}
		}
	}
	
	/**
	 * @param array
	 * @return List containing each element in the array. Returns an empty list of array is null.
	 */
	private static List arrayAsList(Object[] array) {
		if(array == null) {
			return new LinkedList();
		}
		
		return Arrays.asList(array);
		
	}

}
