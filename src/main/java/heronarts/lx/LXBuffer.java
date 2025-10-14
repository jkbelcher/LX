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

/**
 * Generic buffer interface allows any underlying data type
 */
public interface LXBuffer<T> {

  /** Model has changed. Buffer should re-allocate to accommodate the new model size */
  public void initialize(LXModel model);

  public void clear(boolean startOfFrame);

  public void muteFixture(LXFixture fixture);

  public void colorFixture(LXFixture fixture, int color);

  /** Structure-level mute */
  public void mute();

  /** Structure-level all white */
  public void allWhite();

  public T getBuffer();

  // TODO: remove copyTo? It is not used.
  // public LXBuffer<T> copyTo(LXBuffer<T> that);

  public void copyFrom(T that);

  public default void dispose() { }

}
