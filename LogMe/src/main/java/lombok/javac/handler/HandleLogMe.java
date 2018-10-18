package lombok.javac.handler;



import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;

import java.util.logging.Logger;
import com.sun.tools.javac.util.Name;
import lombok.LogMe;
import lombok.core.AST;
import lombok.javac.JavacNode;
import lombok.javac.handlers.JavacHandlerUtil;
import lombok.javac.handlers.JavacHandlerUtil.MemberExistsResult;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import com.sun.tools.javac.util.List;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.util.ListBuffer;


@ProviderFor(JavacAnnotationHandler.class)
@SuppressWarnings("restriction") 
public class HandleLogMe implements JavacAnnotationHandler<LogMe> {

	private static Logger logger = Logger.getLogger(HandleLogMe.class.getName());

	@Override
	public boolean handle(AnnotationValues<LogMe> annotation, JCAnnotation ast, JavacNode annotationNode) {
		JavacNode methodNode = annotationNode.up();
		switch (methodNode.getKind()) {
		case METHOD: 
			JCMethodDecl methodDecl = (JCMethodDecl) methodNode.get();
			String methodName = methodDecl.getName().toString();
			System.out.println("methodName : "+methodName);
			String logLevel = annotation.getInstance().level();
			//String logFieldName = "log";
			String logFieldName = annotation.getInstance().name();
			String logMethodName = logFieldName + "." + logLevel;
			TreeMaker  maker = annotationNode.getTreeMaker();
			String className = null;
			String logTypeName = null;
			Name logVarName = null;
			JavacNode typeNode = methodNode.up();
			JCClassDecl classDecl = (JCClassDecl) typeNode.get();
			if (AST.Kind.TYPE == typeNode.getKind()) {
				className = classDecl.getSimpleName().toString();
				for (JCTree def : classDecl.defs) {
					if (def instanceof JCVariableDecl) {
						JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) def;
						if (variableDecl.name.toString().equals(logFieldName)) {
							logVarName = variableDecl.name;
							logTypeName = variableDecl.getType().toString();
							break;
						}
					}
				}
				System.out.println("logVarName : "+logVarName);
				if (logVarName == null && JavacHandlerUtil.fieldExists(logFieldName, typeNode) == MemberExistsResult.NOT_EXISTS) {
					System.out.println(logFieldName+ "Log variable not found");
					JCExpression objectType = null;
					JCExpression logFactory = null;
					objectType = JavacHandlerUtil.chainDots(maker, typeNode, "org", "slf4j", "Logger");
					logFactory = JavacHandlerUtil.chainDots(maker, typeNode, "org", "slf4j", "LoggerFactory", "getLogger");
					 //argument list for method
		            ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();

		            //String value = annotation.getInstance().value();
		            /*String logName = (value == null || "".equals(value.trim()))
		                    ? typeDecl.sym.type.toString()
		                    : value;*/
		            String logName;
		            if (typeNode.get() instanceof JCClassDecl) {
		            	logName = classDecl.sym.type.toString();
		            }else {
		            	logName = "LogMe.class";
		            }
		                
		          
		            JCLiteral literal = maker.Literal(logName);
		            args.append(literal);
					JCExpression logValue = maker.Apply(List.<JCExpression> nil(), logFactory, args.toList());

		            JCVariableDecl fieldDecl = maker.VarDef(
		                    maker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC),
		                    typeNode.toName(logFieldName), objectType, logValue);
		            System.out.println("Creating new log field "+ fieldDecl);
		            JavacHandlerUtil.injectField(methodNode.up(), fieldDecl);
		          /*  for (JCTree def : ((JCClassDecl)typeNode.get()).defs) {
		            	System.out.println("Def variable "+ def);
						if (def instanceof JCVariableDecl) {
							System.out.println("Def variable "+ def);
						}
					}*/
				}
			}
		/*	JCBlock block = methodDecl.getBody();
			List<JCStatement> statements = block.stats;
			JCTree.JCExpression logMethod = JavacHandlerUtil.chainDots(maker,methodNode, logMethodName);
			System.out.println("logMethod : "+logMethod);
			JCTree.JCExpression logType = JavacHandlerUtil.chainDots(maker,typeNode, logTypeName);
			System.out.println("logType : "+logType);
			List<JCTree.JCVariableDecl> parameters = methodDecl.getParameters();
			JCMethodInvocation apply = maker.Apply(List.<JCTree.JCExpression>nil(), logMethod,
					generateLogArgs(parameters, className, methodName, maker, typeNode));*/
			List<JCTree.JCVariableDecl> parameters = methodDecl.getParameters();
			ListBuffer<JCTree.JCStatement> listBuffer = new ListBuffer<JCTree.JCStatement>();
			
