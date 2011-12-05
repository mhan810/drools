/*
 * Copyright 2011 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.marshalling.util;

import static java.lang.System.out;
import static org.drools.marshalling.util.CompareViaReflectionUtil.*;
import static org.drools.marshalling.util.MarshallingDBUtil.initializeMarshalledDataEMF;
import static org.drools.marshalling.util.MarshallingTestUtil.*;
import static org.drools.persistence.util.PersistenceUtil.*;
import static org.drools.runtime.EnvironmentName.ENTITY_MANAGER_FACTORY;
import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.SessionConfiguration;
import org.drools.conf.EventProcessingOption;
import org.drools.impl.EnvironmentFactory;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.time.impl.TrackableTimeJobFactoryManager;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestMarshallingUtilsTest {

    private static boolean debug = false;

    @Test
    @Ignore
    public void testUnmarshallingMarshalledData() { 
        HashMap<String, Object> testContext = null;
        List<MarshalledData> marshalledDataList = null;
        try { 
            testContext = initializeMarshalledDataEMF(DROOLS_PERSISTENCE_UNIT_NAME, this.getClass(), true);
            EntityManagerFactory emf = (EntityManagerFactory) testContext.get(ENTITY_MANAGER_FACTORY);
            marshalledDataList = retrieveMarshallingData(emf);
        }
        finally { 
            tearDown(testContext);
        }

        for( MarshalledData marshalledData : marshalledDataList ) { 
            String className = marshalledData.marshalledObjectClassName.substring(marshalledData.marshalledObjectClassName.lastIndexOf('.')+1);
            try { 
                unmarshallObject(marshalledData);
                out.println("- " + className + ": " + marshalledData.getTestMethodAndSnapshotNum() );
            }
            catch( Exception e ) { 
                out.println("X " + className + ": " + marshalledData.getTestMethodAndSnapshotNum() );
            }
        } 
    }
    
    @Test
    @Ignore
    public void testUnmarshallingSpecificMarshalledData() { 
        String testMethodAndSnapNum 
            = "org.drools.persistence.session.RuleFlowGroupRollbackTest.testRuleFlowGroupRollback:1";
//            = "org.drools.timer.integrationtests.TimerAndCalendarTest.testTimerRuleAfterIntReloadSession:1";
        HashMap<String, Object> testContext
            = initializeMarshalledDataEMF(DROOLS_PERSISTENCE_UNIT_NAME, this.getClass(), true);
        EntityManagerFactory emf = (EntityManagerFactory) testContext.get(ENTITY_MANAGER_FACTORY);
        List<MarshalledData> marshalledDataList = retrieveMarshallingData(emf);
        MarshalledData marshalledData = null;
        for( MarshalledData marshalledDataElement : marshalledDataList ) { 
           if( testMethodAndSnapNum.equals(marshalledDataElement.getTestMethodAndSnapshotNum()) ) { 
               marshalledData = marshalledDataElement;
           }
        }
    
        try { 
            Object unmarshalledObject = unmarshallObject(marshalledData);
            assertNotNull(unmarshalledObject);
        } 
        catch( Exception e ) { 
            e.printStackTrace();
            fail( "[" + e.getClass().getSimpleName() + "]: " + e.getMessage() );
        }
        finally { 
            tearDown(testContext);
        }
    }

    @Test
    public void testCompareArrays() { 
       
        int [] testA = { 1, 3 };
        int [] testB = { 1, 3 };
        
        boolean same = compareInstances(testA, testA);
        assertTrue(same);
         printResult(same, testA, testB);
        
        // setup for test
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        KnowledgeBase [] testArrA = { kbase };
        KnowledgeBase [] testArrB = { kbase, null };
        
        same = compareInstances(testArrA, testArrB);
        assertTrue(! same);
         printResult(same, testArrA, testArrB);
       
        Environment [] testEnvA = { EnvironmentFactory.newEnvironment(), EnvironmentFactory.newEnvironment() };
        Environment [] testEnvB = { EnvironmentFactory.newEnvironment(), EnvironmentFactory.newEnvironment() };
       
        testEnvA[0].set(DROOLS_PERSISTENCE_UNIT_NAME, DROOLS_PERSISTENCE_UNIT_NAME);
        
        same = compareInstances(testEnvA, testEnvB);
        assertTrue(! same);
         printResult(same, testEnvA, testEnvB);
    }
    
    private static void printResult(boolean same, Object objA, Object objB) { 
        if( ! debug ) { return; }
        
        out.println( "Same: " + same );
        String outLine =  "a: {";
        for( int i = 0; i < Array.getLength(objA); ++i ) { 
            outLine += Array.get(objA, i) + ",";
        }
        outLine = outLine.substring(0, outLine.lastIndexOf(",")) + "}";
        out.println(outLine);
        outLine = "b: {";
        for( int i = 0; i < Array.getLength(objB); ++i ) { 
            outLine += Array.get(objB, i) + ",";
        }
        outLine = outLine.substring(0, outLine.lastIndexOf(",")) + "}";
        out.println(outLine); 
    }
    
    @Test
    public void testCompareInstances() throws Exception { 

        StatefulKnowledgeSession ksessionA = null;
        {
            KnowledgeBaseConfiguration config = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
            config.setOption( EventProcessingOption.STREAM );
            KnowledgeBase knowledgeBaseA = KnowledgeBaseFactory.newKnowledgeBase( config );
            KnowledgeSessionConfiguration ksconf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
            ksconf.setOption( ClockTypeOption.get( "pseudo" ) );
            ((SessionConfiguration) ksconf).setTimerJobFactoryManager( new TrackableTimeJobFactoryManager( ) );
            ksessionA = knowledgeBaseA.newStatefulKnowledgeSession(ksconf, null);
        }

        StatefulKnowledgeSession ksessionB = null;
        {
            KnowledgeBaseConfiguration config = KnowledgeBaseFactory.newKnowledgeBaseConfiguration(); 
            config.setOption( EventProcessingOption.STREAM );
            KnowledgeBase knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase( config );
            KnowledgeSessionConfiguration ksconf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
            ksconf.setOption( ClockTypeOption.get( "pseudo" ) );
            ((SessionConfiguration) ksconf).setTimerJobFactoryManager( new TrackableTimeJobFactoryManager( ) );
            ksessionB = knowledgeBase.newStatefulKnowledgeSession(ksconf, null);
        }

        Assert.assertTrue(CompareViaReflectionUtil.class.getSimpleName() + " is broken!", 
                compareInstances(ksessionA, ksessionB) );
    }
}