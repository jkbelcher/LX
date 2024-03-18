package heronarts.lx.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.modulator.Smoother;
import heronarts.lx.output.LXOutput.GammaTable;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameter.Units;

/**
 * Restricts luminosity of a channel when it exceeds a user-defined threshold.
 * Includes adjustable weights for R, G, B.
 *
 * @author Justin K. Belcher <justin@jkb.studio>
 */
@LXCategory(LXCategory.COLOR)
public class LuminosityEffect extends LXEffect implements UIDeviceControls<LuminosityEffect> {

  public BoundedParameter limit =
    new BoundedParameter("Limit", 1)
    .setDescription("Luminosity limit as a percentage of full white")
    .setUnits(Units.PERCENT_NORMALIZED);

  public final BooleanParameter postFader =
    new BooleanParameter("Post Fader", true)
    .setDescription("Take into account the dimming applied by channel fader");

  public final BoundedParameter rWeight =
    new BoundedParameter("RWeight", .299)
    .setDescription("Weight of Red in luminosity calculation");

  public final BoundedParameter gWeight =
    new BoundedParameter("GWeight", .587)
    .setDescription("Weight of Red in luminosity calculation");

  public final BoundedParameter bWeight =
    new BoundedParameter("BWeight", .114)
    .setDescription("Weight of Red in luminosity calculation");

  public final BoundedParameter gamma = (BoundedParameter)
    new BoundedParameter("Gamma", 1, 1, 4)
    .setMappable(false)
    .setOscMode(BoundedParameter.OscMode.ABSOLUTE)
    .setDescription("This setting should match any gamma correction that will be applied further down the chain, such as on a hardware controller");

  // Pass-through to Smoother
  public final BoundedParameter windowRangeMs =
    new BoundedParameter("Smooth", 4000, 100, 30000)
    .setUnits(BoundedParameter.Units.MILLISECONDS)
    .setDescription("Time in milliseconds over which to smooth the limiting");

  private final Smoother smoother = new Smoother();

  public BoundedParameter input =
    new BoundedParameter("Input")
    .setDescription("Input level (luminosity of current frame) (read only)")
    .setUnits(Units.PERCENT_NORMALIZED);

  public BoundedParameter clip =
    new BoundedParameter("Clip")
    .setDescription("The percentage being clipped (read only)")
    .setUnits(Units.PERCENT_NORMALIZED);

