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

package heronarts.lx.pattern.color;

import heronarts.lx.LXCategory;
import heronarts.lx.LX;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.GradientUtils.BlendMode;
import heronarts.lx.color.GradientUtils.ColorStops;
import heronarts.lx.color.GradientUtils.GradientFunction;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.transform.LXParameterizedMatrix;
import heronarts.lx.utils.LXUtils;

@LXCategory(LXCategory.COLOR)
public class GradientPattern extends LXPattern implements GradientFunction {

  public enum ColorMode {
    FIXED("Fixed"),
    PRIMARY("Primary"),
    PALETTE("Palette");

    public final String label;

    private ColorMode(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  };

  private interface CoordinateFunction {
    float getCoordinate(LXPoint p, float normalized, float offset);
  }

  public static enum CoordinateMode {

    NORMAL("Normal", (p, normalized, offset) ->  {
      return normalized - offset;
    }),

    CENTER("Center", (p, normalized, offset) -> {
      return 2 * Math.abs(normalized - (.5f + offset * .5f));
    }),

    RADIAL("Radial", (p, normalized, offset) -> {
      return p.rcn - offset;
    });

    public final String name;
    public final CoordinateFunction function;
    public final CoordinateFunction invert;

    private CoordinateMode(String name, CoordinateFunction function) {
      this.name = name;
      this.function = function;
      this.invert = (p, normalized, offset) -> { return function.getCoordinate(p, normalized, offset) - 1; };
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public final EnumParameter<ColorMode> colorMode =
    new EnumParameter<ColorMode>("Color Mode", ColorMode.PRIMARY)
    .setDescription("Which source the gradient selects colors from");

  public final EnumParameter<BlendMode> blendMode =
    new EnumParameter<BlendMode>("Blend Mode", BlendMode.HSV)
    .setDescription("How to blend between colors in the gradient");

  public final ColorParameter fixedColor =
    new ColorParameter("Fixed", LXColor.RED)
    .setDescription("Fixed color to start the gradient from");

  public final ColorParameter secondaryColor =
    new ColorParameter("Secondary", LXColor.RED)
    .setDescription("Secondary color that the gradient blends to");

  public final DiscreteParameter paletteIndex =
    new DiscreteParameter("Index", 1, LXSwatch.MAX_COLORS + 1)
    .setDescription("Which index at the palette to start from");

  public final DiscreteParameter paletteStops =
    new DiscreteParameter("Stops", LXSwatch.MAX_COLORS, 2, LXSwatch.MAX_COLORS + 1)
    .setDescription("How many color stops to use in the palette");

  public final CompoundParameter gradient =
    new CompoundParameter("Amount", 0, -1, 1)
    .setDescription("Amount of color gradient")
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter gradientRange =
    new CompoundParameter("Range", 360, 0, 360 * 10)
    .setUnits(CompoundParameter.Units.DEGREES)
    .setDescription("Range of total possible color hue gradient");

  public final CompoundParameter saturationRange =
    new CompoundParameter("Saturation Range", 0, 100)
    .setUnits(CompoundParameter.Units.PERCENT)
    .setDescription("Range of total possible saturation gradient");

  public final CompoundParameter brightnessRange =
    new CompoundParameter("Brightness Range", 0, 100)
    .setUnits(CompoundParameter.Units.PERCENT)
    .setDescription("Range of total possible brightness gradient");

  public final EnumParameter<CoordinateMode> xMode =
    new EnumParameter<CoordinateMode>("X Mode", CoordinateMode.NORMAL)
    .setDescription("Which coordinate mode the X-dimension uses");

  public final EnumParameter<CoordinateMode> yMode =
    new EnumParameter<CoordinateMode>("Y Mode", CoordinateMode.NORMAL)
    .setDescription("Which coordinate mode the Y-dimension uses");

  public final EnumParameter<CoordinateMode> zMode =
    new EnumParameter<CoordinateMode>("Z Mode", CoordinateMode.NORMAL)
    .setDescription("Which coordinate mode the Z-dimension uses");

  public final CompoundParameter xAmount =
    new CompoundParameter("X-Amt", 0, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sets the amount of hue spread on the X axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter yAmount =
    new CompoundParameter("Y-Amt", 0, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sets the amount of hue spread on the Y axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter zAmount =
    new CompoundParameter("Z-Amt", 0, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sets the amount of hue spread on the Z axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter xOffset =
    new CompoundParameter("X-Off", 0, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sets the offset of the hue spread point on the X axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter yOffset =
    new CompoundParameter("Y-Off", 0, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sets the offset of the hue spread point on the Y axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final CompoundParameter zOffset =
    new CompoundParameter("Z-Off", 0, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sets the offset of the hue spread point on the Z axis")
    .setPolarity(LXParameter.Polarity.BIPOLAR);

  public final BooleanParameter rotate =
    new BooleanParameter("Rotate", false)
    .setDescription("Whether to rotate the geometry");

  public final CompoundParameter yaw = new CompoundParameter("Yaw", 0, 360)
  .setWrappable(true)
  .setUnits(CompoundParameter.Units.DEGREES)
  .setDescription("Yaw rotation");

  public final CompoundParameter pitch = new CompoundParameter("Pitch", 0, 360)
  .setWrappable(true)
  .setUnits(CompoundParameter.Units.DEGREES)
  .setDescription("Pitch rotation");

  public final CompoundParameter roll = new CompoundParameter("Roll", 0, 360)
  .setWrappable(true)
  .setUnits(CompoundParameter.Units.DEGREES)
  .setDescription("Roll rotation");

  private final LXParameterizedMatrix transform = new LXParameterizedMatrix();

  private final ColorStops colorStops = new ColorStops();

  public GradientPattern(LX lx) {
    super(lx);
    addParameter("xAmount", this.xAmount);
    addParameter("yAmount", this.yAmount);
    addParameter("zAmount", this.zAmount);
    addParameter("xOffset", this.xOffset);
    addParameter("yOffset", this.yOffset);
    addParameter("zOffset", this.zOffset);
    addParameter("colorMode", this.colorMode);
    addParameter("blendMode", this.blendMode);
    addParameter("gradient", this.gradient);
    addParameter("fixedColor", this.fixedColor);
    addParameter("xMode", this.xMode);
    addParameter("yMode", this.yMode);
    addParameter("zMode", this.zMode);
    addParameter("paletteIndex", this.paletteIndex);
    addParameter("paletteStops", this.paletteStops);
    addParameter("gradientRange", this.gradientRange);
    addParameter("saturationRange", this.saturationRange);
    addParameter("brightnessRange", this.brightnessRange);
    addTransformParameter("rotate", this.rotate);
    addTransformParameter("yaw", this.yaw);
    addTransformParameter("pitch", this.pitch);
    addTransformParameter("roll", this.roll);
  }

  private void addTransformParameter(String key, LXParameter parameter) {
    addParameter(key, parameter);
    this.transform.addParameter(parameter);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (this.gradient == p ||
        this.gradientRange == p ||
        this.saturationRange == p ||
        this.brightnessRange == p ||
        this.colorMode == p ||
        this.paletteIndex == p ||
        this.fixedColor.hue == p ||
        this.fixedColor.saturation == p ||
        this.fixedColor.brightness == p) {
      setSecondaryColor();
    }
  }

  public LXDynamicColor getPrimaryColor() {
    return this.lx.engine.palette.getSwatchColor(this.paletteIndex.getValuei() - 1);
  }

  private void setSecondaryColor() {
    final double gradient = this.gradient.getValue();
    final double hueGradient = gradient * this.gradientRange.getValue();
    final double satGradient = gradient * this.saturationRange.getValue();
    final double brtGradient = gradient * this.brightnessRange.getValue();

    switch (this.colorMode.getEnum()) {
    case FIXED:
      this.secondaryColor.setColor(LXColor.hsb(
        this.fixedColor.hue.getValue() + hueGradient,
        LXUtils.constrain(this.fixedColor.saturation.getValue() + satGradient, 0, 100),
        LXUtils.constrain(this.fixedColor.brightness.getValue() + brtGradient, 0, 100)
      ));
      break;
    case PRIMARY:
      LXDynamicColor primary = getPrimaryColor();
      int c = primary.getColor();
      this.secondaryColor.setColor(LXColor.hsb(
        primary.getHue() + hueGradient,
        LXUtils.constrain(LXColor.s(c) + satGradient, 0, 100),
        LXUtils.constrain(LXColor.b(c) + brtGradient, 0, 100)
      ));
      break;
    default:
      break;
    }
  }

  private void setColorStops() {
    final float gradientf = this.gradient.getValuef();
    final float huef = gradientf * this.gradientRange.getValuef();
    final float satf = gradientf * this.saturationRange.getValuef();
    final float brtf = gradientf * this.brightnessRange.getValuef();

    switch (this.colorMode.getEnum()) {
    case FIXED:
      this.colorStops.stops[0].set(this.fixedColor);
      this.colorStops.stops[1].set(this.fixedColor, huef, satf, brtf);
      this.colorStops.setNumStops(2);
      break;
    case PRIMARY:
      LXDynamicColor swatchColor = getPrimaryColor();
      this.colorStops.stops[0].set(swatchColor);
      this.colorStops.stops[1].set(swatchColor, huef, satf, brtf);
      this.colorStops.setNumStops(2);
      setSecondaryColor();
      break;
    case PALETTE:
      this.colorStops.setPaletteGradient(
        this.lx.engine.palette,
        this.paletteIndex.getValuei() - 1,
        this.paletteStops.getValuei()
      );
      break;
    }
  }

  @Override
  public int getGradientColor(float lerp) {
    return this.colorStops.getColor(lerp, this.blendMode.getEnum().function);
  }

  @Override
  public void run(double deltaMs) {
    setColorStops();

    float xAmount = this.xAmount.getValuef();
    float yAmount = this.yAmount.getValuef();
    float zAmount = this.zAmount.getValuef();

    final float total = Math.abs(xAmount) + Math.abs(yAmount) + Math.abs(zAmount);
    if (total > 1) {
      xAmount /= total;
      yAmount /= total;
      zAmount /= total;
    }

    final float xOffset = this.xOffset.getValuef();
    final float yOffset = this.yOffset.getValuef();
    final float zOffset = this.zOffset.getValuef();

    final CoordinateMode xMode = this.xMode.getEnum();
    final CoordinateMode yMode = this.yMode.getEnum();
    final CoordinateMode zMode = this.zMode.getEnum();

    final CoordinateFunction xFunction = (xAmount < 0) ? xMode.invert : xMode.function;
    final CoordinateFunction yFunction = (yAmount < 0) ? yMode.invert : yMode.function;
    final CoordinateFunction zFunction = (zAmount < 0) ? zMode.invert : zMode.function;

    final GradientUtils.BlendFunction blendFunction = this.blendMode.getEnum().function;

    if (this.rotate.isOn()) {
      this.transform.update(matrix -> {
        matrix
          .translate(.5f, .5f, .5f)
          .rotateZ((float) Math.toRadians(-this.roll.getValue()))
          .rotateX((float) Math.toRadians(-this.pitch.getValue()))
          .rotateY((float) Math.toRadians(-this.yaw.getValue()))
          .translate(-.5f, -.5f, -.5f);
      });

      for (LXPoint p : model.points) {
        final float xn = this.transform.xn(p);
        final float yn = this.transform.yn(p);
        final float zn = this.transform.zn(p);

        float lerp = (this.colorStops.numStops - 1) * LXUtils.clampf(
          xAmount * xFunction.getCoordinate(p, xn, xOffset) +
          yAmount * yFunction.getCoordinate(p, yn, yOffset) +
          zAmount * zFunction.getCoordinate(p, zn, zOffset),
          0, 1
        );
        int stop = (int) Math.floor(lerp);
        colors[p.index] = blendFunction.blend(this.colorStops.stops[stop], this.colorStops.stops[stop+1], lerp - stop);
      }
    } else {
      for (LXPoint p : model.points) {
        float lerp = (this.colorStops.numStops - 1) * LXUtils.clampf(
          xAmount * xFunction.getCoordinate(p, p.xn, xOffset) +
          yAmount * yFunction.getCoordinate(p, p.yn, yOffset) +
          zAmount * zFunction.getCoordinate(p, p.zn, zOffset),
          0, 1
        );
        int stop = (int) Math.floor(lerp);
        colors[p.index] = blendFunction.blend(this.colorStops.stops[stop], this.colorStops.stops[stop+1], lerp - stop);
      }
    }
  }
}
