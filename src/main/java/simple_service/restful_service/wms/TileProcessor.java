package simple_service.restful_service.wms;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class TileProcessor {
    TileDaoProvider tileDaoProvider;
    GetMapRequest mapRequest;

    public TileProcessor(GetMapRequest mapRequest) {
        this.mapRequest = mapRequest;
        tileDaoProvider = new TileDaoProvider(mapRequest);
    }

    public int getZoom() {
        //TODO: This is the point where we throw away all other levels and daos and just process the top layer
        //TODO: enhance processing to allow for many Daos
        TileDao tileDao = tileDaoProvider.getTileDaos().get(0);
        double maxLon = tileDao.getTileMatrixSet().getMaxX();
        double minLon = tileDao.getTileMatrixSet().getMinX();
        double maxLat = tileDao.getTileMatrixSet().getMaxY();
        double minLat = tileDao.getTileMatrixSet().getMinY();
        double layerLonTotal = maxLon - minLon;
        double layerLatTotal = maxLat - minLat;
        BoundingBox bbox = mapRequest.getBbox();
        BoundingBox effectiveBox = new BoundingBox();
        effectiveBox.setMinLongitude(Math.max(bbox.getMinLongitude(), minLon));
        effectiveBox.setMinLatitude(Math.max(bbox.getMinLatitude(), minLat));
        effectiveBox.setMaxLongitude(Math.min(bbox.getMaxLongitude(), maxLon));
        effectiveBox.setMaxLatitude(Math.min(bbox.getMaxLatitude(), maxLat));
        double reqLonTotal = effectiveBox.getMaxLongitude() - effectiveBox.getMinLongitude();
        double reqLatTotal = effectiveBox.getMaxLatitude() - effectiveBox.getMinLatitude();

        long maxZoomLevel = tileDao.getMaxZoom();
        int zoomLevel = 0;
        double tileLonTotal;
        double tileLatTotal;
        TileMatrix tileMatrix = null;
        for (int i = 0; i <= maxZoomLevel; i++) {
            //Pick first zoom level where a tile is smaller than request bbox
            tileMatrix = tileDao.getTileMatrix(i);
            tileLonTotal = layerLonTotal / tileMatrix.getMatrixWidth();
            tileLatTotal = layerLatTotal / tileMatrix.getMatrixHeight();
            zoomLevel = i;
            if ((reqLatTotal >= tileLatTotal) && reqLonTotal >= tileLonTotal) {
                break;
            }
        }
        return zoomLevel;
    }

    public static void appendBufferedImage(BufferedImage originalImg,
                                           BufferedImage appendedImg,
                                           int x,
                                           int y,
                                           double xScale,
                                           double yScale) {
        int scaledWidth = (int) Math.round(appendedImg.getWidth() * xScale);
        int scaledHeight = (int) Math.round(appendedImg.getHeight() * yScale);

        BufferedImage resized = new BufferedImage(scaledWidth, scaledHeight, appendedImg.getType());
        Graphics2D scaledAppender = resized.createGraphics();
        scaledAppender.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        scaledAppender.drawImage(appendedImg, 0, 0, scaledWidth, scaledHeight, Color.CYAN, null);

        //For debugging                     ............................//
        scaledAppender.setStroke(new BasicStroke(1));             //
        scaledAppender.drawRect(0, 0, scaledWidth, scaledHeight); //
        //                                  ............................//

        scaledAppender.dispose();

        Graphics2D g2 = originalImg.createGraphics();
        Color oldColor = g2.getColor();
        g2.setPaint(Color.BLACK);
        g2.setColor(oldColor);
        g2.drawImage(resized, null, x, y);
        g2.dispose();
    }

    public BufferedImage getMap() {
        int zoom = getZoom();
        TileGrid tileGrid = TileBoundingBoxUtils.getTileGridWGS84(mapRequest.getBbox(), zoom);
        double targetHeight = mapRequest.getHeight() / (tileGrid.getMaxY() - tileGrid.getMinY() + 1);
        double targetWidth = mapRequest.getWidth() / (tileGrid.getMaxX() - tileGrid.getMinX() + 1);

        TileMatrix tileMatrix= tileDaoProvider.getTileDaos().get(0).getTileMatrix(zoom);
        int tileWidth = (int) tileMatrix.getTileWidth();
        int tileHeight = (int) tileMatrix.getTileHeight();

        BufferedImage fullBufferedImage = new BufferedImage(mapRequest.getWidth(), mapRequest.getHeight(), BufferedImage.TYPE_INT_RGB);

        int xPlace = 0;
        int yPlace = 0;
        for (long row = tileGrid.getMinY(); row <= tileGrid.getMaxY(); row++) {
            for (long col = tileGrid.getMinX(); col <= tileGrid.getMaxX(); col++) {
                try {
                    TileRow tileRow = tileDaoProvider.getTileDaos().get(0).queryForTile(col, row, zoom);
                    if (tileRow != null) {
                        appendBufferedImage(fullBufferedImage,
                                tileRow.getTileDataImage(),
                                (int) Math.floor(xPlace * targetWidth),
                                (int) Math.floor(yPlace * targetHeight),
                                targetWidth / tileWidth,
                                targetHeight / tileHeight);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                xPlace++;
            }
            xPlace = 0;
            yPlace++;
        }
        return fullBufferedImage;
    }
}
