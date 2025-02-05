/**
 * Copyright 2024- Justin K. Belcher
 *
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package heronarts.lx.structure;

import java.util.Arrays;

public enum DistanceUnits {
  INCHES("Inches", "Inch", "in", 1),
  FEET("Feet", "Foot", "ft",1/12.),
  YARDS("Yards", "Yard", "yd", 1/36.),
  MILLIMETERS("Millimeters", "Millimeter", "mm", 25.4),
  CENTIMETERS("Centimeters", "Centimeter", "cm", 2.54),
  METERS("Meters", "Meter", "m", .0254);

  public final String name;
  public final String singular;
  public final String abbrev;
  public final double scaleFactor;

  DistanceUnits(String name, String singular, String abbreviation, double scaleFactor) {
    this.name = name;
    this.singular = singular;
    this.abbrev = abbreviation;
    this.scaleFactor = scaleFactor;
  }

  @Override
  public String toString() {
    return this.name;
  }

  public String getSingular() {
    return this.singular;
  }

  public String getAbbrev() {
    return this.abbrev;
  }

  public double getScaleFactor() {
    return this.scaleFactor;
  }

  public double to(DistanceUnits to, double value) {
    return convert(this, to, value);
  }

  public double from(DistanceUnits from, double value) {
    return convert(from, this, value);
  }

  public static double convert(DistanceUnits from, DistanceUnits to, double value) {
    return value / from.scaleFactor * to.scaleFactor;
  }

  public static String[] getOptionsSingular() {
    return Arrays.stream(DistanceUnits.values())
      .map(DistanceUnits::getSingular)
      .toArray(String[]::new);
  }
}
