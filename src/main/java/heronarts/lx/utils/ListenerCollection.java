package heronarts.lx.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Would something like this be worthwhile? This code gets repeated a lot.
 */
public class ListenerCollection<T> implements Iterable<T> {
  private final List<T> listeners = new ArrayList<T>();

  public void add(T listener) {
    Objects.requireNonNull(listener);
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate listener: " + listener);
    }
    this.listeners.add(listener);
  }

  public void remove(T listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("May not remove non-registered listener: " + listener);
    }
    this.listeners.remove(listener);
  }

  @Override
  public Iterator<T> iterator() {
    return this.listeners.iterator();
  }
}
