/**
 * Copyright 2023- Mark C. Slee, Heron Arts LLC
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

package heronarts.lx.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.MutableParameter;
import heronarts.lx.parameter.NormalizedParameter;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory(LXCategory.AUDIO)
@LXModulator.Global("Sound Object")
public class SoundObject extends LXModulator implements Comparable<SoundObject>, LXOscComponent, LXNormalizedParameter {

  public enum Source {
    OSC("OSC"),
    AUDIO("Audio");

    public final String label;

    private Source(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  };

  public final EnumParameter<Source> sourceMode =
    new EnumParameter<Source>("Source", Source.OSC)
    .setDescription("Source of the sound object data");

  public final NormalizedParameter input =
    new NormalizedParameter("Input", 0)
    .setDescription("Raw input level of the sound object");

  public final BoundedParameter gain =
    new BoundedParameter("Gain", 0, -1, 1)
    .setUnits(BoundedParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Meter gain");

  public final BoundedParameter range =
    new BoundedParameter("Range", 1)
    .setUnits(BoundedParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Range of meter sensitivity");

  public final BoundedParameter attackMs =
    new BoundedParameter("Attack", 0, 0, 1000)
    .setDescription("Attack time of the smoothed meter in milliseconds")
    .setUnits(BoundedParameter.Units.MILLISECONDS_RAW);

  public final BoundedParameter releaseMs =
    new BoundedParameter("Release", 500, 0, 10000)
    .setDescription("Release time of the smoothed meter in milliseconds")
    .setUnits(BoundedParameter.Units.MILLISECONDS_RAW);

  public final CompoundParameter azimuth =
    new CompoundParameter("Azimuth", 0, -180, 180)
    .setUnits(CompoundParameter.Units.DEGREES)
    .setPolarity(CompoundParameter.Polarity.BIPOLAR)
    .setWrappable(true)
    .setDescription("Azimuth of the sound object, clockwise about the X-Z plane");

  public final CompoundParameter elevation =
    new CompoundParameter("Elevation", 0, -90, 90)
    .setUnits(CompoundParameter.Units.DEGREES)
    .setPolarity(CompoundParameter.Polarity.BIPOLAR)
    .setDescription("Elevation of the sound object against the X-Z plane");

  public final CompoundParameter radius =
    new CompoundParameter("Radius", 1, 0, Math.sqrt(3))
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Radius of the sound object");

  public final MutableParameter cartesianChanged =
    new MutableParameter("Cartesian Changed");

  private double x, y, z;

  private double prevAzimuth = 0, prevElevation = 0, prevRadius = 1;

  public static SoundObject get(LX lx) {
    for (LXModulator modulator : lx.engine.modulation.modulators) {
      if (modulator instanceof SoundObject) {
        return (SoundObject) modulator;
      }
    }
    return null;
  }

  public SoundObject(LX lx) {
    super("Sound Object");
    addParameter("sourceMode", this.sourceMode);
    addParameter("input", this.input);
    addParameter("gain", this.gain);
    addParameter("range", this.range);
    addParameter("attackMs", this.attackMs);
    addParameter("releaseMs", this.releaseMs);
    addParameter("azimuth", this.azimuth);
    addParameter("elevation", this.elevation);
    addParameter("radius", this.radius);
    updateCartesian();

    // A new sound object is created, register it with the audio engine
    // as an eligible target for the sound object selectors
    soundObjects.add(this);
    lx.engine.audio.numSoundObjects.setValue(LXUtils.max(
      lx.engine.audio.numSoundObjects.getValuei(),
      soundObjects.size()
    ));
    updateSelectors(lx);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);
    if (p == this.label) {
      // Note: only fire after LX is set and the modulator
      // has been added. Otherwise this is the initial
      // constructor setting the label...
      if (this.lx != null) {
        updateSelectors(this.lx);
      }
    }
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
  }

  public void updateCartesian() {
    final double radius = .5 * this.radius.getValue();
    final double azimuth = Math.toRadians(this.azimuth.getValue());
    final double elevation = Math.toRadians(this.elevation.getValue());
    final double sinAzim = Math.sin(azimuth);
    final double cosAzim = Math.cos(azimuth);
    final double cosElev = Math.cos(elevation);
    final double sinElev = Math.sin(elevation);

    this.x = LXUtils.constrain(.5 + radius * sinAzim * cosElev, 0, 1);
    this.z = LXUtils.constrain(.5 + radius * cosAzim * cosElev, 0, 1);
    this.y = LXUtils.constrain(.5 + radius * sinElev, 0, 1);

    this.cartesianChanged.bang();
  }

  @Override
  protected double computeValue(double deltaMs) {
    final double azimuth = this.azimuth.getValue();
    final double elevation = this.elevation.getValue();
    final double radius = this.radius.getValue();
    if (azimuth != this.prevAzimuth ||
        elevation != this.prevElevation ||
        radius != this.prevRadius) {
      this.prevAzimuth = azimuth;
      this.prevElevation = elevation;
      this.prevRadius = radius;
      updateCartesian();
    }

    if (this.sourceMode.getEnum() == Source.AUDIO) {
      this.input.setValue(getLX().engine.audio.meter.getNormalized());
    }
    final double range = this.range.getValue();

    final double value = LXUtils.constrain(
      LXUtils.ilerp(this.input.getValue() + this.gain.getValue(), 1 - range, 1),
      0, 1
    );

    final double currentLevel = getValue();
    if (value > currentLevel) {
      final double attackMs = this.attackMs.getValue();
      if (attackMs > 0) {
        return LXUtils.min(value, currentLevel + deltaMs / attackMs);
      }
    } else {
      final double releaseMs = this.releaseMs.getValue();
      if (releaseMs > 0) {
        return LXUtils.max(value, currentLevel - deltaMs / releaseMs);
      }
    }
    return value;
  }

  @Override
  public LXNormalizedParameter setNormalized(double value) {
    throw new UnsupportedOperationException("Cannot setNormalized on SoundObject");
  }

  @Override
  public double getNormalized() {
    return getValue();
  }

  @Override
  public int compareTo(SoundObject that) {
    return this.getLabel().compareTo(that.getLabel());
  }

  @Override
  public void dispose() {
    soundObjects.remove(this);
    int numObjects = this.lx.engine.audio.numSoundObjects.getValuei();
    if (numObjects <= 0) {
      LX.error("LXAidioEngine sound object count was already 0 upon disposal: " + this);
    } else {
      this.lx.engine.audio.numSoundObjects.setValue(numObjects-1);
    }
    updateSelectors(this.lx);
    super.dispose();
  }

  private static final String NO_OBJECT = "None";
  private static List<SoundObject> soundObjects = new ArrayList<SoundObject>();
  private static SoundObject[] objects = { null };
  private static String[] options = { NO_OBJECT };
  private static List<Selector> selectors = new ArrayList<Selector>();

  static void updateSelectors(LX lx) {
    Collections.sort(soundObjects);

    int numOptions = 1 + lx.engine.audio.numSoundObjects.getValuei();
    objects = new SoundObject[numOptions];
    options = new String[numOptions];
    objects[0] = null;
    options[0] = NO_OBJECT;
    int i = 1;
    for (SoundObject soundObject : soundObjects) {
      objects[i] = soundObject;
      options[i] = soundObject.getLabel();
      ++i;
    }

    // Update all of the selectors to have new range/options
    for (Selector selector : selectors) {
      // Check if a selector had a non-null selection, if so
      // it should be restored in the case of renaming/reordering
      // where it is still in the list but its index may be different
      final SoundObject selected = selector.getObject();
      selector.setObjects(objects, options);
      if ((selected != null) && soundObjects.contains(selected)) {
        selector.setValue(selected);
      } else {
        selector.bang();
      }
    }
  }

  public static class Selector extends ObjectParameter<SoundObject> {

    public Selector(String label) {
      super(label, objects, options);
      selectors.add(this);
    }

    @Override
    public void dispose() {
      selectors.remove(this);
      super.dispose();
    }

  }

}
