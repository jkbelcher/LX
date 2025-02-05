/**
 * Copyright 2024- Justin K. Belcher
 *
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package heronarts.lx.structure;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.MutableParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.transform.LXMatrix;
import heronarts.lx.utils.LXUtils;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A path is defined by a series of coordinates.  From the path
 * a point list is generated using either fixed spacing or a given
 * number of points to spread evenly along the path.
 */
public class PathFixture extends LXBasicFixture implements JsonKeys {

  private static final DecimalFormat decimalFormat = new DecimalFormat("#0.##");

  public static final LXParameter.Formatter DECIMAL_CLEAN = decimalFormat::format;

  public static final int MAX_POINTS = 4096;

  public enum PointMode {
    DIRECT("Direct"),
    DENSITY("Density"),
    SPACING("Spacing"),
    NUMPOINTS("NumPoints");

    public final String label;

    PointMode(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  // Static parameter builders allow matching SyncParameters to be built

  public static EnumParameter<DistanceUnits> newPathUnits() {
    return new EnumParameter<DistanceUnits>("Path Units", DistanceUnits.INCHES)
      .setDescription("Units of the SVG path");
  }

  public static EnumParameter<DistanceUnits> newModelUnits() {
    return new EnumParameter<>("Model Units", DistanceUnits.INCHES)
      .setDescription("Units of the Chromatik model");
  }

  public static EnumParameter<PointMode> newPointMode() {
    return new EnumParameter<PointMode>("Mode", PointMode.SPACING)
      .setDescription("How points are placed along the path: either a fixed number of points, a fixed spacing between points, or one point per path coordinate.");
  }

  public static DiscreteParameter newNumPoints() {
    return new DiscreteParameter("Num Points", 10, 1, MAX_POINTS + 1)
      .setUnits(LXParameter.Units.INTEGER)
      .setDescription("Number of points on the path, when using NumPoints mode");
  }

  public static BoundedParameter newSpacing() {
    return new BoundedParameter("Spacing", 1, 0.1, 1000)
      .setDescription("Spacing between points, when using Spacing mode")
      .setFormatter(DECIMAL_CLEAN);
  }

  public static EnumParameter<DistanceUnits> newSpacingUnits() {
    return (EnumParameter<DistanceUnits>)
      new EnumParameter<>("Spacing Units", DistanceUnits.INCHES)
      .setDescription("Units for Spacing mode");
  }

  public static BoundedParameter newDensity() {
    return new BoundedParameter("Density", 60, 0.1, 1000)
      .setDescription("Number of points per [unit], when using Density mode")
      .setFormatter(DECIMAL_CLEAN);
  }

  public static EnumParameter<DistanceUnits> newDensityUnits() {
    return (EnumParameter<DistanceUnits>)
      new EnumParameter<>("Density Units", DistanceUnits.METERS)
      .setDescription("In Density mode, the number of points is per each of these units")
      .setOptions(DistanceUnits.getOptionsSingular());
  }

  public static BooleanParameter newReversePath() {
    return new BooleanParameter("Reverse Path", false)
      .setDescription("Direction to traverse the SVG path (Forward = false, Reverse = true)");
  }

  public static BoundedParameter newPadStart() {
    return new BoundedParameter("PadStart", 0, 0, 10000)
      .setDescription("Distance between path start and first pixel")
      .setFormatter(DECIMAL_CLEAN);
  }

  public static BoundedParameter newPadEnd() {
    return new BoundedParameter("PadEnd", 0, 0, 10000)
      .setDescription("Distance between last pixel and path end")
      .setFormatter(DECIMAL_CLEAN);
  }

  public final EnumParameter<DistanceUnits> pathUnits = newPathUnits();

  public final EnumParameter<DistanceUnits> modelUnits = newModelUnits();

  public final EnumParameter<PointMode> pointMode = newPointMode();

  public final DiscreteParameter numPoints = newNumPoints();

  public final BoundedParameter spacing = newSpacing();

  public final EnumParameter<DistanceUnits> spacingUnits = newSpacingUnits();

  public final BoundedParameter density = newDensity();

  public final EnumParameter<DistanceUnits> densityUnits = newDensityUnits();

  public final BooleanParameter reversePath = newReversePath();

  public final BoundedParameter padStart = newPadStart();

  public final BoundedParameter padEnd = newPadEnd();

  public final MutableParameter size = new MutableParameter("Size", 0)
      .setDescription("Calculated number of points in this fixture, read-only");

  public final StringParameter pathData = new StringParameter("Path", "");

  //private String pathData;
  private Path2D path;

  /**
   * Coordinates define the path on which the points are placed.
   */
  private final List<Coordinate> coordinates = new ArrayList<Coordinate>();

  /**
   * Calculated length of coordinates path, in model units
   */
  private double coordsLength;

  /**
   * Calculated number of points in fixture when pointMode is Spacing
   */
  private int sizeForSpacingMode = 0;

  public PathFixture(LX lx) {
    this(lx, null);
  }

  public PathFixture(LX lx, String pathData) {
    super(lx, "Path");

    addMetricsParameter("pathUnits", this.pathUnits);
    addMetricsParameter("modelUnits", this.modelUnits);
    addMetricsParameter("pointMode", this.pointMode);
    addMetricsParameter("numPoints", this.numPoints);
    addMetricsParameter("spacing", this.spacing);
    addMetricsParameter("spacingUnits", this.spacingUnits);
    addMetricsParameter("density", this.density);
    addMetricsParameter("densityUnits", this.densityUnits);
    addMetricsParameter("reversePath", this.reversePath);
    addMetricsParameter("padStart", this.padStart);
    addMetricsParameter("padEnd", this.padEnd);

    addMetricsParameter("svgPath", this.pathData);
//    addInternalParameter("svgPath", this.pathData);
//    this.pathData.addListener(this);

    this.pathData.setValue(pathData);
  }

  @Override
  public void addModelMetaData(Map<String, String> metaData) {
    metaData.put("pathUnits", String.valueOf(this.pathUnits.getEnum()));
    metaData.put("modelUnits", String.valueOf(this.modelUnits.getEnum()));
    metaData.put("pointMode", this.pointMode.getEnum().toString());
    metaData.put("numPoints", String.valueOf(this.numPoints.getValuei()));
    metaData.put("spacing", String.valueOf(this.spacing.getValue()));
    metaData.put("spacingUnits", String.valueOf(this.spacingUnits.getEnum()));
    metaData.put("density", String.valueOf(this.density.getValue()));
    metaData.put("densityUnits", String.valueOf(this.densityUnits.getEnum()));
    metaData.put("reversePath", String.valueOf(this.reversePath.getValueb()));
    metaData.put("padStart", String.valueOf(this.padStart.getValue()));
    metaData.put("padEnd", String.valueOf(this.padEnd.getValue()));
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.pathUnits || p == this.modelUnits) {
      rebuildCoordinates();
      refreshSizeForSpacing();
    } else if (this.pointMode.getEnum() == PointMode.SPACING &&
      (p == this.pointMode || p == this.spacing || p == this.spacingUnits || p == this.padStart || p == this.padEnd)) {
      refreshSizeForSpacing();
    } else if (this.pointMode.getEnum() == PointMode.DENSITY &&
      (p == this.pointMode || p == this.density || p == this.densityUnits || p == this.padStart || p == this.padEnd)) {
      refreshSizeForSpacing();
    } else if (p == this.pathData) {
      setPathData(this.pathData.getString());
    }
    super.onParameterChanged(p);
  }

