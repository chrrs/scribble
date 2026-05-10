package me.chrr.scribble.screen;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.gui.PageNumberWidget;
import me.chrr.scribble.gui.TextArea;
import me.chrr.scribble.gui.button.IconButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        this.pagesToShow = Scribble.config().pagesToShow.get();
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
            PageNumberWidget widget = new PageNumberWidget(page -> jumpToPage(page - 1), x + 148 + i * 126, y + 16, this.font);
            this.pageNumbers.add(addRenderableWidget(widget));
        }

        initMenuControls(getMenuControlsY());

        this.backButton = addRenderableWidget(new PageButton(x + 43, y + 157, false,
                (button) -> this.goPageBackward(this.minecraft.hasShiftDown()), this.shouldPlayTurnSound()));
        initPageButtons(y + 157);
        this.forwardButton = addRenderableWidget(new PageButton(x + 126 * this.pagesToShow - 10, y + 157, true,
                (button) -> this.goPageForward(this.minecraft.hasShiftDown()), this.shouldPlayTurnSound()));

        if (shouldShowActionButtons()) {
            initActionButtons(x, this.getBackgroundY() + 12 + 4);
            initSettingsButton(x, this.getBackgroundY() + this.getBackgroundHeight() - 12 - 4 - 12);
        }

        this.updateCurrentPages();
    }

    private void initSettingsButton(int x, int y) {
        MutableComponent settingsText = Component.literal("Scribble " + Scribble.platform().VERSION + "\n")
                .setStyle(Style.EMPTY.withBold(true))
                .append(Component.translatable("text.scribble.action.settings")
                        .setStyle(Style.EMPTY.withBold(false)));

        addRenderableWidget(new IconButtonWidget(
                settingsText,
                () -> minecraft.setScreen(Scribble.buildConfigScreen(this)),
                x, y, 96, 90, 12, 12));
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

    public void jumpToPage(int page) {
        this.showPage(page, false);
    }

    public void showPage(int page, boolean insertIfMissing) {
        // Insert pages so the requested page exists if needed.
        // (note that min+max instead of clamp here is deliberate)
        int newPage = Math.max(Math.min(page, this.getTotalPages() - 1), 0);
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
            showPage(this.getTotalPages() - 1, false);
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

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_PAGE_UP) {
            Objects.requireNonNull(this.backButton).onPress(event);
            return true;
        } else if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            Objects.requireNonNull(this.forwardButton).onPress(event);
            return true;
        }

        return super.keyPressed(event);
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

    @Override
    public void onClose() {
        this.closeRemoteContainer();
        super.onClose();
    }

    public int getBackgroundX() {
        return this.width / 2 - getBackgroundWidth() / 2;
    }

    public int getBackgroundY() {
        if (Scribble.config().centerBookGui.get()) {
            // Perfect centering actually doesn't look great, so we put it on a third.
            return 2 + this.height / 3 - getMenuHeight() / 3;
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

    public static int getMenuHeight() {
        return 194 + 20;
    }

    public int getMenuControlsY() {
        return this.getBackgroundY() + getMenuHeight() - 20;
    }
    //endregion

    //region Abstract methods
    protected void closeRemoteContainer() {
    }

    protected boolean canInsertPages() {
        return false;
    }

    protected boolean shouldPlayTurnSound() {
        return true;
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
