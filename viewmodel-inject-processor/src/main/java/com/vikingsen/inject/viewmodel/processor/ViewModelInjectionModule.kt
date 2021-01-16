package com.vikingsen.inject.viewmodel.processor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.vikingsen.inject.viewmodel.processor.square.applyEach
import com.vikingsen.inject.viewmodel.processor.square.assistedInjectFactoryName
import com.vikingsen.inject.viewmodel.processor.square.peerClassWithReflectionNesting
import javax.lang.model.element.Modifier.*

private val MODULE = ClassName.get("dagger", "Module")
private val BINDS = ClassName.get("dagger", "Binds")
private val INTO_MAP = ClassName.get("dagger.multibindings", "IntoMap")
private val CLASS_KEY = ClassName.get("dagger.multibindings", "ClassKey")
private val ABSTRACT_FACTORY =
    ClassName.get("com.vikingsen.inject.viewmodel", "AbstractViewModelFactory")


data class ViewModelInjectionModule(
    val moduleName: ClassName,
    val public: Boolean,
    val injectedNames: List<ClassName>,
    /** An optional `@Generated` annotation marker. */
    val generatedAnnotation: AnnotationSpec? = null
) {
    val generatedType = moduleName.viewModelInjectModuleName()

    fun brewJava(): TypeSpec {
        return TypeSpec.classBuilder(generatedType)
            .addAnnotation(MODULE)
            .apply {
                if (generatedAnnotation != null) {
                    addAnnotation(generatedAnnotation)
                }
            }
            .addModifiers(ABSTRACT)
            .apply {
                if (public) {
                    addModifiers(PUBLIC)
                }
            }
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PRIVATE)
                    .build()
            )
            .applyEach(injectedNames) { injectedName ->
                addMethod(
                    MethodSpec.methodBuilder(injectedName.bindMethodName())
                        .addAnnotation(BINDS)
                        .addAnnotation(INTO_MAP)
                        .addAnnotation(
                            AnnotationSpec.builder(CLASS_KEY)
                                .addMember("value", "\$T.class", injectedName)
                                .build()
                        )
                        .addModifiers(ABSTRACT)
                        .returns(ABSTRACT_FACTORY)
                        .addParameter(injectedName.assistedInjectFactoryName(), "factory")
                        .build()
                )
            }
            .build()
    }
}

private fun ClassName.bindMethodName() = "bind_" + reflectionName().replace('.', '_')

internal fun ClassName.viewModelInjectModuleName() =
    peerClassWithReflectionNesting("""ViewModelInject_${simpleName()}""")
