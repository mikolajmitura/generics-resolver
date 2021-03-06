package ru.vyarus.java.generics.resolver.inlying

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.inlying.support.BaseIface
import ru.vyarus.java.generics.resolver.inlying.support.DeclarationType
import ru.vyarus.java.generics.resolver.inlying.support.Err
import ru.vyarus.java.generics.resolver.inlying.support.NoGenericType
import ru.vyarus.java.generics.resolver.inlying.support.RootType
import ru.vyarus.java.generics.resolver.inlying.support.SubType
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 07.05.2018
 */
class InlyingGenericsResolutionTest extends Specification {

    def "Check inlying contexts resolution"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)

        when: "field context"
        def res = context.fieldType(DeclarationType.getDeclaredField("one"))
        then:
        res.generic("T") == Integer
        res.inlying
        res.rootContext().currentClass() == DeclarationType.class

        when: "field context with interface"
        res = context.fieldType(DeclarationType.getDeclaredField("two"))
        then:
        res.generic("T") == Integer
        res.rootContext().currentClass() == DeclarationType.class

        when: "method return context"
        res = context.method(DeclarationType.getMethod("ret")).returnType()
        then:
        res.generic("T") == String
        res.rootContext().currentClass() == DeclarationType.class

        when: "method param context"
        res = context.method(DeclarationType.getMethod("param", SubType.class)).parameterType(0)
        then:
        res.generic("T") == Double
        res.rootContext().currentClass() == DeclarationType.class

        when: "wrong method param position"
        context.method(DeclarationType.getMethod("param", SubType.class)).parameterType(2)
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Can't request parameter 2 of method 'void param(SubType<Double>)' (DeclarationType) because it has only 1 parameters"

        when: "wrong field"
        context.fieldType(Err.getDeclaredField("wrongField"))
        then: "err"
        ex = thrown(IllegalArgumentException)
        ex.message.replace('\r', '') == """Field 'wrongField' declaration type Err is not present in current hierarchy:
class RootType
  extends DeclarationType<Integer, String, Double>
"""
    }

    def "Check inlying type without generics"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)

        when: "field without generics"
        def res = context.fieldType(RootType.getDeclaredField("nogen"))
        then:
        res.rootContext().currentClass() == RootType.class
    }

    def "cannot find field in whole hierarchy"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)
        GenericsContext nogenFieldGenContext = context.fieldTypeByName("nogen")

        when: "try get generic context for field by name"
        nogenFieldGenContext.fieldTypeByName("someUnknownField")
        then:
        IllegalArgumentException ex = thrown()
        ex.message == "cannot find field: 'someUnknownField' in hierarchy of class: " + NoGenericType
    }

    def "GenericsContext by field by name"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)

        when: "generic context for field by name"
        GenericsContext twoFieldContext = context.fieldTypeByName("two")
        then:
        twoFieldContext.getGenericsInfo().rootClass == BaseIface
        twoFieldContext.generic(0) == Integer
    }

    def "GenericsContexts for for field with generic types"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)

        when: "generic context for field by name"
        GenericsContext someListContext = context.fieldTypeByName("someList")
        GenericsContext genericTypeOfListContext = someListContext.genericContextOf(0)
        then:
        genericTypeOfListContext.getGenericsInfo().rootClass == Map
        genericTypeOfListContext.genericContextOf(0).getGenericsInfo().rootClass == Integer
        genericTypeOfListContext.genericContextOf(1).getGenericsInfo().rootClass == Double
    }

    def "cannot get index of generic type for field"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)
        GenericsContext nogenFieldGenContext = context.fieldTypeByName("nogen")
        when:
        nogenFieldGenContext.genericContextOf(0)
        then:
        Exception ex = thrown()
        ex instanceof IndexOutOfBoundsException
    }
}