  public LuminosityEffect(LX lx) {
    super(lx);

    addParameter("limit", this.limit);
    addParameter("postFader", this.postFader);
    addParameter("rWeight", this.rWeight);
    addParameter("gWeight", this.gWeight);
    addParameter("bWeight", this.bWeight);
    addParameter("gamma", this.gamma);
    addParameter("windowRangeMs", this.windowRangeMs);
    addParameter("input", this.input);
    addParameter("clip", this.clip);

    buildGammaTable();
    updateSmoother();
    startModulator(this.smoother);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.gamma) {
      buildGammaTable();
    } else if (p == this.windowRangeMs) {
      updateSmoother();
    }
  }

  private void updateSmoother() {
    this.smoother.windowRangeMs.setValue(this.windowRangeMs.getValue());
  }

  // TODO: Use core gamma table methods

  private GammaTable gammaLut = null;

  private static final double INV_255_2 = 1. / (255. * 255.);
  private static final double INV_255_3 = 1. / (255. * 255. * 255.);

  private void buildGammaTable() {
    if (this.gammaLut == null) {
      this.gammaLut = new GammaTable();
    }

    final double gamma = this.gamma.getValue();
    final double whitePointRed = 255;
    final double whitePointGreen = 255;
    final double whitePointBlue = 255;
    final double whitePointWhite = 255;
    for (int b = 0; b < 256; ++b) {
      generate(this.gammaLut.level[b].red, b, gamma, whitePointRed);
      generate(this.gammaLut.level[b].green, b, gamma, whitePointGreen);
      generate(this.gammaLut.level[b].blue, b, gamma, whitePointBlue);
      generate(this.gammaLut.level[b].white, b, gamma, whitePointWhite);
    }
  }

  private static void generate(byte[] output, int b, double gamma, double whitePoint) {
    if (gamma == 1) {
      for (int in = 0; in < 256; ++in) {
        output[in] = (byte) (0xff & (int) Math.round(in * b * whitePoint * INV_255_2));
      }
    } else {
      for (int in = 0; in < 256; ++in) {
        output[in] = (byte) (0xff & (int) Math.round(Math.pow(in * b * whitePoint * INV_255_3, gamma) * 255.f));
      }
    }
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    if (this.colors.length == 0) {
      return;
    }

    final float rWeight = this.rWeight.getValuef();
    final float gWeight = this.gWeight.getValuef();
    final float bWeight = this.bWeight.getValuef();
    final float totalWeight = rWeight + gWeight + bWeight;

    final boolean postFader = this.postFader.getValueb();
    final LXBus bus = this.getBus();
    final float fader = bus != null ? bus.fader.getNormalizedf() : 1f;

    // Take into account gamma correction settings applied later in the chain (such as on hardware controller)
    final int glutIndex = (int) Math.round(255. * (postFader ? fader : 1));
    final GammaTable.Curve gamma = this.gammaLut.level[glutIndex];

    // Sum luminosity of all points in frame
    float input = 0;
    if (totalWeight > 0) {
      int ri, gi, bi;
      float r, g, b;
      for (int i = 0; i < this.colors.length; i++) {
        ri = ((this.colors[i] & LXColor.R_MASK) >> LXColor.R_SHIFT);
        gi = ((this.colors[i] & LXColor.G_MASK) >> LXColor.G_SHIFT);
        bi = (this.colors[i] & LXColor.B_MASK);

        /*
        // Apply fader before gamma correction?
        if (postFader) {
          r *= fader;
          g *= fader;
          b *= fader;
        }*/

        r = Byte.toUnsignedInt(gamma.red[ri]);
        g = Byte.toUnsignedInt(gamma.green[gi]);
        b = Byte.toUnsignedInt(gamma.blue[bi]);

        // Scale to normalized
        r /= 255.;
        g /= 255.;
        b /= 255.;

        input += (r * rWeight + g * gWeight + b * bWeight);
      }
      input /= totalWeight;
      input /= this.colors.length;
    }
    this.input.setValue(input);

    final float limit = this.limit.getValuef();
    final float multiplier = input > limit ? limit / input : 1;
    this.smoother.input.setValue(multiplier);
    final float multiplierSmoothed = this.smoother.getValuef();
    this.clip.setValue(1 - multiplierSmoothed);

    if (multiplierSmoothed < 1) {
      for (int i=0; i < this.colors.length; i++) {
        this.colors[i] = LXColor.scaleBrightness(this.colors[i], multiplierSmoothed);
      }
    }
  }

  @Override
  public void dispose() {
    removeModulator(this.smoother);
    super.dispose();
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, LuminosityEffect device) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL);

    final int colWidth = 70;

    addColumn(uiDevice, "Limit",
      newVerticalSlider(this.limit, 80).setShowLabel(false).setX(8),
      newButton(this.postFader).setWidth(colWidth)
      )
    .setWidth(colWidth);
    this.addVerticalBreak(ui, uiDevice).setLeftMargin(5);
    addColumn(uiDevice, "Weights",
      newDoubleBox(this.rWeight),
      controlLabel(ui, "Red"),
      newDoubleBox(this.gWeight),
      controlLabel(ui, "Green"),
      newDoubleBox(this.bWeight),
      controlLabel(ui, "Blue")
      )
    .setLeftMargin(1);
    addColumn(uiDevice, "Gamma",
      newDoubleBox(this.gamma)
      )
    .setLeftMargin(1);
    addColumn(uiDevice, "Smooth",
      newKnob(this.windowRangeMs)
      )
    .setLeftMargin(1);
    this.addVerticalBreak(ui, uiDevice).setLeftMargin(5);
    addColumn(uiDevice, "Meters",
      UIMeter.newVerticalMeter(ui, this.input, 12, UIKnob.HEIGHT).setX(20),
      controlLabel(ui, "Input"),
      UIMeter.newVerticalMeter(ui, this.clip, 12, UIKnob.HEIGHT).setX(20),
      controlLabel(ui, "Clipping")
      )
    .setLeftMargin(1);
  }

}
