package com.groupwork.mahjong.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SortedArrayList<E extends Comparable<E>> extends ArrayList<E> {
    public SortedArrayList() {
        super();
    }

    public SortedArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public SortedArrayList(Collection<? extends E> collection) {
        super(collection);
    }

    @Override
    public boolean add(E e) {
        boolean result = super.add(e);
        if (result) Collections.sort(this);
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = super.addAll(c);
        if (result) Collections.sort(this);
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = super.remove(o);
        if (result) Collections.sort(this);
        return result;
    }
}
