package com.realdolmen.course.utilities.integration;

import com.realdolmen.course.utilities.persistence.DataSetPersistenceTest;
import com.realdolmen.course.utilities.persistence.PersistenceTest;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

import static org.junit.Assume.assumeTrue;

/**
 * Remote integration tests require a running server, so don't forget to fire up your Wildfly first!
 * This is not an ideal way of running unit tests, because the tests become more dependent on infrastructure, but it can
 * still be quite convenient.
 * Integration tests are disabled by default, unless a {@link #INTEGRATION_ENABLED_SYSTEM_PROPERTY system parameter} is
 * present.
 */
public abstract class RemoteIntegrationTest extends DataSetPersistenceTest {
    public static final String INTEGRATION_ENABLED_SYSTEM_PROPERTY = "integration";

    private static InitialContext context;

    @BeforeClass
    public static void initializeJndiContext() throws Exception {
        context = new InitialContext(jdniProperties());
    }

    /**
     * Integration tests invoke RMI calls on an actual running server. Since they assert database state, this means they need to run on the same datasource as the server does, which currently is mysql.
     */
    @Before
    public void verifyCorrectDatabaseEngine() {
        assumeTrue("Integration testing is disabled (enable using -Dintegration)", isPropertySet());
    }

    /**
     * Ensure the current database engine is selected for running the integration tests.
     * Since integration tests run on a running server, the unit tests should also use that server's database schema
     * to perform assertions on. This would likely need to be an acceptance environment of some sort.
     */
    @Override
    protected DatabaseEngine selectDatabaseEngine() {
        DatabaseEngine defaultEngine = super.selectDatabaseEngine();
        defaultEngine.assertEquals(DatabaseEngine.mysql, "Integration testing should be run on " + DatabaseEngine.mysql + " database engine (in current implementation) but selected engine is " + defaultEngine + ". See " + PersistenceTest.class + " for details.");
        return defaultEngine;
    }

    private static boolean isPropertySet() {
        return System.getProperty(INTEGRATION_ENABLED_SYSTEM_PROPERTY) != null;
    }

    private static Hashtable<String, Object> jdniProperties() {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        properties.put(Context.PROVIDER_URL, "http-remoting://127.0.0.1:8080");
        properties.put("jboss.naming.client.ejb.context", true);
        return properties;
    }

    protected <T> T lookup(String jndiString) throws NamingException {
        try {
            T resource = (T) context.lookup(jndiString);
            logger.debug("Lookup performed on JNDI string '" + jndiString + "' yields '" + resource + "'");
            return resource;
        } catch(CommunicationException e) {
            throw new RuntimeException("Unable to perform JNDI lookup. Did you start up your application server? Is it running the latest update (redeploy if necessary)?", e);
        }
    }
}
