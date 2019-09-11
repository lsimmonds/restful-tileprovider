package simple_service.restful_service.wms;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.tiles.user.TileDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TileDaoProvider {
    private GetMapRequest mapRequest;
    private List<TileDao> tileDaos = new ArrayList<>();
    private List<GeoPackage> geoPackages = new ArrayList<>();

    public TileDaoProvider(GetMapRequest mapRequest) {
        this.mapRequest = mapRequest;
        setGeoPackages(mapRequest.getLayers(), mapRequest.getRootGPPath());
        setTileDaos(geoPackages);
    }

    public void setGeoPackages(List<String> layers, String rootGPPath) {
        for (String layer : layers) {
            //Change ":" to "-"
            String fileName = layer.replace(":", "-") + ".gpkg";
            File newFile = new File(rootGPPath, fileName);
            GeoPackage geoPackage;
            if (newFile.exists()) {
                geoPackage = GeoPackageManager.open(newFile);
                geoPackages.add(geoPackage);
            }
        }
    }

    public List<TileDao> getTileDaos() {
        return tileDaos;
    }

    public void setTileDaos(List<GeoPackage> geoPackages) {
        geoPackages.forEach(geoPackage -> geoPackage.getTileTables().forEach(tileTable -> {
            tileDaos.add(geoPackage.getTileDao(tileTable));
        }));

    }
}
