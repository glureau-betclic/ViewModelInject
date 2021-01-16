package com.vikingsen.inject.viewmodel.processor.square

import com.squareup.javapoet.*
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

sealed class MirrorValue {
    data class Type(private val value: TypeMirror) : MirrorValue(), TypeMirror by value
    data class Array(private val value: List<MirrorValue>) : MirrorValue(),
        List<MirrorValue> by value

    object Unmapped : MirrorValue()
    object Error : MirrorValue()
}

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

//https://github.com/square/AssistedInject/blob/418981b158c74e5d9d783f6bb1f386259d0d42a1/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/DependencyRequest.kt

/** Associates a [Key] with its desired use as assisted or not. */
data class DependencyRequest(
    val namedKey: NamedKey,
    /** True when fulfilled by the caller. Otherwise fulfilled by a JSR 330 provider. */
    val isAssisted: Boolean
) {
    val key get() = namedKey.key
    val name get() = namedKey.name

    override fun toString() = (if (isAssisted) "@Assisted " else "") + "$key $name"
}

fun VariableElement.asDependencyRequest() =
    DependencyRequest(asNamedKey(), hasAnnotation<Assisted>())

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

private val namedKeyComparator = compareBy<NamedKey>({ it.key }, { it.name })

// https://github.com/square/AssistedInject/blob/0.5.2/assisted-inject-processor/src/main/java/com/squareup/inject/assisted/processor/NamedKey.kt

/** Represents a [Key] associated with a name. */
data class NamedKey(
    val key: Key,
    val name: String
) : Comparable<NamedKey> {
    override fun toString() = "$key $name"
    override fun compareTo(other: NamedKey) = namedKeyComparator.compare(this, other)
}

/** Create a [NamedKey] from this type, any qualifier annotation, and the name. */
fun VariableElement.asNamedKey(mirror: TypeMirror = asType()) =
    NamedKey(asKey(mirror), simpleName.toString())