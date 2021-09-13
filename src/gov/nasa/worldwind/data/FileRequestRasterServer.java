package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class FileRequestRasterServer implements RasterServer {

    /**
     * @param params required to contain values for keys: AVKey.FILE_NAME
     */
    @Override
    public ByteBuffer getRasterAsByteBuffer(AVList params) {

        if (params == null || !params.hasKey(AVKey.FILE_NAME)) {
            String message = "AVKey.FILE_NAME param not provided";
            Logging.logger().finest(message);
            throw new WWRuntimeException(message);
        }

        try {
            return getByteBuffer(params);
        } catch (IOException e) {
            String message = "Error reading file content";
            Logging.logger().finest(message);
            throw new WWRuntimeException(message);
        }
    }

    private ByteBuffer getByteBuffer(AVList reqParams) throws IOException {
        if (!reqParams.hasKey(AVKey.FILE_NAME)) {
            String message = Logging.getMessage("generic.MissingRequiredParameter", AVKey.FILE_NAME);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        String filePath = reqParams.getStringValue(AVKey.FILE_NAME);
        File rasterSourceFile = new File(filePath);
        InputStream inputStream = new FileInputStream(rasterSourceFile);
        return ByteBuffer.wrap(inputStream.readAllBytes());
    }


    @Override
    public Sector getSector() {
        return null; // not appropriate for this class
    }

}
