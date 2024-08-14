package me.chrr.scribble.tool.commandmanager;


/**
 * Describes the objects that can create and restore memento-based snapshots of their state.
 *
 * <p>This interface provides methods for creating a memento representing the current state of an object
 * and restoring the object's state from a previously created memento.</p>
 *
 * @param <T> The type of the memento object used to capture the object's state.
 */
public interface Restorable<T> {

    /**
     * Creates a memento capturing the current state of the object.
     */
    T scribble$createMemento();

    /**
     * Restores the object's state from the given memento.
     */
    void scribble$restore(T memento);
}

