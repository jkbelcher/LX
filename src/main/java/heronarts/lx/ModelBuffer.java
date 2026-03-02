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

import heronarts.lx.model.LXModel;
import heronarts.lx.structure.LXFixture;

public class ModelBuffer {

  private final LX lx;

  private final LXPointBuffer pointsBuffer;
  private final LXDmxBuffer dmxBuffer;

  private final boolean listenForModelChanges;
  private final LX.Listener modelListener = new LX.Listener() {
    @Override
    public void modelChanged(LX lx, LXModel model) {
      setModel(model);
    }
  };

  public ModelBuffer(LX lx) {
    this(lx, true);
  }

  public ModelBuffer(LX lx, boolean listenForModelChanges) {
    this.lx = lx;
    this.listenForModelChanges = listenForModelChanges;

    // Allocate buffer for LXPoints
    this.pointsBuffer = new LXPointBuffer();
    this.pointsBuffer.setModel(lx.model);

    // Allocate buffer for DMX fixtures
    this.dmxBuffer = new LXDmxBuffer();
    this.dmxBuffer.setModel(lx.model);

    if (listenForModelChanges) {
      lx.addListener(this.modelListener);
    }
  }

  public void setModel(LXModel model) {
    this.pointsBuffer.setModel(model);
    this.dmxBuffer.setModel(model);
  }

  public void clear(boolean startOfFrame) {
    this.pointsBuffer.clear(startOfFrame);
    this.dmxBuffer.clear(startOfFrame);
  }

  public void muteFixture(LXFixture fixture) {
    this.pointsBuffer.muteFixture(fixture);
    this.dmxBuffer.muteFixture(fixture);
  }

  public void colorFixture(LXFixture fixture, int color) {
    this.pointsBuffer.colorFixture(fixture, color);
    this.dmxBuffer.colorFixture(fixture, color);
  }

  /** Structure-level mute */
  public void mute() {
    this.pointsBuffer.mute();
    this.dmxBuffer.mute();
  }

  /** Structure-level all white */
  public void allWhite() {
    this.pointsBuffer.allWhite();
    this.dmxBuffer.allWhite();
  }

  /** Get the standard points buffer */
  public int[] getColors() {
    return this.pointsBuffer.getBuffer();
  }

  /** Get the dmx buffer */
  public float[] getDmx() {
    return this.dmxBuffer.getBuffer();
  }

  public ModelBuffer copyFrom(ModelBuffer that) {
    this.pointsBuffer.copyFrom(that.pointsBuffer);
    this.dmxBuffer.copyFrom(that.dmxBuffer);
    return this;
  }

  public void dispose() {
    if (this.listenForModelChanges) {
      this.lx.removeListener(this.modelListener);
    }
  }

}
