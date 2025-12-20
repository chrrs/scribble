package me.chrr.scribble.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import me.chrr.scribble.KeyboardUtil;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.SetReturnScreen;
import me.chrr.scribble.book.BookFile;
import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.ScribbleConfig;
import me.chrr.scribble.gui.TextArea;
import me.chrr.scribble.gui.button.ColorSwatchWidget;
import me.chrr.scribble.gui.button.IconButtonWidget;
import me.chrr.scribble.gui.button.ModifierButtonWidget;
import me.chrr.scribble.gui.edit.RichEditBox;
import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import me.chrr.scribble.history.CommandManager;
import me.chrr.scribble.history.HistoryListener;
import me.chrr.scribble.history.command.Command;
import me.chrr.scribble.history.command.EditCommand;
import me.chrr.scribble.history.command.PageDeleteCommand;
import me.chrr.scribble.history.command.PageInsertCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

@NullMarked
public class ScribbleBookEditScreen extends ScribbleBookScreen<RichText> implements HistoryListener {
    private static final ChatFormatting[] COLORS = new ChatFormatting[]{
            ChatFormatting.BLACK, ChatFormatting.DARK_GRAY,
            ChatFormatting.GRAY, ChatFormatting.WHITE,
            ChatFormatting.DARK_RED, ChatFormatting.RED,
            ChatFormatting.GOLD, ChatFormatting.YELLOW,
            ChatFormatting.DARK_GREEN, ChatFormatting.GREEN,
            ChatFormatting.DARK_AQUA, ChatFormatting.AQUA,
            ChatFormatting.DARK_BLUE, ChatFormatting.BLUE,
            ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE,
    };

    private final Player player;
    private final ItemStack itemStack;
    private final InteractionHand hand;

    private final List<RichText> pages;
    private final CommandManager commandManager = new CommandManager(this);

    private @Nullable RichEditBox lastFocusedEditBox = null;
    private boolean dirty = false;

    private @Nullable IconButtonWidget undoButton;
    private @Nullable IconButtonWidget redoButton;

    private final List<IconButtonWidget> insertPageButtons = new ArrayList<>();

    private @Nullable ModifierButtonWidget boldButton;
    private @Nullable ModifierButtonWidget italicButton;
    private @Nullable ModifierButtonWidget underlineButton;
    private @Nullable ModifierButtonWidget strikethroughButton;
    private @Nullable ModifierButtonWidget obfuscatedButton;

    private List<ColorSwatchWidget> colorSwatches = List.of();

    public ScribbleBookEditScreen(Player player, ItemStack itemStack, InteractionHand hand, WritableBookContent book) {
        super(Component.translatable("book.edit.title"));

        this.player = player;
        this.itemStack = itemStack;
        this.hand = hand;

        this.pages = new ArrayList<>();
        book.getPages(Minecraft.getInstance().isTextFilteringEnabled())
                .forEach((page) -> this.pages.add(RichText.fromFormattedString(page)));

        if (this.pages.isEmpty()) {
            for (int i = 0; i < this.pagesToShow; i++) {
                this.pages.add(RichText.EMPTY);
            }
        }
    }

    //region Widgets (Action, Menu & TextArea)
    @Override
    protected boolean shouldShowActionButtons() {
        return ScribbleConfig.INSTANCE.showActionButtons != ScribbleConfig.ShowActionButtons.NEVER;
    }

