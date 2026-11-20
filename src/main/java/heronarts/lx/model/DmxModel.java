package heronarts.lx.model;

import heronarts.lx.LXDmxBuffer;

public abstract class DmxModel {

  public final float[] defaultBuffer;

  protected DmxModel(float[] defaultBuffer) {
    this.defaultBuffer = defaultBuffer;
  }

  public abstract int getIndexBufferOffset();

  public abstract void mute(LXDmxBuffer buffer);

}
