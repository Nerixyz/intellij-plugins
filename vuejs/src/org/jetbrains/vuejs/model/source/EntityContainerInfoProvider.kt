// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model.source

import com.intellij.lang.ecmascript6.psi.ES6ImportExportDeclarationPart
import com.intellij.lang.javascript.JSStubElementTypes
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.impl.JSPsiImplUtils
import com.intellij.lang.javascript.psi.util.JSClassUtils
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.vuejs.codeInsight.getStringLiteralsFromInitializerArray
import org.jetbrains.vuejs.codeInsight.getTextIfLiteral
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

interface EntityContainerInfoProvider<T> {

  fun getInfo(initializer: JSObjectLiteralExpression?, clazz: JSClass?): T?

  abstract class DecoratedContainerInfoProvider<T>(val createInfo: (clazz: JSClass) -> T) : EntityContainerInfoProvider<T> {

    final override fun getInfo(initializer: JSObjectLiteralExpression?, clazz: JSClass?): T? =
      clazz?.let {
        val manager = CachedValuesManager.getManager(it.project)
        manager.getCachedValue(it, manager.getKeyForClass<T>(this::class.java), {
          val dependencies = mutableListOf<Any>()
          JSClassUtils.processClassesInHierarchy(it, true) { aClass, _, _ ->
            dependencies.add(aClass)
            dependencies.add(aClass.containingFile)
            true
          }
          CachedValueProvider.Result.create(createInfo(it), dependencies)
        }, false)
      }
  }

  abstract class InitializedContainerInfoProvider<T>(val createInfo: (initializer: JSObjectLiteralExpression) -> T) : EntityContainerInfoProvider<T> {

    final override fun getInfo(initializer: JSObjectLiteralExpression?, clazz: JSClass?): T? =
      initializer?.let {
        val manager = CachedValuesManager.getManager(it.project)
        manager.getCachedValue(it, manager.getKeyForClass<T>(this::class.java), {
          CachedValueProvider.Result.create(createInfo(it), PsiModificationTracker.MODIFICATION_COUNT)
        }, false)
      }

    protected abstract class InitializedContainerInfo(val declaration: JSObjectLiteralExpression) {
      private val values: MutableMap<MemberAccessor<*>, Any?> = ConcurrentHashMap()

      protected fun <T> get(accessor: MemberAccessor<T>): T {
        @Suppress("UNCHECKED_CAST")
        return values.computeIfAbsent(accessor, Function { it.build(declaration) }) as T
      }
    }

    abstract class MemberAccessor<T> {
      abstract fun build(declaration: JSObjectLiteralExpression): T
    }

    abstract class ListAccessor<T> : MemberAccessor<List<T>>()

    abstract class MapAccessor<T> : MemberAccessor<Map<String, T>>()

    class SimpleMemberAccessor<T>(private val memberReader: MemberReader,
                                  private val provider: (String, JSElement) -> T)
      : ListAccessor<T>() {

      override fun build(declaration: JSObjectLiteralExpression): List<T> {
        return memberReader.readMembers(declaration).map { (name, element) -> provider(name, element) }
      }
    }


    class SimpleMemberMapAccessor<T>(private val memberReader: MemberReader,
                                     private val provider: (String, JSElement) -> T) : MapAccessor<T>() {

      override fun build(declaration: JSObjectLiteralExpression): Map<String, T> {
        return memberReader.readMembers(declaration)
          .asSequence()
          .map {
            Pair(it.first, provider(it.first, (VueComponents.meaningfulExpression(it.second) ?: it.second) as JSElement))
          }
          .distinctBy { it.first }
          .toMap(TreeMap())
      }

    }

    open class MemberReader(private val propertyName: String,
                            private val canBeArray: Boolean = false,
                            private val canBeObject: Boolean = true) {
      fun readMembers(descriptor: JSObjectLiteralExpression): List<Pair<String, JSElement>> {
        val property = descriptor.findProperty(propertyName) ?: return emptyList()

        var propsObject = property.objectLiteralExpressionInitializer ?: getObjectLiteral(property)
        val initializerReference = JSPsiImplUtils.getInitializerReference(property)
        if (propsObject == null && initializerReference != null) {
          var resolved = JSStubBasedPsiTreeUtil.resolveLocally(initializerReference, property)
          if (resolved is ES6ImportExportDeclarationPart) {
            resolved = VueComponents.meaningfulExpression(resolved)
          }
          if (resolved is JSObjectLiteralExpression) {
            propsObject = resolved
          }
          else if (resolved != null) {
            propsObject = JSStubBasedPsiTreeUtil.findDescendants(resolved, JSStubElementTypes.OBJECT_LITERAL_EXPRESSION)
                            .find { it.context == resolved } ?: getObjectLiteralFromResolved(resolved)
            if ((propsObject == null && canBeArray) || !canBeObject) {
              return readPropsFromArray(resolved)
            }
          }
        }
        if (propsObject != null && canBeObject) {
          return filteredObjectProperties(propsObject)
        }
        return if (canBeArray) readPropsFromArray(property) else return emptyList()
      }

      protected open fun getObjectLiteral(property: JSProperty): JSObjectLiteralExpression? = null
      protected open fun getObjectLiteralFromResolved(resolved: PsiElement): JSObjectLiteralExpression? = null

      private fun filteredObjectProperties(propsObject: JSObjectLiteralExpression) =
        propsObject.properties.filter { it.name != null }.map { Pair(it.name!!, it) }

      private fun readPropsFromArray(holder: PsiElement): List<Pair<String, JSElement>> =
        getStringLiteralsFromInitializerArray(holder)
          .map { Pair(getTextIfLiteral(it) ?: "", it) }

      companion object {
        private fun findReturnedObjectLiteral(resolved: PsiElement): JSObjectLiteralExpression? {
          if (resolved !is JSFunction) return null
          return JSStubBasedPsiTreeUtil.findDescendants<JSObjectLiteralExpression>(
            resolved, TokenSet.create(
            JSStubElementTypes.OBJECT_LITERAL_EXPRESSION))
            .find {
              it.context == resolved ||
              it.context is JSParenthesizedExpression && it.context?.context == resolved ||
              it.context is JSReturnStatement
            }
        }
      }
    }

  }

}