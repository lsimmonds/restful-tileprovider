package simple_service.restful_service.wms;

import org.locationtech.jts.util.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GetMapResponseTest {

    @Mock
    BufferedImage mockBufferedImage;

    @Mock
    GetMapResponse mockGetMapResponse;

    @BeforeMethod
    public void setUp() {
        mockBufferedImage = new BufferedImage(25, 25, BufferedImage.TYPE_INT_RGB);
        mockGetMapResponse = Mockito.spy(GetMapResponse.class);
        mockGetMapResponse.mapImage = mockBufferedImage;
//        Mockito.doReturn(mockGetMapResponse)
//                .when(mockGetMapResponse)
//                .makeGetMapResponse(Mockito.any(HttpStatus.class), Mockito.any(BufferedImage.class));
    }

    @Test
    public void testGetBody() throws IOException {
        //It writes the mapImage to a ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] imageInByte = null;
        try {
            ImageIO.write(mockGetMapResponse.mapImage, "jpg", baos);
            baos.flush();
            imageInByte = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //It returns an InputStream
        org.testng.Assert.assertEquals(mockGetMapResponse.getBody().readAllBytes(),(new ByteArrayInputStream(imageInByte)).readAllBytes());
    }

    @Test
    public void testGetHeaders() {
        //It returns a JPEG header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        org.testng.Assert.assertEquals(headers, mockGetMapResponse.getHeaders());
    }
}