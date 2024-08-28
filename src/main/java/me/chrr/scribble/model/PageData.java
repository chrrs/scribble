package me.chrr.scribble.model;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.tool.commandmanager.CommandManager;

/**
 * Represents the data associated with a page, including its formatted content and edit history.
 *
 * @param text The formatted content of the page, represented as a {@link RichText} object.
 * @param manager The command manager responsible for tracking and managing the edit history of the page.
 */
public record PageData(RichText text, CommandManager manager) {

    public static PageData empty() {
        return new PageData(RichText.empty(), new CommandManager());
    }

    public PageData(RichText text) {
        this(text, new CommandManager());
    }

    public PageData(String string) {
        this(RichText.fromFormattedString(string), new CommandManager());
    }

    public PageData withText(RichText newText) {
        return new PageData(newText, this.manager);
    }
}
