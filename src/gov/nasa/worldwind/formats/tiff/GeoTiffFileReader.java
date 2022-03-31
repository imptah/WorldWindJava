package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.data.ByteBufferRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.ElevationsUtil;
import gov.nasa.worldwind.util.ImageUtil;
import gov.nasa.worldwind.util.Logging;
import mil.nga.tiff.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.plugins.tiff.TIFFDirectory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GeoTiffFileReader implements Disposable {

    public ArrayList<AVList> getMetadata() {
        return metadata;
    }

    private ArrayList<AVList> metadata;

    private final TIFFImage tiffImage;

    public GeoTiffFileReader(File file) throws IOException {
        this.tiffImage = TiffReader.readTiff(file);
        validateTiffImage(tiffImage);
        metadata = getFileDirectoriesMetadata(tiffImage, file);
    }

    public static boolean isGeoTiff(ArrayList<AVList> metadata) {
        AVList values = metadata.get(0);
        return (null != values && values.hasKey(AVKey.COORDINATE_SYSTEM));
    }

    public boolean isGeotiff() {
        return isGeoTiff(this.metadata);
    }

    public AVList copyMetadataTo(AVList values) {
        AVList list = metadata.get(0);
        if (null != values) {
            values.setValues(list);
        } else {
            values = list;
        }
        return values;
    }

    public DataRaster[] readDataRaster() {
        List<FileDirectory> fileDirectories = tiffImage.getFileDirectories();

        DataRaster[] rasters = new DataRaster[fileDirectories.size()];
        for (int i = 0; i < fileDirectories.size(); i++) {
            FileDirectory directory = fileDirectories.get(i);

            Rasters directoryRasters = directory.readRasters();
            // VALIDATE WIDTH
            // VALIDATE HEIGHT
            // VALIDATE SAMPLES PER PIXEL
            // VALIDATE PHOTOMETRIC
            // VALIDATE ROWS PER STRIP
            // VALIDATE PLANAR CONFIG
            // CHECK MISSING TAG - STRIP OFFSET?
            // CHECK MISSING TAG - STRIP COUNTS?
            // CHECK LZW COMPRESSION - OTHER NOT SUPPORTED?
            // CHECK TILED GEOTIFF?

            AVList directoryParams = metadata.get(i);
            try {
                rasters[i] = convertToDataRaster(directoryRasters, directoryParams);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rasters;
    }

    private static ArrayList<AVList> getFileDirectoriesMetadata(TIFFImage tiffImage, File file) throws IOException {
        ArrayList<AVList> metadata = new ArrayList<>();
        List<FileDirectory> fileDirectories = tiffImage.getFileDirectories();
        GeoCodec fileGeoCodec = null;
        for (int i = 0; i < fileDirectories.size(); i++) {
            FileDirectory fileDirectory = fileDirectories.get(i);
            if (i == 0) {
                fileGeoCodec = createGeoCodecFromFileImageDirectory(fileDirectory);
            }
            metadata.add(i, createImageFileDirectoryMetadata(file, fileDirectory, i, fileGeoCodec));
        }
        return metadata;
    }

    private static AVList createImageFileDirectoryMetadata(File file, FileDirectory fileDirectory, int index, GeoCodec gc) throws IOException {
        AVListImpl params = new AVListImpl();
        // FILE NAME
        params.setValue(AVKey.FILE_NAME, file.getAbsolutePath());
        // BYTE ORDER
        params.setValue(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN); // after we read all data, we have everything as BIG_ENDIAN
        // WIDTH
        int imageWidth = fileDirectory.getImageWidth().intValue();
        params.setValue(AVKey.WIDTH, imageWidth);
        // HEIGHT
        int imageHeight = fileDirectory.getImageHeight().intValue();
        params.setValue(AVKey.HEIGHT, imageHeight);
        // omit AVKey.DISPLAY_NAME
        // omit AVKey.DESCRIPTION
        // omit AVKey.VERSION
        // omit AVKey.DATE_TIME
        Integer photometricInterpretation = fileDirectory.getPhotometricInterpretation();
        if (photometricInterpretation == Tiff.Photometric.Color_RGB) {
            params.setValue(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
            params.setValue(AVKey.IMAGE_COLOR_FORMAT, AVKey.COLOR);
            params.setValue(AVKey.DATA_TYPE, AVKey.INT8);
        } else if (photometricInterpretation == Tiff.Photometric.CMYK) {
            params.setValue(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
            params.setValue(AVKey.IMAGE_COLOR_FORMAT, AVKey.COLOR);
            params.setValue(AVKey.DATA_TYPE, AVKey.INT8);
        } else if (photometricInterpretation == Tiff.Photometric.Color_Palette) {
            params.setValue(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
            params.setValue(AVKey.IMAGE_COLOR_FORMAT, AVKey.COLOR);
            params.setValue(AVKey.DATA_TYPE, AVKey.INT8);
        } else if (fileDirectory.getSamplesPerPixel() == Tiff.SamplesPerPixel.MONOCHROME) {   // Tiff.Photometric.Grayscale_BlackIsZero or Tiff.Photometric.Grayscale_WhiteIsZero
            Integer sampleFormat = fileDirectory.getSampleFormat().get(0); // only first element
            Integer bitsPerSample = fileDirectory.getBitsPerSample().get(0);
            if (sampleFormat == Tiff.SampleFormat.SIGNED) {
                params.setValue(AVKey.PIXEL_FORMAT, AVKey.ELEVATION);
                if (bitsPerSample == Short.SIZE) {
                    params.setValue(AVKey.DATA_TYPE, AVKey.INT16);
                } else if (bitsPerSample == Byte.SIZE) {
                    params.setValue(AVKey.DATA_TYPE, AVKey.INT8);
                } else if (bitsPerSample == Integer.SIZE) {
                    params.setValue(AVKey.DATA_TYPE, AVKey.INT32);
                }
            } else if (sampleFormat == Tiff.SampleFormat.IEEEFLOAT) {
                params.setValue(AVKey.PIXEL_FORMAT, AVKey.ELEVATION);
                if (bitsPerSample == Float.SIZE) {
                    params.setValue(AVKey.DATA_TYPE, AVKey.FLOAT32);
                }
            } else if (sampleFormat == Tiff.SampleFormat.UNSIGNED) {
                params.setValue(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
                params.setValue(AVKey.IMAGE_COLOR_FORMAT, AVKey.GRAYSCALE);
                if (bitsPerSample == Short.SIZE) {
                    params.setValue(AVKey.DATA_TYPE, AVKey.INT16);
                } else if (bitsPerSample == Byte.SIZE) {
                    params.setValue(AVKey.DATA_TYPE, AVKey.INT8);
                } else if (bitsPerSample == Integer.SIZE) {
                    params.setValue(AVKey.DATA_TYPE, AVKey.INT32);
                }
            }
        }

        if (!params.hasKey(AVKey.PIXEL_FORMAT) || !params.hasKey(AVKey.DATA_TYPE)) {
            String message = Logging.getMessage("Geotiff.UnsupportedDataTypeRaster", fileDirectory.toString());
            Logging.logger().severe(message);
        }


        // GDAL_NODATA
        String gdalNoData = fileDirectory.getStringEntryValue(FieldTagType.GDAL_NODATA);
        if (gdalNoData != null) params.setValue(AVKey.MISSING_DATA_SIGNAL, Double.parseDouble(gdalNoData));
        // MIN_SAMPLE_VALUE
        List<Double> minSampleValue = fileDirectory.getDoubleListEntryValue(FieldTagType.MinSampleValue);
        if (minSampleValue != null) params.setValue(AVKey.ELEVATION_MIN, minSampleValue.get(0));
        // MAX_SAMPLE_VALUE
        List<Double> maxSampleValue = fileDirectory.getDoubleListEntryValue(FieldTagType.MaxSampleValue);
        if (maxSampleValue != null) params.setValue(AVKey.ELEVATION_MAX, maxSampleValue.get(0));

        // --- PROCESS GEO KEYS ---

        // VERTICAL TYPE
        // geo-tiff spec requires the VerticalCSType to be present for elevations (but ignores its value)
        if (gc.hasGeoKey(GeoTiff.GeoKey.VerticalCSType)) params.setValue(AVKey.PIXEL_FORMAT, AVKey.ELEVATION);

        // VERTICAL UNITS
        if (gc.hasGeoKey(GeoTiff.GeoKey.VerticalUnits)) {
            int units = getUnits(gc);
            if (units == GeoTiff.Unit.Linear.Meter) {
                params.setValue(AVKey.ELEVATION_UNIT, AVKey.UNIT_METER);
            } else if (units == GeoTiff.Unit.Linear.Foot) {
                params.setValue(AVKey.ELEVATION_UNIT, AVKey.UNIT_FOOT);
            }
        }

        // RASTER TYPE
        if (gc.hasGeoKey(GeoTiff.GeoKey.RasterType)) {
            int rasterType = getRasterType(gc);
            if (rasterType == GeoTiff.RasterType.RasterPixelIsArea) {
                params.setValue(AVKey.RASTER_PIXEL, AVKey.RASTER_PIXEL_IS_AREA);
            } else if (rasterType == GeoTiff.RasterType.RasterPixelIsPoint) {
                params.setValue(AVKey.RASTER_PIXEL, AVKey.RASTER_PIXEL_IS_POINT);
            }
        }

        // MODEL TYPE
        int gtModelTypeGeoKey = GeoTiff.ModelType.Undefined;
        if (gc.hasGeoKey(GeoTiff.GeoKey.ModelType)) {
            gtModelTypeGeoKey = getModelTypeGeoKey(gc);
        }

        if (gtModelTypeGeoKey == GeoTiff.ModelType.Geographic) {
            params.setValue(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_GEOGRAPHIC);

            // GEOGRAPHIC TYPE
            int epsg = GeoTiff.GCS.Undefined;
            if (gc.hasGeoKey(GeoTiff.GeoKey.GeographicType)) {
                epsg = getEPSGGeographicType(gc);
            }
            if (epsg != GeoTiff.GCS.Undefined) params.setValue(AVKey.PROJECTION_EPSG_CODE, epsg);

            // BOUNDING BOX
            // TODO Assumes WGS84(4326)- should we check for this ?
            double[] bbox = gc.getBoundingBox(imageWidth, imageHeight);
            params.setValue(AVKey.SECTOR, Sector.fromDegrees(bbox[3], bbox[1], bbox[0], bbox[2]));
            params.setValue(AVKey.ORIGIN, LatLon.fromDegrees(bbox[1], bbox[0]));
        } else if (gtModelTypeGeoKey == GeoTiff.ModelType.Projected) {
            params.setValue(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_PROJECTED);

            // PROJECTION
            int projection = getProjection(gc);
            if (projection != GeoTiff.PCS.Undefined) params.setValue(AVKey.PROJECTION_EPSG_CODE, projection);

            // HEMISPHERE AND ZONE
            String projectionHemisphere = getProjectionHemisphere(projection);
            Integer zone = getZone(projection);
            if (projectionHemisphere == null || zone == null) {
                String message = Logging.getMessage("generic.UnknownProjection", projection);
                Logging.logger().severe(message);
                throw new IOException(message);
            }
            params.setValue(AVKey.PROJECTION_HEMISPHERE, projectionHemisphere);
            params.setValue(AVKey.PROJECTION_ZONE, zone);

            // PIXEL SCALES X AND Y
            double pixelScaleX = gc.getModelPixelScaleX();
            double pixelScaleY = Math.abs(gc.getModelPixelScaleY());
            params.setValue(WorldFile.WORLD_FILE_X_PIXEL_SIZE, pixelScaleX);
            params.setValue(WorldFile.WORLD_FILE_Y_PIXEL_SIZE, -pixelScaleY);


            //shift to center
            GeoCodec.ModelTiePoint[] tps = gc.getTiePoints();
            if (null != tps && tps.length > index) {
                GeoCodec.ModelTiePoint tp = tps[index];

                double xD = tp.getX() + (pixelScaleX / 2d);
                double yD = tp.getY() - (pixelScaleY / 2d);

                params.setValue(WorldFile.WORLD_FILE_X_LOCATION, xD);
                params.setValue(WorldFile.WORLD_FILE_Y_LOCATION, yD);
            }

            params.setValue(AVKey.SECTOR, ImageUtil.calcBoundingBoxForUTM(params));
        } else {
            String msg = Logging.getMessage("Geotiff.UnknownGeoKeyValue", gtModelTypeGeoKey, GeoTiff.GeoKey.ModelType, index);
            Logging.logger().severe(msg);
        }

        return params;
    }

    private static GeoCodec createGeoCodecFromFileImageDirectory(FileDirectory fileDirectory) {
        GeoCodec gc = new GeoCodec();
        // MODEL_PIXELSCALE
        List<Double> modelPixelScale = fileDirectory.getModelPixelScale();
        if (modelPixelScale != null) gc.setModelPixelScale(mapDoublesArray(modelPixelScale));
        // MODEL_TIEPOINT
        List<Double> modelTiePointParams = fileDirectory.getDoubleListEntryValue(FieldTagType.ModelTiepoint);
        if (modelTiePointParams != null) gc.addModelTiePoints(mapDoublesArray(modelTiePointParams));
        // MODEL_TRANSFORMATION
        List<Double> modelTransformationParams = fileDirectory.getDoubleListEntryValue(FieldTagType.ModelTransformation);
        if (modelTransformationParams != null) gc.setModelTransformation(mapDoublesArray(modelTransformationParams));
        // GEO_KEY_DIRECTORY
        List<Number> geoKeyParams = fileDirectory.getNumberListEntryValue(FieldTagType.GeoKeyDirectory);
        if (geoKeyParams != null) gc.setGeokeys(mapShortArray(geoKeyParams));
        // GEO_DOUBLE_PARAMS
        List<Double> geoDoubleParams = fileDirectory.getDoubleListEntryValue(FieldTagType.GeoDoubleParams);
        if (geoDoubleParams != null) gc.setDoubleParams(mapDoublesArray(geoDoubleParams));
        // GEO_ASCII_PARAMS
        String asciiParams = fileDirectory.getStringEntryValue(FieldTagType.GeoAsciiParams);
        if (asciiParams != null) gc.setAsciiParams(asciiParams.getBytes());
        return gc;
    }

    private static String getProjectionHemisphere(int projection) {
        String hemi = null;
        if ((projection >= 16100) && (projection <= 16199)) { //UTM Zone South
            hemi = AVKey.SOUTH;
        } else if ((projection >= 16000) && (projection <= 16099)) { //UTM Zone North
            hemi = AVKey.NORTH;
        } else if ((projection >= 26900) && (projection <= 26999)) { //UTM : NAD83
            hemi = AVKey.NORTH;
        } else if ((projection >= 32201) && (projection <= 32260)) {//UTM : WGS72 N
            hemi = AVKey.NORTH;
        } else if ((projection >= 32301) && (projection <= 32360)) {//UTM : WGS72 S
            hemi = AVKey.SOUTH;
        } else if ((projection >= 32401) && (projection <= 32460)) {//UTM : WGS72BE N
            hemi = AVKey.NORTH;
        } else if ((projection >= 32501) && (projection <= 32560)) {//UTM : WGS72BE S
            hemi = AVKey.SOUTH;
        } else if ((projection >= 32601) && (projection <= 32660)) {//UTM : WGS84 N
            hemi = AVKey.NORTH;
        } else if ((projection >= 32701) && (projection <= 32760)) { //UTM : WGS84 S
            hemi = AVKey.SOUTH;
        }
        return hemi;
    }

    private static Integer getZone(int projection) {
        Integer zone = null;
        if ((projection >= 16100) && (projection <= 16199)) { //UTM Zone South
            zone = projection - 16100;
        } else if ((projection >= 16000) && (projection <= 16099)) { //UTM Zone North
            zone = projection - 16000;
        } else if ((projection >= 26900) && (projection <= 26999)) { //UTM : NAD83
            zone = projection - 26900;
        } else if ((projection >= 32201) && (projection <= 32260)) {//UTM : WGS72 N
            zone = projection - 32200;
        } else if ((projection >= 32301) && (projection <= 32360)) {//UTM : WGS72 S
            zone = projection - 32300;
        } else if ((projection >= 32401) && (projection <= 32460)) {//UTM : WGS72BE N
            zone = projection - 32400;
        } else if ((projection >= 32501) && (projection <= 32560)) {//UTM : WGS72BE S
            zone = projection - 32500;
        } else if ((projection >= 32601) && (projection <= 32660)) {//UTM : WGS84 N
            zone = projection - 32600;
        } else if ((projection >= 32701) && (projection <= 32760)) { //UTM : WGS84 S
            zone = projection - 32700;
        }
        return zone;
    }

    private static int getProjection(GeoCodec gc) {
        int[] vals = null;
        if (gc.hasGeoKey(GeoTiff.GeoKey.Projection)) {
            vals = gc.getGeoKeyAsInts(GeoTiff.GeoKey.Projection);
        } else if (gc.hasGeoKey(GeoTiff.GeoKey.ProjectedCSType)) {
            vals = gc.getGeoKeyAsInts(GeoTiff.GeoKey.ProjectedCSType);
        }
        if (null != vals && vals.length > 0) {
            return vals[0];
        } else {
            return GeoTiff.PCS.Undefined;
        }
    }

    private static int getEPSGGeographicType(GeoCodec gc) {
        int[] gkValues = gc.getGeoKeyAsInts(GeoTiff.GeoKey.GeographicType);
        if (null != gkValues && gkValues.length > 0) {
            return gkValues[0];
        } else {
            return GeoTiff.GCS.Undefined;
        }
    }

    private static int getModelTypeGeoKey(GeoCodec gc) {
        int[] gkValues = gc.getGeoKeyAsInts(GeoTiff.GeoKey.ModelType);
        if (null != gkValues && gkValues.length > 0) {
            return gkValues[0];
        } else {
            return GeoTiff.ModelType.Undefined;
        }
    }

    private static int getUnits(GeoCodec gc) {
        int[] v = gc.getGeoKeyAsInts(GeoTiff.GeoKey.VerticalUnits);
        return (null != v && v.length > 0) ? v[0] : GeoTiff.Undefined;
    }


    private static double[] mapDoublesArray(List<Double> list) {
        double[] target = new double[list.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = list.get(i);
        }
        return target;
    }

    private static short[] mapShortArray(List<Number> list) {
        short[] target = new short[list.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = list.get(i).shortValue();
        }
        return target;
    }

    private void validateTiffImage(TIFFImage tiffImage) throws IOException {
        if (tiffImage == null) {
            throw new IOException("GeoTiff file is not read");
        }
        if (tiffImage.getFileDirectories().isEmpty()) {
            throw new IOException("Empty Image File Directories in GeoTiff");
        }
    }

    public DataRaster convertToDataRaster(Rasters rasters, AVList directoryParams) throws IOException {

        Object pixelFormat = directoryParams.getValue(AVKey.PIXEL_FORMAT);
        Object imageColorFormat = directoryParams.getValue(AVKey.IMAGE_COLOR_FORMAT);
        Sector sector = (Sector) directoryParams.getValue(AVKey.SECTOR);

        if (pixelFormat == AVKey.ELEVATION) {
            int width = rasters.getWidth();
            int height = rasters.getHeight();
            ByteBufferRaster raster = new ByteBufferRaster(width, height, sector, directoryParams);

            // TRANSLATE PIXELS FROM GEO-INT BUFFER TO WW BUFFER (BECAUSE OF DIFFERENT KEYS STRATEGY)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Number value = rasters.getPixel(x, y)[0]; // we support only first value
                    raster.setDoubleAtPosition(y, x, value.doubleValue());
                }
            }

            ElevationsUtil.rectify(raster);

            return raster;
        } else if (pixelFormat == AVKey.IMAGE && imageColorFormat == AVKey.GRAYSCALE) {
            // RETURNS BUFFERED IMAGE RASTER
        } else if (pixelFormat == AVKey.IMAGE && imageColorFormat == AVKey.COLOR) {
            // RETURNS BUFFERED IMAGE RASTER
        }

        String message = Logging.getMessage("Geotiff.UnsupportedDataTypeRaster", pixelFormat);
        Logging.logger().severe(message);
        throw new IOException(message);
    }

    private static int getRasterType(GeoCodec gc) {
        int[] v = gc.getGeoKeyAsInts(GeoTiff.GeoKey.RasterType);
        return (null != v && v.length > 0) ? v[0] : GeoTiff.Undefined;
    }

    @Override
    public void dispose() {
        metadata = null;
    }

}
