package gov.nasa.worldwind.layers.mercator;

public interface SharedTileSource {
    String getSourcePath();
    String getTilePath(int x, int y, int z);
}
