package com.yahoo.sshd.tools.artifactory;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestJFrogArtifactoryClientHelper {

    private static Server server;
    private static ServerConnector connector;
    private static int retryCountMetaData;
    private static int retryCountContent;
    private static String artifactoryUrl;
    private static ArtifactoryInformation artInfo;
    private static JFrogArtifactoryClientHelper client;

    @BeforeClass
    public static void startServer() throws Exception
    {
        server = new Server();
        connector = new ServerConnector(server);
        server.addConnector(connector);
        server.setHandler(new ArtifactoryHandler());
        server.start();
        artifactoryUrl = "http://localhost:" + connector.getLocalPort();
        artInfo = new ArtifactoryInformation(artifactoryUrl, "test", "test");
        client = new JFrogArtifactoryClientHelper(artInfo, "testrepo");
    }
    
    private static class ArtifactoryHandler extends AbstractHandler
    {
        @Override
        public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException
        {
            
            String statusCode = httpRequest.getParameter("status");
            if (path.contains("/api/storage")){
                if (Integer.valueOf(statusCode) == 500){
                    retryCountMetaData++;
                }
            }else if (path.contains("/testrepo/test.txt")){
                if (Integer.valueOf(statusCode) == 500){
                    retryCountContent++;
                }
            }
            StringBuilder storageJson = new StringBuilder();
            storageJson.append("{")
            .append("\"created\": \"2016-04-05\",")
            .append("\"size\": \"1024\"")
            .append("}");
            request.setHandled(true);
            httpResponse.setStatus(Integer.valueOf(statusCode));
            OutputStream out=httpResponse.getOutputStream();
            byte[] bytes=storageJson.toString().getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.flush();
            out.close();
        }
    }
    
    @DataProvider(name = "retryData")
    public static Object[][] getRetryData() {
        return new Object[][] { {200}, {404}, {500}};
    }


    @Test(description="retry logic on /api/storage which is for getting meta-data", dataProvider = "retryData")
    public void testgetArtifactRetryLogic(int statusCode) throws Exception{
        try {
            client.getArtifact("test.txt?status=" + statusCode);
        }catch (Exception e){
            validateException(retryCountMetaData, statusCode, e);
        }
    }

    @Test(description="retry logic on /testrepo/test.txt which is for getting an actual content", dataProvider = "retryData")
    public void testgetArtifactContentsRetryLogic(int statusCode) throws Exception{
        try {
            client.getArtifactContents("test.txt?status=" + statusCode);
        }catch (Exception e){
            validateException(retryCountContent, statusCode, e);
        }
    }

    private void validateException(int expectedRetryCount, int statusCode, Exception e){
        if (statusCode == 200){
            Assert.fail("Should have have caused an exception " + e);
        }else if (statusCode == 404){
            Assert.assertTrue(e instanceof ArtifactNotFoundException);
        }else{
            Assert.assertTrue(e instanceof IOException);
            Assert.assertEquals(retryCountContent, 5);
        }
    }
}
