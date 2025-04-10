package Interactables;

import boilerplate.rendering.BufferBuilder2f;
import boilerplate.rendering.Shape2d;
import boilerplate.rendering.ShapeMode;
import boilerplate.rendering.text.FontManager;
import boilerplate.rendering.text.TextRenderer;
import boilerplate.utility.Logging;
import boilerplate.utility.Vec2;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Input {
    public interface InputCallback {
        void call(Input input, String value);
    }

    public Vec2 pos;
    public Color color = Color.white;
    public FontManager.LoadedFont font = FontManager.getLoadedFont(3);

    public String title;
    public float titleScale = 1;

    public String defaultVal;
    public String value = "";
    public float valueScale = 1;
    public int maxChars = 10;

    public boolean intValuesOnly = true;
    public boolean mouseHovering = false;
    public boolean wobbling = false;
    public boolean selected = false;

    private Vec2 areaPos;
    private Vec2 areaSize;
    private final Vec2 areaMargin = new Vec2(20, 10);

    private InputGroup group;
    private final ArrayList<InputCallback> callbacks = new ArrayList<>();

    public Input(Vec2 pos, String title, String defaultVal) {
        this.pos = pos;
        this.title = title;
        this.defaultVal = defaultVal;
        this.value = defaultVal;
    }

    public Input(Vec2 pos, String title, String defaultVal, Color color) {
        this(pos, title, defaultVal);
        this.color = color;
    }

    public void setMouseHovering(boolean val) {
        mouseHovering = val;
        if (!selected) wobbling = val;
    }

    public void setGroup(InputGroup group) {
        if (this.group != null) {
            Logging.warn("Input already belongs to a group, this will produce unexpected behaviour!");
            return;
        }
        this.group = group;
    }

    public void addCallback(InputCallback callback) {
        callbacks.add(callback);
    }

    public void clearCallbacks() {
        callbacks.clear();
    }

    public void unselect() {
        if (!selected) return;
        selected = false;
        setMouseHovering(mouseHovering);  // update this!
        fireCallbacks();
    }

    public void select() {
        selected = true;
    }

    public void keyPressed(int key, int scancode) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && !value.isEmpty()) {
            value = value.substring(0, value.length() - 1);
            group.hasChanged = true;
            return;
        }
        if (key == GLFW.GLFW_KEY_DELETE && !value.isEmpty()) {
            value = "";
            group.hasChanged = true;
            return;
        }

        if (value.length() >= maxChars) return;

        String keyChar = GLFW.glfwGetKeyName(key, scancode);
        if (keyChar == null) return;
        if (intValuesOnly) {
            try {
                Integer.parseInt(keyChar);
            } catch (NumberFormatException _) {
                return;
            }
        }

        value = value.concat(keyChar);
        group.hasChanged = true;
    }

    public void fireCallbacks() {
        for (InputCallback callback : callbacks) callback.call(this, value);
    }

    public boolean isPointInBounds(Vec2 point) {
        return areaPos.x - areaMargin.x < point.x && point.x < areaPos.x + areaSize.x + areaMargin.x &&
                areaPos.y < point.y && point.y < areaPos.y + areaSize.y;
    }

    public void appendToBufferBuilder(BufferBuilder2f sb) {
        // title
        int titleHeight = 0;
        if (!title.isEmpty()) {
            titleHeight = (int) (font.glyphMap.get(' ').height * titleScale);
            float titleWidth = font.findLineWidth(title) * titleScale;

            float[] textFloats = new float[]{color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 0, 0};
            Vec2 linePos = pos.sub(titleWidth * .5f, 0);
            TextRenderer.pushTextToBuilder(sb, title, font, linePos, textFloats, titleScale);
        }

        // find area pos & size
        float valueHeight = (int) (font.glyphMap.get(' ').height * valueScale);
        float valueWidth = font.findLineWidth(value) * valueScale;
        areaPos = pos.add(-(valueWidth * .5f) - areaMargin.x * .5f, titleHeight + areaMargin.y);
        areaSize = new Vec2(valueWidth, valueHeight).add(areaMargin);

        // value
        if (!value.isEmpty()) {
            float[] textFloats = new float[]{color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 0, 0};
            Vec2 midOffset = areaSize.mul(.5f).sub(new Vec2(valueWidth, valueHeight).mul(.5f));
            TextRenderer.pushTextToBuilder(sb, value, font, areaPos.add(midOffset), textFloats, valueScale);
        }

        // value outline
        float[] outlineFloats = new float[]{-1, -1, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 0, 0};
        Shape2d.Poly outlinePoly = Shape2d.createRectOutline(areaPos, areaSize, 3, new ShapeMode.Append(outlineFloats));
        sb.pushSeparatedPolygon(outlinePoly);

        // hovering wobble
        if (wobbling) {
            float[] wobbleFloats = new float[]{-1, -1, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), selected ? .4f:1};
            List<float[]> wobbleIndexes = List.of(new float[]{0}, new float[]{1}, new float[]{2}, new float[]{3});
            Shape2d.Poly poly = Shape2d.createRect(areaPos, areaSize, new ShapeMode.AppendUnpack(wobbleFloats, wobbleIndexes));
            sb.pushSeparatedPolygon(poly);
        }
    }
}
