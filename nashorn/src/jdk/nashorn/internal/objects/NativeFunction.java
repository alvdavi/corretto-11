/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.nashorn.internal.objects;

import static jdk.nashorn.internal.runtime.ECMAErrors.typeError;
import static jdk.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.util.List;
import jdk.nashorn.internal.objects.annotations.Attribute;
import jdk.nashorn.internal.objects.annotations.Constructor;
import jdk.nashorn.internal.objects.annotations.Function;
import jdk.nashorn.internal.objects.annotations.ScriptClass;
import jdk.nashorn.internal.runtime.JSType;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;

/**
 * ECMA 15.3 Function Objects
 *
 * Note: instances of this class are never created. This class is not even a
 * subclass of ScriptObject. But, we use this class to generate prototype and
 * constructor for "Function".
 */
@ScriptClass("Function")
public final class NativeFunction {
    // do *not* create me!
    private NativeFunction() {
    }

    /**
     * ECMA 15.3.4.2 Function.prototype.toString ( )
     *
     * @param self self reference
     * @return string representation of Function
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object toString(final Object self) {
        if (!(self instanceof ScriptFunction)) {
            throw typeError("not.a.function", ScriptRuntime.safeToString(self));
        }
        return ((ScriptFunction)self).toSource();
    }

    /**
     * ECMA 15.3.4.3 Function.prototype.apply (thisArg, argArray)
     *
     * @param self   self reference
     * @param thiz   {@code this} arg for apply
     * @param array  array of argument for apply
     * @return result of apply
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object apply(final Object self, final Object thiz, final Object array) {
        if (!(self instanceof ScriptFunction)) {
            throw typeError("not.a.function", ScriptRuntime.safeToString(self));
        }

        Object[] args = null;

        if (ScriptObject.isArray(array)) {
            args = ((NativeArray)array).asObjectArray();
        } else if (array instanceof ScriptObject) {
            // look for array-like object
            final ScriptObject sobj = (ScriptObject)array;
            final Object       len  = sobj.getLength();

            if (len == UNDEFINED || len == null) {
                throw typeError("function.apply.expects.array");
            }

            final int n = (int)JSType.toUint32(len);
            if (n != JSType.toNumber(len)) {
                throw typeError("function.apply.expects.array");
            }

            args = new Object[(int)JSType.toUint32(len)];
            for (int i = 0; i < args.length; i++) {
                args[i] = sobj.get(i);
            }
        } else if (array instanceof Object[]) {
            args = (Object[])array;
        } else if (array instanceof List) {
            final List<?> list = (List<?>)array;
            list.toArray(args = new Object[list.size()]);
        } else if (array == null || array == UNDEFINED) {
            args = ScriptRuntime.EMPTY_ARRAY;
        } else {
            throw typeError("function.apply.expects.array");
        }

        return ScriptRuntime.apply((ScriptFunction)self, thiz, args);
    }

    /**
     * ECMA 15.3.4.4 Function.prototype.call (thisArg [ , arg1 [ , arg2, ... ] ] )
     *
     * @param self self reference
     * @param args arguments for call
     * @return result of call
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static Object call(final Object self, final Object... args) {
        if (!(self instanceof ScriptFunction)) {
            throw typeError("not.a.function", ScriptRuntime.safeToString(self));
        }

        Object thiz = (args.length == 0) ? UNDEFINED : args[0];
        Object[] arguments;

        if (args.length > 1) {
            arguments = new Object[args.length - 1];
            System.arraycopy(args, 1, arguments, 0, arguments.length);
        } else {
            arguments = ScriptRuntime.EMPTY_ARRAY;
        }

        return ScriptRuntime.apply((ScriptFunction)self, thiz, arguments);
    }

    /**
     * ECMA 15.3.4.5 Function.prototype.bind (thisArg [, arg1 [, arg2, ...]])
     *
     * @param self self reference
     * @param args arguments for bind
     * @return function with bound arguments
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE, arity = 1)
    public static Object bind(final Object self, final Object... args) {
        if (!(self instanceof ScriptFunction)) {
            throw typeError("not.a.function", ScriptRuntime.safeToString(self));
        }

        final Object thiz = (args.length == 0) ? UNDEFINED : args[0];

        Object[] arguments;
        if (args.length > 1) {
            arguments = new Object[args.length - 1];
            System.arraycopy(args, 1, arguments, 0, arguments.length);
        } else {
            arguments = ScriptRuntime.EMPTY_ARRAY;
        }

        return ((ScriptFunctionImpl)self).makeBoundFunction(thiz, arguments);
    }

    /**
     * Nashorn extension: Function.prototype.toSource
     *
     * @param self self reference
     * @return source for function
     */
    @Function(attributes = Attribute.NOT_ENUMERABLE)
    public static Object toSource(final Object self) {
        if (!(self instanceof ScriptFunction)) {
            throw typeError("not.a.function", ScriptRuntime.safeToString(self));
        }
        return ((ScriptFunction)self).toSource();
    }

    /**
     * ECMA 15.3.2.1 new Function (p1, p2, ... , pn, body)
     *
     * Constructor
     *
     * @param newObj is the new operator used for constructing this function
     * @param self   self reference
     * @param args   arguments
     * @return new NativeFunction
     */
    @Constructor(arity = 1)
    public static Object function(final boolean newObj, final Object self, final Object... args) {
        final StringBuilder sb = new StringBuilder();

        sb.append("(function (");
        if (args.length > 0) {
            for (int i = 0; i < args.length - 1; i++) {
                sb.append(JSType.toString(args[i]));
                if (i < args.length - 2) {
                    sb.append(",");
                }
            }
        }
        sb.append(") {\n");
        if (args.length > 0) {
            sb.append(JSType.toString(args[args.length - 1]));
            sb.append('\n');
        }
        sb.append("})");

        final Global global = Global.instance();

        return Global.directEval(global, sb.toString(), global, "<function>", Global.isStrict());
    }
}