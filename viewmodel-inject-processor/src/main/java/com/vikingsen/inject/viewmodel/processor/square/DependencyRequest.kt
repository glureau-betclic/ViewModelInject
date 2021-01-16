package com.vikingsen.inject.viewmodel.processor.square

import dagger.assisted.Assisted
import javax.lang.model.element.VariableElement

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