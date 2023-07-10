package org.esa.snap.engine_utilities.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class TestTestUtils {

    @Test
    public void testTestDataRoot() throws Exception {
        assertNotNull(TestUtils.TESTDATA_ROOT);

        Path testDataRootPath = Paths.get(TestUtils.TESTDATA_ROOT);
        if(Files.exists(testDataRootPath)) {
            assertTrue(Files.isDirectory(testDataRootPath));
            assertTrue(Files.isReadable(testDataRootPath));
        } else {
            System.out.println("Warning: Test data root does not exist: " + testDataRootPath);

            Path currentPath = Paths.get("");
            System.out.println("Current path is: " + currentPath.toAbsolutePath());
            File currentRelativePath = new File(currentPath.toAbsolutePath().toString()).getParentFile().getParentFile();
            System.out.println("Trying relative path: " + currentPath.toAbsolutePath());

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentRelativePath.toPath())) {
                for (Path path : stream) {
                    if (Files.isDirectory(path)) {
                        System.out.println("  " + path.getFileName());
                        try (DirectoryStream<Path> stream2 = Files.newDirectoryStream(path)) {
                            for (Path p : stream2) {
                                if (Files.isDirectory(p)) {
                                    System.out.println("     " + p.getFileName());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testCreateTestProduct() throws Exception {
        final Product product = TestUtils.createProduct("test", 100, 100);
        assertNotNull(product);
        assertNotNull(product.getDescription());
        assertNotNull(product.getSceneGeoCoding());
        assertNotNull(product.getStartTime());
        assertNotNull(product.getEndTime());
        assertNotNull(product.getMetadataRoot());
        assertEquals("test", product.getProductType());
        assertEquals(100, product.getSceneRasterWidth());
        assertEquals(100, product.getSceneRasterHeight());

        final Band srcBand1 = TestUtils.createBand(product, "band1", 100, 100);
        assertTrue(product.containsBand("band1"));
        assertNotNull(srcBand1.getUnit());
        assertEquals(100, srcBand1.getRasterWidth());
        assertEquals(100, srcBand1.getRasterHeight());

        final Band srcBand2 = TestUtils.createBand(product, "band2", ProductData.TYPE_INT32, "unit", 100, 100, false);

        TestUtils.verifyProduct(product, true, true, true);

        double val1 = srcBand1.getSampleFloat(10, 10);
        assertEquals(1011.0, val1, 0);

        double val2 = srcBand2.getSampleFloat(10, 10);
        assertEquals(8990.0, val2, 0);
    }

    @Test
    public void testCreateMultiSizeTestProduct() throws Exception {
        final Product product = TestUtils.createProduct("test", 100, 100);
        final Band srcBand1 = TestUtils.createBand(product, "band1", 100, 100);
        final Band srcBand2 = TestUtils.createBand(product, "band2", 150, 150);

        assertTrue(product.containsBand("band1"));
        assertTrue(product.containsBand("band2"));

        assertEquals(100, product.getSceneRasterWidth());
        assertEquals(100, product.getSceneRasterHeight());

        assertEquals(100, srcBand1.getRasterWidth());
        assertEquals(100, srcBand1.getRasterHeight());

        assertEquals(150, srcBand2.getRasterWidth());
        assertEquals(150, srcBand2.getRasterHeight());

        TestUtils.verifyProduct(product, true, true, true);

        double val = srcBand1.getSampleFloat(10, 10);
        assertEquals(1011.0, val, 0);
    }

    @Test
    public void testFloatBands() throws Exception {
        final Product product = TestUtils.createProduct("test", 100, 100);
        final Band srcBand1 = TestUtils.createBand(product, "band1", ProductData.TYPE_FLOAT32, "unit", 100, 100, true);
        final Band srcBand2 = TestUtils.createBand(product, "band2", ProductData.TYPE_FLOAT32, "unit", 100, 100, false);
        assertEquals("unit", srcBand1.getUnit());
        assertTrue(product.containsBand("band1"));
        assertTrue(product.containsBand("band2"));

        TestUtils.verifyProduct(product, true, true, true);

        double val1 = srcBand1.getSampleFloat(10, 10);
        assertEquals(1011.5, val1, 0);

        double val2 = srcBand2.getSampleFloat(10, 10);
        assertEquals(8990.5, val2, 0);
    }
}
