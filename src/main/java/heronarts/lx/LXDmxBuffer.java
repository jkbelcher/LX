package heronarts.lx;

import heronarts.lx.model.LXModel;
import heronarts.lx.structure.LXFixture;

public class LXDmxBuffer implements LXBuffer<float[]> {

  private float[] buffer = new float[0];

  private LXModel model;

  public LXDmxBuffer() { }

  @Override
  public void setModel(LXModel model) { // only called with top-level model containing all DMX fixtures
    this.model = model;

    // Pseudo code
    int dmxLength = 0;
    for (DmxModel dmxModel : model.dmxModels) { //
      dmxLength += dmxModel.defaultBuffer.length;
    }
    if (this.buffer.length != dmxLength) {
      this.buffer = new float[dmxLength];
    }
    for (DmxModel dmxModel : model.dmxModels) {
      System.arraycopy(dmxModel.defaultBuffer, 0, this.buffer, dmxModel.getIndexBufferOffset(), dmxModel.defaultBuffer.length);
    }
  }

  @Override
  public void clear(boolean startOfFrame) {
    for (DmxModel dmxModel : this.model.dmxModels) {
      System.arraycopy(dmxModel.defaultBuffer, 0, this.buffer, dmxModel.getIndexBufferOffset(), dmxModel.defaultBuffer.length);
    }
  }

  @Override
  public void muteFixture(LXFixture fixture) {
    // Muting behavior varies by fixture type. It could be a shutter close or a zero brightness.
    if (fixture instanceof DmxFixture dmxFixture) {
      dmxFixture.mute(this);
    }
  }

  @Override
  public void colorFixture(LXFixture fixture, int color) {
    // Color behavior varies by fixture type
    if (fixture instanceof DmxFixture dmxFixture) {
      dmxFixture.setColor(this, color);
    }
  }

  @Override
  public void mute() {
    for (DmxModel dmxModel : this.model.dmxModels) {
      dmxModel.mute(this);
    }
  }

  @Override
  public void allWhite() {
    // TODO
  }

  @Override
  public float[] getBuffer() {
    return this.buffer;
  }

  @Override
  public void copyFrom(LXBuffer<float[]> that) {
    System.arraycopy(that.getBuffer(), 0, this.buffer, 0, this.buffer.length);
  }

}
