// GenericsNote: Converted.
/*
 *  Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections15.set;

import java.util.Comparator;
import java.util.SortedSet;

import org.apache.commons.collections15.Predicate;

/**
 * Decorates another <code>SortedSet</code> to validate that all additions
 * match a specified predicate.
 * <p/>
 * This set exists to provide validation for the decorated set.
 * It is normally created to decorate an empty set.
 * If an object cannot be added to the set, an IllegalArgumentException is thrown.
 * <p/>
 * One usage would be to ensure that no null entries are added to the set.
 * <pre>SortedSet set = PredicatedSortedSet.decorate(new TreeSet(), NotNullPredicate.INSTANCE);</pre>
 * <p/>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @author Stephen Colebourne
 * @author Matt Hall, John Watkinson, Paul Jack
 * @version $Revision: 1.1 $ $Date: 2005/10/11 17:05:39 $
 * @since Commons Collections 3.0
 */
public class PredicatedSortedSet <E> extends PredicatedSet<E> implements SortedSet<E> {

    /**
     * Serialization version
     */
    private static final long serialVersionUID = -9110948148132275052L;

    /**
     * Factory method to create a predicated (validating) sorted set.
     * <p/>
     * If there are any elements already in the set being decorated, they
     * are validated.
     *
     * @param set       the set to decorate, must not be null
     * @param predicate the predicate to use for validation, must not be null
     * @throws IllegalArgumentException if set or predicate is null
     * @throws IllegalArgumentException if the set contains invalid elements
     */
    public static <E> SortedSet<E> decorate(SortedSet<E> set, Predicate<? super E> predicate) {
        return new PredicatedSortedSet<E>(set, predicate);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * <p/>
     * If there are any elements already in the set being decorated, they
     * are validated.
     *
     * @param set       the set to decorate, must not be null
     * @param predicate the predicate to use for validation, must not be null
     * @throws IllegalArgumentException if set or predicate is null
     * @throws IllegalArgumentException if the set contains invalid elements
     */
    protected PredicatedSortedSet(SortedSet<E> set, Predicate<? super E> predicate) {
        super(set, predicate);
    }

    /**
     * Gets the sorted set being decorated.
     *
     * @return the decorated sorted set
     */
    private SortedSet<E> getSortedSet() {
        return (SortedSet<E>) getCollection();
    }

    //-----------------------------------------------------------------------
    public SortedSet<E> subSet(E fromElement, E toElement) {
        SortedSet sub = getSortedSet().subSet(fromElement, toElement);
        return new PredicatedSortedSet<E>(sub, predicate);
    }

    public SortedSet<E> headSet(E toElement) {
        SortedSet sub = getSortedSet().headSet(toElement);
        return new PredicatedSortedSet<E>(sub, predicate);
    }

    public SortedSet<E> tailSet(E fromElement) {
        SortedSet sub = getSortedSet().tailSet(fromElement);
        return new PredicatedSortedSet<E>(sub, predicate);
    }

    public E first() {
        return getSortedSet().first();
    }

    public E last() {
        return getSortedSet().last();
    }

    public Comparator<? super E> comparator() {
        return getSortedSet().comparator();
    }

}