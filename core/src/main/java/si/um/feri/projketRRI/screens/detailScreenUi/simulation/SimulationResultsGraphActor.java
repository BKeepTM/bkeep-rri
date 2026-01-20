package si.um.feri.projketRRI.screens.detailScreenUi.simulation;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

import java.util.ArrayList;
import java.util.List;

public class SimulationResultsGraphActor extends Actor {

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    private List<SimulationCalculator.DayResult> results = new ArrayList<>();

    private float padLeft = 72f;
    private float padRight = 10f;
    private float padTop = 10f;
    private float padBottom = 44f;

    private float pointRadius = 3.5f;

    private int hoveredIndex = -1;
    private float hoveredX = 0f;
    private float hoveredY = 0f;

    private float hoverPickRadius = 20f;
    private float tooltipPad = 6f;
    private float tooltipOffset = 12f;

    public SimulationResultsGraphActor() {
        addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                updateHover(x, y);
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                hoveredIndex = -1;
            }
        });
    }

    public void setResults(List<SimulationCalculator.DayResult> results) {
        this.results = (results == null) ? new ArrayList<>() : results;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (results == null || results.size() < 2) return;

        // --- min/max weight ---
        float minW = Float.MAX_VALUE;
        float maxW = -Float.MAX_VALUE;

        for (SimulationCalculator.DayResult r : results) {
            minW = Math.min(minW, r.weightKg);
            maxW = Math.max(maxW, r.weightKg);
        }

        if (MathUtils.isEqual(minW, maxW)) maxW = minW + 1f;

        // --- graph rect inside actor bounds ---
        float x0 = getX();
        float y0 = getY();
        float w0 = getWidth();
        float h0 = getHeight();

        float gx = x0 + padLeft;
        float gy = y0 + padBottom;
        float gw = w0 - padLeft - padRight;
        float gh = h0 - padTop - padBottom;

        // axis helpers
        float yMin = gy;
        float yMid = gy + 0.5f * gh;
        float yMax = gy + gh;

        float xStart = gx;
        float xMid = gx + 0.5f * gw;
        float xEnd = gx + gw;

        int n = results.size();

        // ---- Tooltip content ----
        boolean hasHover = hoveredIndex >= 0 && hoveredIndex < n;
        String tooltipText = null;
        float tx = 0, ty = 0, boxW = 0, boxH = 0;

        if (hasHover) {
            SimulationCalculator.DayResult r = results.get(hoveredIndex);
            // Multi-line tooltip
            tooltipText =
                "Day " + r.day +
                "\nWeight: " + fmt2(r.weightKg) + " kg" +
                "\nYield:  " + fmt3(r.yieldKg) + " kg" +
                "\nLoss:   " + fmt3(r.lossKg) + " kg" +
                "\nDelta:  " + fmt3(r.deltaKg) + " kg" +
                "\nStress: " + fmt2(r.fStress);

            layout.setText(font, tooltipText);
            boxW = layout.width + tooltipPad * 2f;
            boxH = layout.height + tooltipPad * 2f;

            tx = getX() + hoveredX + tooltipOffset;
            ty = getY() + hoveredY + tooltipOffset;

            float maxX = getX() + getWidth() - boxW - 2f;
            float maxY = getY() + getHeight() - 2f;

            tx = MathUtils.clamp(tx, getX() + 2f, maxX);
            ty = MathUtils.clamp(ty, getY() + boxH + 2f, maxY);
        }

        // --- SHAPES ---
        batch.end();

        shapeRenderer.setProjectionMatrix(getStage().getCamera().combined);

        // Border + ticks + line
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        shapeRenderer.rect(gx, gy, gw, gh);

        float tick = 6f;
        // Y ticks
        shapeRenderer.line(gx - tick, yMin, gx, yMin);
        shapeRenderer.line(gx - tick, yMid, gx, yMid);
        shapeRenderer.line(gx - tick, yMax, gx, yMax);

        // X ticks
        shapeRenderer.line(xStart, gy, xStart, gy - tick);
        shapeRenderer.line(xMid, gy, xMid, gy - tick);
        shapeRenderer.line(xEnd, gy, xEnd, gy - tick);

        // line plot (weightKg)
        for (int i = 0; i < n - 1; i++) {
            SimulationCalculator.DayResult a = results.get(i);
            SimulationCalculator.DayResult b = results.get(i + 1);

            float ny1 = (a.weightKg - minW) / (maxW - minW);
            float ny2 = (b.weightKg - minW) / (maxW - minW);

            float px1 = gx + (float) i / (n - 1) * gw;
            float px2 = gx + (float) (i + 1) / (n - 1) * gw;

            float py1 = gy + ny1 * gh;
            float py2 = gy + ny2 * gh;

            shapeRenderer.line(px1, py1, px2, py2);
        }

        shapeRenderer.end();

        // Points + tooltip background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < n; i++) {
            SimulationCalculator.DayResult r = results.get(i);

            float ny = (r.weightKg - minW) / (maxW - minW);
            float px = gx + (float) i / (n - 1) * gw;
            float py = gy + ny * gh;

            shapeRenderer.circle(px, py, pointRadius, 12);
        }

        if (hasHover) {
            shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
            shapeRenderer.rect(tx, ty - boxH, boxW, boxH);
            shapeRenderer.setColor(1f, 1f, 1f, 1f);
        }

        shapeRenderer.end();

        // --- TEXT ---
        batch.begin();

        // Y labels
        font.draw(batch, fmt1(minW) + " kg", x0 + 6f, yMin + 4f);
        font.draw(batch, fmt1((minW + maxW) * 0.5f) + " kg", x0 + 6f, yMid + 4f);
        font.draw(batch, fmt1(maxW) + " kg", x0 + 6f, yMax + 4f);

        // X labels (days)
        int dayStart = results.get(0).day;
        int dayEnd = results.get(n - 1).day;
        int dayMid = results.get(n / 2).day;

        float yText = y0 + 16f;
        font.draw(batch, "Day " + dayStart, xStart - 2f, yText);
        font.draw(batch, "Day " + dayMid, xMid - 24f, yText);
        font.draw(batch, "Day " + dayEnd, xEnd - 48f, yText);

        // Tooltip text
        if (hasHover) {
            font.draw(batch, tooltipText, tx + tooltipPad, ty - tooltipPad);
        }
    }

    private void updateHover(float localX, float localY) {
        hoveredIndex = -1;

        if (results == null || results.size() < 2) return;

        float gx = padLeft;
        float gy = padBottom;
        float gw = getWidth() - padLeft - padRight;
        float gh = getHeight() - padTop - padBottom;

        if (localX < gx || localX > gx + gw || localY < gy || localY > gy + gh) return;

        float minW = Float.MAX_VALUE;
        float maxW = -Float.MAX_VALUE;
        for (SimulationCalculator.DayResult r : results) {
            minW = Math.min(minW, r.weightKg);
            maxW = Math.max(maxW, r.weightKg);
        }
        if (MathUtils.isEqual(minW, maxW)) maxW = minW + 1f;

        int n = results.size();
        float bestDist2 = hoverPickRadius * hoverPickRadius;

        for (int i = 0; i < n; i++) {
            SimulationCalculator.DayResult r = results.get(i);

            float nx = (float) i / (n - 1);
            float ny = (r.weightKg - minW) / (maxW - minW);

            float px = gx + nx * gw;
            float py = gy + ny * gh;

            float dx = localX - px;
            float dy = localY - py;
            float d2 = dx * dx + dy * dy;

            if (d2 <= bestDist2) {
                bestDist2 = d2;
                hoveredIndex = i;
                hoveredX = px;
                hoveredY = py;
            }
        }
    }

    private static String fmt1(float v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }

    private static String fmt2(float v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private static String fmt3(float v) {
        return String.format(java.util.Locale.US, "%.3f", v);
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }
}
