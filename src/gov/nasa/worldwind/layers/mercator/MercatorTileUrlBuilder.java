/*
 * Copyright (C) 2019 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers.mercator;

import gov.nasa.worldwind.util.*;

import java.io.File;
import java.net.*;

/**
 * @author Sufaev
 */
public abstract class MercatorTileUrlBuilder implements TileUrlBuilder {
    private static final int DEFAULT_FIRST_LEVEL_OFFSET = 3;

    private int firstLevelOffset;

    private SharedTileSource sharedTileSource = null;

    protected MercatorTileUrlBuilder() {
        this.firstLevelOffset = DEFAULT_FIRST_LEVEL_OFFSET;
    }

    public MercatorTileUrlBuilder setFirstLevelOffset(int firstLevelOffset) {
        this.firstLevelOffset = firstLevelOffset;
        return this;
    }

    public SharedTileSource getSharedTileSource() {
        return sharedTileSource;
    }

    public void setSharedTileSource(SharedTileSource sharedTileSource) {
        this.sharedTileSource = sharedTileSource;
    }

    public int getFirstLevelOffset() {
        return firstLevelOffset;
    }

    @Override
    public URL getURL(Tile tile, String imageFormat) throws MalformedURLException {
        int x = tile.getColumn();
        int y = (1 << (tile.getLevelNumber() + firstLevelOffset)) - 1 - tile.getRow();
        int z = tile.getLevelNumber() + firstLevelOffset;
        if (sharedTileSource != null) {
            File sharedTileFile = new File(sharedTileSource.getSourcePath(), sharedTileSource.getTilePath(x, y, z));
            return sharedTileFile.toURI().toURL();
        } else  {
            return getMercatorURL(x, y, z);
        }
    }

    protected abstract URL getMercatorURL(int x, int y, int z) throws MalformedURLException;


}
