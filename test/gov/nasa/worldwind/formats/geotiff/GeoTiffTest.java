package gov.nasa.worldwind.formats.geotiff;

import gov.nasa.worldwind.data.ByteBufferRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.GeotiffRasterReader;
import gov.nasa.worldwind.formats.tiff.GeoTiffFileReader;
import gov.nasa.worldwind.formats.tiff.GeotiffReader;
import mil.nga.tiff.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class GeoTiffTest {

    private static final String ASTER_TIF = "testData/geotiff/ASTGTMV003_N35E033_dem.tif";
    private static final String ASTER_TIF_2 = "testData/geotiff/ASTGTMV003_N35E032_dem.tif";
    private static final String SRTM_TIF = "testData/geotiff/n34_e032_1arc_v3.tif";
    private static final String EXPORTED_TIF = "testData/geotiff/exported_elevation-1632381855947-10.tif";

    // Values: [1, 1, 0, 7, 1024, 0, 1, 2, 1025, 0, 1, 1, 2048, 0, 1, 4326, 2049, 34737, 7, 0, 2054, 0, 1, 9102, 2057, 34736, 1, 1, 2059, 34736, 1, 0]

    @Test
    public void testFile() {

        try {


            File is = new File(ASTER_TIF);

            TIFFImage tiffImage = TiffReader.readTiff(is);

            List<FileDirectory> fileDirectories = tiffImage.getFileDirectories();
            for (int i = 0; i < fileDirectories.size(); i++) {
                FileDirectory fileDirectory = fileDirectories.get(i);
                System.out.println();
                System.out.print("-- File Directory ");
                if (fileDirectories.size() > 1) {
                    System.out.print((i + 1) + " ");
                }
                System.out.println("--");

                for (FileDirectoryEntry entry : fileDirectory.getEntries()) {
                    System.out.println();
                    System.out.println(entry.getFieldTag() + " (" + entry.getFieldTag().getId() + ")");
                    System.out.println(entry.getFieldType() + " (" + entry.getFieldType().getBytes() + " bytes)");
                    System.out.println("Count: " + entry.getTypeCount());
                    System.out.println("Values: " + entry.getValues());
                }

                Rasters rasters = fileDirectory.readRasters();
                System.out.println();
                System.out.println("-- Rasters --");
                System.out.println();
                System.out.println("Width: " + rasters.getWidth());
                System.out.println("Height: " + rasters.getHeight());
                System.out.println("Number of Pixels: " + rasters.getNumPixels());
                System.out.println("Samples Per Pixel: " + rasters.getSamplesPerPixel());
                System.out.println("Bits Per Sample: " + rasters.getBitsPerSample());
                System.out.println();
                printPixel(rasters, 0, 0);

                printPixel(rasters, (int) (rasters.getWidth() / 2.0), (int) (rasters.getHeight() / 2.0));
                printPixel(rasters, rasters.getWidth() - 1, rasters.getHeight() - 1);

                System.out.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Print a pixel from the rasters
     *
     * @param rasters rasters
     * @param x       x coordinate
     * @param y       y coordinate
     */
    private static void printPixel(Rasters rasters, int x, int y) {
        System.out.print("Pixel x = " + x + ", y = " + y + ": [");
        Number[] pixel = rasters.getPixel(x, y);
        for (int i = 0; i < pixel.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(pixel[i]);
        }
        System.out.println("]");
    }


    @Test
    public void testOpenFile() {
        File file = new File(SRTM_TIF);
        GeotiffRasterReader r = new GeotiffRasterReader();
        try {
            DataRaster[] dataRasters = r.read(file, null);
            for (DataRaster dataRaster : dataRasters) {
                ByteBufferRaster byteBufferRaster = (ByteBufferRaster) dataRaster;
                System.out.println("---------------------");
                int height = dataRaster.getHeight();
                System.out.println("Height: " + height);
                int width = dataRaster.getWidth();
                System.out.println("Width: " + width);
                /*for (int y = 0; y < 2; y++) {
                    for (int x = 0; x < width; x++) {
                        double doubleAtPosition = byteBufferRaster.getDoubleAtPosition(y, x);
                        System.out.println("Pixel x = " + x + " y = " + y + " -> " + doubleAtPosition);
                    }
                }*/

                for (Map.Entry<String, Object> entry : dataRaster.getEntries()) {
                    System.out.println("AVList entry: " + entry.getKey() + " -> " + entry.getValue());
                }

                System.out.println(((ByteBufferRaster) dataRaster).getByteBuffer().toString());
                System.out.println("---------------------");
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

/*    @Test
    public void testGeoTiffFileDataRaster() throws IOException {
        File file = new File(EXPORTED_TIF);
        GeoTiffFileReader geoTiffFileReader = new GeoTiffFileReader(file);
        DataRaster[] dataRasters = geoTiffFileReader.readDataRaster();
        for (int i = 0; i < dataRasters.length; i++) {
            DataRaster dataRaster = dataRasters[i];
            System.out.println("Directory " + (i + 1) + " rasters -> " + dataRaster.getSector().toString());
        }
    }*/

    @Test
    public void testCompareGeoIntAndWWRasterResults() throws IOException {
        File file = new File(SRTM_TIF);

        // OLD WW READER
        GeotiffReader geotiffReader = new GeotiffReader(file);
        ByteBufferRaster oldRaster = (ByteBufferRaster) geotiffReader.readDataRaster()[0];

        // NEW READER
        GeoTiffFileReader geoTiffFileReader = new GeoTiffFileReader(file);
        ByteBufferRaster newRaster = (ByteBufferRaster) geoTiffFileReader.readDataRaster()[0];

        int width = newRaster.getWidth();
        int height = newRaster.getHeight();

        // CHECK FIRST TWO ROWS TO AVOID OUT OF MEMORY
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < width; x++) {
                double oldValue = oldRaster.getDoubleAtPosition(y, x);
                double newValue = newRaster.getDoubleAtPosition(y, x);
                System.out.println("Value x="+x+" y="+y+" "+oldValue+" - "+newValue);
                Assert.assertEquals(oldValue, newValue, 0.0);
            }
        }
    }


}
