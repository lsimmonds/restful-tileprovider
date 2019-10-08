package simple_service.restful_service.wms;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSet;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.sf.Point;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileProcessorTest {
    @Spy
    TileProcessor mockTileProcessor;

    @Mock
    TileDaoProvider mockTileDaoProvider;

    @Mock
    GetMapRequest mockGetMapRequest;

    @Mock
    TileDao mockTileDao;

    @Mock
    GeoPackage mockGeoPackage;

    @Mock
    File mockFile;

    @Mock
    GeoPackageManager mockGeoPackageManager;

    List<String> mockLayers = new ArrayList<>();

    @BeforeMethod
    public void setUp() {
        mockGetMapRequest = Mockito.mock(GetMapRequest.class);
        mockLayers.add("mockLayer");
        Mockito.when(mockGetMapRequest.getLayers()).thenReturn(mockLayers);
        Mockito.when(mockGetMapRequest.getRootGPPath()).thenReturn("mockGPPath");
        mockTileDaoProvider = Mockito.mock(TileDaoProvider.class);
        mockTileDaoProvider.setMapRequest(mockGetMapRequest);
        mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.exists()).thenReturn(true);
        Mockito.doReturn(mockFile).when(mockTileDaoProvider).getFile(Mockito.anyString(), Mockito.anyString());
        mockGeoPackageManager = Mockito.mock(GeoPackageManager.class);
        mockGeoPackage = Mockito.mock(GeoPackage.class);
        Mockito.doReturn(mockGeoPackage).when(mockTileDaoProvider).getGeoPackage(Mockito.any(File.class));
        mockTileDao = Mockito.mock(TileDao.class);
        Mockito.doReturn(mockTileDao).when(mockGeoPackage).getTileDao("mockTileTable");
        mockTileProcessor = Mockito.spy(TileProcessor.class);
        Mockito.doReturn(mockGetMapRequest).when(mockTileProcessor).getMapRequest();
    }

    @Test
    public void testAppendBufferedImage1() {
        BufferedImage mockOrigImage = Mockito.mock(BufferedImage.class);
        BufferedImage mockAppendingImage = Mockito.mock(BufferedImage.class);
        int testX = 0;
        int testY = 0;
        int testLeftCut = 0;
        int testTopCut = 0;
        int testRightCut = 0;
        int testBottomCut = 0;
        double testXScale = .75;
        double testYScale = .75;
        int testTransCount = 2;

        Mockito.doReturn(40).when(mockTileProcessor).getScaledWidth(mockAppendingImage,
                testLeftCut, testRightCut, testXScale);
        Mockito.doReturn(50).when(mockTileProcessor).getScaledHeight(mockAppendingImage,
                testTopCut, testBottomCut, testYScale);
        mockTileProcessor.transparent = true;
        mockTileProcessor.debug = true;
        BufferedImage mockResizedImage = Mockito.mock(BufferedImage.class);
        Mockito.doReturn(2).when(mockAppendingImage).getType();
//        Mockito.doCallRealMethod(mockResizedImage).when(mockTileProcessor).getBufferedImage(testTransCount, 40, 50, 2);
        Mockito.when(mockTileProcessor.getBufferedImage(testTransCount, 40, 50, 2)).thenReturn(mockResizedImage);
        Graphics2D mockScaledAppender = Mockito.mock(Graphics2D.class);
        Mockito.doReturn(mockScaledAppender).when(mockResizedImage).createGraphics();
        Mockito.doReturn(50).when(mockAppendingImage).getWidth();
        Mockito.doReturn(50).when(mockAppendingImage).getHeight();
        Graphics2D mockG2 = Mockito.mock(Graphics2D.class);
        Mockito.doReturn(mockG2).when(mockOrigImage).createGraphics();
        Color mockColor = Mockito.mock(Color.class);
        Mockito.doReturn(mockColor).when(mockG2).getColor();

        Point pointReturn = mockTileProcessor.appendBufferedImage(mockOrigImage, mockAppendingImage, testX, testY, testLeftCut, testTopCut,
                testRightCut, testBottomCut, testXScale, testYScale, testTransCount);
        //It gets the width properly scaled
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getScaledWidth(mockAppendingImage,
                testLeftCut, testRightCut, testXScale);
        //It gets the height properly scaled
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getScaledHeight(mockAppendingImage,
                testTopCut, testBottomCut, testYScale);
        //If transparent is set and it's not the first layer it sets composite translucent
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getComposite();
        Mockito.verify(mockScaledAppender, Mockito.times(1)).setComposite(Mockito.any(Composite.class));
        //It draws a scaled, cropped, appended image to a new image
        Mockito.verify(mockScaledAppender, Mockito.times(1)).drawImage(mockAppendingImage,
                0, 0, 40, 50, 0 + testLeftCut, 0 + testTopCut, 50 - testRightCut,
                50 - testBottomCut, Color.CYAN, null);
        //If debug is set it draws a border
        Mockito.verify(mockTileProcessor, Mockito.times(1)).drawBoundary(40, 50, mockScaledAppender);
        //It disposes the scaled appender Graphics object
        Mockito.verify(mockScaledAppender, Mockito.times(1)).dispose();
        //It draws the scaled cropped images to the original image
        Mockito.verify(mockG2, Mockito.times(1)).drawImage(mockResizedImage, null, testX, testY);
        //It disposes the ga Graphics object
        Mockito.verify(mockG2, Mockito.times(1)).dispose();
        //It returns the point of the bottom right corner
        org.testng.Assert.assertEquals(40.0, pointReturn.getX());
        org.testng.Assert.assertEquals(50.0, pointReturn.getY());

        //If transparent is not set or it's not the first layer it creates a regular image
        //If debug is not set it does not draw a border
    }

    @Test
    public void testAppendBufferedImage2() {
        BufferedImage mockOrigImage = Mockito.mock(BufferedImage.class);
        BufferedImage mockAppendingImage = Mockito.mock(BufferedImage.class);
        int testX = 0;
        int testY = 0;
        int testLeftCut = 0;
        int testTopCut = 0;
        int testRightCut = 0;
        int testBottomCut = 0;
        double testXScale = .75;
        double testYScale = .75;
        int testTransCount = 2;

        Mockito.doReturn(40).when(mockTileProcessor).getScaledWidth(mockAppendingImage,
                testLeftCut, testRightCut, testXScale);
        Mockito.doReturn(50).when(mockTileProcessor).getScaledHeight(mockAppendingImage,
                testTopCut, testBottomCut, testYScale);
        mockTileProcessor.transparent = false;
        mockTileProcessor.debug = false;
        BufferedImage mockResizedImage = Mockito.mock(BufferedImage.class);
        Mockito.doReturn(2).when(mockAppendingImage).getType();
        Mockito.when(mockTileProcessor.getBufferedImage(testTransCount, 40, 50, 2)).thenReturn(mockResizedImage);
        Graphics2D mockScaledAppender = Mockito.mock(Graphics2D.class);
        Mockito.doReturn(mockScaledAppender).when(mockResizedImage).createGraphics();
        Mockito.doReturn(50).when(mockAppendingImage).getWidth();
        Mockito.doReturn(50).when(mockAppendingImage).getHeight();
        Graphics2D mockG2 = Mockito.mock(Graphics2D.class);
        Mockito.doReturn(mockG2).when(mockOrigImage).createGraphics();
        Color mockColor = Mockito.mock(Color.class);
        Mockito.doReturn(mockColor).when(mockG2).getColor();

        Point pointReturn = mockTileProcessor.appendBufferedImage(mockOrigImage, mockAppendingImage, testX, testY, testLeftCut, testTopCut,
                testRightCut, testBottomCut, testXScale, testYScale, testTransCount);

        //If debug is not set it does not draw a border
        Mockito.verify(mockTileProcessor, Mockito.never()).drawBoundary(40, 50, mockScaledAppender);
    }

    @Test
    public void testAppendBufferedImage3() {
        BufferedImage mockOrigImage = Mockito.mock(BufferedImage.class);
        BufferedImage mockAppendingImage = Mockito.mock(BufferedImage.class);
        int testX = 0;
        int testY = 0;
        int testLeftCut = 0;
        int testTopCut = 0;
        int testRightCut = 0;
        int testBottomCut = 0;
        double testXScale = .75;
        double testYScale = .75;
        int testTransCount = 2;

        Mockito.doReturn(40).when(mockTileProcessor).getScaledWidth(mockAppendingImage,
                testLeftCut, testRightCut, testXScale);
        Mockito.doReturn(50).when(mockTileProcessor).getScaledHeight(mockAppendingImage,
                testTopCut, testBottomCut, testYScale);
        mockTileProcessor.transparent = false;
        mockTileProcessor.debug = false;
        Mockito.doReturn(2).when(mockAppendingImage).getType();
        Mockito.doReturn(50).when(mockAppendingImage).getWidth();
        Mockito.doReturn(50).when(mockAppendingImage).getHeight();
        Graphics2D mockG2 = Mockito.mock(Graphics2D.class);
        Mockito.doReturn(mockG2).when(mockOrigImage).createGraphics();
        Color mockColor = Mockito.mock(Color.class);
        Mockito.doReturn(mockColor).when(mockG2).getColor();

        Point pointReturn = mockTileProcessor.appendBufferedImage(mockOrigImage, mockAppendingImage, testX, testY, testLeftCut, testTopCut,
                testRightCut, testBottomCut, testXScale, testYScale, testTransCount);

        //If transparent is not set or it's not the first layer it creates a regular image
        Mockito.verify(mockTileProcessor, Mockito.never()).getTransparentBufferedImage(40, 50);
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getRegularBufferedImage(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void testAppendBufferedImage4() {
        BufferedImage mockOrigImage = Mockito.mock(BufferedImage.class);
        BufferedImage mockAppendingImage = Mockito.mock(BufferedImage.class);
        int testX = 0;
        int testY = 0;
        int testLeftCut = 0;
        int testTopCut = 0;
        int testRightCut = 0;
        int testBottomCut = 0;
        double testXScale = .75;
        double testYScale = .75;
        int testTransCount = 2;

        Mockito.doReturn(40).when(mockTileProcessor).getScaledWidth(mockAppendingImage,
                testLeftCut, testRightCut, testXScale);
        Mockito.doReturn(50).when(mockTileProcessor).getScaledHeight(mockAppendingImage,
                testTopCut, testBottomCut, testYScale);
        mockTileProcessor.transparent = true;
        mockTileProcessor.debug = true;
        Mockito.doReturn(2).when(mockAppendingImage).getType();
        Graphics2D mockScaledAppender = Mockito.mock(Graphics2D.class);
        Mockito.doReturn(50).when(mockAppendingImage).getWidth();
        Mockito.doReturn(50).when(mockAppendingImage).getHeight();
        Graphics2D mockG2 = Mockito.mock(Graphics2D.class);
        Mockito.doReturn(mockG2).when(mockOrigImage).createGraphics();
        Color mockColor = Mockito.mock(Color.class);
        Mockito.doReturn(mockColor).when(mockG2).getColor();

        Point pointReturn = mockTileProcessor.appendBufferedImage(mockOrigImage, mockAppendingImage, testX, testY, testLeftCut, testTopCut,
                testRightCut, testBottomCut, testXScale, testYScale, testTransCount);

        //If transparent is set and it's not the first layer it creates a transparent image
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getTransparentBufferedImage(40, 50);
        Mockito.verify(mockTileProcessor, Mockito.never()).getRegularBufferedImage(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void testGetZoom() {
        TileMatrixSet mockTileMatrixSet = Mockito.mock(TileMatrixSet.class);
        Mockito.doReturn(mockTileMatrixSet).when(mockTileDao).getTileMatrixSet();
        Mockito.doReturn(180.0).when(mockTileMatrixSet).getMaxX();
        Mockito.doReturn(90.0).when(mockTileMatrixSet).getMaxY();
        Mockito.doReturn(-180.0).when(mockTileMatrixSet).getMinX();
        Mockito.doReturn(-90.0).when(mockTileMatrixSet).getMinY();
        BoundingBox testBoundingBox = new BoundingBox(-15, -20, 15, 20);
        Mockito.doReturn(testBoundingBox).when(mockGetMapRequest).getBbox();
        Mockito.doReturn(5L).when(mockTileDao).getMaxZoom();

        TileMatrix mockTileMatrix5 = Mockito.mock(TileMatrix.class);
        TileMatrix mockTileMatrix4 = Mockito.mock(TileMatrix.class);
        TileMatrix mockTileMatrix3 = Mockito.mock(TileMatrix.class);
        TileMatrix mockTileMatrix2 = Mockito.mock(TileMatrix.class);
        TileMatrix mockTileMatrix1 = Mockito.mock(TileMatrix.class);
        Mockito.doReturn(32L).when(mockTileMatrix5).getMatrixWidth();
        Mockito.doReturn(16L).when(mockTileMatrix5).getMatrixHeight();
        Mockito.doReturn(16L).when(mockTileMatrix4).getMatrixWidth();
        Mockito.doReturn(8L).when(mockTileMatrix4).getMatrixHeight();
        Mockito.doReturn(8L).when(mockTileMatrix3).getMatrixWidth();
        Mockito.doReturn(4L).when(mockTileMatrix3).getMatrixHeight();
        Mockito.doReturn(4L).when(mockTileMatrix2).getMatrixWidth();
        Mockito.doReturn(2L).when(mockTileMatrix2).getMatrixHeight();
        Mockito.doReturn(2L).when(mockTileMatrix1).getMatrixWidth();
        Mockito.doReturn(1L).when(mockTileMatrix1).getMatrixHeight();

        Mockito.doReturn(mockTileMatrix5).when(mockTileDao).getTileMatrix(4);
        Mockito.doReturn(mockTileMatrix4).when(mockTileDao).getTileMatrix(3);
        Mockito.doReturn(mockTileMatrix3).when(mockTileDao).getTileMatrix(2);
        Mockito.doReturn(mockTileMatrix2).when(mockTileDao).getTileMatrix(1);
        Mockito.doReturn(mockTileMatrix1).when(mockTileDao).getTileMatrix(0);

        int gotZoom = mockTileProcessor.getZoom(mockTileDao);

        //It gets the max Zoom for the Tile Dao
        Mockito.verify(mockTileDao, Mockito.times(1)).getMaxZoom();

        //It picks first zoom level where a tile is smaller than request bbox
        org.testng.Assert.assertEquals(gotZoom, 3);
    }

    @Test
    public void testGetMap() throws IOException {
        BufferedImage mockBufferedImage = Mockito.mock(BufferedImage.class);
        Mockito.doReturn(mockBufferedImage).when(mockTileProcessor).getMapImage();
        List<TileDao> mockTileDaos = new ArrayList<>();
        TileDao mockTileDao1 = Mockito.mock(TileDao.class);
        TileDao mockTileDao2 = Mockito.mock(TileDao.class);
        mockTileDaos.add(mockTileDao1);
        mockTileDaos.add(mockTileDao2);
        TileRow mockTileRow = Mockito.mock(TileRow.class);
        BufferedImage mockTileDataimage = Mockito.mock(BufferedImage.class);
        Mockito.doReturn(256).when(mockTileDataimage).getWidth();
        Mockito.doReturn(256).when(mockTileDataimage).getHeight();
        Mockito.doReturn(mockTileDataimage).when(mockTileRow).getTileDataImage();
        Mockito.doReturn(mockTileRow).when(mockTileDao1).queryForTile(Mockito.anyLong(), Mockito.anyLong(),
                Mockito.anyLong());
        Mockito.doReturn(mockTileRow).when(mockTileDao2).queryForTile(Mockito.anyLong(), Mockito.anyLong(),
                Mockito.anyLong());
        Mockito.doReturn(2).when(mockTileProcessor).getZoom(mockTileDao1);
        Mockito.doReturn(5).when(mockTileProcessor).getZoom(mockTileDao2);
        TileGrid mockTileGrid1 = Mockito.mock(TileGrid.class);
        TileGrid mockTileGrid2 = Mockito.mock(TileGrid.class);
        Mockito.doReturn(0L).when(mockTileGrid1).getMinX();
        Mockito.doReturn(0L).when(mockTileGrid1).getMinY();
        Mockito.doReturn(2L).when(mockTileGrid1).getMaxX();
        Mockito.doReturn(1L).when(mockTileGrid1).getMaxY();
        Mockito.doReturn(0L).when(mockTileGrid2).getMinX();
        Mockito.doReturn(0L).when(mockTileGrid2).getMinY();
        Mockito.doReturn(4L).when(mockTileGrid2).getMaxX();
        Mockito.doReturn(2L).when(mockTileGrid2).getMaxY();
        Mockito.doReturn(mockTileGrid1).when(mockTileProcessor).getTileGrid(2);
        Mockito.doReturn(mockTileGrid2).when(mockTileProcessor).getTileGrid(5);
        BoundingBox mockBoundingBox1 = Mockito.mock(BoundingBox.class);
        Mockito.doReturn(-65.0).when(mockBoundingBox1).getMinLatitude();
        Mockito.doReturn(-80.0).when(mockBoundingBox1).getMinLongitude();
        Mockito.doReturn(65.0).when(mockBoundingBox1).getMaxLatitude();
        Mockito.doReturn(90.0).when(mockBoundingBox1).getMaxLongitude();
        BoundingBox mockBoundingBox2 = Mockito.mock(BoundingBox.class);
        Mockito.doReturn(-85.0).when(mockBoundingBox1).getMinLatitude();
        Mockito.doReturn(-100.0).when(mockBoundingBox1).getMinLongitude();
        Mockito.doReturn(85.0).when(mockBoundingBox1).getMaxLatitude();
        Mockito.doReturn(100.0).when(mockBoundingBox1).getMaxLongitude();
        Mockito.doReturn(mockBoundingBox1).when(mockTileProcessor).getBoundingBox(2, mockTileGrid1);
        Mockito.doReturn(mockBoundingBox2).when(mockTileProcessor).getBoundingBox(5, mockTileGrid2);
        BoundingBox mockRequestBox = Mockito.mock(BoundingBox.class);
        Mockito.doReturn(-75.0).when(mockRequestBox).getMinLatitude();
        Mockito.doReturn(-90.0).when(mockRequestBox).getMinLongitude();
        Mockito.doReturn(75.0).when(mockRequestBox).getMaxLatitude();
        Mockito.doReturn(90.0).when(mockRequestBox).getMaxLongitude();
        Mockito.doReturn(mockRequestBox).when(mockGetMapRequest).getBbox();
        TileMatrix mockTileMatrix1 = Mockito.mock(TileMatrix.class);
        TileMatrix mockTileMatrix2 = Mockito.mock(TileMatrix.class);
        Mockito.doReturn(mockTileMatrix1).when(mockTileDao1).getTileMatrix(2);
        Mockito.doReturn(mockTileMatrix2).when(mockTileDao2).getTileMatrix(5);
        mockTileProcessor.tileDaoProvider = mockTileDaoProvider;
        Mockito.doReturn(mockTileDaos).when(mockTileDaoProvider).getTileDaos();
        mockTileProcessor.mapRequest = mockGetMapRequest;
        Mockito.doReturn(50.0).when(mockTileProcessor).getLatPerTile(mockTileMatrix1);
        Mockito.doReturn(75.0).when(mockTileProcessor).getLatPerTile(mockTileMatrix2);
        Mockito.doReturn(50.0).when(mockTileProcessor).getLonPerTile(mockTileMatrix1);
        Mockito.doReturn(75.0).when(mockTileProcessor).getLonPerTile(mockTileMatrix2);
        Point mockPoint = Mockito.mock(Point.class);
        Mockito.doReturn(mockPoint).when(mockTileProcessor).appendBufferedImage(Mockito.any(BufferedImage.class),
                Mockito.any(BufferedImage.class), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt());

        BufferedImage bufferedImage = mockTileProcessor.getMap();

        //It creates a new BufferedImage
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getMapImage();
        //It process all tile daos
        //It gets zoom for all tile daos
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getZoom(mockTileDao1);
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getZoom(mockTileDao2);
        //It gets tile grid
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getTileGrid(2);
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getTileGrid(5);
        //It gets tile matrix
        Mockito.verify(mockTileDao1, Mockito.times(1)).getTileMatrix(2);
        Mockito.verify(mockTileDao2, Mockito.times(1)).getTileMatrix(5);
        //It gets WGS84BoundingBox from tile grid
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getBoundingBox(2, mockTileGrid1);
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getBoundingBox(5, mockTileGrid2);
        //If tile grid is taller that request it determines what to cut
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getLatPerTile(mockTileMatrix1);
        Mockito.verify(mockTileProcessor, Mockito.never()).getLatPerTile(mockTileMatrix2);
        //If tile grid is wider that request it determines what to cut
        Mockito.verify(mockTileProcessor, Mockito.times(1)).getLonPerTile(mockTileMatrix1);
        Mockito.verify(mockTileProcessor, Mockito.never()).getLonPerTile(mockTileMatrix2);
        //It tries to get tile for every row in tile Grid
        Mockito.verify(mockTileDao1, Mockito.times(1)).queryForTile(0L, 0L, 2L);
        Mockito.verify(mockTileDao1, Mockito.times(1)).queryForTile(0L, 1L, 2L);
        Mockito.verify(mockTileDao1, Mockito.times(1)).queryForTile(1L, 0L, 2L);
        Mockito.verify(mockTileDao1, Mockito.times(1)).queryForTile(1L, 1L, 2L);
        Mockito.verify(mockTileDao1, Mockito.times(1)).queryForTile(2L, 0L, 2L);
        Mockito.verify(mockTileDao1, Mockito.times(1)).queryForTile(2L, 1L, 2L);

        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(0L, 0L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(0L, 1L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(0L, 2L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(1L, 0L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(1L, 1L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(1L, 2L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(2L, 0L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(2L, 1L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(2L, 2L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(3L, 0l, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(3L, 1L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(3L, 2L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(4L, 0L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(4L, 1L, 5L);
        Mockito.verify(mockTileDao2, Mockito.times(1)).queryForTile(4L, 2L, 5L);
        //If Tile exists it calls appendBufferedImage with tile place, cuts, and scale
        Mockito.verify(mockTileProcessor, Mockito.times(21)).appendBufferedImage(Mockito.any(BufferedImage.class),
                Mockito.any(BufferedImage.class), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt());
    }

}