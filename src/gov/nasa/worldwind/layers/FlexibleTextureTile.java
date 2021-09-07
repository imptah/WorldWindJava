package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Level;

public class FlexibleTextureTile extends TextureTile {

    protected final CacheFilePathStrategy filePathStrategy;

    public interface CacheFilePathStrategy {

        String createPath(Level level, int row, int column, String suffix);

    }

    public FlexibleTextureTile(Sector sector, Level level, int row, int col, CacheFilePathStrategy filePathStrategy) {
        super(sector, level, row, col);
        this.filePathStrategy = filePathStrategy;
    }

    @Override
    public String getPath() {
        if (filePathStrategy == null) {
            return super.getPath();
        } else {
            return filePathStrategy.createPath(this.level, this.row, this.column, this.level.getFormatSuffix());
        }
    }

    @Override
    protected TextureTile createSubTile(Sector sector, Level level, int row, int col) {
        return new FlexibleTextureTile(sector, level, row, col, filePathStrategy);
    }
}
