package org.apache.nifi.minifi.c2.client.impl.jersey;

import org.apache.nifi.minifi.c2.client.MiNiFiC2Client;
import org.apache.nifi.minifi.c2.client.MiNiFiC2ClientConfig;
import org.apache.nifi.minifi.c2.client.MiNiFiC2Exception;
import org.apache.nifi.minifi.c2.model.AgentClass;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class JerseyAgentClassClientIT {

    private MiNiFiC2Client c2Client;

    @Before
    public void setUp() throws Exception {
        final MiNiFiC2ClientConfig.Builder clientCfgBuilder = new MiNiFiC2ClientConfig.Builder();
        clientCfgBuilder.baseUrl("http://localhost:10080");

        MiNiFiC2ClientConfig clientConfig = clientCfgBuilder.build();
        c2Client = new JerseyMiNiFiC2Client.Builder().config(clientConfig).build();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void createAgentClass() throws MiNiFiC2Exception, IOException {

        AgentClass agentClass = new AgentClass();
        agentClass.setName("Secret Agent Class");
        agentClass.setDescription("This class is super duper secret.");

        AgentClass returnedAgentClass = c2Client.getAgentClassClient().createAgentClass(agentClass);
        Assert.assertNotNull(returnedAgentClass);
    }

    @Test
    public void getAgentClasses() throws MiNiFiC2Exception, IOException {
        List<AgentClass> agentClasses = c2Client.getAgentClassClient().getAgentClasses();
        Assert.assertTrue(agentClasses.size() > 0);
    }

    @Test
    public void getAgentClass() throws MiNiFiC2Exception, IOException {
        AgentClass retrievedClass = c2Client.getAgentClassClient().getAgentClass("Secret Agent Class");
        Assert.assertNotNull(retrievedClass);
        Assert.assertEquals("Secret Agent Class", retrievedClass.getName());
    }

    @Test
    public void replaceAgentClass() throws MiNiFiC2Exception, IOException {

        final String updatedDescription = "This class is super duper secreter.";

        AgentClass agentClass = new AgentClass();
        agentClass.setName("Secret Agent Class");
        agentClass.setDescription(updatedDescription);

        final AgentClass updatedAgentClass = c2Client.getAgentClassClient().replaceAgentClass(agentClass.getName(), agentClass);

        Assert.assertEquals(updatedDescription, updatedAgentClass.getDescription());

    }

    @Test
    public void deleteAgentClassExistent() throws MiNiFiC2Exception, IOException {
        final AgentClass validClassDeletion = c2Client.getAgentClassClient().deleteAgentClass("Secret Agent Class");
        Assert.assertNotNull(validClassDeletion);
    }

    @Test(expected = MiNiFiC2Exception.class)
    public void deleteAgentClassNonexistent() throws MiNiFiC2Exception, IOException {
        final AgentClass nonExistentClassDeletion = c2Client.getAgentClassClient().deleteAgentClass("No Such Class");
        Assert.fail("Delete action should not have successfully occurred.");
    }
}