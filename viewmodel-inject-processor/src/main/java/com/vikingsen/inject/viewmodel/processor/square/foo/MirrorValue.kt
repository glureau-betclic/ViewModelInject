package com.vikingsen.inject.viewmodel.processor.square.foo

import javax.lang.model.type.TypeMirror

sealed class MirrorValue {
    data class Type(private val value: TypeMirror) : MirrorValue(), TypeMirror by value
    data class Array(private val value: List<MirrorValue>) : MirrorValue(),
        List<MirrorValue> by value

    object Unmapped : MirrorValue()
    object Error : MirrorValue()
}