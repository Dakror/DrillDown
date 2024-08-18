package de.dakror.quarry.structure.base;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.util.SpriterDelegateBatch;

public abstract class PausableStructure<T extends PausableSchema> extends Structure<T> {
    public static final TextureRegion sleepingTex = Quarry.Q.atlas.findRegion("state_sleeping");

    protected boolean sleeping;

    protected PausableStructure(int x, int y, T schema) {
        super(x, y, schema);
    }

    @Override
    public boolean getButtonState(int buttonIndex) {
        if (buttonIndex == 0)
            return sleeping;
        return false;
    }

    public void setSleeping(boolean sleeping) {
        this.sleeping = sleeping;
    }

    public boolean isSleeping() {
        return sleeping;
    }

    @Override
    public float getLoudness() {
        return (sleeping ? 0.1f : 1) * super.getLoudness();
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (sleeping) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(sleepingTex, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f, Const.Z_STATES, size, size);
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Byte("sleeping", (byte) (sleeping ? 1 : 0));
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        sleeping = tag.Byte("sleeping", (byte) 0) == 1;
    }
}
