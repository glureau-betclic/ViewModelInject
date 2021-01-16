package com.vikingsen.inject.viewmodel.processor.square

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror


//https://github.com/square/AssistedInject/blob/418981b158c74e5d9d783f6bb1f386259d0d42a1/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/Key.kt

private val keyComparator = compareBy<Key>({ it.type.toString() }, { it.qualifier == null })

/** Represents a type and an optional qualifier annotation for a binding. */
data class Key(
    val type: TypeName,
    val qualifier: AnnotationSpec? = null
) : Comparable<Key> {
    override fun toString() = qualifier?.let { "$it $type" } ?: type.toString()
    override fun compareTo(other: Key) = keyComparator.compare(this, other)
}

/** Create a [Key] from this type and any qualifier annotation. */
fun VariableElement.asKey(mirror: TypeMirror = asType()) = Key(mirror.toTypeName(),
    annotationMirrors.find {
        it.annotationType.asElement().hasAnnotation("javax.inject.Qualifier")
    }?.let { AnnotationSpec.get(it) })