			JCExpression printlnMethod = 
					JavacHandlerUtil.chainDots(maker, methodNode, logFieldName, logLevel);	
				//List<JCExpression> printlnArgs = List.<JCExpression>of(maker.Literal("hello world"));
			List<JCExpression> printlnArgs = generateLogArgs(parameters, className, methodName, maker, typeNode);
				JCMethodInvocation printlnInvocation = 
					maker.Apply(List.<JCExpression>nil(), printlnMethod, printlnArgs);
			
			
			//JCBlock methodBody = maker.Block(0, List.<JCStatement>of(maker.Exec(printlnInvocation)));
				
			listBuffer.append(maker.Exec(printlnInvocation));
            boolean hasReturn = false;
            JCExpression returnExp = null;
            for (JCTree.JCStatement stat : methodDecl.getBody().getStatements()) {
            	System.out.println("Stats "+stat.getKind());
            	if(Kind.RETURN == stat.getKind()) {
            		returnExp = ((JCReturn)stat).getExpression();
            		System.out.println("Return "+returnExp);
            		hasReturn = true;
            	}else {
            		listBuffer.append(stat);
            	}
            }
           // maker.Exec(apply);
            /*  methodDecl.body.stats = listBuffer.toLi.st();*/
			//annotationNode.getSymbolTable().setC
			//statements.prepend(maker.Exec(apply));
           // statements.append(maker.Exec(apply));
			
			/*for (JCTree.JCStatement stat : statements) {
				System.out.println("statements: "+stat);
            }*/
			
			//System.out.println("methodBody: "+methodBody);
			//methodDecl.body = methodBody;
            StringBuilder stringBuilder = new StringBuilder(className).append(".").append(methodName);
            stringBuilder.append(" ends");
            JCExpression loggersMethod = (JCExpression) printlnMethod.clone();
            ListBuffer<JCTree.JCExpression> returnExpList = new ListBuffer<JCTree.JCExpression>();
            returnExpList.append( maker.Literal(stringBuilder.toString()));
            if(hasReturn && annotation.getInstance().printReturnValue()) {
            	returnExpList.append(returnExp);
            }
            JCMethodInvocation lastLoggers = maker.Apply(List.<JCExpression>nil(), 
            		loggersMethod,returnExpList.toList());
            
            if(hasReturn) {
            	listBuffer.append(maker.Exec(lastLoggers));
            	listBuffer.append(methodDecl.getBody().getStatements().last());
            }else {
            	listBuffer.append(maker.Exec(lastLoggers));
            }
           