  private void setPathData(String pathData) {
    if (LXUtils.isEmpty(pathData)) {
      this.coordinates.clear();
      this.path = null;
      return;
    }

    this.path = parseSVGPath(pathData);
    rebuildCoordinates();
    refreshSizeForSpacing();
  }

  /**
   * Calculate coordinates from the path, apply scaling here.
   */
  private void rebuildCoordinates() {
    if (this.path == null) {
      return;
    }

    final DistanceUnits pathUnits = this.pathUnits.getEnum();
    final DistanceUnits modelUnits = this.modelUnits.getEnum();

    this.coordinates.clear();
    double[] coords = new double[6];

    PathIterator iterator = this.path.getPathIterator(null);
    while (!iterator.isDone()) {
      switch (iterator.currentSegment(coords)) {
        case PathIterator.SEG_MOVETO:
          // Note this doesn't handle moves in the middle of a path.  So far in my testing
          // this gets imported as a separate path.  If we need to handle mid-path moves,
          // we'll need group the coordinates into path segments.
          this.coordinates.add(new Coordinate(
            pathUnits.to(modelUnits, coords[0]),
            pathUnits.to(modelUnits, coords[1])));
          break;

        case PathIterator.SEG_LINETO:
          this.coordinates.add(new Coordinate(
            pathUnits.to(modelUnits, coords[0]),
            pathUnits.to(modelUnits, coords[1])));
          break;

        case PathIterator.SEG_CLOSE:
          break;
      }
      iterator.next();
    }

    calcCoordsLength();
  }

