package com.vikingsen.inject.viewmodel.processor.square

import com.squareup.javapoet.*
import com.vikingsen.inject.viewmodel.processor.square.foo.MirrorValue
import dagger.assisted.Assisted
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.*
import javax.lang.model.type.ErrorType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import javax.lang.model.util.SimpleTypeVisitor6

// From SquareAssistedInject: https://github.com/square/AssistedInject/blob/7912bd7f1e8e53c3e05307a8e65d3199f6ec96c1/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/internal/javaPoet.kt

/**
 * Like [ClassName.peerClass] except instead of honoring the enclosing class names they are
 * concatenated with `$` similar to the reflection name. `foo.Bar.Baz` invoking this function with
 * `Fuzz` will produce `foo.Baz$Fuzz`.
 */
fun ClassName.peerClassWithReflectionNesting(name: String): ClassName {
    var prefix = ""
    var peek = this
    while (true) {
        peek = peek.enclosingClassName() ?: break
        prefix = peek.simpleName() + "$" + prefix
    }
    return ClassName.get(packageName(), prefix + name)
}

// TODO https://github.com/square/javapoet/issues/671
fun TypeName.rawClassName(): ClassName = when (this) {
    is ClassName -> this
    is ParameterizedTypeName -> rawType
    else -> throw IllegalStateException("Cannot extract raw class name from $this")
}

// From SquareAssistedInject: https://github.com/square/AssistedInject/blob/7912bd7f1e8e53c3e05307a8e65d3199f6ec96c1/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/internal/kotlinStdlib.kt
inline fun <T : Any, I> T.applyEach(items: Iterable<I>, func: T.(I) -> Unit): T {
    items.forEach { item -> func(item) }
    return this
}

//https://github.com/square/AssistedInject/blob/7912bd7f1e8e53c3e05307a8e65d3199f6ec96c1/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/internal/javaPoet.kt
fun TypeElement.toClassName(): ClassName = ClassName.get(this)
fun TypeMirror.toTypeName(): TypeName = TypeName.get(this)
fun Iterable<CodeBlock>.joinToCode(separator: String = ", ") = CodeBlock.join(this, separator)

// https://github.com/square/AssistedInject/blob/7912bd7f1e8e53c3e05307a8e65d3199f6ec96c1/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/internal/kotlinStdlib.kt
// TODO https://youtrack.jetbrains.com/issue/KT-4734
fun <K, V : Any> Map<K, V?>.filterNotNullValues(): Map<K, V> {
    @Suppress("UNCHECKED_CAST")
    return filterValues { it != null } as Map<K, V>
}

/** Equivalent to `this as T` for use in function chains. */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any.cast(): T = this as T

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Iterable<*>.castEach() = map { it as T }


// https://github.com/square/AssistedInject/blob/7912bd7f1e8e53c3e05307a8e65d3199f6ec96c1/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/internal/annotationProcessing.kt

fun AnnotationMirror.getValue(property: String, elements: Elements) = elements
    .getElementValuesWithDefaults(this)
    .entries
    .firstOrNull { it.key.simpleName.contentEquals(property) }
    ?.value
    ?.toMirrorValue()

fun AnnotationValue.toMirrorValue(): MirrorValue = accept(MirrorValueVisitor, null)

private object MirrorValueVisitor : SimpleAnnotationValueVisitor6<MirrorValue, Nothing?>() {
    override fun defaultAction(o: Any, ignored: Nothing?) = MirrorValue.Unmapped

    override fun visitType(mirror: TypeMirror, ignored: Nothing?) = mirror.accept(TypeVisitor, null)

    override fun visitArray(values: List<AnnotationValue>, ignored: Nothing?) =
        MirrorValue.Array(values.map { it.accept(this, null) })
}

private object TypeVisitor : SimpleTypeVisitor6<MirrorValue, Nothing?>() {
    override fun visitError(type: ErrorType, ignored: Nothing?) = MirrorValue.Error
    override fun defaultAction(type: TypeMirror, ignored: Nothing?) = MirrorValue.Type(type)
}

/** Return a list of elements annotated with `T`. */
inline fun <reified T : Annotation> RoundEnvironment.findElementsAnnotatedWith(): Set<Element> =
    getElementsAnnotatedWith(T::class.java)


/** Return true if this [AnnotatedConstruct] is annotated with `T`. */
inline fun <reified T : Annotation> AnnotatedConstruct.hasAnnotation() =
    getAnnotation(T::class.java) != null


/** Return true if this [AnnotatedConstruct] is annotated with `qualifiedName`. */
fun AnnotatedConstruct.hasAnnotation(qualifiedName: String) = getAnnotation(qualifiedName) != null

/** Return the first annotation matching [qualifiedName] or null. */
fun AnnotatedConstruct.getAnnotation(qualifiedName: String) = annotationMirrors
    .firstOrNull {
        it.annotationType.asElement().cast<TypeElement>().qualifiedName.contentEquals(qualifiedName)
    }
