package com.example.itemsway;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** In-game settings screen with a slider for every parameter and a live preview. */
public class SwayConfigScreen extends Screen {

    private static final int ROW_H = 24;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();

    private int cat = 0;
    private double scroll = 0;
    private int presetIdx = 0;
    private int panelX, panelW, listX, listW, listTop, listBottom;
    private AbstractWidget active;
    private Row lastRow;
    private long armedUntil = 0;

    private Button previewBtn, presetBtn, resetTabBtn, resetAllBtn, doneBtn;

    public SwayConfigScreen(Screen parent) {
        super(Component.literal("Item Sway Settings"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ rows
    private static class ParamSlider extends AbstractSliderButton {
        final Param p;

        ParamSlider(int x, int y, int w, int h, Param p) {
            super(x, y, w, h, Component.empty(), p.toNorm(p.value));
            this.p = p;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(p.label + ": " + p.format(p.value)));
        }

        @Override
        protected void applyValue() {
            p.value = p.fromNorm(this.value);
            this.value = p.toNorm(p.value);
        }

        void sync() {
            this.value = p.toNorm(p.value);
            updateMessage();
        }

        void nudge(int steps) {
            p.value = p.fromNorm(p.toNorm(p.value + steps * p.step));
            sync();
        }
    }

    private static class Row {
        final Param p;
        AbstractWidget w;
        ParamSlider slider;
        Button reset;

        Row(Param p) {
            this.p = p;
        }

        void refresh() {
            if (slider != null) slider.sync();
            else w.setMessage(Component.literal(p.label + ": " + p.format(p.value)));
        }
    }

    private Row makeRow(Param p) {
        Row r = new Row(p);
        if (p.bool) {
            r.w = Button.builder(Component.empty(), b -> {
                p.value = p.on() ? 0 : 1;
                r.refresh();
            }).bounds(0, 0, 100, 20).build();
        } else {
            r.slider = new ParamSlider(0, 0, 100, 20, p);
            r.w = r.slider;
        }
        r.reset = Button.builder(Component.literal("R"), b -> {
            p.value = p.def;
            r.refresh();
        }).bounds(0, 0, 20, 20).build();
        r.refresh();
        return r;
    }

    private void buildRows() {
        rows.clear();
        active = null;
        lastRow = null;
        SwayParams.Cat c = SwayParams.CATS.get(cat);
        for (Param p : SwayParams.ALL) {
            if (p.cat == c) rows.add(makeRow(p));
        }
    }

    private void refreshRows() {
        for (Row r : rows) r.refresh();
    }

    // ------------------------------------------------------------------ setup
    @Override
    protected void init() {
        panelX = 6;
        panelW = Mth.clamp((int) (this.width * 0.56), 250, 400);
        listX = panelX + 8;
        listW = panelW - 16;
        listTop = 64;
        listBottom = this.height - 56;

        tabButtons.clear();
        int cols = 6;
        int gap = 2;
        int tabW = (listW - gap * (cols - 1)) / cols;
        for (int i = 0; i < SwayParams.CATS.size(); i++) {
            final int idx = i;
            Button b = Button.builder(Component.literal(SwayParams.CATS.get(i).tab()), btn -> selectCat(idx))
                    .bounds(listX + (i % cols) * (tabW + gap), 22 + (i / cols) * 20, tabW, 18).build();
            tabButtons.add(b);
            addRenderableWidget(b);
        }

        int half = (listW - 4) / 2;
        int third = (listW - 8) / 3;
        previewBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            SwayParams.PREVIEW_ON.value = SwayParams.PREVIEW_ON.on() ? 0 : 1;
            refreshButtons();
            refreshRows();
        }).bounds(listX, this.height - 52, half, 20).build());
        presetBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            SwayParams.applyPreset(SwayParams.PRESETS.get(presetIdx));
            presetIdx = (presetIdx + 1) % SwayParams.PRESETS.size();
            refreshButtons();
            refreshRows();
        }).bounds(listX + half + 4, this.height - 52, half, 20).build());

        resetTabBtn = addRenderableWidget(Button.builder(Component.literal("Reset tab"), b -> {
            SwayParams.Cat c = SwayParams.CATS.get(cat);
            for (Param p : SwayParams.ALL) if (p.cat == c) p.value = p.def;
            refreshRows();
        }).bounds(listX, this.height - 28, third, 20).build());
        resetAllBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            long now = Util.getMillis();
            if (now < armedUntil) {
                SwayParams.resetAll();
                armedUntil = 0;
                refreshRows();
            } else {
                armedUntil = now + 3000;
            }
            refreshButtons();
        }).bounds(listX + third + 4, this.height - 28, third, 20).build());
        doneBtn = addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(listX + 2 * (third + 4), this.height - 28, third, 20).build());

        refreshButtons();
        buildRows();
    }

    private void selectCat(int i) {
        cat = i;
        scroll = 0;
        buildRows();
        refreshButtons();
    }

    private void refreshButtons() {
        for (int i = 0; i < tabButtons.size(); i++) tabButtons.get(i).active = i != cat;
        previewBtn.setMessage(Component.literal("Live preview: " + (SwayParams.PREVIEW_ON.on() ? "ON" : "OFF")));
        presetBtn.setMessage(Component.literal("Apply preset: " + SwayParams.PRESETS.get(presetIdx).name()));
        resetAllBtn.setMessage(Component.literal(armedUntil != 0 ? "SURE? Click again" : "Reset ALL"));
    }

    // ------------------------------------------------------------------ rendering
    private double maxScroll() {
        return Math.max(0, rows.size() * ROW_H - (listBottom - listTop));
    }

    private boolean inList(double mx, double my) {
        return my >= listTop && my <= listBottom && mx >= listX && mx <= listX + listW;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // Left-side panel only, so the held item stays visible on the right.
        g.fill(0, 0, panelX + panelW, this.height, 0xC8101010);
        g.drawString(this.font, "Item Sway - " + SwayParams.CATS.get(cat).title(), listX, 8, 0xFFFFFF);

        if (armedUntil != 0 && Util.getMillis() > armedUntil) {
            armedUntil = 0;
            refreshButtons();
        }

        super.render(g, mx, my, pt);

        scroll = Mth.clamp(scroll, 0, maxScroll());
        g.enableScissor(listX - 2, listTop, listX + listW + 2, listBottom);
        int y = listTop - (int) scroll;
        for (Row r : rows) {
            r.w.setX(listX);
            r.w.setY(y + 2);
            r.w.setWidth(listW - 28);
            r.reset.setX(listX + listW - 22);
            r.reset.setY(y + 2);
            if (y + ROW_H > listTop && y < listBottom) {
                r.w.render(g, mx, my, pt);
                r.reset.render(g, mx, my, pt);
            }
            y += ROW_H;
        }
        g.disableScissor();

        double ms = maxScroll();
        if (ms > 0) {
            int listH = listBottom - listTop;
            int barH = Math.max(16, (int) (listH * (double) listH / (rows.size() * ROW_H)));
            int barY = listTop + (int) ((listH - barH) * (scroll / ms));
            g.fill(panelX + panelW - 5, listTop, panelX + panelW - 2, listBottom, 0x60FFFFFF);
            g.fill(panelX + panelW - 5, barY, panelX + panelW - 2, barY + barH, 0xFFC0C0C0);
        }

        g.drawString(this.font, "Ctrl+scroll on a slider = fine adjust (Shift = x10)", listX, listBottom + 4, 0xA0A0A0);

        if (inList(mx, my)) {
            for (Row r : rows) {
                if (r.w.isMouseOver(mx, my)) {
                    String text = r.p.label + " - " + r.p.desc + " (default: " + r.p.format(r.p.def) + ")";
                    g.renderTooltip(this.font, this.font.split(Component.literal(text), 240), mx, my);
                    break;
                }
                if (r.reset.isMouseOver(mx, my)) {
                    g.renderTooltip(this.font, Component.literal("Reset to default: " + r.p.format(r.p.def)), mx, my);
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (inList(mx, my)) {
            for (Row r : rows) {
                if (r.w.mouseClicked(mx, my, btn)) {
                    active = r.w;
                    lastRow = r;
                    return true;
                }
                if (r.reset.mouseClicked(mx, my, btn)) return true;
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (active != null) return active.mouseDragged(mx, my, btn, dx, dy);
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (active != null) {
            active.mouseReleased(mx, my, btn);
            active = null;
            return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (inList(mx, my)) {
            if (hasControlDown()) {
                for (Row r : rows) {
                    if (r.slider != null && r.slider.isMouseOver(mx, my)) {
                        r.slider.nudge((delta > 0 ? 1 : -1) * (hasShiftDown() ? 10 : 1));
                        return true;
                    }
                }
            }
            scroll = Mth.clamp(scroll - delta * ROW_H, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // left / right arrows nudge the last clicked slider by one step
        if (lastRow != null && lastRow.slider != null && (key == 262 || key == 263)) {
            lastRow.slider.nudge((key == 262 ? 1 : -1) * (hasShiftDown() ? 10 : 1));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        SwayConfig.save();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
