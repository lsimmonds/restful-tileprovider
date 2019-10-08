package simple_service.restful_service.wms;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.sf.Point;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;

public class TileProcessor {
    protected TileDaoProvider tileDaoProvider;
    protected GetMapRequest mapRequest;
    protected boolean debug = false;
    protected boolean transparent = false;

    public TileProcessor() {
        this.mapRequest = null;
        tileDaoProvider = null;
        this.debug = false;
        this.transparent = false;
    }

    public TileProcessor(GetMapRequest mapRequest) {
        this.mapRequest = mapRequest;
        tileDaoProvider = new TileDaoProvider(mapRequest);
    }

    public TileProcessor(GetMapRequest mapRequest, boolean debug) {
        this(mapRequest);
        this.debug = debug;
    }

    public TileProcessor(GetMapRequest mapRequest, boolean debug, boolean transparent) {
        this(mapRequest);
        this.debug = debug;
        this.transparent = transparent;
    }

    public Point appendBufferedImage(BufferedImage originalImg,
                                     BufferedImage appendedImg,
                                     int x,
                                     int y,
                                     int leftCut,
                                     int topCut,
                                     int rightCut,
                                     int bottomCut,
                                     double xScale,
                                     double yScale,
                                     int transCount) {
        int scaledWidth = getScaledWidth(appendedImg, leftCut, rightCut, xScale);
        int scaledHeight = getScaledHeight(appendedImg, topCut, bottomCut, yScale);

        int imgType = appendedImg.getType();
        BufferedImage resized = getBufferedImage(transCount, scaledWidth, scaledHeight, imgType);
        Graphics2D scaledAppender = resized.createGraphics();
        scaledAppender.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (this.transparent && transCount > 0) {
            Composite translucent = getComposite();
            scaledAppender.setComposite(translucent);
        }
        Point point = getPoint(scaledWidth, scaledHeight);
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

        if (this.debug) {
            drawBoundary(scaledWidth, scaledHeight, scaledAppender);
        }

        scaledAppender.dispose();

        Graphics2D g2 = originalImg.createGraphics();
        Color oldColor = g2.getColor();
        g2.setPaint(Color.BLACK);
        g2.setColor(oldColor);
        g2.drawImage(resized, null, x, y);
        g2.dispose();

        return point;
    }