  private void calcCoordsLength() {
    this.coordsLength = 0;
    if (this.coordinates.isEmpty()) {
      return;
    }

    // Calculate coordinates length as the sum of all distances between the coordinates
    Coordinate prevCoord = this.coordinates.get(0);
    prevCoord.distOverall = 0;
    prevCoord.distPrev = 0;
    for (int i = 1; i < this.coordinates.size(); i++) {
      Coordinate coord = this.coordinates.get(i);
      double distPrev = coord.dist(prevCoord);
      this.coordsLength += distPrev;
      coord.distPrev = distPrev;
      coord.distOverall = this.coordsLength;
      prevCoord = coord;
    }

    // Calculate normalized position of coordinates along path
    if (this.coordsLength > 0) {
      for (Coordinate coord : this.coordinates) {
        coord.n = coord.distOverall / this.coordsLength;
      }
    } else {
      // Zero net distance between coordinates. Avoid divide by zero.
      for (Coordinate coord : this.coordinates) {
        coord.n = 0;
      }
    }
  }

  /**
   * Returns distance between each point in model units, for Spacing and Density modes.
   */
  private double getModelSpacing() {
    DistanceUnits modelUnits = this.modelUnits.getEnum();
    if (this.pointMode.getEnum() == PointMode.DENSITY) {
      // Density mode
      double density = this.density.getValue();
      double densitySpacing = density > 0 ? 1 / density : 0;
      DistanceUnits densityUnits = this.densityUnits.getEnum();
      return modelUnits.from(densityUnits, densitySpacing);
    } else {
      // Spacing mode
      double spacing = this.spacing.getValue();
      DistanceUnits spacingUnits = this.spacingUnits.getEnum();
      return modelUnits.from(spacingUnits, spacing);
    }
  }

  /**
   * Calculate number of points in fixture for Spacing mode
   */
  protected void refreshSizeForSpacing() {
    double modelSpacing = getModelSpacing();
    if (modelSpacing > 0) {
      this.sizeForSpacingMode = (int) (getActiveLength() / modelSpacing);
    } else {
      this.sizeForSpacingMode = 0;
    }
  }

  /**
   * Length of the coordinates path minus padding, in model units
   */
  private double getActiveLength() {
    return LXUtils.max(0, this.coordsLength - this.padStart.getValue() - this.padEnd.getValue());
  }

  @Override
  protected void computePointGeometry(LXMatrix transform, List<LXPoint> points) {
    switch (this.pointMode.getEnum()) {
      case NUMPOINTS:
        final double activeLength = getActiveLength();
        final double spaces = this.numPoints.getValue() - 1;
        final double spacing = spaces > 0 ? (activeLength / spaces) : 0;
        _computePointsOnPath(transform, points, spacing);
        break;
      case DENSITY:
      case SPACING:
        _computePointsOnPath(transform, points, getModelSpacing());
        break;
      case DIRECT:
      default:
        int i = 0;
        for (LXPoint p : points) {
          Coordinate c = this.coordinates.get(i++);
          transform.translate(c.xf, c.yf, 0);
          p.set(transform);
          transform.translate(-c.xf, -c.yf, 0);
        }
        break;
    }
  }

