///////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2010 Travis Brown, The University of Texas at Austin
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
///////////////////////////////////////////////////////////////////////////////
package opennlp.textgrounder.topo;

public abstract class Region {
  public abstract Coordinate getCenter();
  public abstract boolean contains(double lat, double lng);

  public abstract double getMinLat();
  public abstract double getMaxLat();
  public abstract double getMinLng();
  public abstract double getMaxLng();

  public boolean contains(Coordinate coordinate) {
    return this.contains(coordinate.getLat(), coordinate.getLng());
  }

  public double getMinLatDegrees() {
     return this.getMinLat() * Math.PI / 180.0;
  }

  public double getMaxLatDegrees() {
     return this.getMaxLat() * Math.PI / 180.0;
  }

  public double getMinLngDegrees() {
     return this.getMinLng() * Math.PI / 180.0;
  }

  public double getMaxLngDegrees() {
     return this.getMaxLng() * Math.PI / 180.0;
  }

  public double distance(Region other) {
     return this.getCenter().distance(other.getCenter());
  }
}
