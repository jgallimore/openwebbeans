/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.util;

import java.lang.reflect.TypeVariable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.test.util.differentPackage.MyOtherPackageSubClass;
import org.junit.Test;

import org.junit.Assert;

public class ClassUtilTest {

    @Test
    public void testGetAllNonPrivateMethods()
    {
        List<Method> nonPrivateMethods = ClassUtil.getNonPrivateMethods(SpecificClass.class, false);
        nonPrivateMethods.removeAll(Arrays.asList(Object.class.getDeclaredMethods()));

        // getDeclaredMethods also contains the bridge method, so it's really only 1
        Assert.assertEquals(1, nonPrivateMethods.size());
    }

    @Test
    public void testGetAllNonPrivateMethods_packagePrivate()
    {
        List<Method> nonPrivateMethods = ClassUtil.getNonPrivateMethods(MyOtherPackageSubClass.class, false);

        for (Method m : nonPrivateMethods)
        {
            if (MySuperClass.class.equals(m.getDeclaringClass()) &&
                "packageMethod".equals(m.getName()))
            {
                Assert.fail("invisible package private method must not get listed");
            }
        }
    }

    @Test
    public void testIsOverridden() throws Exception
    {
        Assert.assertTrue(isOverridden(MySubClass.class, "publicMethod"));
        Assert.assertTrue(isOverridden(MySubClass.class, "protectedMethod"));

        Assert.assertTrue(isOverridden(MySubClass.class, "packageMethod"));
        Assert.assertFalse(isOverridden(MyOtherPackageSubClass.class, "packageMethod"));

        Assert.assertFalse(isOverridden(MySubClass.class, "privateMethod"));
        Assert.assertFalse(isOverridden(MyOtherPackageSubClass.class, "privateMethod"));
    }

    @Test
    public void testGetAbstractMethods() throws Exception
    {
        List<Method> methods = ClassUtil.getAbstractMethods(AbstractClassWithAbstractMethod.class);
        Assert.assertNotNull(methods);
        Assert.assertEquals(2, methods.size());

        for (Method method : methods)
        {
            String name = method.getName();
            if (!name.equals("getPublicString") && !name.equals("getProtectedInt"))
            {
                Assert.fail("unexpected Method " + name);
            }
        }

        Assert.assertTrue("returned methods must be empty", ClassUtil.getAbstractMethods(MySubClass.class).isEmpty());
    }

    @Test
    public void testIsMethodDeclared() throws Exception
    {
        Class<AbstractClassWithAbstractMethod> clazz = AbstractClassWithAbstractMethod.class;

        Assert.assertTrue(ClassUtil.isMethodDeclared(clazz, "doSomething"));
        Assert.assertFalse(ClassUtil.isMethodDeclared(clazz, "notExistingMethod"));
    }

    private boolean isOverridden(Class subClass, String methodName) throws Exception
    {
        Method superClassMethod = MySuperClass.class.getDeclaredMethod(methodName);
        Method subClassMethod   = subClass.getDeclaredMethod(methodName);

        return ClassUtil.isOverridden(subClassMethod, superClassMethod);
    }

    @Test
    public void testSatisfiesTypeVariable_unbounded()
    {
        TypeVariable<?> t = Unbounded.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, String.class));
        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, Number.class));
    }

    @Test
    public void testSatisfiesTypeVariable_singleClassBound()
    {
        TypeVariable<?> t = NumberBound.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, Integer.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, String.class));
    }

    @Test
    public void testSatisfiesTypeVariable_singleInterfaceBound()
    {
        TypeVariable<?> t = ComparableBound.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, String.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, Number.class));
    }

    @Test
    public void testSatisfiesTypeVariable_multipleBounds()
    {
        TypeVariable<?> t = MultiBound.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, Integer.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, Number.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, String.class));
    }

    @Test
    public void testSatisfiesTypeVariable_parameterizedBound_usesErasure()
    {
        TypeVariable<?> t = ListBound.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, java.util.ArrayList.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, java.util.HashSet.class));
    }

    @Test
    public void testSatisfiesTypeVariable_wildcardInBound_usesUpperBoundErasure()
    {
        TypeVariable<?> t = ComparableWildcardBound.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, NumberComparable.class));
        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, String.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, Number.class));
    }

    @Test
    public void testSatisfiesTypeVariable_recursiveEnumBound()
    {
        TypeVariable<?> t = EnumBound.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, MyEnum.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, String.class));
    }

    @Test
    public void testSatisfiesTypeVariable_specificSubclassRequired()
    {
        TypeVariable<?> t = ArrayListBound.class.getTypeParameters()[0];

        Assert.assertTrue(ClassUtil.satisfiesTypeVariable(t, java.util.ArrayList.class));
        Assert.assertFalse(ClassUtil.satisfiesTypeVariable(t, java.util.List.class));
    }

}

class Unbounded<T>
{
}

class NumberBound<T extends Number>
{
}

class ComparableBound<T extends Comparable<T>>
{
}

class MultiBound<T extends Number & Comparable<T>>
{
}

class ListBound<T extends java.util.List<String>>
{
}

class ComparableWildcardBound<T extends Comparable<? super Number>>
{
}

class EnumBound<T extends Enum<T>>
{
}

enum MyEnum
{
    A, B
}

class NumberComparable implements Comparable<Number>
{

    @Override
    public int compareTo(Number o)
    {
        return 0;
    }
}

class ArrayListBound<T extends java.util.ArrayList<String>>
{
}