			methodDecl.body.stats = listBuffer.toList();
			break;
		default:
			annotationNode.addError("@LogBefore is legal only on types.");
			break;
		}
		return false;
	}

	public static List<JCTree.JCExpression> generateLogArgs(List<JCTree.JCVariableDecl> parameters, String className, String methodName, TreeMaker maker, JavacNode typeNode) {
		JCTree.JCExpression[] argsArray = new JCTree.JCExpression[parameters.size() + 1];

		StringBuilder stringBuilder = new StringBuilder(className).append(".").append(methodName);
		if (parameters.size() > 0) {
			stringBuilder.append(" ");
			for (JCTree.JCVariableDecl variableDecl : parameters) {
				stringBuilder.append(variableDecl.getName()).append(":{},");
			}
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		} else {
			stringBuilder.append(" begin");
		}

		argsArray[0] = maker.Literal(stringBuilder.toString());

		JCTree.JCExpression jsonStringMethod = JavacHandlerUtil.chainDots(maker,typeNode, "com.alibaba.fastjson.JSON.toJSONString");
		System.out.println("jsonStringMethod :"+jsonStringMethod);
		for (int i = 0; i < parameters.size(); i++) {
			//argsArray[i + 1] = maker.Apply(List.<JCExpression>nil(), jsonStringMethod, List.<JCTree.JCExpression>of(maker.Ident(parameters.get(i))));
			argsArray[i + 1] =  maker.Ident(parameters.get(i));
		}
		System.out.println(argsArray.length);
		return List.<JCTree.JCExpression>from(argsArray);
		//return List.<JCTree.JCExpression>nil();
		
	}

	/*  @Override
    public void handle(AnnotationValues<LogBefore> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        JavacNode methodNode = annotationNode.up();
        switch (methodNode.getKind()) {
            case METHOD:
                JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) methodNode.get();
                String methodName = methodDecl.getName().toString();
                String logLevel = annotation.getInstance().level();

                if (logLevel == null) {
                    logLevel = "info";
                }

                String logFieldName = "log";
                String logMethodName = logFieldName + "." + logLevel;

                String className = null;
                String logTypeName = null;
                Name logVarName = null;

                JavacNode typeNode = methodNode.up();
                if (AST.Kind.TYPE == typeNode.getKind()) {
                    JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) typeNode.get();
                    className = classDecl.getSimpleName().toString();
                    // ?????????log???
                    for (JCTree def : classDecl.defs) {
                        if (def instanceof JCTree.JCVariableDecl) {
                            JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) def;
                            if (variableDecl.name.toString().equals(logFieldName)) {
                                logVarName = variableDecl.name;
                                logTypeName = variableDecl.getType().toString();
                                break;
                            }
                        }
                    }
                    // ??log?????????
                    if (logVarName == null) {
                        return;
                    }
                }

                JCTree.JCBlock block = methodDecl.getBody();
                List<JCTree.JCStatement> statements = block.stats;

                TreeMaker  maker = annotationNode.getTreeMaker();

                JCTree.JCExpression logMethod = JavacHandlerUtil.chainDotsString(typeNode, logMethodName);
                JCTree.JCExpression logType = JavacHandlerUtil.chainDotsString(typeNode, logTypeName);

                List<JCTree.JCVariableDecl> parameters = methodDecl.getParameters();

                JCTree.JCExpression apply = maker.Apply(List.<JCTree.JCExpression>of(logType), logMethod,
                                                        generateLogArgs(parameters, className, methodName, maker, typeNode));

                ListBuffer<JCTree.JCStatement> listBuffer = new ListBuffer<JCTree.JCStatement>();
                listBuffer.append(maker.Exec(apply));

                for (JCTree.JCStatement stat : statements) {
                    listBuffer.append(stat);
                }
                methodDecl.body.stats = listBuffer.toList();
                annotationNode.getAst().setChanged();

                break;
            default:
                annotationNode.addError("@LogBefore is legal only on types.");
                break;
        }
    }

	 *//**
	 * ??log??????
	 *//*
    public static List<JCTree.JCExpression> generateLogArgs(List<JCTree.JCVariableDecl> parameters, String className, String methodName, JavacTreeMaker maker, JavacNode typeNode) {
        JCTree.JCExpression[] argsArray = new JCTree.JCExpression[parameters.size() + 1];

        StringBuilder stringBuilder = new StringBuilder(className).append(".").append(methodName);
        if (parameters.size() > 0) {
            stringBuilder.append(" ");
            for (JCTree.JCVariableDecl variableDecl : parameters) {
                stringBuilder.append(variableDecl.getName()).append(":{},");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        } else {
            stringBuilder.append(" begin");
        }

        argsArray[0] = maker.Literal(stringBuilder.toString());

        JCTree.JCExpression jsonStringMethod = JavacHandlerUtil.chainDots(typeNode, "com.alibaba.fastjson.JSON.toJSONString");

        for (int i = 0; i < parameters.size(); i++) {
            argsArray[i + 1] = maker.Apply(List.<JCTree.JCExpression>nil(), jsonStringMethod, List.<JCTree.JCExpression>of(maker.Ident(parameters.get(i))));
        }

        return List.<JCTree.JCExpression>from(argsArray);
    }*/

}
