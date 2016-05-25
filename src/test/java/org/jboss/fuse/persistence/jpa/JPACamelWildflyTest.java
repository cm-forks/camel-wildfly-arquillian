package org.jboss.fuse.persistence.jpa;

import org.apache.camel.CamelContext;
import org.apache.camel.component.jpa.JpaComponent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.fuse.persistence.jpa.model.Account;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
//import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.camel.SpringCamelContextFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.net.URL;

@RunWith(Arquillian.class)
@CamelAware
public class JPACamelWildflyTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    protected static final String[] DEPENDENCIES = {
            "org.wildfly.camel:wildfly-camel-subsystem:3.3.0","org.wildfly:wildfly-arquillian-protocol-jmx:1.0.1.Final",
            "org.apache.camel:camel-core:2.15.3"
            /*
                "org.apache.camel:camel-core:2.15.3",
                "org.apache.camel:camel-spring:2.15.3",
                "org.springframework:spring-beans:4.1.6.RELEASE",
                "org.springframework:spring-context:4.1.6.RELEASE",
            */
    };

    protected static JavaArchive thirdPartyLibs() {
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "libs.jar");
        for (String dependency : DEPENDENCIES) {
           // lib.merge(Maven.resolver().resolve(dependency).withoutTransitivity().asSingle(JavaArchive.class));
        }
        return lib;
    }

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jpa-test.jar");
        archive.addClass(Account.class);
        archive.addAsResource("org/jboss/fuse/persistence/jpa/persistence-local.xml", "META-INF/persistence.xml");
        archive.addAsResource("org/jboss/fuse/persistence/jpa/jpa-camel-context.xml", "META-INF/jboss-camel-context.xml");
        // archive.merge(thirdPartyLibs());
        return archive;
    }

    @Test
    public void testJpaCamelRoute() throws Exception {

        //URL resourceUrl = getClass().getResource("/META-INF/jboss-camel-context.xml");
        //CamelContext camelctx = SpringCamelContextFactory.createSingleCamelContext(resourceUrl, null);

        CamelContext camelctx = contextRegistry.getCamelContext("jpa-context");
        Assert.assertNotNull("Expected jpa-context to not be null", camelctx);

        // Persist a new account entity
        Account account = new Account(1, 500);
        camelctx.createProducerTemplate().sendBody("direct:start", account);

        JpaComponent component = camelctx.getComponent("jpa", JpaComponent.class);
        EntityManagerFactory entityManagerFactory = component.getEntityManagerFactory();

        // Read the saved entity back from the database
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        Account result = em.getReference(Account.class, 1);
        em.getTransaction().commit();

        Assert.assertEquals(account, result);
    }

}

