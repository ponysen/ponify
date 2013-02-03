package net.fimfiction.tgtipmeogc.apitools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.fimfiction.tgtipmeogc.apitools.AndroidApi.AndroidPackage.AndroidClass.AndroidField;
import net.fimfiction.tgtipmeogc.apitools.AndroidApi.AndroidPackage.AndroidClass.AndroidMethod;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class AndroidApi {
	public List<AndroidPackage> mPackages = new LinkedList();
	private HashMap<String, AndroidMethod> mMethodMap = new HashMap();
	private HashMap<String, AndroidField> mFieldMap = new HashMap();
	
	public AndroidApi(Document dom) {
		Element api = dom.getDocumentElement();
		
		for(Node node = api.getFirstChild(); node != null; node = node.getNextSibling()) {
			String type = node.getNodeName();
			
			if("#text".equals(type)) {
				continue;
			}
			
			if("package".equals(type)) {
				mPackages.add(new AndroidPackage(node));
			} else {
				throw new RuntimeException("unknown node type \"" + type + "\" in api");
			}
		}
		
	}
	
	public AndroidMethod getMethod(String signature) {
		return mMethodMap.get(signature);
	}
	
	public AndroidField getField(String name) {
		return mFieldMap.get(name);
	}
	
	
	
	
	public class AndroidPackage {
		public String mPackageName;
		public List<AndroidClass> mClasses = new LinkedList();
		
		public AndroidPackage(Node packageNode) {
			mPackageName = packageNode.getAttributes().getNamedItem("name").getNodeValue();

			for(Node node = packageNode.getFirstChild(); node != null; node = node.getNextSibling()) {
				String type = node.getNodeName();
				
				if("#text".equals(type)) {
					continue;
				}
				
				if("class".equals(type)) {
					mClasses.add(new AndroidClass(node));
				} else if("interface".equals(type)) {
					mClasses.add(new AndroidClass(node));
				} else {
					throw new RuntimeException("unknown node type \"" + type + "\" in api package");
				}
			}
		}
		
		public class AndroidClass {
			public String mClassType;
			
			public List<AndroidField> mFields = new LinkedList();
			public List<AndroidMethod> mMethods = new LinkedList();
			public List<AndroidImplements> mImplements = new LinkedList();
			
			public AndroidClass(Node classNode) {
				mClassType = mPackageName + "." + classNode.getAttributes().getNamedItem("name").getNodeValue();
				mClassType = removeGeneric(mClassType);
				
				for(Node node = classNode.getFirstChild(); node != null; node = node.getNextSibling()) {
					String type = node.getNodeName();
					
					if("#text".equals(type)) {
						continue;
					}
					
					if("constructor".equals(type)) {
						mMethods.add(new AndroidMethod(node));
					} else if("field".equals(type)) {
						mFields.add(new AndroidField(node));
					} else if("method".equals(type)) {
						mMethods.add(new AndroidMethod(node));
					} else if("implements".equals(type)) {
						mImplements.add(new AndroidImplements(node));
					} else {
						throw new RuntimeException("unknown node type \"" + type + "\" in api class");
					}
				}
			}
			
			public class AndroidField {
				public String mFieldName, mFieldType;
				
				public AndroidField(Node fieldNode) {
					mFieldName = mClassType + "." + fieldNode.getAttributes().getNamedItem("name").getNodeValue();
					mFieldType = fieldNode.getAttributes().getNamedItem("type").getNodeValue();
					mFieldType = removeGeneric(mFieldType);
					
					mFieldMap.put(mFieldName, this);
				}
			}
			
			public class AndroidMethod {
				public String mMethodName, mMethodType;
				public List<AndroidParameter> mParameters = new ArrayList();
				public List<AndroidException> mExceptions = new ArrayList();
				
				public AndroidMethod(Node methodNode) {
					String methodType = methodNode.getNodeName();
					
					if("constructor".equals(methodType)) {
						mMethodName = mClassType + ".<init>";
						mMethodType = "void";
					} else if("method".equals(methodType)) {
						mMethodName = mClassType + "." + methodNode.getAttributes().getNamedItem("name").getNodeValue();
						mMethodType = methodNode.getAttributes().getNamedItem("return").getNodeValue();
						mMethodType = removeGeneric(mMethodType);
					} else {
						throw new RuntimeException("unknown method type " + methodType);
					}
					
					for(Node node = methodNode.getFirstChild(); node != null; node = node.getNextSibling()) {
						String type = node.getNodeName();
						
						if("#text".equals(type)) {
							continue;
						}
						
						if("parameter".equals(type)) {
							mParameters.add(new AndroidParameter(node));
						} else if("exception".equals(type)) {
							mExceptions.add(new AndroidException(node));
						} else {
							throw new RuntimeException("unknown node type \"" + type + "\" in api method");
						}
					}
					
					String parameterString = "";
					
					if(mParameters.size() != 0) {
						StringBuffer parameters = new StringBuffer();
						for(AndroidParameter param : mParameters) {
							parameters.append(",");
							parameters.append(param.mParameterType);
						}
						
						parameterString = parameters.toString().substring(1);
					}
					
					mMethodMap.put(mMethodName + "(" + parameterString + ")", this);
				}
			}
		}
		
	}
	
	public class AndroidImplements {
		public String mImplementsType;
		
		public AndroidImplements(Node implementsNode) {
			mImplementsType = implementsNode.getAttributes().getNamedItem("name").getNodeValue();
			mImplementsType = removeGeneric(mImplementsType);
		}
	}
	
	public class AndroidParameter {
		public String mParameterName, mParameterType;
		
		public AndroidParameter(Node parameterNode) {
			mParameterName = parameterNode.getAttributes().getNamedItem("name").getNodeValue();
			mParameterType = parameterNode.getAttributes().getNamedItem("type").getNodeValue();
			mParameterType = removeGeneric(mParameterType);
		}
		
	}
	
	public class AndroidException {
		public String mExceptionType;
		
		public AndroidException(Node exceptionNode) {
			mExceptionType = exceptionNode.getAttributes().getNamedItem("type").getNodeValue();
			mExceptionType = removeGeneric(mExceptionType);
		}
		
	}
	
	private static String removeGeneric(String type) {
		int typeEnd = type.indexOf('<');
		
		if(typeEnd > 0) {
			return type.substring(0, typeEnd);
		} else {
			return type;
		}
	}
	
}