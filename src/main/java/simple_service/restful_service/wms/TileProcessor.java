package simple_service.restful_service.wms;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.sf.Point;

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

    public static Point appendBufferedImage(BufferedImage originalImg,
                                            BufferedImage appendedImg,
                                            int x,
                                            int y,
                                            int leftCut,
                                            int topCut,
                                            int rightCut,
                                            int bottomCut,
                                            double xScale,
                                            double yScale) {
        int scaledWidth = (int) Math.round((appendedImg.getWidth() - leftCut - rightCut) * xScale);
        int scaledHeight = (int) Math.round((appendedImg.getHeight() - topCut - bottomCut) * yScale);

        BufferedImage resized = new BufferedImage(scaledWidth, scaledHeight, appendedImg.getType());
        Graphics2D scaledAppender = resized.createGraphics();
        scaledAppender.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Point point = new Point(scaledWidth, scaledHeight);
        scaledAppender.drawImage(appendedImg,
                0,
                0,
                scaledWidth,
                scaledHeight,
                0 + leftCut,
                0 + topCut,
                appendedImg.getWidth() - rightCut,
                appendedImg.getHeight() - bottomCut,
                Color.CYAN,
                null);

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

        return point;
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

    public BufferedImage getMap() {
        int zoom = getZoom();
        TileGrid tileGrid = TileBoundingBoxUtils.getTileGridWGS84(mapRequest.getBbox(), zoom);

        TileMatrix tileMatrix = tileDaoProvider.getTileDaos().get(0).getTileMatrix(zoom);
        //Get Lat lon for tileGrid to see if we need to cut any off.
        BoundingBox tileGridLatLon = TileBoundingBoxUtils.getWGS84BoundingBox(tileGrid, zoom);

        double leftChopPercent = 0;
        double rightChopPercent = 0;
        double topChopPercent = 0;
        double bottomChopPercent = 0;
        if ((tileGridLatLon.getMinLatitude() < mapRequest.getBbox().getMinLatitude())
                || (tileGridLatLon.getMaxLatitude() > mapRequest.getBbox().getMaxLatitude())) {
            double deltaTopLat = tileGridLatLon.getMaxLatitude() - mapRequest.getBbox().getMaxLatitude();
            double deltaBottomLat = mapRequest.getBbox().getMinLatitude() - tileGridLatLon.getMinLatitude();
            double latPerTile = TileBoundingBoxUtils.tileSizeLatPerWGS84Side((int) tileMatrix.getMatrixHeight());
            topChopPercent = deltaTopLat / latPerTile;
            bottomChopPercent = deltaBottomLat / latPerTile;
        }
        if ((tileGridLatLon.getMinLongitude() < mapRequest.getBbox().getMinLongitude())
                || (tileGridLatLon.getMaxLongitude() > mapRequest.getBbox().getMaxLongitude())) {
            double lonPerTile = TileBoundingBoxUtils.tileSizeLonPerWGS84Side((int) tileMatrix.getMatrixWidth());
            double deltaLeftLon = mapRequest.getBbox().getMinLongitude() - tileGridLatLon.getMinLongitude();
            double deltaRightLon = tileGridLatLon.getMaxLongitude() - mapRequest.getBbox().getMaxLongitude();
            leftChopPercent = deltaLeftLon / lonPerTile;
            rightChopPercent = deltaRightLon / lonPerTile;
        }
        long displayRows = (tileGrid.getMaxY() - tileGrid.getMinY() + 1);
        long displayColumns = (tileGrid.getMaxX() - tileGrid.getMinX() + 1);
        double targetHeight = mapRequest.getHeight() / ((double) displayRows - topChopPercent - bottomChopPercent);
        double targetWidth = mapRequest.getWidth() / ((double) displayColumns - leftChopPercent - rightChopPercent);
        int tileWidth = (int) tileMatrix.getTileWidth();
        int tileHeight = (int) tileMatrix.getTileHeight();

        BufferedImage fullBufferedImage = new BufferedImage(mapRequest.getWidth(), mapRequest.getHeight(), BufferedImage.TYPE_INT_RGB);

        int xPlace = 0;
        int yPlace = 0;
        int lastX = xPlace;
        int lastY = yPlace;
        int lastXtracker = xPlace;
        int lastYtracker = yPlace;
        for (long row = tileGrid.getMinY(); row <= tileGrid.getMaxY(); row++) {
            for (long col = tileGrid.getMinX(); col <= tileGrid.getMaxX(); col++) {
                try {
                    TileRow tileRow = tileDaoProvider.getTileDaos().get(0).queryForTile(col, row, zoom);
                    if (tileRow != null) {
                        int leftCut = (int) (tileRow.getTileDataImage().getWidth() * leftChopPercent);
                        int rightCut = (int) (tileRow.getTileDataImage().getWidth() * rightChopPercent);
                        int topCut = (int) (tileRow.getTileDataImage().getHeight() * topChopPercent);
                        int bottomCut = (int) (tileRow.getTileDataImage().getHeight() * bottomChopPercent);
                        Point point = appendBufferedImage(fullBufferedImage,
                                tileRow.getTileDataImage(),
                                lastX,
                                lastY,
                                xPlace == 0 ? leftCut : 0,
                                yPlace == 0 ? topCut : 0,
                                col == tileGrid.getMaxX() ? rightCut : 0,
                                row == tileGrid.getMaxY() ? bottomCut : 0,
                                targetWidth / tileWidth,
                                targetHeight / tileHeight);
                        lastXtracker = (int) (lastX + point.getX());
                        lastYtracker = (int) (lastY + point.getY());
                    } else {
                        lastXtracker = (int) (lastX + targetWidth);
                        lastYtracker = (int) (lastY + targetHeight);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lastX = lastXtracker;
                xPlace++;
            }
            xPlace = 0;
            lastX = 0;
            lastY = lastYtracker;
            yPlace++;
        }
        return fullBufferedImage;
    }
}
