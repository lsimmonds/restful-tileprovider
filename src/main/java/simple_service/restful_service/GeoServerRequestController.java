package simple_service.restful_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import simple_service.restful_service.wms.GetMapRequest;
import simple_service.restful_service.wms.GetMapResponse;
import simple_service.restful_service.wms.TileProcessor;

import java.io.IOException;
import java.util.Map;

@RestController
public class GeoServerRequestController {
    @Autowired
    public Environment env;

    @RequestMapping(produces = MediaType.IMAGE_JPEG_VALUE, value = {"/service-instances/{applicationName}", "/service-instances/ows", "/service-instances/wms"})
    public byte[] simpleResponse(@RequestParam Map<String, String> queryParameters) {
        GetMapRequest mapRequest = new GetMapRequest(queryParameters, env.getProperty("geopackage.rootPath"));
        boolean debug = env.getProperty("debug", Boolean.class) == null ? false : env.getProperty("debug", Boolean.class);
        TileProcessor tileProcessor = new TileProcessor(mapRequest, debug);

        try {
            return new GetMapResponse(tileProcessor.getMap()).getBody().readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
