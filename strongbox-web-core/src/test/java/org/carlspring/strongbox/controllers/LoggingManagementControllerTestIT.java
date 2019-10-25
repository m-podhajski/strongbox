package org.carlspring.strongbox.controllers;

import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Pablo Tirado
 * @author Aditya Srinivasan
 * @author Przemyslaw Fusik
 */
@IntegrationTest
public class LoggingManagementControllerTestIT
        extends RestAssuredBaseTest
{

    @Inject
    private PropertiesBooter propertiesBooter;

    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
        setContextBaseUrl(getContextBaseUrl() + LoggingManagementController.ROOT_CONTEXT);
    }

    @Test
    @WithMockUser(authorities = { "CONFIGURE_LOGS" })
    public void testDownloadLog()
            throws Exception
    {
        String testLogName = "strongbox-test.log";
        Files.createFile(Paths.get(propertiesBooter.getLogsDirectory(), testLogName))
             .toFile()
             .deleteOnExit();

        String url = getContextBaseUrl() + "/log/" + testLogName;

        mockMvc.header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
               .when()
               .get(url)
               .peek() // Use peek() to print the output
               .then()
               .statusCode(HttpStatus.OK.value()); // check http status code
    }

    @Test
    @WithMockUser(authorities = { "VIEW_LOGS" })
    public void testLogDirectoryForListOfLogFiles()
    {
        //Given
        //Create dummy test files here
        Path[] paths = createTestLogFilesAndDirectories(false);

        String[] tempLogFilesArray = new String[4];
        for (int i = 0; i < paths.length; i++)
        {
            tempLogFilesArray[i] = paths[i].getFileName()
                                           .toString();
        }

        String logDirectoryHomeUrl = getContextBaseUrl() + "/browse/";

        //When
        //Getting the table elements
        String tableElementsAsString = mockMvc.contentType(MediaType.TEXT_HTML_VALUE)
                                              .when()
                                              .get(logDirectoryHomeUrl)
                                              .body()
                                              .htmlPath()
                                              .getString("html.body.table");

        //Assertion Test to see if given file names are contained in the HTML body
        boolean shouldContainLogFilesInHtmlTableElement = false;
        if (tableElementsAsString.contains(tempLogFilesArray[0])
            && tableElementsAsString.contains(tempLogFilesArray[1])
            && tableElementsAsString.contains(tempLogFilesArray[2])
            && tableElementsAsString.contains(tempLogFilesArray[3]))
        {
            shouldContainLogFilesInHtmlTableElement = true;
        }

        try
        {
            assertThat(shouldContainLogFilesInHtmlTableElement).isTrue();
        }
        finally
        {
            //Delete the temporary log files even if the test fails
            deleteTestLogFilesAndDirectories(false);
        }
    }

    @Test
    @WithMockUser(authorities = { "VIEW_LOGS" })
    public void testAbilityToNavigateToSubLogDirectories()
    {
        //Given
        //Creating the test sub directory and dummy files here
        Path[] paths = createTestLogFilesAndDirectories(true);

        String[] tempLogFilesArray = new String[4];
        for (int i = 0; i < paths.length; i++)
        {
            tempLogFilesArray[i] = paths[i].getFileName()
                                           .toString();
        }

        String logSubDirectoryUrl = getContextBaseUrl() + "/browse/test/";

        //When
        //Getting the table elements
        String tableElementsAsString = mockMvc.contentType(MediaType.TEXT_HTML_VALUE)
                                              .when()
                                              .get(logSubDirectoryUrl)
                                              .body()
                                              .htmlPath()
                                              .getString("html.body");

        //Assertion Test to see if given file names and test folder are contained in the HTML body
        boolean shouldContainLogFilesInHtmlTableElement = false;
        if (tableElementsAsString.contains(tempLogFilesArray[0])
            && tableElementsAsString.contains(tempLogFilesArray[1])
            && tableElementsAsString.contains(tempLogFilesArray[2])
            && tableElementsAsString.contains(tempLogFilesArray[3])
            && tableElementsAsString.contains("test"))
        {
            shouldContainLogFilesInHtmlTableElement = true;
        }

        try
        {
            assertThat(shouldContainLogFilesInHtmlTableElement).isTrue();
        }
        finally
        {
            //Delete the test sub directory even if the test fails
            deleteTestLogFilesAndDirectories(true);
        }
    }

    //This method creates temporary log files, and if necessary for subdirectory browsing, a log subdirectory.
    private Path[] createTestLogFilesAndDirectories(boolean shouldICreateATestSubDirectory)
    {
        //If a test directory is needed, a new directory called `test` under `/logs/` will be created.
        //Otherwise the path of `/logs` will be returned.
        Path logDirectoryPath;
        Path[] paths = new Path[4];
        try
        {

            if (shouldICreateATestSubDirectory)
            {
                logDirectoryPath = Paths.get(propertiesBooter.getLogsDirectory(), "/test");
                Files.createDirectory(logDirectoryPath);
            }
            else
            {
                logDirectoryPath = Paths.get(propertiesBooter.getLogsDirectory());
            }

            //Create 4 temporary log files from 0 to 3.
            for (int i = 0; i < 4; i++)
            {
                paths[i] = Files.createTempFile(logDirectoryPath, "TestLogFile" + i, ".log");
            }
        }
        catch (IOException e)
        {
            fail("Unable to create test log files and/or directories!");
        }

        return paths;
    }

    //This method deletes temporary log files, and if used for subdirectory browsing, the test log subdirectory.
    private void deleteTestLogFilesAndDirectories(boolean wasATestSubDirectoryCreated)
    {

        //This local class extends the SimpleFileVisitor and overrides the `visitFile` method to delete any
        //Test Log Files upon encountering it.
        class LogFileVisitor
                extends SimpleFileVisitor<Path>
        {

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs)
                    throws IOException
            {
                //Possessive Regex for speed
                if (file.getFileName().toString().matches("TestLog.*+"))
                {
                    Files.delete(file);
                }

                return FileVisitResult.CONTINUE;
            }
        }

        try
        {
            if (wasATestSubDirectoryCreated)
            {
                Path pathToLogHomeDirectory = Paths.get(propertiesBooter.getLogsDirectory(), "/test");

                Files.walkFileTree(pathToLogHomeDirectory, new LogFileVisitor());

                Files.delete(Paths.get(propertiesBooter.getLogsDirectory(), "/test"));
            }
            else
            {
                Path pathToLogHomeDirectory = Paths.get(propertiesBooter.getLogsDirectory());

                Files.walkFileTree(pathToLogHomeDirectory, new LogFileVisitor());
            }
        }
        catch (IOException e)
        {
            fail("Unable to delete test log files and/or directories!");
        }
    }

}