    @Override
    protected void initActionButtons(int x, int y) {
        this.undoButton = addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.undo"),
                () -> {
                    this.commandManager.tryUndo();
                    this.invalidateActionButtons();
                },
                x, y, 24, 90, 12, 12));
        this.redoButton = addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.redo"),
                () -> {
                    this.commandManager.tryRedo();
                    this.invalidateActionButtons();
                },
                x, y + 12, 36, 90, 12, 12));

        addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.save_book_to_file"),
                () -> FileChooser.chooseFile(true, this::saveToFile),
                x, y + 12 * 2 + 4, 48, 90, 12, 12));
        addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.load_book_from_file"),
                () -> this.confirmIf(this.dirty || !this.isEmpty(), "overwrite_warning",
                        () -> FileChooser.chooseFile(false, this::loadFromFile)),
                x, y + 12 * 3 + 4, 60, 90, 12, 12));

        this.invalidateActionButtons();
    }

    private void invalidateActionButtons() {
        if (undoButton != null && redoButton != null) {
            undoButton.active = commandManager.canUndo();
            redoButton.active = commandManager.canRedo();
        }
    }

    @Override
    protected void initPageButtons(int y) {
        this.insertPageButtons.clear();

        for (int i = 0; i < this.pagesToShow; i++) {
            int xOffset = this.pagesToShow == 1
                    ? 0 : i == 0
                    ? 32 : i == this.pagesToShow - 1
                    ? -32 : 0;

            // When we only show a single page, it's clearer to show something like
            // 'insert new page _before_ current' instead of just 'here'.
            Component insertText = this.pagesToShow == 1
                    ? Component.translatable("text.scribble.action.insert_new_page")
                    : Component.translatable("text.scribble.action.insert_new_page_here");
            Component deleteText = Component.translatable("text.scribble.action.delete_page");

            int pageOffset = i;
            this.insertPageButtons.add(addRenderableWidget(new IconButtonWidget(insertText,
                    () -> {
                        PageInsertCommand command = new PageInsertCommand(this.currentPage + pageOffset);
                        command.execute(this);
                        commandManager.push(command);
                    },
                    getBackgroundX() + 78 + xOffset + i * 126, y + 2, 12, 90, 12, 12)));
            addRenderableWidget(new IconButtonWidget(deleteText,
                    () -> {
                        PageDeleteCommand command = new PageDeleteCommand(this.currentPage + pageOffset,
                                this.pages.get(this.currentPage + pageOffset));
                        command.execute(this);
                        commandManager.push(command);
                    },
                    getBackgroundX() + 94 + xOffset + i * 126, y + 2, 0, 90, 12, 12));
        }
    }

    @Override
    public void updateCurrentPages() {
        super.updateCurrentPages();
        this.insertPageButtons.forEach((button) -> button.visible = this.getTotalPages() < 100);
    }

    @Override
    protected void initMenuControls(int y) {
        this.addRenderableWidget(Button.builder(Component.translatable("book.signButton"), (button) -> {
            @SuppressWarnings("DataFlowIssue")
            BookSignScreen screen = new BookSignScreen(null, this.player, this.hand, getPagesAsStrings(true));
            ((SetReturnScreen) screen).scribble$setReturnScreen(this);
            this.minecraft.setScreen(screen);
        }).pos(this.width / 2 - 98 - 2, y).width(98).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
            this.minecraft.setScreen(null);
            this.saveChanges();
        }).pos(this.width / 2 + 2, y).width(98).build());
    }

    @Override
    protected void setInitialFocus() {
        super.setInitialFocus(this.textAreas.getFirst());
    }

    @Override
    protected TextArea<RichText> createTextArea(int x, int y, int width, int height, int pageOffset) {
        RichEditBox editBox = (RichEditBox) new RichEditBox.Builder()
                .onHistoryPush((command) -> this.pushCommand(pageOffset, command))
                .onInvalidateFormat(this::invalidateFormattingButtons)
                .setShowDecorations(false)
                .setTextColor(0xff000000).setCursorColor(0xff000000)
                .setShowBackground(false).setTextShadow(false)
                .setX(x - 4).setY(y - 4)
                .build(this.font, width + 8, height + 6, CommonComponents.EMPTY);

        editBox.setCharacterLimit(1024);
        editBox.setLineLimit(height / this.font.lineHeight);

        editBox.setRichValueListener((text) -> {
            RichText existing = this.pages.get(this.currentPage + pageOffset);
            if (existing != text) {
                this.pages.set(this.currentPage + pageOffset, text);
                this.dirty = true;
            }
        });

        return editBox;
    }

    private void updateFocusedEditBox() {
        if (this.getFocused() instanceof RichEditBox focusedEditBox && this.lastFocusedEditBox != focusedEditBox) {
            this.lastFocusedEditBox = focusedEditBox;
            this.textAreas.stream().map((textArea) -> (RichEditBox) textArea)
                    .filter((editBox) -> !editBox.isFocused())
                    .forEach((editBox) -> editBox.getRichTextField().resetCursor(false));
            focusedEditBox.getRichTextField().sendUpdateFormat();
        }
    }

    @Override
    protected void changeFocus(ComponentPath componentPath) {
        super.changeFocus(componentPath);
        this.updateFocusedEditBox();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        super.setFocused(guiEventListener);
        this.updateFocusedEditBox();
    }
    //endregion

    //region Saving and Loading
    public void confirmIf(boolean condition, String name, Runnable runnable) {
        if (!condition) {
            runnable.run();
            return;
        }

        BooleanConsumer onConfirmed = (confirmed) -> {
            this.minecraft.setScreen(this);
            if (confirmed) runnable.run();
        };

        this.minecraft.setScreen(new ConfirmScreen(
                onConfirmed,
                Component.translatable("text.scribble." + name + ".title"),
                Component.translatable("text.scribble." + name + ".description")
        ));
    }

    private void saveToFile(Path path) {
        try {
            BookFile bookFile = new BookFile(this.player.getName().getString(), this.getPagesAsStrings(true));
            bookFile.writeJson(path);
        } catch (Exception e) {
            Scribble.LOGGER.error("could not save book to file", e);
        }
    }

    private void loadFromFile(Path path) {
        try {
            BookFile bookFile = BookFile.readFile(path);

            this.pages.clear();
            this.pages.addAll(bookFile.pages().stream().map(RichText::fromFormattedString).toList());
            this.commandManager.clear();
            this.dirty = true;

            this.showPage(0, false);
            this.updateCurrentPages();
        } catch (Exception e) {
            Scribble.LOGGER.error("could not load book from file", e);
        }
    }

    @Override
    public void onClose() {
        this.confirmIf(this.dirty, "quit_without_saving", super::onClose);
    }
    //endregion

    //region Hotkeys
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.hasControlDown() && !keyEvent.hasAltDown()) {
            // On Ctrl-Z, undo.
            if ((KeyboardUtil.isKey(keyEvent.key(), "Z") && !keyEvent.hasShiftDown()
                    && undoButton != null && undoButton.active)) {
                this.commandManager.tryUndo();
                this.invalidateActionButtons();
                return true;
            }

            // On Ctrl-Shift-Z or Ctrl-Y, redo.
            if (((KeyboardUtil.isKey(keyEvent.key(), "Z") && keyEvent.hasShiftDown())
                    || (KeyboardUtil.isKey(keyEvent.key(), "Y") && !keyEvent.hasShiftDown()))
                    && redoButton != null && redoButton.active) {
                this.commandManager.tryRedo();
                this.invalidateActionButtons();
                return true;
            }
        }

        return super.keyPressed(keyEvent);
    }
    //endregion

    //region Formatting
    @Override
    protected void init() {
        super.init();

        if (ScribbleConfig.INSTANCE.showFormattingButtons) {
            int x = this.width / 2 + this.getBackgroundWidth() / 2 - 20;
            int y = this.getBackgroundY() + 12;

            // Modifier buttons (but in reverse!)
            obfuscatedButton = addRenderableWidget(new ModifierButtonWidget(
                    Component.translatable("text.scribble.modifier.obfuscated"),
                    (toggled) -> this.applyFormat(ChatFormatting.OBFUSCATED, toggled),
                    x, y + 70, 0, 70, 22, 18));
            strikethroughButton = addRenderableWidget(new ModifierButtonWidget(
                    Component.translatable("text.scribble.modifier.strikethrough"),
                    (toggled) -> this.applyFormat(ChatFormatting.STRIKETHROUGH, toggled),
                    x, y + 53, 0, 53, 22, 17));
            underlineButton = addRenderableWidget(new ModifierButtonWidget(
                    Component.translatable("text.scribble.modifier.underline"),
                    (toggled) -> this.applyFormat(ChatFormatting.UNDERLINE, toggled),
                    x, y + 36, 0, 36, 22, 17));
            italicButton = addRenderableWidget(new ModifierButtonWidget(
                    Component.translatable("text.scribble.modifier.italic"),
                    (toggled) -> this.applyFormat(ChatFormatting.ITALIC, toggled),
                    x, y + 19, 0, 19, 22, 17));
            boldButton = addRenderableWidget(new ModifierButtonWidget(
                    Component.translatable("text.scribble.modifier.bold"),
                    (toggled) -> this.applyFormat(ChatFormatting.BOLD, toggled),
                    x, y, 0, 0, 22, 19));

            // Color swatches
            colorSwatches = new ArrayList<>(COLORS.length);
            for (int i = 0; i < COLORS.length; i++) {
                int dx = (i % 2) * 8;
                int dy = (i / 2) * 8;

                ChatFormatting color = COLORS[i];
                colorSwatches.add(addRenderableWidget(new ColorSwatchWidget(
                        Component.translatable("text.scribble.color." + color.getName()), color,
                        () -> this.applyFormat(color, true),
                        x + 3 + dx, y + 95 + dy, 8, 8
                )));
            }

            this.invalidateFormattingButtons();
        }
    }

    private void applyFormat(ChatFormatting formatting, boolean enabled) {
        if (this.lastFocusedEditBox == null)
            return;
        this.lastFocusedEditBox.applyFormat(formatting, enabled);
    }

    private void setSwatchColor(@Nullable ChatFormatting color) {
        for (ColorSwatchWidget swatch : colorSwatches) {
            swatch.setToggled(swatch.getColor() == color);
        }
    }

    private void invalidateFormattingButtons() {
        RichEditBox editBox = this.lastFocusedEditBox;
        if (editBox == null)
            return;

        if (boldButton == null || italicButton == null || underlineButton == null
                || strikethroughButton == null || obfuscatedButton == null)
            return;

        boldButton.toggled = editBox.modifiers.contains(ChatFormatting.BOLD);
        italicButton.toggled = editBox.modifiers.contains(ChatFormatting.ITALIC);
        underlineButton.toggled = editBox.modifiers.contains(ChatFormatting.UNDERLINE);
        strikethroughButton.toggled = editBox.modifiers.contains(ChatFormatting.STRIKETHROUGH);
        obfuscatedButton.toggled = editBox.modifiers.contains(ChatFormatting.OBFUSCATED);

        setSwatchColor(editBox.color);
    }
    //endregion

    //region Page Management
    @Override
    protected RichText getPage(int page) {
        return this.pages.get(page);
    }

    @Override
    protected int getTotalPages() {
        return this.pages.size();
    }

    @Override
    public void showPage(int page, boolean insertIfMissing) {
        super.showPage(page, insertIfMissing);
        this.setFocused(this.textAreas.get(Math.min(page, this.getTotalPages()) % this.textAreas.size()));
    }

    private List<String> getPagesAsStrings(boolean removeTrailingPages) {
        List<RichText> pages = new ArrayList<>(this.pages);

        if (removeTrailingPages) {
            ListIterator<RichText> listIterator = pages.listIterator(pages.size());
            while (listIterator.hasPrevious() && listIterator.previous().isEmpty()) {
                listIterator.remove();
            }
        }

        return pages.stream().map(RichText::getAsFormattedString).toList();
    }

    private void saveChanges() {
        // Update local copy.
        List<String> pages = getPagesAsStrings(true);
        this.itemStack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages.stream().map(Filterable::passThrough).toList()));

        // Update remote copy.
        int slot = this.hand == InteractionHand.MAIN_HAND ? this.player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
        ClientPacketListener connection = this.minecraft.getConnection();
        Objects.requireNonNull(connection).send(new ServerboundEditBookPacket(slot, pages, Optional.empty()));
    }

    @Override
    protected boolean canInsertPages() {
        return this.getTotalPages() < 100;
    }

    @Override
    protected void insertEmptyPageAt(int page) {
        this.pages.add(page, RichText.EMPTY);
    }

    private boolean isEmpty() {
        return this.pages.stream().allMatch(RichText::isEmpty);
    }
    //endregion

    //region History
    public void pushCommand(int pageOffset, Command command) {
        if (command instanceof EditCommand editCommand) {
            editCommand.page = this.currentPage + pageOffset;
        }

        this.commandManager.push(command);
        this.dirty = true;
        this.invalidateActionButtons();
    }

    @Override
    public RichMultiLineTextField switchAndFocusPage(int page) {
        this.showPage(page, false);
        int shownPage = Math.min(page, this.getTotalPages());

        RichEditBox editBox = (RichEditBox) this.textAreas.get(shownPage % this.pagesToShow);
        setFocused(editBox);

        return editBox.getRichTextField();
    }

    @Override
    public void setFormat(@Nullable ChatFormatting color, Set<ChatFormatting> modifiers) {
        RichEditBox editBox = this.lastFocusedEditBox;
        if (editBox == null)
            return;

        editBox.color = color;
        editBox.modifiers = modifiers;
        this.invalidateFormattingButtons();
    }

    @Override
    public void insertPageAt(int page, @Nullable RichText content) {
        this.pages.add(page, Optional.ofNullable(content).orElse(RichText.EMPTY));
        this.dirty = true;
        this.showPage(page, false);
        this.updateCurrentPages();
    }

    @Override
    public void deletePage(int page) {
        this.pages.remove(page);
        this.dirty = true;

        if (page >= this.pages.size() - 1)
            this.showPage(page - 1, false);
        this.updateCurrentPages();
    }
    //endregion
}
