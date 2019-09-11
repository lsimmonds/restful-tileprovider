package simple_service.restful_service.wms;

import mil.nga.geopackage.BoundingBox;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by joshua.johnson on 4/23/2019.
 * Adapted for http rest service by leon.simmonds 9/10/2019
 */

public class GetMapRequest {
    static final Logger LOGGER = Logging.getLogger(GetMapRequest.class);
    private Map<String, String> mParameters = new HashMap<>();
    private List<String> layers = new ArrayList<>();
    private BoundingBox bbox = new BoundingBox();
    private int width;
    private int height;
    private String rootGPPath;

    public GetMapRequest(Map<String, String> parameters, String rootGeoPackagePath) {
        //Always store keys as lower case
        for (String string : parameters.keySet()) {
            mParameters.put(string.toLowerCase(), parameters.get(string));
        }
        mParameters = parameters;

        rootGPPath = rootGeoPackagePath;

        String layersString = mParameters.get("layers");

        //extract each layer name
        String[] layerNames;
        if (layersString != null) {
            layerNames = layersString.split(",");
        } else {
            //no layers provided?
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": No layers parameter found");
            throw new IllegalArgumentException("Missing parameter layers");
        }

        //extract width and height
        try {
            if (mParameters.containsKey("height") && mParameters.containsKey("width")) {
                width = Integer.parseInt(mParameters.get("width"));
                height = Integer.parseInt(mParameters.get("height"));
            } else {
                throw new IllegalArgumentException("Missing parameter width or height");
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            width = 256;
            height = 256;
        }

        //extract bounding box
        if (!mParameters.containsKey("bbox")) {
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Bounding box not present");
            throw new IllegalArgumentException("Missing parameter bbox");
        }
        String bboxString = mParameters.get("bbox");
        String[] bboxParams = bboxString.split(",");
        if (bboxParams.length != 4) {
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Bounding box malformed?");
            throw new IllegalArgumentException("Malformed parameter bbox");
        }
        try {
            bbox.setMinLongitude(Double.parseDouble(bboxParams[0]));
            bbox.setMinLatitude(Double.parseDouble(bboxParams[1]));
            bbox.setMaxLongitude(Double.parseDouble(bboxParams[2]));
            bbox.setMaxLatitude(Double.parseDouble(bboxParams[3]));
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not parse Bounding Box");
            throw new IllegalArgumentException("Malformed parameter bbox");
        }

        for (String layerName : layerNames) {
            int index = layerName.indexOf(':');
            if (index != -1 && index != layerName.length()) {
                layerName = layerName.substring(index + 1);
            }
            layers.add(layerName);
        }
    }

    public List<String> getLayers() {
        return layers;
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getRootGPPath() {
        return rootGPPath;
    }
}
