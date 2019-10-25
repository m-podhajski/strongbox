package org.carlspring.strongbox.controllers;

import org.carlspring.logging.exceptions.LoggingConfigurationException;
import org.carlspring.logging.services.LoggingManagementService;
import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.domain.DirectoryListing;
import org.carlspring.strongbox.services.DirectoryListingService;
import org.carlspring.strongbox.services.DirectoryListingServiceImpl;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import static org.carlspring.strongbox.controllers.LoggingManagementController.ROOT_CONTEXT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * This controllers provides a simple wrapper over REST API for the LoggingManagementService.
 *
 * @author Martin Todorov
 * @author Pablo Tirado
 * @author Aditya Srinivasan
 * @author Przemyslaw Fusik
 */
@Controller
@Api(value = ROOT_CONTEXT)
@RequestMapping(ROOT_CONTEXT)
public class LoggingManagementController
        extends BaseController
{

    public final static String ROOT_CONTEXT = "/api/monitoring/logs";

    @Inject
    private PropertiesBooter propertiesBooter;

    @Inject
    private LoggingManagementService loggingManagementService;

    private DirectoryListingService directoryListingService;

    public DirectoryListingService getDirectoryListingService()
    {
        return Optional.ofNullable(directoryListingService).orElseGet(() -> {
            String baseUrl = StringUtils.chomp(configurationManager.getConfiguration().getBaseUrl(), "/");

            return directoryListingService = new DirectoryListingServiceImpl(String.format("%s/api/logging", baseUrl));
        });
    }

    @ApiOperation(value = "Used to download log data.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The log file was retrieved successfully."),
                            @ApiResponse(code = 400, message = "Could not download log data.") })
    @PreAuthorize("hasAnyAuthority('CONFIGURATION_RETRIEVE_LOG','CONFIGURE_LOGS')")
    @GetMapping(value = "/log/{path:.+}", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity downloadLog(@PathVariable("path") String path,
                                      @RequestHeader(HttpHeaders.ACCEPT) String accept)
    {
        try
        {
            Path logsBaseDir = Paths.get(propertiesBooter.getLogsDirectory());
            Path requestedLogPath = Paths.get(logsBaseDir.toString(), path);

            logger.debug(String.format("Requested downloading log from path: [%s] resolved to [%s]",
                                       path,
                                       requestedLogPath));

            if (!Files.exists(requestedLogPath))
            {
                return getNotFoundResponseEntity("Requested path does not exist!", accept);
            }
            if (Files.isDirectory(requestedLogPath))
            {
                return getBadRequestResponseEntity("Requested path is a directory!", accept);
            }

            return getStreamToResponseEntity(loggingManagementService.downloadLog(path),
                                             FilenameUtils.getName(path));
        }
        catch (LoggingConfigurationException e)
        {
            String message = "Could not download log data.";

            return getExceptionResponseEntity(BAD_REQUEST, message, e, accept);
        }
    }

    @ApiOperation(value = "Used to get logs directory.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The logs directory was retrieved successfully."),
                            @ApiResponse(code = 500, message = "Server error.") })
    @PreAuthorize("hasAuthority('VIEW_LOGS')")
    @GetMapping(value = { "/browse/{path:.+}" },
                produces = { MediaType.TEXT_PLAIN_VALUE,
                             MediaType.TEXT_HTML_VALUE,
                             MediaType.APPLICATION_JSON_VALUE })
    public Object browseLogsDirectory(@PathVariable("path") Optional<String> path,
                                      ModelMap model,
                                      HttpServletRequest request,
                                      @RequestHeader(value = HttpHeaders.ACCEPT,
                                                     required = false) String acceptHeader)
    {
        logger.debug("Requested directory listing of logs {}/logs/{}", ROOT_CONTEXT, path.orElse(""));

        try
        {
            Path logsBaseDir = Paths.get(propertiesBooter.getLogsDirectory());
            Path requestedLogPath = Paths.get(logsBaseDir.toString(), path.orElse(""));

            logger.debug("Requested directory listing of path: [{}] resolved to [{}]", path, requestedLogPath);

            if (!Files.exists(requestedLogPath))
            {
                return getNotFoundResponseEntity("Requested path does not exist!", acceptHeader);
            }
            if (!Files.isDirectory(requestedLogPath))
            {
                return getBadRequestResponseEntity("Requested path is not a directory!", acceptHeader);
            }

            DirectoryListing directoryListing = getDirectoryListingService().fromPath(logsBaseDir, requestedLogPath);

            if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE))
            {
                return ResponseEntity.ok(objectMapper.writer().writeValueAsString(directoryListing));
            }

            String currentUrl = StringUtils.chomp(request.getRequestURI(), "/");
            String downloadUrl = currentUrl.replaceFirst("/browse", "/log");

            model.addAttribute("showBack", false);
            model.addAttribute("currentUrl", currentUrl);
            model.addAttribute("downloadBaseUrl", downloadUrl);
            model.addAttribute("directories", directoryListing.getDirectories());
            model.addAttribute("files", directoryListing.getFiles());

            return new ModelAndView("directoryListing", model);
        }
        catch (Exception e)
        {
            String message = "Attempt to browse logs failed. Check server logs for more information.";
            return getExceptionResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message, e, acceptHeader);
        }
    }

}
