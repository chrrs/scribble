package me.chrr.scribble.history;

/**
 * Interface for creating and restoring memento-based snapshots of an object's state.
 *
 * @param <T> Type of the memento object.
 */
public interface Restorable<T> {

    /**
     * Creates a memento of the current state.
     */
    T scribble$createMemento();

    /**
     * Restores the state from the given memento.
     */
    void scribble$restore(T memento);
}

