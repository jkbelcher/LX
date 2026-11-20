package heronarts.lx.structure;

import heronarts.lx.LXDmxBuffer;

public interface DmxFixture {

  public void mute(LXDmxBuffer buffer);

  public void setColor(LXDmxBuffer buffer, int color);

}
