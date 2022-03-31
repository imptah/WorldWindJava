package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.formats.tiff.GeoTiffFileReader;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.ImageUtil;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

import java.io.File;
import java.io.IOException;

public class GeoTiffFileRasterReader extends AbstractDataRasterReader {

    private static final String[] geotiffMimeTypes = {"image/tiff", "image/geotiff"};
    private static final String[] geotiffSuffixes = {"tif", "tiff", "gtif", "tif.zip", "tiff.zip", "tif.gz", "tiff.gz"};

    public GeoTiffFileRasterReader() {
        super(geotiffMimeTypes, geotiffSuffixes);
    }

    @Override
    protected boolean doCanRead(Object source, AVList params) {
        String path = WWIO.getSourcePath(source);

        if (path == null) return false;

        boolean canRead;
        try {
            GeoTiffFileReader geoTiffFileReader = new GeoTiffFileReader(new File(path));
            canRead = geoTiffFileReader.isGeotiff() || WorldFile.hasWorldFiles(source);
            geoTiffFileReader.dispose();
        } catch (IOException e) {
            canRead = false;
        }
        return canRead;
    }

    @Override
    protected DataRaster[] doRead(Object source, AVList params) throws IOException {

        String path = WWIO.getSourcePath(source);
        if (path == null) {
            throw cannotReadSourceException(source);
        }

        GeoTiffFileReader reader = null;
        DataRaster[] rasters = null;
        try {
            reader = new GeoTiffFileReader(new File(path));
            // READ RASTERS
            rasters = reader.readDataRaster();
        } finally {
            if (reader != null) reader.dispose();
        }

        return rasters;
    }

    @Override
    protected void doReadMetadata(Object source, AVList params) throws IOException {
        String path = WWIO.getSourcePath(source);
        if (path == null) {
            throw cannotReadSourceException(source);
        }

        GeoTiffFileReader reader = null;
        try {
            reader = new GeoTiffFileReader(new File(path));
            reader.copyMetadataTo(params);

            Integer width = (Integer) params.getValue(AVKey.WIDTH);
            Integer height = (Integer) params.getValue(AVKey.HEIGHT);

            //FIXME: what case for this condition? (copied from GeotiffRasterReader)
            if (!reader.isGeotiff() && width != null && height != null) {
                int[] size = new int[2];

                size[0] = width;
                size[1] = height;

                params.setValue(WorldFile.WORLD_FILE_IMAGE_SIZE, size);
                WorldFile.readWorldFiles(source, params);

                Object o = params.getValue(AVKey.SECTOR);
                if (o == null || !(o instanceof Sector)) {
                    ImageUtil.calcBoundingBoxForUTM(params);
                }
            }
        } finally {
            if (reader != null) reader.dispose();
        }
    }

    private IOException cannotReadSourceException(Object source) {
        String message = Logging.getMessage("DataRaster.CannotRead", source);
        Logging.logger().severe(message);
        return new IOException(message);
    }
}