  private void _computePointsOnPath(LXMatrix transform, List<LXPoint> points, double spacing) {
    if (this.coordinates.size() == 0) {
      return;
    }
    final double nSpacing = this.coordsLength == 0 ? 0 : spacing / this.coordsLength;
    final double padStart = this.padStart.getValue();
    final double nPadStart = this.coordsLength == 0 ? 0 : LXUtils.constrain(padStart / this.coordsLength, 0, 1);

    // Walk along the path, assigning locations to points as we pass their normalized positions.
    // The point coordinates are never used as an intermediate position in a rolling calculation
    // of distance traveled, to avoid rounding errors.
    int iCoord = 0;
    Coordinate coord = this.coordinates.get(0);

    for (int i = 0; i < points.size(); i++) {
      LXPoint point = points.get(i);
      // Normalized position of point along path, accounting for offsets.
      // Constrained because the math will sometimes slip above 1 on the last point.
      double nPoint = LXUtils.constrain(nPadStart + (i * nSpacing), 0, 1);
      while (iCoord < this.coordinates.size()) {
        if (coord.n == nPoint) {
          // Point lines up with previous coordinate.
          transform.translate(coord.xf, coord.yf, 0);
          point.set(transform);
          transform.translate(-coord.xf, -coord.yf, 0);
          // Done with this point, but don't assume we're done with the coordinate until we pass it
          break;
        }

        if (iCoord + 1 == this.coordinates.size()) {
          // We ran off the end. This shouldn't happen. Set it to the last coordinate.
          LX.error("Point didn't fit on the path");
          transform.translate(coord.xf, coord.yf, 0);
          point.set(transform);
          transform.translate(-coord.xf, -coord.yf, 0);
          break;
        }
        Coordinate coordNext = this.coordinates.get(iCoord + 1);

        if (coordNext.n < nPoint) {
          // Advance to next coordinate
          ++iCoord;
          coord = coordNext;
        } else if (coordNext.n == nPoint) {
          // Point lines up with next coordinate
          transform.translate(coordNext.xf, coordNext.yf, 0);
          point.set(transform);
          transform.translate(-coordNext.xf, -coordNext.yf, 0);
          // Done with this point, but don't assume we're done with the coordinate until we pass it
          break;
        } else {
          // Point falls between these two coordinates
          double lerp = (nPoint - coord.n) / (coordNext.n - coord.n);
          Coordinate pointCoord = coord.lerp(coordNext, lerp);
          transform.translate(pointCoord.xf, pointCoord.yf, 0);
          point.set(transform);
          transform.translate(-pointCoord.xf, -pointCoord.yf, 0);
          break;
        }
      }
    }
  }

  @Override
  protected int size() {
    switch (this.pointMode.getEnum()) {
      case NUMPOINTS:
        return this.numPoints.getValuei();
      case DENSITY:
      case SPACING:
        // We updated this already from onParameterChanged()
        return this.sizeForSpacingMode;
      case DIRECT:
      default:
        return this.coordinates.size();
    }
  }

  @Override
  protected void regenerateOutputs() {
    super.regenerateOutputs();

    // Using this as an "afterRegenerate" override
    this.size.setValue(this.size());
  }

  private String defaultFilePath;

  public String exportToDefault() {
    String filePath = this.lx.getMediaFolder(LX.Media.FIXTURES).toString() + File.separator + getLabel() + ".lxf";
    return exportTo(new File(filePath));
  }

