///////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2010 Taesun Moon, The University of Texas at Austin
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 3 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////
package opennlp.textgrounder.models.callbacks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.textgrounder.io.DocumentSet;
import opennlp.textgrounder.topostructs.*;

/**
 * A callback class to 
 *
 * @author tsmoon
 */
public abstract class RegionMapperCallback {

    /**
     * Table from index to region
     */
    protected Map<Integer, Region> regionMap;
    /**
     * Table from region to index. Reverse storage table for regionMap.
     */
    protected Map<Region, Integer> reverseRegionMap;
    /**
     * Table from placename to set of region indexes. The indexes and their
     * referents are stored in regionMap.
     */
    protected Map<String, HashSet<Integer>> nameToRegionIndex;
    /**
     * 
     */
    protected Set<Integer> currentRegionHashSet;
    /**
     * 
     */
    protected Map<Region, HashSet<Location>> regionToLocations;
    /**
     *
     */
    protected Map<String, HashSet<Location>> placenameToLocations;
    /**
     *
     */
    protected Map<Location, Region> locationToRegion;
    /**
     * 
     */
    protected int numRegions;
    /**
     * 
     */
    protected Map<ToponymRegionPair, HashSet<Location>> toponymRegionToLocations;

    /**
     *
     */
    protected RegionMapperCallback() {
        numRegions = 0;
        regionMap = new HashMap<Integer, Region>();
        reverseRegionMap = new HashMap<Region, Integer>();
        nameToRegionIndex = new HashMap<String, HashSet<Integer>>();
        regionToLocations = new HashMap<Region, HashSet<Location>>();
        locationToRegion = new HashMap<Location, Region>();
        placenameToLocations = new HashMap<String, HashSet<Location>>();
        toponymRegionToLocations = new HashMap<ToponymRegionPair, HashSet<Location>>();
    }

    public RegionMapperCallback(Map<Integer, Region> regionMap,
          Map<Region, Integer> reverseRegionMap,
          Map<String, HashSet<Integer>> nameToRegionIndex) {
        setMaps(regionMap, reverseRegionMap, nameToRegionIndex);
    }

    /**
     * 
     */
    public void setMaps(Map<Integer, Region> regionMap,
          Map<Region, Integer> reverseRegionMap,
          Map<String, HashSet<Integer>> nameToRegionIndex) {
        this.regionMap = regionMap;
        this.reverseRegionMap = reverseRegionMap;
        this.nameToRegionIndex = nameToRegionIndex;
    }

    /**
     *
     * @param region
     */
    public abstract void addRegion(Region region);

    /**
     *
     * @param region
     */
    public abstract void addToPlace(Location loc, Region region);

    /**
     * 
     * @param placename
     */
    public abstract void setCurrentRegion(String placename);

    public abstract void addAll(String placename, DocumentSet docSet,
          List<Integer> wordVector, List<Integer> toponymVector,
          List<Integer> documentVector, int docIndex, List<Location> locs);

    /**
     * 
     * @param placename
     * @param docSet
     */
    public void confirmPlacenameTokens(String placename,
          DocumentSet docSet) {
        if (!docSet.hasWord(placename)) {
            docSet.addWord(placename);
        }
    }

    /**
     * @return the regionMap
     */
    public Map<Integer, Region> getRegionMap() {
        return regionMap;
    }

    /**
     * @return the reverseRegionMap
     */
    public Map<Region, Integer> getReverseRegionMap() {
        return reverseRegionMap;
    }

    /**
     * @return the nameToRegionIndex
     */
    public Map<String, HashSet<Integer>> getNameToRegionIndex() {
        return nameToRegionIndex;
    }

    /**
     * @return the numRegions
     */
    public int getNumRegions() {
        return numRegions;
    }

    /**
     * @return the regionToLocations
     */
    public Map<Region, HashSet<Location>> getRegionToLocations() {
        return regionToLocations;
    }

    /**
     * @return the locationToRegion
     */
    public Map<Location, Region> getLocationToRegion() {
        return locationToRegion;
    }

    /**
     * @return the placenameToLocations
     */
    public Map<String, HashSet<Location>> getPlacenameToLocations() {
        return placenameToLocations;
    }

    /**
     * @return the toponymRegionToLocations
     */
    public Map<ToponymRegionPair, HashSet<Location>> getToponymRegionToLocations() {
        return toponymRegionToLocations;
    }
}
