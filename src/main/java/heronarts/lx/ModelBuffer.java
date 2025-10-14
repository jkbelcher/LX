/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx;

import java.util.ArrayList;
import java.util.List;

import heronarts.lx.model.LXModel;
import heronarts.lx.structure.LXFixture;

public class ModelBuffer {

  // Registration of custom buffers
  private static final List<LXBufferSource> bufferSources = new ArrayList<>();
  private static boolean isRunning = false;

  /**
   * Registers a new buffer source. Only available at application start
   * before the first buffer is created.
   * @param bufferSource A class that can allocate new buffer instances
   * @return index of this buffer type in buffers[] array
   */
  public static int registerBuffer(LXBufferSource bufferSource) {
    if (isRunning) {
      throw new IllegalStateException("New buffer types can only be registered at application startup");
    }
    bufferSources.add(bufferSource);
    return bufferSources.indexOf(bufferSource);
  }

  private final LX lx;

  // Standard buffer
  private final LXPointBuffer pointsBuffer;

  // Custom buffers
  private final LXBuffer[] buffers;

  private final LX.Listener modelListener = new LX.Listener() {
    @Override
    public void modelChanged(LX lx, LXModel model) {
      setModel(model);
    }
  };

  public ModelBuffer(LX lx) {
    this.lx = lx;

    // Prevent registration of other custom buffers
    isRunning = true;

    // Allocate points buffer
    this.pointsBuffer = new LXPointBuffer();
    this.pointsBuffer.initialize(lx.model);

    // Allocate custom buffers
    final int numBuffers = bufferSources.size();
    this.buffers = new LXBuffer[numBuffers];
    for (int i = 0; i < numBuffers; i++) {
      LXBufferSource bufferSource = bufferSources.get(i);
      LXBuffer buffer = bufferSource.createBuffer();
      buffer.initialize(lx.model);
      this.buffers[i] = buffer;
    }

    lx.addListener(this.modelListener);
  }

  // TODO: should we avoid listening to lx.modelChanged for ModelBuffers that are owned by Frame?
  public void setModel(LXModel model) {
    this.pointsBuffer.initialize(model);
    for (LXBuffer buffer : this.buffers) {
      buffer.initialize(model);
    }
  }

  public void clear(boolean startOfFrame) {
    this.pointsBuffer.clear(startOfFrame);
    for (LXBuffer buffer : this.buffers) {
      buffer.clear(startOfFrame);
    }
  }

  public void muteFixture(LXFixture fixture) {
    this.pointsBuffer.muteFixture(fixture);
    for (LXBuffer buffer : this.buffers) {
      buffer.muteFixture(fixture);
    }
  }

  public void colorFixture(LXFixture fixture, int color) {
    this.pointsBuffer.colorFixture(fixture, color);
    for (LXBuffer buffer : this.buffers) {
      buffer.colorFixture(fixture, color);
    }
  }

  /** Structure-level mute */
  public void mute() {
    this.pointsBuffer.mute();
    for (LXBuffer buffer : this.buffers) {
      buffer.mute();
    }
  }

  /** Structure-level all white */
  public void allWhite() {
    this.pointsBuffer.allWhite();
    for (LXBuffer buffer : this.buffers) {
      buffer.allWhite();
    }
  }

  /** Get the standard points buffer */
  public int[] getColors() {
    return this.pointsBuffer.getBuffer();
  }

  /** Get a custom buffer by index */
  public Object getBuffer(int index) {
    return this.buffers[index].getBuffer();
  }

  public ModelBuffer copyFrom(ModelBuffer that) {
    this.pointsBuffer.copyFrom(that.pointsBuffer.getBuffer());

    for (int i = 0; i < this.buffers.length; i++) {
      this.buffers[i].copyFrom(that.buffers[i].getBuffer());
    }
    return this;
  }

  public void dispose() {
    this.lx.removeListener(this.modelListener);

    // Dispose standard buffer
    this.pointsBuffer.dispose();

    // Dispose custom buffers
    for (LXBuffer buffer : this.buffers) {
      buffer.dispose();
    }
  }

}
