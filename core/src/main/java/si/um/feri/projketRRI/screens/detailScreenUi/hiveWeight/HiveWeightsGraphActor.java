package si.um.feri.projketRRI.screens.detailScreenUi.hiveWeight;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import si.um.feri.projketRRI.api.calls.model.HiveWeight;

public class HiveWeightsGraphActor extends Actor {

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont(); // or use your skin font if you want

    private Array<HiveWeight> weights = new Array<>();

    // extra room for labels
    private float padLeft = 72f;
    private float padRight = 10f;
    private float padTop = 10f;
    private float padBottom = 44f;

    private float pointRadius = 3.5f;

    private final GlyphLayout layout = new GlyphLayout();

    private int hoveredIndex = -1;
    private float hoveredX = 0f;
    private float hoveredY = 0f;

    private float hoverPickRadius = 20f;
    private float tooltipPad = 6f;
    private float tooltipOffset = 12f;

    public HiveWeightsGraphActor() {
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

    private final DateTimeFormatter timeFmt =
        DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public void setWeights(Array<HiveWeight> weights) {
        this.weights = (weights == null) ? new Array<>() : weights;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (weights == null || weights.size < 2) return;

        // --- min/max weight + times ---
        float minW = Float.MAX_VALUE;
        float maxW = -Float.MAX_VALUE;

        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;

        for (HiveWeight w : weights) {
            minW = Math.min(minW, w.weight);
            maxW = Math.max(maxW, w.weight);

            long t = parseIsoToMillis(w.timeWeight);
            minT = Math.min(minT, t);
            maxT = Math.max(maxT, t);
        }

        if (MathUtils.isEqual(minW, maxW)) maxW = minW + 1f;
        if (minT == Long.MAX_VALUE || maxT == Long.MIN_VALUE || minT == maxT) {
            minT = 0;
            maxT = weights.size - 1;
        }

        // --- graph rect inside actor bounds ---
        float x0 = getX();
        float y0 = getY();
        float w0 = getWidth();
        float h0 = getHeight();

        float gx = x0 + padLeft;
        float gy = y0 + padBottom;
        float gw = w0 - padLeft - padRight;
        float gh = h0 - padTop - padBottom;

        // Precompute axis positions
        float yMin = gy;
        float yMid = gy + 0.5f * gh;
        float yMax = gy + gh;

        float xStart = gx;
        float xMid = gx + 0.5f * gw;
        float xEnd = gx + gw;

        int n = weights.size;

        // ---- Tooltip content (computed here, drawn later) ----
        boolean hasHover = hoveredIndex >= 0 && hoveredIndex < n;
        String tooltipText = null;
        float tx = 0, ty = 0, boxW = 0, boxH = 0;

        if (hasHover) {
            HiveWeight hw = weights.get(hoveredIndex);
            String line1 = formatTime(parseIsoToMillis(hw.timeWeight));
            String line2 = String.format(java.util.Locale.US, "%.2f kg", hw.weight);
            tooltipText = line1 + "\n" + line2;

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

        // line plot
        for (int i = 0; i < n - 1; i++) {
            HiveWeight a = weights.get(i);
            HiveWeight b = weights.get(i + 1);

            float ny1 = (a.weight - minW) / (maxW - minW);
            float ny2 = (b.weight - minW) / (maxW - minW);

            float px1 = gx + (float) i / (n - 1) * gw;
            float px2 = gx + (float) (i + 1) / (n - 1) * gw;

            float py1 = gy + ny1 * gh;
            float py2 = gy + ny2 * gh;

            shapeRenderer.line(px1, py1, px2, py2);
        }

        shapeRenderer.end();

        // Points + tooltip background (filled)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < n; i++) {
            HiveWeight w = weights.get(i);
            float ny = (w.weight - minW) / (maxW - minW);

            float px = gx + (float) i / (n - 1) * gw;
            float py = gy + ny * gh;

            shapeRenderer.circle(px, py, pointRadius, 12);
        }

        if (hasHover) {
            shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
            shapeRenderer.rect(tx, ty - boxH, boxW, boxH);
        }

        shapeRenderer.setColor(1f, 1f, 1f, 1f);

        shapeRenderer.end();

        batch.begin();

        // Y labels
        font.draw(batch, formatKg(maxW), x0 + 6f, yMax + 4f);
        font.draw(batch, formatKg((minW + maxW) * 0.5f), x0 + 6f, yMid + 4f);
        font.draw(batch, formatKg(minW), x0 + 6f, yMin + 4f);

        // X labels
        String tStart = formatTime(minT);
        String tEnd = formatTime(maxT);
        String tMid = formatTime(minT + (maxT - minT) / 2);

        float yText = y0 + 16f;
        font.draw(batch, tStart, xStart - 2f, yText);
        font.draw(batch, tMid, xMid - 28f, yText);
        font.draw(batch, tEnd, xEnd - 56f, yText);

        // Tooltip text
        if (hasHover) {
            font.draw(batch, tooltipText, tx + tooltipPad, ty - tooltipPad);
        }
    }


    private void updateHover(float localX, float localY) {
        hoveredIndex = -1;

        if (weights == null || weights.size < 2) return;

        float gx = padLeft;
        float gy = padBottom;
        float gw = getWidth() - padLeft - padRight;
        float gh = getHeight() - padTop - padBottom;

        if (localX < gx || localX > gx + gw || localY < gy || localY > gy + gh) return;

        float minW = Float.MAX_VALUE;
        float maxW = -Float.MAX_VALUE;
        for (HiveWeight w : weights) {
            minW = Math.min(minW, w.weight);
            maxW = Math.max(maxW, w.weight);
        }
        if (MathUtils.isEqual(minW, maxW)) maxW = minW + 1f;

        int n = weights.size;

        float bestDist2 = hoverPickRadius * hoverPickRadius;

        for (int i = 0; i < n; i++) {
            HiveWeight w = weights.get(i);

            float nx = (float) i / (n - 1);
            float ny = (w.weight - minW) / (maxW - minW);

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


    private String formatKg(float w) {
        // adjust decimals as you like
        return String.format(java.util.Locale.US, "%.1f kg", w);
    }

    private String formatTime(long ms) {
        try {
            if (ms <= 0) return "-";
            return timeFmt.format(Instant.ofEpochMilli(ms));
        } catch (Exception e) {
            return "-";
        }
    }

    private long parseIsoToMillis(String iso) {
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (Exception ex) {
            return 0L;
        }
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }
}
