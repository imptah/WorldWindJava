package gov.nasa.worldwind;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@RunWith(JUnit4.class)
public class ImageTransformationTest {

    @Test
    public void testMissionPlannerImageTransformation() {
        // get file
        File file = new File("testData/16.jpg");

        // set sector for tile (values are hardcoded - it's right values for the tile)
        Sector sector = new MercatorSector(0.3125, 0.375, Angle.fromDegrees(0.0), Angle.fromDegrees(11.25));
        // transform original image
        BufferedImage image = transformPixels(sector, file);

        // try to write image to file
        String transformedFilePath = "testData/16-transformed.jpg";
        try {
            File transformedFile = new File(transformedFilePath);
            ImageIO.write(image, "jpg", transformedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertTrue(new File(transformedFilePath).exists());
    }


    protected BufferedImage transformPixels(Sector sector, File file) {
        // Make parent transformations
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Transform mercator tile to equirectangular projection
        if (image != null) {
            int type = image.getType();
            switch (type) {
                case BufferedImage.TYPE_CUSTOM:
                case BufferedImage.TYPE_BYTE_BINARY:
                    type = BufferedImage.TYPE_INT_RGB;
                    break;
                case BufferedImage.TYPE_BYTE_INDEXED:
                    type = BufferedImage.TYPE_INT_ARGB;
                    break;
                default:
                    // leave value returned from image.getType()
                    break;
            }

            BufferedImage trans = new BufferedImage(image.getWidth(), image.getHeight(), type);
            double miny = ((MercatorSector) sector).getMinLatPercent();
            double maxy = ((MercatorSector) sector).getMaxLatPercent();
            for (int y = 0; y < image.getHeight(); y++) {
                double sy = 1.0 - y / (double) (image.getHeight() - 1);
                Angle lat = Angle.fromRadians(sy * sector.getDeltaLatRadians() + sector.getMinLatitude().radians);
                double dy = 1.0 - (MercatorSector.gudermannianInverse(lat) - miny) / (maxy - miny);
                dy = Math.max(0.0, Math.min(1.0, dy));
                int iy = (int) (dy * (image.getHeight() - 1));
                for (int x = 0; x < image.getWidth(); x++) {
                    trans.setRGB(x, y, image.getRGB(x, iy));
                }
            }
            return trans;
        } else {
            return null;
        }
    }

}
