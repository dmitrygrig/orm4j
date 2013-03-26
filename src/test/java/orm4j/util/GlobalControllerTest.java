/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j.util;

import java.text.Format;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author grig
 */
public class GlobalControllerTest {
    
    public GlobalControllerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getClassesForPackage method, of class GlobalController.
     */
    @Test
    public void testGetClassesForPackage() {
        System.out.println("getClassesForPackage");
        String packageName = "orm4j.util";
        List result = GlobalController.getClassesForPackage(packageName);
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }
}
