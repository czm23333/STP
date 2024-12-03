package org.softstar.stp.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class CircularArray<T> implements List<T> {
    private final int capacity;
    private T[] storage;
    private int size;
    private int head = 0;

    public CircularArray(int capacity) {
        this(capacity, 0);
    }

    public CircularArray(int capacity, int size) {
        this.capacity = capacity;
        this.size = size;
        this.storage = (T[]) new Object[capacity];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return new CircularArrayIterator();
    }

    @Override
    public @NotNull Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull <T1> T1[] toArray(@NotNull T1[] t1s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T t) {
        if (size == capacity) throw new IndexOutOfBoundsException();
        storage[(head + (size++)) % capacity] = t;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> collection) {
        collection.forEach(this::add);
        return true;
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends T> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        size = 0;
    }

    @Override
    public T get(int i) {
        if (i >= size) throw new IndexOutOfBoundsException();
        return storage[(head + i) % capacity];
    }

    @Override
    public T set(int i, T t) {
        if (i >= size) throw new IndexOutOfBoundsException();
        var old = get(i);
        storage[(head + i) % capacity] = t;
        return old;
    }

    @Override
    public void add(int i, T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ListIterator<T> listIterator(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull List<T> subList(int i, int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T removeFirst() {
        if (size == 0) throw new IndexOutOfBoundsException();
        var old = storage[head];
        head = (head + 1) % capacity;
        --size;
        return old;
    }

    @Override
    public T removeLast() {
        if (size == 0) throw new IndexOutOfBoundsException();
        return storage[(head + (--size)) % capacity];
    }

    private class CircularArrayIterator implements Iterator<T> {
        private int current = 0;

        @Override
        public boolean hasNext() {
            return current < size;
        }

        @Override
        public T next() {
            return get(current++);
        }
    }
}
