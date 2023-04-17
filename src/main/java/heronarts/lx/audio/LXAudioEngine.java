/**
 * Copyright 2016- Mark C. Slee, Heron Arts LLC
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

import com.google.gson.JsonObject;

import heronarts.lx.LX;
import heronarts.lx.LXModulatorComponent;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.MutableParameter;

public class LXAudioEngine extends LXModulatorComponent implements LXOscComponent {

  public final BooleanParameter enabled =
    new BooleanParameter("Enabled", false)
    .setDescription("Sets whether the audio engine is active");

  public final BooleanParameter expandedPerformance =
    new BooleanParameter("Enabled", true)
    .setDescription("Whether the audio pane is expanded in performance mode");

  /**
   * Sound objects are modulators, which are restored *after* the mixer and channel
   * engines. However, patterns + effects etc. may make reference to SoundObject.Selector,
   * which needs to know the total number of sound objects that are going to exist. So we
   * track and restore that number here. When the sound objects are actually instantiated,
   * their names will be restored to the selector.
   */
  final MutableParameter numSoundObjects = (MutableParameter)
    new MutableParameter("Sound Objects", 0)
    .setDescription("Number of registered sound objects");

  /**
   * Audio input object
   */
  public final LXAudioInput input;

  public final LXAudioOutput output;

  public final GraphicMeter meter;

  public enum Mode {
    INPUT,
    OUTPUT
  };

  public final EnumParameter<Mode> mode = new EnumParameter<Mode>("Mode", Mode.INPUT);

  public LXAudioEngine(LX lx) {
    super(lx, "Audio");
    addParameter("enabled", this.enabled);
    addParameter("mode", this.mode);
    addParameter("expandedPerformance", this.expandedPerformance);
    addInternalParameter("numSoundObjects", this.numSoundObjects);

    this.mode.setOptions(new String[] { "Input", "Output" });

    this.input = new LXAudioInput(lx);
    this.output = new LXAudioOutput(lx);
    this.meter = new GraphicMeter("Meter", this.input.mix);

    addChild("input", this.input);
    addChild("output", this.output);
    addModulator("meter", this.meter);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.enabled) {
      if (this.enabled.isOn()) {
        this.input.open();
        this.input.start();
        // TODO(mcslee): start/stop output?
      } else {
        this.input.stop();
        // TODO(mcslee): start/stop output?
      }
      this.meter.running.setValue(this.enabled.isOn());
    } else if (p == this.mode) {
      switch (this.mode.getEnum()) {
      case INPUT: this.meter.setBuffer(this.input.mix); break;
      case OUTPUT: this.meter.setBuffer(this.output.mix); break;
      }
    }
  }

  /**
   * Retrieves the audio input object at default sample rate of 44.1kHz
   *
   * @return Audio input object
   */
  public final LXAudioInput getInput() {
    return this.input;
  }

  @Override
  public void dispose() {
    this.input.close();
    this.output.close();
    super.dispose();
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    this.output.reset();
    this.numSoundObjects.setValue(0);
    super.load(lx, obj);
    SoundObject.updateSelectors(lx);
  }

}
