package me.chrr.scribble.screen;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.gui.PageNumberWidget;
import me.chrr.scribble.gui.TextArea;
import me.chrr.scribble.gui.button.IconButtonWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public abstract class ScribbleBookScreen<T> extends Screen {
    public int currentPage = 0;
    public int pagesToShow = 1;

    public Identifier backgroundTexture = BookViewScreen.BOOK_LOCATION;

    public final List<TextArea<T>> textAreas = new ArrayList<>();
    public final List<PageNumberWidget> pageNumbers = new ArrayList<>();

    public @Nullable PageButton backButton;
    public @Nullable PageButton forwardButton;

    protected ScribbleBookScreen(Component title) {
        super(title);
    }

    //region Widgets
    @Override
    protected void init() {
        this.pagesToShow = Scribble.config().pagesToShow;
        if (this.pagesToShow == 1) {
            this.backgroundTexture = BookViewScreen.BOOK_LOCATION;
        } else {
            this.backgroundTexture = Scribble.id("textures/gui/book_" + this.pagesToShow + ".png");
        }

        int x = this.getBackgroundX();
        int y = this.getBackgroundY();

        this.textAreas.clear();
        for (int i = 0; i < this.pagesToShow; i++) {
            TextArea<T> textArea = createTextArea(x + 36 + i * 126, y + 30, 114, 128, i);
            this.textAreas.add(addRenderableWidget(textArea));
        }

        this.pageNumbers.clear();
        for (int i = 0; i < this.pagesToShow; i++) {
            PageNumberWidget widget = new PageNumberWidget(page -> showPage(page - 1, false), x + 148 + i * 126, y + 16, this.font);
            this.pageNumbers.add(addRenderableWidget(widget));
        }

        initMenuControls(getMenuControlsY());

        this.backButton = addRenderableWidget(new PageButton(x + 43, y + 157, false,
                (button) -> this.goPageBackward(this.minecraft.hasShiftDown()), true));
        initPageButtons(y + 157);
        this.forwardButton = addRenderableWidget(new PageButton(x + 126 * this.pagesToShow - 10, y + 157, true,
                (button) -> this.goPageForward(this.minecraft.hasShiftDown()), true));

        if (shouldShowActionButtons()) {
            initActionButtons(x, this.getBackgroundY() + 12 + 4);
            initSettingsButton(x, this.getBackgroundY() + this.getBackgroundHeight() - 12 - 4 - 12);
        }

        this.updateCurrentPages();
    }

    private void initSettingsButton(int x, int y) {
        MutableComponent settingsText = Component.literal("Scribble " + Scribble.platform().VERSION + "\n")
                .setStyle(Style.EMPTY.withBold(true));

        boolean canOpenConfigScreen = Scribble.platform().HAS_YACL;
        if (canOpenConfigScreen) {
            settingsText.append(Component.translatable("text.scribble.action.settings")
                    .setStyle(Style.EMPTY.withBold(false)));
        } else {
            // FIXME: make this a translatable string
            settingsText.append(Component.literal("YACL needs to be installed to access the settings menu.")
                    .setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.RED)));
        }

        IconButtonWidget widget = addRenderableWidget(new IconButtonWidget(
                settingsText,
                () -> minecraft.setScreen(Scribble.buildConfigScreen(this)),
                x, y, 96, 90, 12, 12));
        widget.active = canOpenConfigScreen;
    }

    public void updateCurrentPages() {
        // Insert pages so every visible page exists.
        int pagesFromEnd = this.getTotalPages() - this.currentPage;
        while (pagesFromEnd < this.pagesToShow && this.canInsertPages()) {
            this.insertEmptyPageAt(this.getTotalPages());
            pagesFromEnd += 1;
        }

        // Switch the text areas and page numbers.
        for (int i = 0; i < this.pagesToShow; i++) {
            boolean visible = i < pagesFromEnd;
            this.textAreas.get(i).setVisible(visible);
            this.pageNumbers.get(i).visible = visible;

            if (visible) {
                this.textAreas.get(i).setText(this.getPage(this.currentPage + i));
                this.pageNumbers.get(i).setPageNumber(this.currentPage + i + 1, Math.max(1, this.getTotalPages()));
            }
        }

        // Show / hide the page switch buttons if necessary.
        if (this.backButton != null && this.forwardButton != null) {
            this.backButton.visible = this.currentPage > 0;
            this.forwardButton.visible = pagesFromEnd > this.pagesToShow || this.canInsertPages();
        }
    }

    public void showPage(int page, boolean insertIfMissing) {
        // Insert pages so the requested page exists if needed.
        int newPage = Math.clamp(page, 0, this.getTotalPages() - 1);
        if (insertIfMissing) {
            while (newPage < page && this.canInsertPages()) {
                this.insertEmptyPageAt(this.getTotalPages());
                newPage += 1;
            }
        }

        // Show the left most page, so the page numbers don't get offset.
        int shownPage = newPage - newPage % this.pagesToShow;
        if (shownPage != this.currentPage) {
            this.currentPage = shownPage;
            this.updateCurrentPages();

            // When switching to a new page, always focus the left most page.
            if (this.textAreas.stream().anyMatch(GuiEventListener::isFocused))
                this.setFocused(this.textAreas.getFirst());
        }
    }

    public void goPageForward(boolean toEnd) {
        if (toEnd) {
            showPage(this.getTotalPages(), false);
        } else {
            showPage(this.currentPage + this.pagesToShow, true);
        }
    }

    public void goPageBackward(boolean toStart) {
        if (toStart) {
            showPage(0, false);
        } else {
            showPage(this.currentPage - 1, false);
        }
    }
    //endregion

    //region Rendering and dimensions
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);

        int textureSize = this.pagesToShow == 1 ? 256 : 512;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, this.getBackgroundX(), this.getBackgroundY(),
                0.0F, 0.0F, 122 * this.pagesToShow + 70, 192, textureSize, textureSize);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    public int getBackgroundX() {
        return this.width / 2 - getBackgroundWidth() / 2;
    }

    public int getBackgroundY() {
        if (Scribble.config().centerBookGui) {
            // Perfect centering actually doesn't look great, so we put it on a third.
            return this.height / 3 - this.getMenuHeight() / 3;
        } else {
            return 2;
        }
    }

    public int getBackgroundWidth() {
        return this.pagesToShow * 126 + 66;
    }

    public int getBackgroundHeight() {
        return 182;
    }

    public int getMenuHeight() {
        return 192 + 2 + 20;
    }

    public int getMenuControlsY() {
        return this.getBackgroundY() + getMenuHeight() - 20;
    }
    //endregion

    //region Abstract methods
    protected boolean canInsertPages() {
        return false;
    }

    protected void insertEmptyPageAt(int page) {
    }

    protected abstract boolean shouldShowActionButtons();

    protected abstract void initActionButtons(int x, int y);

    protected void initPageButtons(int y) {
    }

    protected abstract void initMenuControls(int y);

    protected abstract TextArea<T> createTextArea(int x, int y, int width, int height, int pageOffset);

    protected abstract T getPage(int page);

    protected abstract int getTotalPages();
    //endregion
}
