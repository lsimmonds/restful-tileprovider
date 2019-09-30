package simple_service.restful_service.wms;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.tiles.user.TileDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TileDaoProvider {
    protected List<GeoPackage> geoPackages = new ArrayList<>();
    private GetMapRequest mapRequest;
    private List<TileDao> tileDaos = new ArrayList<>();

    public TileDaoProvider() {
    }

    public TileDaoProvider(GetMapRequest mapRequest) {
        this.mapRequest = mapRequest;
        setGeoPackages(mapRequest.getLayers(), mapRequest.getRootGPPath());
        setTileDaos(geoPackages);
    }

    public void setGeoPackages(List<String> layers, String rootGPPath) {
        for (String layer : layers) {
            //Change ":" to "-"
            String fileName = layer.replace(":", "-") + ".gpkg";
            File newFile = getFile(rootGPPath, fileName);
            GeoPackage geoPackage;
            if (newFile.exists()) {
                geoPackage = getGeoPackage(newFile);
                geoPackages.add(geoPackage);
            }
        }
    }

    public GeoPackage getGeoPackage(File newFile) {
        GeoPackage geoPackage;
        geoPackage = GeoPackageManager.open(newFile);
        return geoPackage;
    }

    public void setMapRequest(GetMapRequest mapRequest) {
        this.mapRequest = mapRequest;
    }

    protected File getFile(String rootGPPath, String fileName) {
        return new File(rootGPPath, fileName);
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