    public int getZoom(TileDao tileDao) {
        double maxLon = tileDao.getTileMatrixSet().getMaxX();
        double minLon = tileDao.getTileMatrixSet().getMinX();
        double maxLat = tileDao.getTileMatrixSet().getMaxY();
        double minLat = tileDao.getTileMatrixSet().getMinY();
        double layerLonTotal = maxLon - minLon;
        double layerLatTotal = maxLat - minLat;
        BoundingBox bbox = getMapRequest().getBbox();
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

        BufferedImage fullBufferedImage = getMapImage();

        int transCount = 0;
        for (TileDao tileDao : tileDaoProvider.getTileDaos()) {
            int zoom = getZoom(tileDao);
            TileGrid tileGrid = getTileGrid(zoom);

            TileMatrix tileMatrix = tileDao.getTileMatrix(zoom);
            //Get Lat lon for tileGrid to see if we need to cut any off.
            BoundingBox tileGridLatLon = getBoundingBox(zoom, tileGrid);

            double leftChopPercent = 0;
            double rightChopPercent = 0;
            double topChopPercent = 0;
            double bottomChopPercent = 0;
            if ((tileGridLatLon.getMinLatitude() < mapRequest.getBbox().getMinLatitude())
                    || (tileGridLatLon.getMaxLatitude() > mapRequest.getBbox().getMaxLatitude())) {
                double deltaTopLat = tileGridLatLon.getMaxLatitude() - mapRequest.getBbox().getMaxLatitude();
                double deltaBottomLat = mapRequest.getBbox().getMinLatitude() - tileGridLatLon.getMinLatitude();
                double latPerTile = getLatPerTile(tileMatrix);
                topChopPercent = deltaTopLat / latPerTile;
                bottomChopPercent = deltaBottomLat / latPerTile;
            }
            if ((tileGridLatLon.getMinLongitude() < mapRequest.getBbox().getMinLongitude())
                    || (tileGridLatLon.getMaxLongitude() > mapRequest.getBbox().getMaxLongitude())) {
                double lonPerTile = getLonPerTile(tileMatrix);
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

            int xPlace = 0;
            int yPlace = 0;
            int lastX = xPlace;
            int lastY = yPlace;
            int lastXTracker = xPlace;
            int lastYTracker = yPlace;
            for (long row = tileGrid.getMinY(); row <= tileGrid.getMaxY(); row++) {
                for (long col = tileGrid.getMinX(); col <= tileGrid.getMaxX(); col++) {
                    try {
                        TileRow tileRow = tileDao.queryForTile(col, row, zoom);
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
                                    targetHeight / tileHeight,
                                    transCount);
                            lastXTracker = (int) (lastX + point.getX());
                            lastYTracker = (int) (lastY + point.getY());
                        } else {
                            lastXTracker = (int) (lastX + targetWidth);
                            lastYTracker = (int) (lastY + targetHeight);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    lastX = lastXTracker;
                    xPlace++;
                }
                xPlace = 0;
                lastX = 0;
                lastY = lastYTracker;
                yPlace++;
            }
            transCount++;
        }
        return fullBufferedImage;
    }

    protected double getLonPerTile(TileMatrix tileMatrix) {
        return TileBoundingBoxUtils.tileSizeLonPerWGS84Side((int) tileMatrix.getMatrixWidth());
    }

    protected double getLatPerTile(TileMatrix tileMatrix) {
        return TileBoundingBoxUtils.tileSizeLatPerWGS84Side((int) tileMatrix.getMatrixHeight());
    }

    protected BufferedImage getMapImage() {
        return new BufferedImage(mapRequest.getWidth(), mapRequest.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    protected BoundingBox getBoundingBox(int zoom, TileGrid tileGrid) {
        return TileBoundingBoxUtils.getWGS84BoundingBox(tileGrid, zoom);
    }

    protected TileGrid getTileGrid(int zoom) {
        return TileBoundingBoxUtils.getTileGridWGS84(mapRequest.getBbox(), zoom);
    }

    public GetMapRequest getMapRequest() {
        return mapRequest;
    }

    public void setMapRequest(GetMapRequest mapRequest) {
        this.mapRequest = mapRequest;
    }


    protected void drawBoundary(int scaledWidth, int scaledHeight, Graphics2D scaledAppender) {
        scaledAppender.setStroke(new BasicStroke(1));
        scaledAppender.drawRect(0, 0, scaledWidth, scaledHeight);
    }

    protected Point getPoint(int scaledWidth, int scaledHeight) {
        return new Point(scaledWidth, scaledHeight);
    }

    protected int getScaledHeight(BufferedImage appendedImg, int topCut, int bottomCut, double yScale) {
        return (int) Math.round((appendedImg.getHeight() - topCut - bottomCut) * yScale);
    }

    protected int getScaledWidth(BufferedImage appendedImg, int leftCut, int rightCut, double xScale) {
        return (int) Math.round((appendedImg.getWidth() - leftCut - rightCut) * xScale);
    }

    protected Composite getComposite() {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    }

    protected BufferedImage getBufferedImage(int transCount, int scaledWidth, int scaledHeight, int imgType) {
        BufferedImage resized = null;
        if (this.transparent && transCount > 0) {
            resized = getTransparentBufferedImage(scaledWidth, scaledHeight);
        } else {
            resized = getRegularBufferedImage(scaledWidth, scaledHeight, imgType);
        }
        return resized;
    }

    protected BufferedImage getRegularBufferedImage(int scaledWidth, int scaledHeight, int imgType) {
        BufferedImage resized;
        resized = new BufferedImage(scaledWidth, scaledHeight, imgType);
        return resized;
    }

    protected BufferedImage getTransparentBufferedImage(int scaledWidth, int scaledHeight) {
        BufferedImage resized;
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(cs, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, scaledWidth, scaledHeight, 4, null);

        resized = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
        return resized;
    }
}
