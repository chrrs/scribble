package me.chrr.scribble.screen;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.BookFile;
import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.ScribbleConfig;
import me.chrr.scribble.gui.BookTextWidget;
import me.chrr.scribble.gui.TextArea;
import me.chrr.scribble.gui.button.IconButtonWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

@NullMarked
public class ScribbleBookViewScreen extends ScribbleBookScreen<Component> {
    protected BookViewScreen.BookAccess book;

    public ScribbleBookViewScreen(BookViewScreen.BookAccess book) {
        super(Component.translatable("book.view.title"));
        this.book = book;
    }

    @Override
    protected boolean shouldShowActionButtons() {
        return ScribbleConfig.INSTANCE.showActionButtons == ScribbleConfig.ShowActionButtons.ALWAYS;
    }

    @Override
    protected void initActionButtons(int x, int y) {
        addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.save_book_to_file"),
                this::saveBookToFile,
                x, y, 48, 90, 12, 12));
    }

    @Override
    protected void initMenuControls(int y) {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose())
                .pos((this.width - 200) / 2, y).width(200).build());
    }

    @Override
    protected TextArea<Component> createTextArea(int x, int y, int width, int height, int pageOffset) {
        return new BookTextWidget(x, y, width, height, this.font, this::handleClickEvent);
    }

    @Override
    protected Component getPage(int page) {
        return book.getPage(page);
    }

    @Override
    protected int getTotalPages() {
        // Always return at least 1 page, so we don't show an empty book (see #100).
        return Math.max(1, book.getPageCount());
    }

    private void saveBookToFile() {
        FileChooser.chooseFile(true, (path) -> {
            try {
                List<String> pages = this.book.pages().stream()
                        .map(RichText::fromFormattedTextLossy)
                        .map(RichText::getAsFormattedString)
                        .toList();

                BookFile bookFile = new BookFile("<written book>", pages);
                bookFile.writeJson(path);
            } catch (Exception e) {
                Scribble.LOGGER.error("could not save book to file", e);
            }
        });
    }

    private void handleClickEvent(ClickEvent event) {
        switch (event) {
            case ClickEvent.ChangePage(int page) -> this.jumpToPage(page - 1);
            case ClickEvent.RunCommand(String command) -> {
                this.closeRemoteContainer();
                clickCommandAction(Objects.requireNonNull(this.minecraft.player), command, null);
            }
            default -> Screen.defaultHandleGameClickEvent(event, this.minecraft, this);
        }
    }
}
