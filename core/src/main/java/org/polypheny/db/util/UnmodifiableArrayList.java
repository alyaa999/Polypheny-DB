/*
 * Copyright 2019-2024 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.util;


import java.util.AbstractList;
import java.util.Objects;
import java.util.RandomAccess;


/**
 * A view onto an array that cannot be modified by the client.
 * <p>
 * Since the array is not copied, modifications to the array will be reflected in the list.
 * <p>
 * Null elements are allowed.
 * <p>
 * Quick and low-memory, like {@link java.util.Arrays#asList(Object[])}, but unmodifiable.
 *
 * @param <E> Element type
 */
public class UnmodifiableArrayList<E> extends AbstractList<E> implements RandomAccess {

    private final E[] elements;


    private UnmodifiableArrayList( E[] elements ) {
        this.elements = Objects.requireNonNull( elements );
    }


    @SafeVarargs
    public static <E> UnmodifiableArrayList<E> of( E... elements ) {
        return new UnmodifiableArrayList<>( elements );
    }


    @Override
    public E get( int index ) {
        return elements[index];
    }


    @Override
    public int size() {
        return elements.length;
    }

}
