package com.giladcourse.map;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.giladcourse.ConcurrentMapWithTimedEviction;
import com.giladcourse.EvictionScheduler;


public class ConcurrentMapWithTimedEvictionDecorator<K, V> extends AbstractMap<K, V> implements ConcurrentMapWithTimedEviction<K, V> {

    private final ConcurrentMap<K, EvictibleEntry<K, V>> delegate;

    private final EvictionScheduler<K, V> scheduler;

    private final transient EntrySet entrySet;


    public ConcurrentMapWithTimedEvictionDecorator(ConcurrentMap<K, EvictibleEntry<K, V>> delegate, EvictionScheduler<K, V> scheduler) {
        super();

        if (delegate == null || scheduler == null) {
            throw new NullPointerException("Delegate to be used cannot be null");
        }

        this.delegate = delegate;
        this.scheduler = scheduler;
        this.entrySet = new EntrySet();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }


    @Override
    public boolean containsKey(Object key) {
        EvictibleEntry<K, V> e = this.delegate.get(key);
        return (e != null) && !evictIfExpired(e);
    }


    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException("Value to be checked for contains cannot be null");
        }

        for (EvictibleEntry<K, V> e : delegate.values()) {
            if (e.getValue().equals(value)) {
                if (evictIfExpired(e)) {
                    continue;
                }

                return true;
            }
        }

        return false;
    }


    @Override
    public V get(Object key) {
        EvictibleEntry<K, V> e = this.delegate.get(key);
        return ((e == null) || evictIfExpired(e)) ? null : e.getValue();
    }


    @Override
    public V put(K key, V value) {
        return put(key, value, 0);
    }


    @Override
    public V put(K key, V value, long evictMs) {
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, value, evictMs);
        EvictibleEntry<K, V> oe = this.delegate.put(key, e);
        if (oe != null) {
            // An entry is being removed, cancel its automatic eviction
            cancelEviction(oe);
        }

        scheduleEviction(e);
        return ((oe == null) || oe.shouldEvict()) ? null : oe.getValue();
    }


    @Override
    public V putIfAbsent(K key, V value) {
        return putIfAbsent(key, value, 0);
    }


    @Override
    public V putIfAbsent(K key, V value, long evictMs) {
        while (true) {
            EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, value, evictMs);
            EvictibleEntry<K, V> oe = this.delegate.putIfAbsent(key, e);
            if (oe == null) {
                // An entry is being added, schedule its automatic eviction
                scheduleEviction(e);
                return null;
            }

            if (evictIfExpired(oe)) {
                continue;
            }

            return oe.getValue();
        }
    }


    @Override
    public V remove(Object key) {
        EvictibleEntry<K, V> oe = this.delegate.remove(key);
        if (oe != null) {
            // An entry is being removed, cancel its automatic eviction
            cancelEviction(oe);
        }
        return ((oe == null) || oe.shouldEvict()) ? null : oe.getValue();
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (value == null) {
            throw new NullPointerException("Value to be checked to cannot be null");
        }

        EvictibleEntry<K, V> oe = this.delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oe.getValue().equals(value)) {
            return false;
        }

        boolean removed = this.delegate.remove(key, oe);
        // An entry is being removed, cancel its automatic eviction
        cancelEviction(oe);
        return removed;
    }

    @Override
    public V replace(K key, V value) {
        return replace(key, value, 0);
    }

    @Override
    public V replace(K key, V value, long evictMs) {
        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = this.delegate.get(key);
        if ((oe == null) || evictIfExpired(oe)) {
            return null;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, value, evictMs);
        oe = this.delegate.replace(key, e);
        if (oe != null) {
            // An entry is being replaced, cancel the automatic eviction of the
            // old entry
            // and schedule it for the new entry
            cancelEviction(oe);
            scheduleEviction(e);
        }

        return (oe != null) ? oe.getValue() : null;
    }


    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return replace(key, oldValue, newValue, 0);
    }


    @Override
    public boolean replace(K key, V oldValue, V newValue, long evictMs) {
        if (oldValue == null) {
            throw new NullPointerException("Old value cannot be nul");
        }

        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oldValue.equals(oe.getValue())) {
            return false;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, newValue, evictMs);
        boolean replaced = delegate.replace(key, oe, e);
        if (replaced) {
            // An entry is being replaced, cancel the automatic eviction of the
            // old entry
            // and schedule it for the new entry
            cancelEviction(oe);
            scheduleEviction(e);
        }

        return replaced;
    }


    @Override
    public void clear() {
        cancelAllEvictions();
        this.delegate.clear();
    }


    @Override
    public Set<K> keySet() {
        return this.delegate.keySet();
    }


    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.entrySet;
    }

    private boolean evictIfExpired(EvictibleEntry<K, V> e) {
        return evictIfExpired(e, true);
    }

    /*
     * Removes the entry from the map if it has already expired, and optionally
     * cancels its automatic eviction.
     */
    private boolean evictIfExpired(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
        boolean result = e.shouldEvict();
        if (result) {
            evict(e, cancelPendingEviction);
        }

        return result;
    }

    /*
     * Removes the entry from the map and optionally cancels its automatic
     * eviction.
     */
    void evict(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
        this.delegate.remove(e.getKey(), e);

        if (cancelPendingEviction) {
            cancelEviction(e);
        }
    }

    /*
     * Schedules the automatic eviction for the entry. This method is invoked on
     * new entries that have just been added to the map.
     */
    private void scheduleEviction(EvictibleEntry<K, V> e) {
        this.scheduler.scheduleEviction(e);
    }

    /*
     * Cancels the automatic eviction for the entry. This method is invoked on
     * old entries that have just been removed from the map.
     */
    private void cancelEviction(EvictibleEntry<K, V> e) {
        this.scheduler.cancelEviction(e);
    }

    /*
     * Cancels all pending evictions. This method is invoked when the map is
     * cleared.
     */
    private void cancelAllEvictions() {
        for (EvictibleEntry<K, V> e : delegate.values()) {
            scheduler.cancelEviction(e);
        }
    }

    /*
     * An entry set view on this map.
     */
    private final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?>)) {
                return false;
            }

            Entry<K, V> e = (Entry<K, V>) o;
            V value = get(e.getKey());
            return (value != null) && value.equals(e.getValue());
        }

        @Override
        public boolean add(Entry<K, V> entry) {
            return (putIfAbsent(entry.getKey(), entry.getValue()) == null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry<?, ?>)) {
                return false;
            }

            Entry<K, V> e = (Entry<K, V>) o;
            return ConcurrentMapWithTimedEvictionDecorator.this.remove(e.getKey(), e.getValue());
        }

        @Override
        public int size() {
            return ConcurrentMapWithTimedEvictionDecorator.this.size();
        }

        @Override
        public boolean isEmpty() {
            return (size() == 0);
        }

        @Override
        public void clear() {
            ConcurrentMapWithTimedEvictionDecorator.this.clear();
        }
    }

    /*
     * An iterator for this map.
     */
    private final class EntryIterator implements Iterator<Entry<K, V>> {

        private final Iterator<Entry<K, EvictibleEntry<K, V>>> iterator = delegate.entrySet().iterator();

        private volatile Entry<K, V> currentEntry = null;

        public EntryIterator() {
        }

        @Override
        public Entry<K, V> next() {
            this.currentEntry = this.iterator.next().getValue();
            return this.currentEntry;
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public synchronized void remove() {
            if (this.currentEntry == null) {
                throw new IllegalStateException("There is no entry that can be removed");
            }

            ConcurrentMapWithTimedEvictionDecorator.this.remove(currentEntry.getKey(), currentEntry.getValue());
            this.currentEntry = null;
        }
    }

}
