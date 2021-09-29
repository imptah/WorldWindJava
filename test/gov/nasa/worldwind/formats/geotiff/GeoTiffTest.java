package gov.nasa.worldwind.formats.geotiff;

import gov.nasa.worldwind.data.ByteBufferRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.GeotiffRasterReader;
import gov.nasa.worldwind.formats.tiff.GeoTiffFileReader;
import gov.nasa.worldwind.formats.tiff.GeotiffReader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RunWith(JUnit4.class)
public class GeoTiffTest {

    private static final String ASTER_TIF = "testData/geotiff/ASTGTMV003_N35E033_dem.tif";
    private static final String SRTM_TIF = "testData/geotiff/n34_e032_1arc_v3.tif";

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

    @Test
    public void testGeoTiffFileDataRaster() throws IOException {
        File file = new File(ASTER_TIF);
        GeoTiffFileReader geoTiffFileReader = new GeoTiffFileReader(file);
        DataRaster[] dataRasters = geoTiffFileReader.readDataRaster();
        for (int i = 0; i < dataRasters.length; i++) {
            DataRaster dataRaster = dataRasters[i];
            System.out.println("Directory " + (i + 1) + " rasters -> " + dataRaster.getSector().toString());
        }
    }

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
