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

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.structure.LXFixture;

import java.util.Arrays;

public class LXPointBuffer implements LXBuffer<int[]> {

  private static final int DEFAULT_COLOR = 0;
  private static final int TRANSPARENT_COLOR = 0;
  private static final int MUTE_COLOR = LXColor.BLACK;

  private int[] buffer = new int[0];

  public LXPointBuffer() { }

  @Override
  public void initialize(LXModel model) {
    if (this.buffer.length != model.size) {
      this.buffer = new int[model.size];
      Arrays.fill(this.buffer, DEFAULT_COLOR);
    }
  }

  @Override
  public void clear(boolean startOfFrame) {
    Arrays.fill(this.buffer, startOfFrame ? TRANSPARENT_COLOR : LXColor.BLACK);
  }

  @Override
  public void muteFixture(LXFixture fixture) {
    colorFixture(fixture, MUTE_COLOR);
  }

  @Override
  public void colorFixture(LXFixture fixture, int color) {
    int start = fixture.getIndexBufferOffset();
    int end = start + fixture.totalSize();
    for (int i = start; i < end; ++i) {
      this.buffer[i] = color;
    }
  }

  public void mute() {
    Arrays.fill(this.buffer, MUTE_COLOR);
  }

  public void allWhite() {
    Arrays.fill(this.buffer, LXColor.WHITE);
  }

  public int[] getBuffer() {
    return this.buffer;
  }

  @Override
  public void copyFrom(int[] that) {
    System.arraycopy(that, 0, this.buffer, 0, this.buffer.length);
  }
}
