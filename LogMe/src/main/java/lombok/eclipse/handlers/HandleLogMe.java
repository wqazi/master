package lombok.eclipse.handlers;

import java.util.Arrays;
import java.lang.reflect.Modifier;

import lombok.HelloWorld;
import lombok.core.AnnotationValues;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.mangosdk.spi.ProviderFor;

import static lombok.eclipse.Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;

@ProviderFor(EclipseAnnotationHandler.class)
public class HandleLogMe implements EclipseAnnotationHandler<LogMe> {

	@Override
	public boolean handle(AnnotationValues<HelloWorld> annotation, Annotation ast,
			EclipseNode annotationNode) {
		EclipseNode typeNode = annotationNode.up();

		if(notAClass(typeNode)) {
			annotationNode.addError("@LogMe is only supported on a class.");
			return false;
		}
		
		MethodDeclaration helloWorldMethod = 
			createHelloWorld(typeNode, annotationNode, annotationNode.get(), ast);
		
		EclipseHandlerUtil.injectMethod(typeNode, helloWorldMethod);
		
		return true;
	}

	private boolean notAClass(EclipseNode typeNode) {
		TypeDeclaration typeDecl = null;
		if (typeNode.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) typeNode.get();
		int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;
		return typeDecl != null &&
		   (modifiers &	(ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation | ClassFileConstants.AccEnum)) != 0;
	}

	private MethodDeclaration createHelloWorld(EclipseNode typeNode, EclipseNode errorNode, ASTNode astNode, Annotation source) {
		TypeDeclaration typeDecl = (TypeDeclaration) typeNode.get();

		MethodDeclaration method = new MethodDeclaration(typeDecl.compilationResult);
		Eclipse.setGeneratedBy(method, astNode);
		method.annotations = null;
		method.modifiers = Modifier.PUBLIC;
		method.typeParameters = null;
		method.returnType = new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
		method.selector = "helloWorld".toCharArray();
		method.arguments = null;
		method.binding = null;
		method.thrownExceptions = null;
		method.bits |= ECLIPSE_DO_NOT_TOUCH_FLAG;
		
		NameReference systemOutReference = createNameReference("System.out", source);
		Expression [] printlnArguments = new Expression[] { 
			new StringLiteral("Hello World".toCharArray(), astNode.sourceStart, astNode.sourceEnd, 0)
		};
		
		MessageSend printlnInvocation = new MessageSend();
		printlnInvocation.arguments = printlnArguments;
		printlnInvocation.receiver = systemOutReference;
		printlnInvocation.selector = "println".toCharArray();
		Eclipse.setGeneratedBy(printlnInvocation, source);
		
		method.bodyStart = method.declarationSourceStart = method.sourceStart = astNode.sourceStart;
		method.bodyEnd = method.declarationSourceEnd = method.sourceEnd = astNode.sourceEnd;
		method.statements = new Statement[] { printlnInvocation };
		return method;
	}

    private static NameReference createNameReference(String name, Annotation source) {
        int pS = source.sourceStart, pE = source.sourceEnd;
        long p = (long)pS << 32 | pE;

        char[][] nameTokens = Eclipse.fromQualifiedName(name);
        long[] pos = new long[nameTokens.length];
        Arrays.fill(pos, p);

        QualifiedNameReference nameReference = new QualifiedNameReference(nameTokens, pos, pS, pE);
        nameReference.statementEnd = pE;

        Eclipse.setGeneratedBy(nameReference, source);
        return nameReference;
    }

}
