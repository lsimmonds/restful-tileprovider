package simple_service.restful_service.wms;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.tiles.user.TileDao;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TileDaoProviderTest {

    @Mock
    GetMapRequest mockGetMapRequest;

    @Mock
    TileDao mockTileDao;

    @Mock
    GeoPackage mockGeoPackage;

    @Mock
    TileDaoProvider mockTileDaoProvider;

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
        mockTileDaoProvider = Mockito.spy(TileDaoProvider.class);
        mockTileDaoProvider.setMapRequest(mockGetMapRequest);
        mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.exists()).thenReturn(true);
        Mockito.doReturn(mockFile).when(mockTileDaoProvider).getFile(Mockito.anyString(), Mockito.anyString());
        mockGeoPackageManager = Mockito.mock(GeoPackageManager.class);
        mockGeoPackage = Mockito.mock(GeoPackage.class);
        Mockito.doReturn(mockGeoPackage).when(mockTileDaoProvider).getGeoPackage(Mockito.any(File.class));
    }

    @Test
    public void testSetGeoPackages() {
        mockTileDaoProvider.setGeoPackages(mockLayers, "mockGPPath");
        org.testng.Assert.assertEquals(mockTileDaoProvider.geoPackages.get(0), mockGeoPackage);
        org.testng.Assert.assertTrue(mockTileDaoProvider.geoPackages.size() == 1);
    }

    @Test
    public void testSetTileDaos() {
        mockTileDaoProvider.setGeoPackages(mockLayers, "mockGPPath");
        List<String> mockTileTables = new ArrayList<>();
        mockTileTables.add("mockTileTable");
        Mockito.doReturn(mockTileTables).when(mockGeoPackage).getTileTables();
        mockTileDao = Mockito.mock(TileDao.class);
        Mockito.doReturn(mockTileDao).when(mockGeoPackage).getTileDao("mockTileTable");
        List<GeoPackage> mockGeoPackages = new ArrayList<>();
        mockGeoPackages.add(mockGeoPackage);
        mockTileDaoProvider.setTileDaos(mockGeoPackages);
        org.testng.Assert.assertEquals(mockTileDaoProvider.getTileDaos().get(0), mockTileDao);
        org.testng.Assert.assertTrue(mockTileDaoProvider.getTileDaos().size() == 1);
    }
}