  /**
   * Export this fixture to a specified LXF file
   * @param file Target LXF file
   * @return The fixture name for use in a parent fixture, or null if error.
   */
  public String exportTo(File file) {
    if (!this.lx.permissions.canSave()) {
      LX.error(Math.random() > .5 ? "Savings Await - Sign Up and Start Saving!" : "Sign Up & Save More Today!");
      return null;
    }

    JsonObject obj = new JsonObject();
    obj.addProperty(KEY_LABEL, this.getLabel());
    obj.addProperty(KEY_TAG, "svg");

    // metadata
    JsonObject metadata = new JsonObject();
    metadata.addProperty("generator", "Chromatik, SVG Import Plugin, Version 0.1.1.LX");
    metadata.addProperty("length", getActiveLength());
    metadata.addProperty("numPoints", this.points.size());
    obj.add(KEY_METADATA, metadata);

    // components
    JsonArray components = new JsonArray();
    obj.add(KEY_COMPONENTS, components);

    // component: points list
    JsonObject component = new JsonObject();
    components.add(component);

    //   type
    component.addProperty("type", KEY_POINTS);

    //   tags
    if (this.tagList.size() > 0) {
      JsonArray tags = new JsonArray();
      for (String tag : this.tagList) {
        tags.add(tag);
      }
      component.add(KEY_TAGS, tags);
    }

    //   coordinates
    JsonArray coords = new JsonArray();
    component.add(KEY_COORDINATES, coords);
    for (LXPoint p : this.points) {
      JsonObject coord = new JsonObject();
      coord.addProperty("x", p.x);
      coord.addProperty("y", p.y);
      coord.addProperty("z", p.z);
      coords.add(coord);
    }

    // output
    Protocol protocol = this.protocol.getEnum();
    if (protocol != Protocol.NONE) {
      JsonArray outputs = new JsonArray();
      component.add(KEY_OUTPUTS, outputs);
      JsonObject output = new JsonObject();
      output.addProperty(KEY_PROTOCOL, getProtocolForLXF(this.protocol.getEnum()));
      output.addProperty(KEY_ENABLED, true);
      output.addProperty(KEY_BYTE_ORDER, this.byteOrder.getEnum().name());
      output.addProperty(KEY_REVERSE, this.reverse.getValueb());
      output.addProperty(KEY_HOST, this.host.getString());
      switch (protocol) {
        case ARTNET:
          output.addProperty(KEY_UNIVERSE, this.artNetUniverse.getValuei());
          output.addProperty(KEY_CHANNEL, this.dmxChannel.getValuei());
          output.addProperty(KEY_SEQUENCE_ENABLED, this.artNetSequenceEnabled.getValueb());
          break;
        case SACN:
          output.addProperty(KEY_UNIVERSE, this.artNetUniverse.getValuei());
          output.addProperty(KEY_CHANNEL, this.dmxChannel.getValuei());
          if (!this.sacnPriority.isDefault()) {
            output.addProperty(KEY_PRIORITY, this.sacnPriority.getValuei());
          }
          break;
        case OPC:
          output.addProperty(KEY_TRANSPORT, this.transport.getEnum().name());
          output.addProperty(KEY_PORT, this.port.getValuei());
          output.addProperty(KEY_OPC_CHANNEL, this.opcChannel.getValuei());
          output.addProperty(KEY_OFFSET, this.opcOffset.getValuei());
          break;
        case DDP:
          output.addProperty(KEY_DDP_DATA_OFFSET, this.ddpDataOffset.getValuei());
          break;
        case KINET:
          output.addProperty(KEY_KINET_VERSION, this.kinetVersion.getEnum().name());
          output.addProperty(KEY_KINET_PORT, this.kinetPort.getValuei());
          output.addProperty(KEY_CHANNEL, this.dmxChannel.getValuei());
          break;
      }
      outputs.add(output);
    }

    try (JsonWriter writer = new JsonWriter(new FileWriter(file))) {
      writer.setIndent("  ");
      new GsonBuilder().create().toJson(obj, writer);
      LX.log("Fixture exported successfully to " + file);
      return removeExtension(file.getName());
    } catch (IOException iox) {
      LX.error(iox, "Exception writing fixture file to " + file);
      return null;
    }
  }

