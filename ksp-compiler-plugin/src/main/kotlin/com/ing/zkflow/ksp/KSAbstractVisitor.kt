package com.ing.zkflow.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSDynamicReference
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSParenthesizedReference
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor

@Suppress("TooManyFunctions")
abstract class KSAbstractVisitor<D, R> : KSVisitor<D, R> {
    abstract fun defaultVisit(annotated: KSNode, data: D): R

    override fun visitAnnotated(annotated: KSAnnotated, data: D): R {
        return defaultVisit(annotated, data)
    }

    override fun visitAnnotation(annotation: KSAnnotation, data: D): R {
        return defaultVisit(annotation, data)
    }

    override fun visitCallableReference(reference: KSCallableReference, data: D): R {
        return defaultVisit(reference, data)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: D): R {
        return defaultVisit(classDeclaration, data)
    }

    override fun visitClassifierReference(reference: KSClassifierReference, data: D): R {
        return defaultVisit(reference, data)
    }

    override fun visitDeclaration(declaration: KSDeclaration, data: D): R {
        return defaultVisit(declaration, data)
    }

    override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: D): R {
        return defaultVisit(declarationContainer, data)
    }

    override fun visitDynamicReference(reference: KSDynamicReference, data: D): R {
        return defaultVisit(reference, data)
    }

    override fun visitFile(file: KSFile, data: D): R {
        return defaultVisit(file, data)
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: D): R {
        return defaultVisit(function, data)
    }

    override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: D): R {
        return defaultVisit(modifierListOwner, data)
    }

    override fun visitNode(node: KSNode, data: D): R {
        return defaultVisit(node, data)
    }

    override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: D): R {
        return defaultVisit(reference, data)
    }

    override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: D): R {
        return defaultVisit(accessor, data)
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: D): R {
        return defaultVisit(property, data)
    }

    override fun visitPropertyGetter(getter: KSPropertyGetter, data: D): R {
        return defaultVisit(getter, data)
    }

    override fun visitPropertySetter(setter: KSPropertySetter, data: D): R {
        return defaultVisit(setter, data)
    }

    override fun visitReferenceElement(element: KSReferenceElement, data: D): R {
        return defaultVisit(element, data)
    }

    override fun visitTypeAlias(typeAlias: KSTypeAlias, data: D): R {
        return defaultVisit(typeAlias, data)
    }

    override fun visitTypeArgument(typeArgument: KSTypeArgument, data: D): R {
        return defaultVisit(typeArgument, data)
    }

    override fun visitTypeParameter(typeParameter: KSTypeParameter, data: D): R {
        return defaultVisit(typeParameter, data)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: D): R {
        return defaultVisit(typeReference, data)
    }

    override fun visitValueArgument(valueArgument: KSValueArgument, data: D): R {
        return defaultVisit(valueArgument, data)
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: D): R {
        return defaultVisit(valueParameter, data)
    }
}