  /**
   * Get an LXF-compatible value for the output.protocol property.
   * Currently these definitions only exist in the private enum JsonFixture.JsonProtocolDefinition
   */
  private static String getProtocolForLXF(Protocol protocol) {
    // JSON protocol keys are defined
    switch (protocol) {
      case ARTNET:
        return "artnet";
      case SACN:
        return "sacn";
      case OPC:
        return "opc";
      case DDP:
        return "ddp";
      case KINET:
        return "kinet";
      case NONE:
        return "";
    }
    return "";
  }

  public static String removeExtension(String filename) {
    int dot = filename.lastIndexOf(".");
    if (dot > 0) {
      return filename.substring(0, dot);
    }
    return filename;
  }

  private static final String KEY_SVG_PATH = "svgpath";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
/*    if (this.pathData != null) {
      obj.addProperty(KEY_SVG_PATH, this.pathData);
    }*/
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    super.load(lx, obj);
  }

  /**
   * Similar to LXVector except:
   * - Uses double to reduce accumulated error in distance
   * - Lerp does not modify the position of this object
   */
  private static class Coordinate {
    public final double x;
    public final double y;
    public final float xf;
    public final float yf;

    /**
     * Distance from previous point along path
     */
    public double distPrev;

    /**
     * Distance from first point along path
     */
    public double distOverall;

    /**
     * Normalized distance from first point along path
     */
    public double n;

    public Coordinate(double x, double y) {
      this.x = x;
      this.y = y;
      this.xf = (float)x;
      this.yf = (float)y;
    }

    public double dist(Coordinate that) {
      double dx = this.x - that.x;
      double dy = this.y - that.y;
      return Math.sqrt(dx*dx + dy*dy);
    }

    public Coordinate lerp(Coordinate that, double amt) {
      // NOTE THE DIFFERENCE FROM LXVECTOR, THIS DOES NOT MODIFY THE POSITION WITH EACH LERP()
      return new Coordinate(
        LXUtils.lerp(this.x, that.x, amt),
        LXUtils.lerp(this.y, that.y, amt)
      );
    }
  }

  /**
   * Parse SVG path data into a Path2D object
   */
  public static Path2D parseSVGPath(String pathData) {
    Path2D path = new Path2D.Double();
    String[] commands = pathData.split("(?=[MmLlHhVvCcSsQqTtAaZz])");
    double currentX = 0;
    double currentY = 0;

    for (String cmd : commands) {
      if (cmd.isEmpty()) continue;
      char command = cmd.charAt(0);
      String[] sParams = cmd.substring(1).trim().split("[ ,]+");
      boolean isRelative = Character.isLowerCase(command);
      command = Character.toUpperCase(command);

      // Convert parameters to doubles
      List<Double> params = new ArrayList<>();
      for (String sParam : sParams) {
        if (!sParam.isEmpty()) {
          try {
            params.add(Double.parseDouble(sParam));
          } catch (NumberFormatException e) {
            LX.warning("Failed to parse SVG parameter. Expected double, found: " + sParam);
          }
        }
      }

      switch (Character.toUpperCase(command)) {
        case 'M': // Move
          currentX = (isRelative ? currentX : 0) + params.get(0);
          currentY = (isRelative ? currentY : 0) + params.get(1);
          path.moveTo(currentX, currentY);
          break;

        case 'L': // Line
          for (int p = 0; p < params.size() - 1; p += 2) {
            currentX = (isRelative ? currentX : 0) + params.get(p);
            currentY = (isRelative ? currentY : 0) + params.get(p + 1);
            path.lineTo(currentX, currentY);
          }
          break;

        case 'H': // Horizontal line
          currentX = (isRelative ? currentX : 0) + params.get(0);
          path.lineTo(currentX, currentY);
          break;

        case 'V': // Vertical line
          currentY = (isRelative ? currentY : 0) + params.get(1);
          path.lineTo(currentX, currentY);
          break;

        case 'Z': // Close path
          path.closePath();
          break;

        default:
          LX.error("Unknown SVG command: " + Character.toUpperCase(command));
          break;
      }
    }
    return path;
  }

}
